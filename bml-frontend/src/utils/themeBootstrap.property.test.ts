/**
 * `themeBootstrap` 属性测试 - Property 9: 首屏无 FOUC（P_NO_FOUC）。
 *
 * 验证目标（与 design.md / Correctness Properties / Property 9 对齐）：
 *   - 在 `<head>` 内联引导脚本同步执行完毕（也即任何渲染开始之前）的瞬间，
 *     `document.documentElement.style.getPropertyValue('--bml-color-primary')`
 *     SHALL 已被设置为非空且匹配 `/^#[0-9a-fA-F]{6}$/` 的合法 HEX 颜色；
 *   - `localStorage` **缺失** 与 **损坏** 两种场景下，`--bml-color-primary` 必须
 *     回退到 `PRESET_BEST` 中对应作用域 + 解析后明暗模式的主色（jsdom 默认无
 *     `window.matchMedia` 实现，引导脚本在 `try/catch` 中静默失败，
 *     `prefersDark` 退化为 `false`，因此期望分支恒为 `best-light-{scope}`）；
 *   - `localStorage` **命中** 场景下，无论 `mode` 取 LIGHT / DARK / AUTO，
 *     只要 `profile.primaryColor` 是合法 HEX，引导脚本 SHALL 用其覆盖主色，
 *     即在 `documentElement.style` 上读到的 `--bml-color-primary` 与
 *     `profile.primaryColor` 逐字符相等（大小写不敏感）；
 *   - scope 推断逻辑遵循引导脚本：`location.pathname` 以 `/business` 开头时
 *     scope = 'business'，其余路径（`/admin`、`/dashboard`、`/system/users`...）
 *     均视为 'admin'。
 *
 * 测试设计：
 *   1. 使用 `@vitest-environment jsdom` 提供完整 DOM / localStorage 桩；
 *   2. 通过 `Object.defineProperty(window, 'location', { value: { pathname } })`
 *      重写 `window.location`，避免修改真实 jsdom Location 对象导致的非法
 *      URL 错误，且 `configurable: true` 允许在每次属性迭代中重新设置；
 *   3. 直接将 {@link PRESET_BEST_TOKENS} 赋给 `window.__BML_PRESET_BEST__`，
 *      与 Vite 插件 `vite-plugin-theme-bootstrap` 在 `<head>` 注入的语义一致；
 *   4. 通过 `new Function(BOOTSTRAP_SCRIPT_SOURCE)()` 在测试上下文中同步
 *      执行 IIFE，等价于浏览器内联 `<script>` 标签的执行时机；
 *   5. 每次属性迭代之前彻底清理 `documentElement.style` / `body` 属性 /
 *      `localStorage` / 全局 preset，以杜绝跨迭代污染。
 *
 * 关联需求：Requirements 6.3, 6.5, 13.2, 10.2。
 *
 * **Validates: Requirements 6.3, 6.5**
 *
 * @vitest-environment jsdom
 */

// @vitest-environment jsdom

import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import fc from 'fast-check';

import {
    BOOTSTRAP_SCRIPT_SOURCE,
    PRESET_BEST_TOKENS,
    THEME_LOCALSTORAGE_KEY_PREFIX,
    THEME_PRESET_GLOBAL_KEY,
    type PresetBestVariantKey,
} from './themeBootstrap';

/* ──────────────────────────────────────────────────────
   一、常量与生成器
   ────────────────────────────────────────────────────── */

/** 合法 6 位 HEX 颜色字符串正则（与引导脚本 IIFE 内置正则一致）。 */
const HEX6_RE = /^#[0-9a-fA-F]{6}$/;

/**
 * 引导脚本所识别的 `Theme_Profile.mode` 三种合法值。
 *
 * 与 `BootstrapProfile.mode` 类型完全一致。
 */
type BootstrapMode = 'LIGHT' | 'DARK' | 'AUTO';

/**
 * 生成形如 `#rrggbb` 的合法 HEX 颜色（统一小写，便于稳定比较）。
 */
const hexColorArb: fc.Arbitrary<string> = fc
    .integer({ min: 0, max: 0xffffff })
    .map((n) => `#${n.toString(16).padStart(6, '0')}`);

/**
 * `Theme_Mode` 三种合法值生成器。
 */
const modeArb: fc.Arbitrary<BootstrapMode> = fc.constantFrom<BootstrapMode>(
    'LIGHT',
    'DARK',
    'AUTO',
);

/**
 * 应被解析为 `admin` scope 的代表性 pathname；与引导脚本
 * `pathname.indexOf("/business") === 0` 判定一致——任何不以 `/business` 开头的
 * pathname 一律落到 admin 分支。
 */
const adminPathArb = fc.constantFrom(
    '/admin',
    '/dashboard',
    '/profile',
    '/system/users',
    '/',
);

/**
 * 应被解析为 `business` scope 的代表性 pathname。
 */
const businessPathArb = fc.constantFrom(
    '/business',
    '/business/users',
    '/business/dashboard',
);

/** admin / business 两类 pathname 的并集生成器。 */
const allPathArb = fc.oneof(adminPathArb, businessPathArb);

/**
 * `localStorage` 损坏值生成器。
 *
 * 覆盖以下损坏类别（任何一种引导脚本都应静默回退到 PRESET_BEST）：
 *   - 不可解析的 JSON 字符串（残缺括号 / 非法 token / 纯文本）；
 *   - 合法 JSON 但非对象（`null` / 数组 / 字符串 / 数字 / 布尔）；
 *   - 合法 JSON 对象但缺少颜色字段或字段类型不匹配。
 */
const corruptRawArb: fc.Arbitrary<string> = fc.oneof(
    // —— 残缺 JSON：解析必抛异常 ——
    fc.constant('{'),
    fc.constant('}'),
    fc.constant('{"primaryColor":}'),
    fc.constant('not json at all'),
    fc.constant('{"a":'),
    // —— 合法 JSON 但非有效对象 —— 引导脚本对 `null` / 数组 / 标量进行兜底
    fc.constant('null'),
    fc.constant('true'),
    fc.constant('123'),
    fc.constant('"a string"'),
    fc.constant('[]'),
    // —— 合法 JSON 对象但缺少颜色字段或字段非法 ——
    fc.constant('{}'),
    fc.constant('{"foo":"bar"}'),
    fc.constant('{"primaryColor":"not-a-color"}'),
    fc.constant('{"primaryColor":42}'),
    fc.constant('{"mode":"INVALID"}'),
);

/* ──────────────────────────────────────────────────────
   二、辅助函数
   ────────────────────────────────────────────────────── */

/**
 * 推断 pathname 应映射到的引导脚本 scope。
 *
 * 实现严格遵循 IIFE 内部 `pathname.indexOf("/business") === 0 ? "business" : "admin"`。
 *
 * @param pathname 待推断的 URL pathname。
 * @returns `'admin'` 或 `'business'`。
 */
function pathnameToScope(pathname: string): 'admin' | 'business' {
    return pathname.indexOf('/business') === 0 ? 'business' : 'admin';
}

/**
 * 重写 `window.location` 以模拟不同首屏 URL。
 *
 * 直接修改 jsdom 真实 `Location` 对象的 pathname 会触发非法 URL 校验；
 * 此处用 `Object.defineProperty` 完整替换为最小桩对象，仅保留引导脚本
 * 所需的 `pathname` 字段，并设置 `configurable: true` 允许在循环迭代中
 * 重复替换。
 */
function applyPathname(pathname: string): void {
    Object.defineProperty(window, 'location', {
        value: { pathname },
        writable: true,
        configurable: true,
    });
}

/**
 * 重置测试夹具到“首屏未执行引导脚本”状态。
 *
 * 清理范围：
 *   - `documentElement.style` 全部内联属性（含上一次迭代写入的 `--bml-*`）；
 *   - `body` 上的 `arco-theme` / `data-bml-scope` 属性；
 *   - `window.localStorage` 全部键值；
 *   - `window.__BML_PRESET_BEST__` 全局变量。
 */
function resetBootstrapEnvironment(): void {
    const root = document.documentElement;
    root.removeAttribute('style');
    if (document.body) {
        document.body.removeAttribute('arco-theme');
        document.body.removeAttribute('data-bml-scope');
    }
    try {
        window.localStorage.clear();
    } catch (_) {
        // ignore
    }
    delete (window as unknown as Record<string, unknown>)[THEME_PRESET_GLOBAL_KEY];
}

/**
 * 同步执行引导脚本 IIFE，模拟浏览器在 `<head>` 内联 `<script>` 的执行时机。
 *
 * 使用 `new Function` 而非 `eval` 以避免污染当前作用域的局部变量。
 */
function runBootstrap(): void {
    // eslint-disable-next-line @typescript-eslint/no-implied-eval, no-new-func
    new Function(BOOTSTRAP_SCRIPT_SOURCE)();
}

/**
 * 读取经引导脚本设置后的 `--bml-color-primary` 当前值。
 *
 * 引导脚本通过 `documentElement.style.setProperty` 写入，因此直接读取
 * `documentElement.style.getPropertyValue` 即可拿到“synchronously before render”
 * 的值（CSS 自定义属性默认继承，`getComputedStyle(body)` 在真实浏览器中
 * 也能读到，但 jsdom 的样式计算并非全量实现，直接读 inline style 更稳）。
 */
function readPrimaryColor(): string {
    return document.documentElement.style.getPropertyValue('--bml-color-primary');
}

/**
 * 计算 PRESET_BEST 在缺失 / 损坏场景下应回退的主色。
 *
 * jsdom 默认未实现 `window.matchMedia`，IIFE 中 `prefersDark` 进入 catch 分支
 * 后保持初始 `false`，因此明暗模式恒解析为 `'light'`，对应变体键
 * `best-light-{scope}`。
 */
function expectedFallbackPrimary(scope: 'admin' | 'business'): string {
    const variantKey: PresetBestVariantKey = `best-light-${scope}`;
    const value = PRESET_BEST_TOKENS[variantKey]['--bml-color-primary'];
    if (!value) {
        throw new Error(`PRESET_BEST_TOKENS[${variantKey}] 缺少 --bml-color-primary 字段`);
    }
    return value;
}

/* ──────────────────────────────────────────────────────
   三、属性测试 - Property 9: 首屏无 FOUC (P_NO_FOUC)
   ────────────────────────────────────────────────────── */

describe('themeBootstrap 属性测试 - Property 9: 首屏无 FOUC (P_NO_FOUC)', () => {
    beforeEach(() => {
        resetBootstrapEnvironment();
    });

    afterEach(() => {
        resetBootstrapEnvironment();
    });

    /**
     * 子属性 9.1：`localStorage` 缺失时，引导脚本同步写入 PRESET_BEST 对应
     * 作用域的主色，且值合法非空。
     *
     * 不变量：
     *   1. 同步执行 IIFE 之后 `--bml-color-primary` 已经存在（非空）；
     *   2. 该值匹配 `/^#[0-9a-fA-F]{6}$/`；
     *   3. 该值等于 PRESET_BEST `best-light-{scope}` 变体的主色（与 jsdom
     *      无 matchMedia → prefersDark=false 的退化逻辑一致）。
     */
    it('localStorage 缺失：--bml-color-primary 同步落地为合法 HEX 且等于 PRESET_BEST 对应主色', () => {
        fc.assert(
            fc.property(allPathArb, (pathname) => {
                resetBootstrapEnvironment();
                applyPathname(pathname);
                (window as unknown as Record<string, unknown>)[THEME_PRESET_GLOBAL_KEY] =
                    PRESET_BEST_TOKENS;

                runBootstrap();

                const primary = readPrimaryColor();
                const scope = pathnameToScope(pathname);
                const expected = expectedFallbackPrimary(scope);

                // 不变量 1 & 2：非空 + 合法 HEX
                expect(primary).not.toBe('');
                expect(primary).toMatch(HEX6_RE);

                // 不变量 3：等于 PRESET_BEST 对应主色（大小写不敏感比较）
                expect(primary.toLowerCase()).toBe(expected.toLowerCase());

                // 副断言：scope 推断同步反映到 `body[data-bml-scope]`，
                // 进一步保证 “首屏无 FOUC” 涉及的 body 属性也已就位。
                expect(document.body.getAttribute('data-bml-scope')).toBe(scope);
            }),
            { numRuns: 30 },
        );
    });

    /**
     * 子属性 9.2：`localStorage` 命中（含合法 `primaryColor`）时，引导脚本
     * 同步写入 `profile.primaryColor`，覆盖 PRESET_BEST 默认值。
     *
     * 不变量：
     *   1. `--bml-color-primary` 同步落地为非空合法 HEX；
     *   2. 该值等于 `profile.primaryColor`（大小写不敏感）；
     *   3. 上述行为对 mode ∈ {LIGHT, DARK, AUTO} 三种取值均成立。
     */
    it('localStorage 命中：--bml-color-primary 等于 profile.primaryColor（覆盖 PRESET_BEST）', () => {
        fc.assert(
            fc.property(
                allPathArb,
                modeArb,
                hexColorArb,
                (pathname, mode, primaryColor) => {
                    resetBootstrapEnvironment();
                    applyPathname(pathname);
                    (window as unknown as Record<string, unknown>)[THEME_PRESET_GLOBAL_KEY] =
                        PRESET_BEST_TOKENS;

                    const scope = pathnameToScope(pathname);
                    const profile = { mode, primaryColor };
                    window.localStorage.setItem(
                        `${THEME_LOCALSTORAGE_KEY_PREFIX}${scope}`,
                        JSON.stringify(profile),
                    );

                    runBootstrap();

                    const primary = readPrimaryColor();

                    // 不变量 1：合法 HEX 非空
                    expect(primary).not.toBe('');
                    expect(primary).toMatch(HEX6_RE);

                    // 不变量 2：精确等于 profile.primaryColor
                    expect(primary.toLowerCase()).toBe(primaryColor.toLowerCase());
                },
            ),
            { numRuns: 30 },
        );
    });

    /**
     * 子属性 9.3：`localStorage` 损坏时，引导脚本静默回退到 PRESET_BEST，
     * 不抛异常、不留下空白主色。
     *
     * 不变量：
     *   1. 整个 IIFE 同步执行不抛异常（由 `new Function` 包装内部 try/catch 守护）；
     *   2. 同步执行结束后 `--bml-color-primary` 仍为合法 HEX；
     *   3. 其值等于 PRESET_BEST `best-light-{scope}` 主色，与缺失场景一致。
     */
    it('localStorage 损坏：引导脚本静默回退，--bml-color-primary 仍等于 PRESET_BEST 对应主色', () => {
        fc.assert(
            fc.property(allPathArb, corruptRawArb, (pathname, corrupted) => {
                resetBootstrapEnvironment();
                applyPathname(pathname);
                (window as unknown as Record<string, unknown>)[THEME_PRESET_GLOBAL_KEY] =
                    PRESET_BEST_TOKENS;

                const scope = pathnameToScope(pathname);
                window.localStorage.setItem(
                    `${THEME_LOCALSTORAGE_KEY_PREFIX}${scope}`,
                    corrupted,
                );

                // 不变量 1：脚本同步执行不抛异常
                expect(() => runBootstrap()).not.toThrow();

                const primary = readPrimaryColor();
                const expected = expectedFallbackPrimary(scope);

                // 不变量 2：合法 HEX 非空
                expect(primary).not.toBe('');
                expect(primary).toMatch(HEX6_RE);

                // 不变量 3：回退到 PRESET_BEST 对应主色
                expect(primary.toLowerCase()).toBe(expected.toLowerCase());
            }),
            { numRuns: 60 },
        );
    });

    /**
     * 综合不变量：在 “缺失 / 命中 / 损坏” 三种场景任意切换的序列下，
     * 同一 jsdom 实例反复执行 IIFE 之后，`--bml-color-primary` 始终是合法
     * 非空 HEX。
     *
     * 这一属性进一步保证引导脚本在 SPA 路由切换或开发期 HMR 反复触发的
     * 场景下，依然不会出现 “刚清空 documentElement.style 之后未及时重写”
     * 的真空窗口（FOUC 的潜在源）。
     */
    it('缺失 / 命中 / 损坏 任意混合序列下 --bml-color-primary 恒为合法非空 HEX', () => {
        type ScenarioKind = 'missing' | 'hit' | 'corrupt';
        const scenarioArb = fc.oneof(
            fc.record({
                kind: fc.constant<ScenarioKind>('missing'),
                pathname: allPathArb,
            }),
            fc.record({
                kind: fc.constant<ScenarioKind>('hit'),
                pathname: allPathArb,
                mode: modeArb,
                primaryColor: hexColorArb,
            }),
            fc.record({
                kind: fc.constant<ScenarioKind>('corrupt'),
                pathname: allPathArb,
                corrupted: corruptRawArb,
            }),
        );

        fc.assert(
            fc.property(
                fc.array(scenarioArb, { minLength: 1, maxLength: 8 }),
                (sequence) => {
                    for (const step of sequence) {
                        resetBootstrapEnvironment();
                        applyPathname(step.pathname);
                        (window as unknown as Record<string, unknown>)[
                            THEME_PRESET_GLOBAL_KEY
                        ] = PRESET_BEST_TOKENS;

                        const scope = pathnameToScope(step.pathname);

                        if (step.kind === 'hit') {
                            const profile = {
                                mode: step.mode,
                                primaryColor: step.primaryColor,
                            };
                            window.localStorage.setItem(
                                `${THEME_LOCALSTORAGE_KEY_PREFIX}${scope}`,
                                JSON.stringify(profile),
                            );
                        } else if (step.kind === 'corrupt') {
                            window.localStorage.setItem(
                                `${THEME_LOCALSTORAGE_KEY_PREFIX}${scope}`,
                                step.corrupted,
                            );
                        }
                        // 'missing' 分支已由 resetBootstrapEnvironment() 清空 localStorage

                        runBootstrap();

                        const primary = readPrimaryColor();
                        expect(primary).not.toBe('');
                        expect(primary).toMatch(HEX6_RE);
                    }
                },
            ),
            { numRuns: 20 },
        );
    });
});
