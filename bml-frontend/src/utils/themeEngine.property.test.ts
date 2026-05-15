/**
 * `themeEngine` 属性测试 - Property 8: AUTO 模式跟随（P_AUTO_FOLLOW）。
 *
 * 验证目标（与 design.md / Correctness Properties / Property 8 对齐）：
 *   - 当 `ThemeProfile.mode === 'AUTO'` 时，`<body arco-theme>` 必须随
 *     浏览器 `prefers-color-scheme` 媒体查询的 `matches` 状态同步切换；
 *   - 当 `mode === 'LIGHT'` 或 `'DARK'` 时，系统模式如何变化都不影响
 *     `<body arco-theme>` —— 即固定模式对系统切换无副作用；
 *   - {@link subscribeAutoMode} 在系统模式变化时回调对应的
 *     `'LIGHT'` / `'DARK'`（用于让 `useTheme` Composable 在 AUTO 模式下
 *     重新调用 `applyTokens`）。
 *
 * 由于 jsdom 默认不实现 `window.matchMedia`，本测试通过 {@link installMatchMediaStub}
 * 注入一个可控 `MediaQueryList` 桩实现，并使用 `__set(matches)` 模拟系统
 * 切换 “浅色 ⇆ 深色”。
 *
 * 关联需求：Requirements 4.2。
 *
 * **Validates: Requirements 4.2**
 *
 * @vitest-environment jsdom
 */

// @vitest-environment jsdom

import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import fc from 'fast-check';

import type { ThemeMode, ThemeProfile } from '@/types/theme';
import { applyTokens, subscribeAutoMode } from './themeEngine';

/* ──────────────────────────────────────────────────────
   一、构造测试用 ThemeProfile（PRESET_BEST 同款基线骨架）
   ────────────────────────────────────────────────────── */

/**
 * 与 `tokens.preset-best.scss` 中 ADMIN / LIGHT 变体保持一致的字段集合，
 * 仅 `mode` 在每个用例中由 fast-check 生成器决定。
 */
const BASE_PROFILE: Omit<ThemeProfile, 'mode'> = {
    primaryColor: '#165DFF',
    secondaryColor: '#722ED1',
    accentColor: '#F7BA1E',
    successColor: '#00B42A',
    warningColor: '#FF7D00',
    errorColor: '#F53F3F',
    infoColor: '#86909C',
    textColor: '#1D2129',
    backgroundColor: '#FFFFFF',
    borderColor: '#E5E6EB',
    radius: 'MEDIUM',
    density: 'DEFAULT',
    sidebarStyle: 'LIGHT',
    sidebarCollapsedStyle: 'LIGHT',
    headerStyle: 'LIGHT',
    fontScale: 'DEFAULT',
    presetRef: 'PRESET_BEST',
};

/** 使用给定模式构造一份完整 Profile。 */
function buildProfile(mode: ThemeMode): ThemeProfile {
    return { ...BASE_PROFILE, mode };
}

/* ──────────────────────────────────────────────────────
   二、可控 matchMedia 桩
   ────────────────────────────────────────────────────── */

/** 桩 `MediaQueryList`：通过 `__set(matches)` 切换并触发监听器。 */
interface StubMediaQueryList extends MediaQueryList {
    /** 设置 `matches` 并向所有 `change` 监听器派发事件。 */
    __set(matches: boolean): void;
}

/**
 * 安装 `window.matchMedia` 桩。
 *
 * jsdom 默认不实现 `matchMedia`，本函数为 `(prefers-color-scheme: dark)`
 * 查询返回一个可控 `MediaQueryList`，并将同一份引用对所有匹配同样
 * `media` 字符串的查询请求复用，确保 {@link subscribeAutoMode} 注册的
 * 监听器与本测试用例中的 `__set` 操作位于同一对象上。
 */
function installMatchMediaStub(initialDark: boolean): StubMediaQueryList {
    let matches = initialDark;
    const listeners = new Set<(event: MediaQueryListEvent) => void>();

    const mql: StubMediaQueryList = {
        media: '(prefers-color-scheme: dark)',
        get matches() {
            return matches;
        },
        onchange: null,
        addEventListener(type: string, cb: EventListenerOrEventListenerObject) {
            if (type === 'change' && typeof cb === 'function') {
                listeners.add(cb as (event: MediaQueryListEvent) => void);
            }
        },
        removeEventListener(type: string, cb: EventListenerOrEventListenerObject) {
            if (type === 'change' && typeof cb === 'function') {
                listeners.delete(cb as (event: MediaQueryListEvent) => void);
            }
        },
        // 兼容 Safari < 14 等旧 API（themeEngine 同时支持新旧路径）
        addListener(cb: (event: MediaQueryListEvent) => void) {
            listeners.add(cb);
        },
        removeListener(cb: (event: MediaQueryListEvent) => void) {
            listeners.delete(cb);
        },
        dispatchEvent() {
            return true;
        },
        __set(next: boolean) {
            matches = next;
            const event = {
                matches: next,
                media: '(prefers-color-scheme: dark)',
            } as MediaQueryListEvent;
            listeners.forEach((l) => l(event));
        },
    } as unknown as StubMediaQueryList;

    Object.defineProperty(window, 'matchMedia', {
        configurable: true,
        writable: true,
        value: (query: string) =>
            query === '(prefers-color-scheme: dark)' ? mql : ({
                media: query,
                matches: false,
                onchange: null,
                addEventListener() { /* noop */ },
                removeEventListener() { /* noop */ },
                addListener() { /* noop */ },
                removeListener() { /* noop */ },
                dispatchEvent() { return true; },
            } as unknown as MediaQueryList),
    });

    return mql;
}

/** 移除 `window.matchMedia` 桩，恢复 jsdom 默认状态。 */
function uninstallMatchMediaStub(): void {
    Object.defineProperty(window, 'matchMedia', {
        configurable: true,
        writable: true,
        value: undefined,
    });
}

/** 重置 `<body>` 上由 `applyTokens` 写入的所有主题相关属性。 */
function resetBody(): void {
    const body = document.body;
    body.removeAttribute('arco-theme');
    body.removeAttribute('data-bml-mode');
    body.removeAttribute('data-bml-density');
    body.removeAttribute('data-bml-radius');
    body.removeAttribute('data-bml-sidebar');
    body.removeAttribute('data-bml-sidebar-collapsed');
    body.removeAttribute('data-bml-header');
    body.removeAttribute('data-bml-scope');
    body.classList.remove('theme-transitioning');
}

/* ──────────────────────────────────────────────────────
   三、属性测试
   ────────────────────────────────────────────────────── */

describe('themeEngine 属性测试 - Property 8: AUTO 模式跟随 (P_AUTO_FOLLOW)', () => {
    beforeEach(() => {
        resetBody();
    });

    afterEach(() => {
        uninstallMatchMediaStub();
        resetBody();
    });

    /**
     * 主属性：mode=AUTO 时跟随系统，mode=LIGHT|DARK 时与系统状态完全无关。
     *
     * 在每次迭代中：
     *   1. 构造一个初始 `prefers-color-scheme: dark` 状态；
     *   2. `applyTokens(profile)` 后断言 `<body arco-theme>` 等于期望值
     *      （AUTO 跟随系统 / LIGHT|DARK 等于自身）；
     *   3. 切换系统状态到 `nextDark`，再次 `applyTokens` 后断言：
     *        - AUTO 时 `<body arco-theme>` 必须随 `nextDark` 改变；
     *        - LIGHT|DARK 时 `<body arco-theme>` 始终保持模式自身值
     *          （证明 “系统切换不产生副作用”）。
     */
    it('mode=AUTO 时 <body arco-theme> 与 prefers-color-scheme 同步；其它模式下不受系统切换影响', () => {
        fc.assert(
            fc.property(
                fc.constantFrom<ThemeMode>('LIGHT', 'DARK', 'AUTO'),
                fc.boolean(),
                fc.boolean(),
                (mode, initialDark, nextDark) => {
                    resetBody();
                    installMatchMediaStub(initialDark);

                    const expectedAttr = (m: ThemeMode, dark: boolean): 'light' | 'dark' => {
                        if (m === 'LIGHT') return 'light';
                        if (m === 'DARK') return 'dark';
                        return dark ? 'dark' : 'light';
                    };

                    /* —— 初始状态下应用 Profile —— */
                    applyTokens(buildProfile(mode), 'ADMIN');
                    expect(document.body.getAttribute('arco-theme')).toBe(
                        expectedAttr(mode, initialDark),
                    );

                    /* —— 切换系统模式后再次应用 —— */
                    const mql = window.matchMedia(
                        '(prefers-color-scheme: dark)',
                    ) as StubMediaQueryList;
                    mql.__set(nextDark);
                    applyTokens(buildProfile(mode), 'ADMIN');
                    expect(document.body.getAttribute('arco-theme')).toBe(
                        expectedAttr(mode, nextDark),
                    );

                    /* —— 不变量：非 AUTO 模式下，body[arco-theme] 与系统状态正交 —— */
                    if (mode !== 'AUTO') {
                        expect(document.body.getAttribute('arco-theme')).toBe(
                            mode.toLowerCase(),
                        );
                    }

                    uninstallMatchMediaStub();
                },
            ),
            { numRuns: 64 },
        );
    });

    /**
     * 辅助属性：subscribeAutoMode 在系统模式变化时回调正确的解析模式。
     *
     * 这是 useTheme/themeStore 在 AUTO 模式下让 `<body arco-theme>` 跟随系统
     * 切换的底层机制：每次系统状态变化，回调会接收 `'LIGHT'` 或 `'DARK'`，
     * 调用方据此重新调用 `applyTokens`。
     */
    it('subscribeAutoMode 在系统模式变化时回调对应的 LIGHT / DARK', () => {
        fc.assert(
            fc.property(fc.boolean(), fc.boolean(), (initialDark, nextDark) => {
                const mql = installMatchMediaStub(initialDark);
                const received: Array<'LIGHT' | 'DARK'> = [];
                const unsubscribe = subscribeAutoMode((m) => {
                    received.push(m);
                });
                try {
                    mql.__set(nextDark);
                    expect(received).toEqual([nextDark ? 'DARK' : 'LIGHT']);
                } finally {
                    unsubscribe();
                    uninstallMatchMediaStub();
                }
            }),
            { numRuns: 50 },
        );
    });

    /**
     * 反订阅幂等性：取消订阅后，系统模式变化不应再触发回调。
     *
     * 同时保证多次调用 `unsubscribe()` 不抛错，符合 themeEngine 的 “幂等可
     * 多次调用” 注释。
     */
    it('subscribeAutoMode 反订阅后系统切换不再触发回调，且反订阅幂等', () => {
        fc.assert(
            fc.property(fc.boolean(), fc.boolean(), (initialDark, nextDark) => {
                const mql = installMatchMediaStub(initialDark);
                const received: Array<'LIGHT' | 'DARK'> = [];
                const unsubscribe = subscribeAutoMode((m) => {
                    received.push(m);
                });

                unsubscribe();
                // 多次调用应保持幂等
                expect(() => unsubscribe()).not.toThrow();

                mql.__set(nextDark);
                expect(received).toEqual([]);

                uninstallMatchMediaStub();
            }),
            { numRuns: 30 },
        );
    });
});
