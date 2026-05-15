/**
 * `useTheme` 属性测试 - Property 3: 持久化三方一致（P_DUAL_WRITE）。
 *
 * 验证目标（与 design.md / Correctness Properties / Property 3 对齐）：
 *   对任意合法的 `Partial<ThemeProfile>` `p`，调用
 *   `useTheme(scope).updateProfile(p)` 后：
 *     1. `themeStore[scope]`（即 Pinia 状态）；
 *     2. `localStorage['bml-theme-{scope}']` 反序列化得到的对象；
 *     3. 后端 `updateMyProfile(scope, profile)` 实际收到的请求体 / 服务端
 *        回填的最终 Profile；
 *   三者 SHALL 在结构与字段值上完全等价（合并 “初始 Profile + 入参 partial”
 *   得到的预期 Profile）。
 *
 * 该不变量是 R5.AC1（服务端持久化）/ R5.AC2（本地持久化）/ R5.AC4（服务端
 * 优先于本地缓存）的核心运行期保证：双作用域共用同一份 Composable / Store /
 * Engine / Storage 实现，但任意一次 `updateProfile` 必须保证三方写入结果
 * 严格一致，避免 “服务端回填了，但本地缓存没刷新” 之类的暗藏 bug。
 *
 * 测试设计要点：
 *
 *   1. 使用 `vi.mock('@/api/theme')` 将 `updateMyProfile` 替换为 “回声” 桩：
 *      把请求体原样作为统一响应体的 `data` 字段返回，从而在本测试中模拟
 *      “服务端归一化后的 Profile = 客户端发送的 Profile” 这一最强等价场景；
 *      入参 `(scope, profile)` 同时被记录到 {@link mockState.calls} 列表
 *      以供断言 “服务端实际收到的 profile = 三方一致的预期 Profile”；
 *
 *   2. 不再次去测 “服务端可能修改字段” 的弱等价场景 —— 该路径属于
 *      `useTheme.test.ts` 单元测试范畴；属性测试关注的是 `updateProfile`
 *      自身在 “store / localStorage / 服务端入参” 三个写入入口上的等价性；
 *
 *   3. 每次 fast-check 迭代独立创建 Pinia 实例与清理 `localStorage`，
 *      并独立重置 mock 调用记录；jsdom 提供 `BroadcastChannel`，因此
 *      `themeStore.patch` 内部的 `themeBroadcast.publish` 不会抛错；
 *
 *   4. 同时覆盖 `ADMIN` 与 `BUSINESS` 两个作用域 —— 由 fast-check
 *      `constantFrom('ADMIN','BUSINESS')` 在每次迭代中均匀采样，
 *      并辅以两个固定作用域的小型 “冒烟” 用例兜底，确保即便随机采样
 *      偏向其中一侧，另一侧的代码路径依然被显式覆盖；
 *
 *   5. 不直接 mock `useRoute()`：本属性始终以显式 `scope` 调用 `useTheme`，
 *      在 `useTheme.ts` 中 `scope ?? inferScopeFromLayout()` 因短路求值
 *      不会触达 vue-router，避免引入额外测试依赖。
 *
 * 关联需求：Requirements 5.1 / 5.2 / 5.4。
 *
 * **Validates: Requirements 5.1, 5.2, 5.4**
 *
 * @vitest-environment jsdom
 */

// @vitest-environment jsdom

import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import fc from 'fast-check';

import type {
    Density,
    FontScale,
    HeaderStyle,
    RadiusStyle,
    SidebarCollapsedStyle,
    SidebarStyle,
    ThemeMode,
    ThemeProfile,
    ThemeScope,
} from '@/types/theme';

/* ──────────────────────────────────────────────────────
   一、@/api/theme 模块 mock：updateMyProfile “回声” 桩
   ──────────────────────────────────────────────────────
   说明：
     - `vi.mock` 必须出现在被测模块 `import` 之前；vitest 会自动提升
       本调用到文件顶部，因此这里写在测试主体引用之前即可；
     - mock 工厂内部不能闭包外部 `let`（提升后外部尚未初始化），
       因此使用挂载在 globalThis 上的可变对象保存调用记录。
   ────────────────────────────────────────────────────── */

interface ThemeApiMockState {
    /** updateMyProfile 的调用记录，下标按调用顺序。 */
    calls: Array<{ scope: ThemeScope; profile: ThemeProfile }>;
}

declare global {
    // eslint-disable-next-line no-var
    var __BML_THEME_API_MOCK_STATE__: ThemeApiMockState | undefined;
}

/** 获取或初始化 mock 共享状态（避免 vi.mock 工厂的提升闭包陷阱）。 */
function getMockState(): ThemeApiMockState {
    if (!globalThis.__BML_THEME_API_MOCK_STATE__) {
        globalThis.__BML_THEME_API_MOCK_STATE__ = { calls: [] };
    }
    return globalThis.__BML_THEME_API_MOCK_STATE__!;
}

vi.mock('@/api/theme', () => {
    return {
        /**
         * `updateMyProfile` 回声桩：将客户端提交的 `profile` 原样作为
         * 统一响应体的 `data` 字段返回；同时把 `(scope, profile)` 记录到
         * 共享状态中，供测试断言 “服务端实际收到的请求体” 与三方等价。
         *
         * 这里返回的形态严格匹配 `request` 拦截器在成功路径上的产物
         * （即完整 ApiResponse 对象），与 `useTheme.unwrapResponseData`
         * 的解包逻辑一致。
         */
        updateMyProfile: vi.fn(async (scope: ThemeScope, profile: ThemeProfile) => {
            getMockState().calls.push({
                scope,
                // 深拷贝避免后续 store.patch 写入污染断言快照
                profile: JSON.parse(JSON.stringify(profile)) as ThemeProfile,
            });
            return {
                code: 200,
                message: 'ok',
                data: profile,
                timestamp: Date.now(),
            };
        }),
        /** 字段级错误反序列化：在本属性测试中走的是成功分支，恒返回空数组。 */
        parseFieldErrors: () => [],
    };
});

/* —— mock 完成后才能 import 被测代码（vitest 已自动提升 vi.mock，
       此处显式排在 vi.mock 之后只是为了阅读直观） —— */
import { useTheme } from './useTheme';
import { useThemeStore } from '@/store/theme';

/* ──────────────────────────────────────────────────────
   二、fast-check Arbitrary：合法 Partial<ThemeProfile> 与作用域
   ──────────────────────────────────────────────────────
   说明：
     - 颜色字段统一生成 `#RRGGBB`（大写）以与后端 `@HexColor` 校验对齐；
     - 18 个字段每个都可独立缺失或出现（`requiredKeys: []`），覆盖
       “全字段写入” 与 “仅改一个字段” 的边界情况；
     - 通过 `filter` 排除完全空的对象，确保至少触发一次写入。
   ────────────────────────────────────────────────────── */

/** 生成 `#RRGGBB` 格式的合法十六进制颜色字符串。 */
const hexColorArb = fc
    .integer({ min: 0, max: 0xffffff })
    .map((n) => `#${n.toString(16).padStart(6, '0').toUpperCase()}`);

/** 生成可空的预设引用 ID（与字段语义对齐）。 */
const presetRefArb: fc.Arbitrary<string | null> = fc.option(
    fc.string({ minLength: 1, maxLength: 24 }),
    { nil: null },
);

/**
 * 生成一份非空的 `Partial<ThemeProfile>`：18 个字段每个都可独立缺失。
 */
const partialProfileArb: fc.Arbitrary<Partial<ThemeProfile>> = fc
    .record(
        {
            primaryColor: hexColorArb,
            secondaryColor: hexColorArb,
            accentColor: hexColorArb,
            successColor: hexColorArb,
            warningColor: hexColorArb,
            errorColor: hexColorArb,
            infoColor: hexColorArb,
            textColor: hexColorArb,
            backgroundColor: hexColorArb,
            borderColor: hexColorArb,
            mode: fc.constantFrom<ThemeMode>('LIGHT', 'DARK', 'AUTO'),
            radius: fc.constantFrom<RadiusStyle>('SHARP', 'SMALL', 'MEDIUM', 'LARGE'),
            density: fc.constantFrom<Density>('COMPACT', 'DEFAULT', 'LOOSE'),
            sidebarStyle: fc.constantFrom<SidebarStyle>(
                'LIGHT',
                'DARK',
                'TRANSPARENT',
                'PRIMARY',
            ),
            sidebarCollapsedStyle: fc.constantFrom<SidebarCollapsedStyle>('LIGHT', 'DARK'),
            headerStyle: fc.constantFrom<HeaderStyle>(
                'LIGHT',
                'DARK',
                'PRIMARY',
                'TRANSPARENT',
            ),
            fontScale: fc.constantFrom<FontScale>('SMALL', 'DEFAULT', 'LARGE', 'XLARGE'),
            presetRef: presetRefArb,
        },
        { requiredKeys: [] },
    )
    .filter((p) => Object.keys(p).length > 0);

/** ADMIN / BUSINESS 作用域均匀采样。 */
const scopeArb: fc.Arbitrary<ThemeScope> = fc.constantFrom<ThemeScope>('ADMIN', 'BUSINESS');

/* ──────────────────────────────────────────────────────
   三、辅助函数：localStorage 键名、深拷贝快照、环境重置
   ────────────────────────────────────────────────────── */

/** 与 `store/theme.ts` 内部 `LOCALSTORAGE_KEY_PREFIX` 保持一致。 */
const LOCALSTORAGE_KEY_PREFIX = 'bml-theme-';

/** 将作用域映射为 localStorage 键名（小写拼接）。 */
function lsKey(scope: ThemeScope): string {
    return `${LOCALSTORAGE_KEY_PREFIX}${scope.toLowerCase()}`;
}

/**
 * 对任意可序列化对象做深拷贝快照，避免后续 store 替换引用时同步污染快照。
 */
function snapshot<T>(value: T): T {
    return JSON.parse(JSON.stringify(value)) as T;
}

/**
 * 强制清理 jsdom 下的 localStorage 与 mock 调用记录。
 *
 * 由于 `useThemeStore` 在 state 初始化时会读取 localStorage，
 * 必须在 `setActivePinia` 之前清空，否则前一次迭代写入的键会
 * 被当作 “初始值” 影响下一次迭代。
 */
function resetEnvironment(): void {
    try {
        window.localStorage.clear();
    } catch {
        /* 静默：测试环境理应可访问 localStorage */
    }
    getMockState().calls.length = 0;
}

/* ──────────────────────────────────────────────────────
   四、属性测试主体
   ────────────────────────────────────────────────────── */

describe('useTheme 属性测试 - Property 3: 持久化三方一致 (P_DUAL_WRITE)', () => {
    beforeEach(() => {
        resetEnvironment();
        setActivePinia(createPinia());
    });

    /**
     * 主属性：updateProfile 完成后 store / localStorage / 服务端三方等价。
     *
     * 不变量（按三方写入入口分别断言）：
     *   1. **store 侧**：`themeStore[scope]` 逐字段等于 `{ ...initial, ...partial }`，
     *      其中 `initial` 是迭代起点的 Profile（由 store 初始化得到）；
     *   2. **localStorage 侧**：`bml-theme-{scope}` 反序列化对象与 (1) 完全相等；
     *   3. **服务端侧**：`updateMyProfile` 被恰好调用一次，作用域参数等于
     *      `scope`，请求体逐字段等于 (1)；
     *   4. **跨作用域不串台**：另一侧 store 对应 namespace 与其 localStorage
     *      键完全未被本次写入触及（仍维持初始 PRESET_BEST 兜底）。
     */
    it('任意 Partial<ThemeProfile> 经 updateProfile 后 store / localStorage / 服务端入参三者结构等价', async () => {
        await fc.assert(
            fc.asyncProperty(
                scopeArb,
                partialProfileArb,
                async (scope, partial) => {
                    /* —— 每次迭代独立重置环境与 Pinia 实例 —— */
                    resetEnvironment();
                    setActivePinia(createPinia());

                    const store = useThemeStore();
                    const otherScope: ThemeScope = scope === 'ADMIN' ? 'BUSINESS' : 'ADMIN';

                    /* —— 起点：store 初始化时已写入 PRESET_BEST 兜底 Profile —— */
                    const initial = snapshot(scope === 'ADMIN' ? store.admin : store.business);
                    const otherInitial = snapshot(
                        otherScope === 'ADMIN' ? store.admin : store.business,
                    );
                    const expected: ThemeProfile = { ...initial, ...partial };

                    /* —— 触发被测动作 —— */
                    const theme = useTheme(scope);
                    await theme.updateProfile(partial);

                    /* —— 断言 1：store 侧 —— */
                    const storeAfter = snapshot(
                        scope === 'ADMIN' ? store.admin : store.business,
                    );
                    expect(storeAfter).toEqual(expected);

                    /* —— 断言 2：localStorage 侧 —— */
                    const raw = window.localStorage.getItem(lsKey(scope));
                    expect(raw).not.toBeNull();
                    const parsed = JSON.parse(raw as string) as ThemeProfile;
                    expect(parsed).toEqual(expected);

                    /* —— 断言 3：服务端侧（mock 入参） —— */
                    const calls = getMockState().calls;
                    expect(calls).toHaveLength(1);
                    expect(calls[0].scope).toBe(scope);
                    expect(calls[0].profile).toEqual(expected);

                    /* —— 断言 4：跨作用域不串台 —— */
                    const otherAfter = snapshot(
                        otherScope === 'ADMIN' ? store.admin : store.business,
                    );
                    expect(otherAfter).toEqual(otherInitial);
                    expect(window.localStorage.getItem(lsKey(otherScope))).toBeNull();
                },
            ),
            { numRuns: 30 },
        );
    });

    /**
     * 冒烟用例：显式覆盖 ADMIN 作用域的最小 “只改一个字段” 场景。
     *
     * 该用例与上方随机属性互补：fast-check 采样可能在 30 次迭代中偶然偏向
     * BUSINESS，本用例确保 ADMIN 路径在每次 CI 运行中都被显式覆盖。
     */
    it('ADMIN 作用域单字段更新后三方仍一致', async () => {
        const store = useThemeStore();
        const initial = snapshot(store.admin);
        const partial: Partial<ThemeProfile> = { primaryColor: '#FF0066' };
        const expected: ThemeProfile = { ...initial, ...partial };

        await useTheme('ADMIN').updateProfile(partial);

        expect(snapshot(store.admin)).toEqual(expected);
        const parsed = JSON.parse(
            window.localStorage.getItem(lsKey('ADMIN')) as string,
        ) as ThemeProfile;
        expect(parsed).toEqual(expected);

        const calls = getMockState().calls;
        expect(calls).toHaveLength(1);
        expect(calls[0]).toEqual({ scope: 'ADMIN', profile: expected });
    });

    /**
     * 冒烟用例：显式覆盖 BUSINESS 作用域的最小 “只改一个字段” 场景。
     */
    it('BUSINESS 作用域单字段更新后三方仍一致', async () => {
        const store = useThemeStore();
        const initial = snapshot(store.business);
        const partial: Partial<ThemeProfile> = { mode: 'DARK' };
        const expected: ThemeProfile = { ...initial, ...partial };

        await useTheme('BUSINESS').updateProfile(partial);

        expect(snapshot(store.business)).toEqual(expected);
        const parsed = JSON.parse(
            window.localStorage.getItem(lsKey('BUSINESS')) as string,
        ) as ThemeProfile;
        expect(parsed).toEqual(expected);

        const calls = getMockState().calls;
        expect(calls).toHaveLength(1);
        expect(calls[0]).toEqual({ scope: 'BUSINESS', profile: expected });
    });
});
