/**
 * `themeEngine` 单元测试。
 *
 * 本文件以**示例驱动**的方式覆盖 {@link generatePalette} / {@link hexToRgb} /
 * {@link applyTokens} / {@link subscribeAutoMode} / {@link resolveMode} 等
 * 核心 API 的关键代码路径，与 `themeEngine.property.test.ts` 中针对 AUTO
 * 模式跟随的 fast-check 属性测试互补。
 *
 * 关注点：
 *   - **色阶生成**：长度恒为 10、第 6 位等于输入主色、浅档接近白色、深档比主色暗；
 *   - **HEX 解析**：`#RGB` 简写与 `#RRGGBB` 等价；非法输入回退为 `(0,0,0)`；
 *   - **Token 写入**：`<html>`（root）上的 `--bml-color-*` / `--bml-radius-md` /
 *     `--bml-spacing-md` / `--bml-font-size-base` 等 CSS 自定义属性；
 *   - **`<body>` 属性**：`arco-theme`、`data-bml-density`、`data-bml-radius`、
 *     `data-bml-sidebar`、`data-bml-header`、`data-bml-scope` 等；
 *   - **过渡 class**：`applyTokens` 调用瞬间 `body` 加上 `theme-transitioning`，
 *     约 300ms 后自动移除（使用 vi 的假定时器精确控制）；
 *   - **AUTO 订阅**：`subscribeAutoMode` 在 matchMedia 变化时触发回调；
 *     反订阅函数能正确移除监听器，并在多次调用时保持幂等。
 *
 * 关联需求：Requirements 1.1 / 4.2 / 4.3 / 11.4。
 *
 * @vitest-environment jsdom
 */

// @vitest-environment jsdom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { ThemeProfile } from '@/types/theme';
import {
    applyTokens,
    generatePalette,
    hexToRgb,
    resolveMode,
    subscribeAutoMode,
} from './themeEngine';

/* ──────────────────────────────────────────────────────
   一、构造完整 ThemeProfile 用于 applyTokens
   ────────────────────────────────────────────────────── */

/**
 * 与 `tokens.preset-best.scss` ADMIN/LIGHT 变体保持同步的基线 Profile，
 * 单测用例可通过解构 `{ ...BASE_PROFILE, mode: 'DARK' }` 形式快速派生变体。
 */
const BASE_PROFILE: ThemeProfile = {
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
    mode: 'LIGHT',
    radius: 'MEDIUM',
    density: 'DEFAULT',
    sidebarStyle: 'LIGHT',
    sidebarCollapsedStyle: 'LIGHT',
    headerStyle: 'LIGHT',
    fontScale: 'DEFAULT',
    presetRef: 'PRESET_BEST',
};

/* ──────────────────────────────────────────────────────
   二、可控 matchMedia 桩
   ────────────────────────────────────────────────────── */

/**
 * 桩 `MediaQueryList`：暴露 `__set(matches)` 方法以模拟系统
 * `prefers-color-scheme` 的 “浅色 ⇆ 深色” 切换。
 */
interface StubMediaQueryList extends MediaQueryList {
    /** 派发 `change` 事件并更新 `matches`。 */
    __set(matches: boolean): void;
    /** 当前注册的 change 监听器数量（用于断言反订阅行为）。 */
    readonly __listenerCount: number;
}

/**
 * 安装 `window.matchMedia` 桩。
 *
 * jsdom 默认不实现 `matchMedia`，因此需要手动注入；本桩同时支持
 * 现代 `addEventListener('change')` 与已废弃的 `addListener` API，
 * 与 `themeEngine` 的双路径兼容写法对齐。
 */
function installMatchMediaStub(initialDark: boolean): StubMediaQueryList {
    let matches = initialDark;
    const listeners = new Set<(event: MediaQueryListEvent) => void>();

    const mql = {
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
        get __listenerCount() {
            return listeners.size;
        },
    } as unknown as StubMediaQueryList;

    Object.defineProperty(window, 'matchMedia', {
        configurable: true,
        writable: true,
        value: (query: string) =>
            query === '(prefers-color-scheme: dark)'
                ? mql
                : ({
                    media: query,
                    matches: false,
                    onchange: null,
                    addEventListener() {
                        /* noop */
                    },
                    removeEventListener() {
                        /* noop */
                    },
                    addListener() {
                        /* noop */
                    },
                    removeListener() {
                        /* noop */
                    },
                    dispatchEvent() {
                        return true;
                    },
                } as unknown as MediaQueryList),
    });

    return mql;
}

/** 卸载 `matchMedia` 桩，恢复 jsdom 默认状态（即 `undefined`）。 */
function uninstallMatchMediaStub(): void {
    Object.defineProperty(window, 'matchMedia', {
        configurable: true,
        writable: true,
        value: undefined,
    });
}

/** 重置 `<body>` 上由 `applyTokens` 写入的所有主题相关属性 / class。 */
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

/* ══════════════════════════════════════════════════════
   测试用例
   ══════════════════════════════════════════════════════ */

describe('themeEngine - hexToRgb', () => {
    it('解析 #RRGGBB 为对应的 RGB 三元组', () => {
        expect(hexToRgb('#165DFF')).toEqual({ r: 0x16, g: 0x5d, b: 0xff });
        expect(hexToRgb('#000000')).toEqual({ r: 0, g: 0, b: 0 });
        expect(hexToRgb('#FFFFFF')).toEqual({ r: 255, g: 255, b: 255 });
    });

    it('忽略大小写并兼容 #RGB 简写形式', () => {
        // #abc 应被扩展为 #aabbcc
        expect(hexToRgb('#abc')).toEqual({ r: 0xaa, g: 0xbb, b: 0xcc });
        expect(hexToRgb('#FFF')).toEqual({ r: 255, g: 255, b: 255 });
        expect(hexToRgb('#165dff')).toEqual({ r: 0x16, g: 0x5d, b: 0xff });
    });

    it('对非法 HEX 输入回退至 (0, 0, 0)', () => {
        expect(hexToRgb('not-a-color')).toEqual({ r: 0, g: 0, b: 0 });
        expect(hexToRgb('#12')).toEqual({ r: 0, g: 0, b: 0 });
        expect(hexToRgb('#GGGGGG')).toEqual({ r: 0, g: 0, b: 0 });
        // 非字符串入参亦不应抛错（防御式实现）
        expect(hexToRgb(undefined as unknown as string)).toEqual({ r: 0, g: 0, b: 0 });
    });
});

describe('themeEngine - generatePalette', () => {
    it('生成长度恒为 10 的 HEX 数组', () => {
        const palette = generatePalette('#165DFF');
        expect(palette).toHaveLength(10);
        palette.forEach((hex) => {
            expect(hex).toMatch(/^#[0-9A-F]{6}$/);
        });
    });

    it('索引 5（第 6 位）等于输入主色（不区分大小写）', () => {
        expect(generatePalette('#165DFF')[5]).toBe('#165DFF');
        expect(generatePalette('#00B42A')[5]).toBe('#00B42A');
        expect(generatePalette('#f53f3f')[5]).toBe('#F53F3F');
    });

    it('浅色档（索引 0..4）按从浅到深递进，且最浅档接近白色', () => {
        const palette = generatePalette('#165DFF');
        // 最浅档与白色的距离应当较小（亮度足够高）
        const lightest = hexToRgb(palette[0]);
        expect(lightest.r).toBeGreaterThan(230);
        expect(lightest.g).toBeGreaterThan(230);
        expect(lightest.b).toBeGreaterThan(230);

        // 0..5 索引随主色比例递增，平均亮度应单调下降
        const avgBrightness = (hex: string): number => {
            const { r, g, b } = hexToRgb(hex);
            return r + g + b;
        };
        for (let i = 0; i < 5; i++) {
            expect(avgBrightness(palette[i])).toBeGreaterThan(avgBrightness(palette[i + 1]));
        }
    });

    it('深色档（索引 6..9）比主色更暗', () => {
        const palette = generatePalette('#165DFF');
        const baseBrightness = (() => {
            const { r, g, b } = hexToRgb(palette[5]);
            return r + g + b;
        })();
        for (let i = 6; i < 10; i++) {
            const { r, g, b } = hexToRgb(palette[i]);
            expect(r + g + b).toBeLessThan(baseBrightness);
        }
    });
});

describe('themeEngine - resolveMode', () => {
    afterEach(() => {
        uninstallMatchMediaStub();
    });

    it('LIGHT / DARK 直接返回自身', () => {
        expect(resolveMode('LIGHT')).toBe('LIGHT');
        expect(resolveMode('DARK')).toBe('DARK');
    });

    it('AUTO 根据 prefers-color-scheme 解析；matchMedia 不可用时回退到 LIGHT', () => {
        installMatchMediaStub(true);
        expect(resolveMode('AUTO')).toBe('DARK');

        installMatchMediaStub(false);
        expect(resolveMode('AUTO')).toBe('LIGHT');

        // 卸载后无 matchMedia 支持，应回退到 LIGHT 而非抛错
        uninstallMatchMediaStub();
        expect(resolveMode('AUTO')).toBe('LIGHT');
    });
});

describe('themeEngine - applyTokens', () => {
    beforeEach(() => {
        resetBody();
        // 清空根元素上的内联样式，避免用例之间相互影响
        document.documentElement.removeAttribute('style');
        // applyTokens 使用 setTimeout 移除过渡 class，统一启用假定时器
        vi.useFakeTimers();
        // 默认安装 LIGHT 系统模式的 matchMedia 桩；用例可按需重装
        installMatchMediaStub(false);
    });

    afterEach(() => {
        vi.useRealTimers();
        uninstallMatchMediaStub();
        resetBody();
        document.documentElement.removeAttribute('style');
    });

    it('在根元素上写入主色相关 Token（含 RGB 三元组）', () => {
        applyTokens(BASE_PROFILE, 'ADMIN');
        const root = document.documentElement;

        expect(root.style.getPropertyValue('--bml-color-primary')).toBe('#165DFF');
        // primaryColor=#165DFF -> rgb(22, 93, 255)
        expect(root.style.getPropertyValue('--bml-color-primary-rgb')).toBe('22,93,255');

        // 10 级色阶应被同时写入 --arcoblue-* 与 --bml-color-primary-*
        for (let n = 1; n <= 10; n++) {
            expect(root.style.getPropertyValue(`--arcoblue-${n}`)).toMatch(/^#[0-9A-F]{6}$/);
            expect(root.style.getPropertyValue(`--bml-color-primary-${n}`)).toMatch(
                /^#[0-9A-F]{6}$/,
            );
        }
        // 第 6 位（索引 5，色阶 6）应等于输入主色
        expect(root.style.getPropertyValue('--arcoblue-6')).toBe('#165DFF');
        expect(root.style.getPropertyValue('--bml-color-primary-6')).toBe('#165DFF');
    });

    it('写入语义色 / 维度 Token（半径、间距、字号）', () => {
        applyTokens(BASE_PROFILE, 'ADMIN');
        const root = document.documentElement;

        // 语义色
        expect(root.style.getPropertyValue('--bml-color-bg-1')).toBe('#FFFFFF');
        expect(root.style.getPropertyValue('--bml-color-text-1')).toBe('#1D2129');
        expect(root.style.getPropertyValue('--bml-color-border')).toBe('#E5E6EB');
        expect(root.style.getPropertyValue('--bml-color-success')).toBe('#00B42A');

        // MEDIUM 圆角档 -> 4 / 8 / 12px
        expect(root.style.getPropertyValue('--bml-radius-sm')).toBe('4px');
        expect(root.style.getPropertyValue('--bml-radius-md')).toBe('8px');
        expect(root.style.getPropertyValue('--bml-radius-lg')).toBe('12px');

        // DEFAULT 紧凑度 -> spacing 基线
        expect(root.style.getPropertyValue('--bml-spacing-md')).toBe('16px');

        // DEFAULT 字体档 -> 14px
        expect(root.style.getPropertyValue('--bml-font-size-base')).toBe('14px');
    });

    it('在 <body> 上写入主题属性集合（含 scope）', () => {
        applyTokens(
            { ...BASE_PROFILE, density: 'COMPACT', radius: 'SHARP', headerStyle: 'PRIMARY' },
            'BUSINESS',
        );

        const body = document.body;
        expect(body.getAttribute('arco-theme')).toBe('light');
        expect(body.getAttribute('data-bml-mode')).toBe('light');
        expect(body.getAttribute('data-bml-density')).toBe('compact');
        expect(body.getAttribute('data-bml-radius')).toBe('sharp');
        expect(body.getAttribute('data-bml-sidebar')).toBe('light');
        expect(body.getAttribute('data-bml-sidebar-collapsed')).toBe('light');
        expect(body.getAttribute('data-bml-header')).toBe('primary');
        expect(body.getAttribute('data-bml-scope')).toBe('business');
    });

    it('mode=DARK 时 body[arco-theme]=dark', () => {
        applyTokens({ ...BASE_PROFILE, mode: 'DARK' }, 'ADMIN');
        expect(document.body.getAttribute('arco-theme')).toBe('dark');
        expect(document.body.getAttribute('data-bml-mode')).toBe('dark');
    });

    it('mode=AUTO 跟随系统 prefers-color-scheme 解析', () => {
        // 系统当前 = DARK
        installMatchMediaStub(true);
        applyTokens({ ...BASE_PROFILE, mode: 'AUTO' }, 'ADMIN');
        expect(document.body.getAttribute('arco-theme')).toBe('dark');
        expect(document.body.getAttribute('data-bml-mode')).toBe('auto');

        // 系统切到 LIGHT，再次 apply
        installMatchMediaStub(false);
        applyTokens({ ...BASE_PROFILE, mode: 'AUTO' }, 'ADMIN');
        expect(document.body.getAttribute('arco-theme')).toBe('light');
    });

    it('调用瞬间为 <body> 加上 theme-transitioning，约 300ms 后自动移除', () => {
        applyTokens(BASE_PROFILE, 'ADMIN');
        expect(document.body.classList.contains('theme-transitioning')).toBe(true);

        // 推进至接近 300ms 但未到，class 仍然存在
        vi.advanceTimersByTime(299);
        expect(document.body.classList.contains('theme-transitioning')).toBe(true);

        // 推进至 300ms 后 class 应被移除
        vi.advanceTimersByTime(1);
        expect(document.body.classList.contains('theme-transitioning')).toBe(false);
    });

    it('连续多次 applyTokens 始终维持 theme-transitioning，最后一次结束才移除', () => {
        applyTokens(BASE_PROFILE, 'ADMIN');
        vi.advanceTimersByTime(200);
        // 第二次切换应当重置定时器，过渡 class 不会在前一次定时器到期时被错误清理
        applyTokens({ ...BASE_PROFILE, primaryColor: '#722ED1' }, 'ADMIN');
        expect(document.body.classList.contains('theme-transitioning')).toBe(true);

        // 距离第二次调用 200ms 时（合计 400ms），class 仍应保持
        vi.advanceTimersByTime(200);
        expect(document.body.classList.contains('theme-transitioning')).toBe(true);

        // 距离第二次调用 300ms 时（合计 500ms），class 才被移除
        vi.advanceTimersByTime(100);
        expect(document.body.classList.contains('theme-transitioning')).toBe(false);
    });

    it('支持自定义 root，将 Token 写入到指定容器而非 documentElement', () => {
        const container = document.createElement('div');
        document.body.appendChild(container);

        applyTokens(BASE_PROFILE, 'ADMIN', container);

        expect(container.style.getPropertyValue('--bml-color-primary')).toBe('#165DFF');
        // 默认 documentElement 不应被污染
        expect(document.documentElement.style.getPropertyValue('--bml-color-primary')).toBe('');

        document.body.removeChild(container);
    });

    it('对非法 Profile 输入静默跳过，不抛异常', () => {
        expect(() =>
            applyTokens(null as unknown as ThemeProfile, 'ADMIN'),
        ).not.toThrow();
        expect(() =>
            applyTokens(undefined as unknown as ThemeProfile, 'ADMIN'),
        ).not.toThrow();
    });
});

describe('themeEngine - subscribeAutoMode', () => {
    afterEach(() => {
        uninstallMatchMediaStub();
    });

    it('当 prefers-color-scheme 由浅色切换为深色时回调 DARK', () => {
        const mql = installMatchMediaStub(false);
        const received: Array<'LIGHT' | 'DARK'> = [];
        const unsubscribe = subscribeAutoMode((m) => received.push(m));

        mql.__set(true);
        expect(received).toEqual(['DARK']);

        unsubscribe();
    });

    it('当 prefers-color-scheme 由深色切换为浅色时回调 LIGHT', () => {
        const mql = installMatchMediaStub(true);
        const received: Array<'LIGHT' | 'DARK'> = [];
        const unsubscribe = subscribeAutoMode((m) => received.push(m));

        mql.__set(false);
        expect(received).toEqual(['LIGHT']);

        unsubscribe();
    });

    it('反订阅函数会移除监听器，且系统切换不再触发回调', () => {
        const mql = installMatchMediaStub(false);
        const received: Array<'LIGHT' | 'DARK'> = [];
        const unsubscribe = subscribeAutoMode((m) => received.push(m));

        // 此时桩内部应当持有 1 个监听器
        expect(mql.__listenerCount).toBe(1);

        unsubscribe();
        expect(mql.__listenerCount).toBe(0);

        mql.__set(true);
        expect(received).toEqual([]);
    });

    it('反订阅幂等：多次调用不抛错且不会多次移除监听器', () => {
        const mql = installMatchMediaStub(false);
        const unsubscribe = subscribeAutoMode(() => {
            /* noop */
        });

        unsubscribe();
        expect(() => unsubscribe()).not.toThrow();
        expect(mql.__listenerCount).toBe(0);
    });

    it('在不支持 matchMedia 的环境下返回 No-op 反订阅函数', () => {
        // 不安装桩，window.matchMedia 缺失
        uninstallMatchMediaStub();
        const unsubscribe = subscribeAutoMode(() => {
            /* noop */
        });
        expect(typeof unsubscribe).toBe('function');
        expect(() => unsubscribe()).not.toThrow();
    });

    it('对非函数回调抛出 TypeError，避免在订阅期内才暴露错误', () => {
        installMatchMediaStub(false);
        expect(() =>
            subscribeAutoMode(undefined as unknown as (m: 'LIGHT' | 'DARK') => void),
        ).toThrow(TypeError);
    });
});
