/**
 * `themeStore` 属性测试 - Property 1: 作用域隔离（P_ISOLATION）。
 *
 * 验证目标（与 design.md / Correctness Properties / Property 1 对齐）：
 *
 *   - 对任意 Profile 序列 `A_admin → A_business`，
 *     调用 `themeStore.patch('ADMIN', A_admin)` 后再调用
 *     `themeStore.patch('BUSINESS', A_business)`，
 *     `themeStore.admin` 与 `localStorage['bml-theme-admin']` SHALL 保持不变；
 *
 *   - 反向 `B_business → B_admin` 同样成立：
 *     `themeStore.business` 与 `localStorage['bml-theme-business']` 在
 *     `patch('ADMIN', ...)` 之后 SHALL 保持不变。
 *
 * 该不变量是 R1.AC4（中台 / 业务作用域互不影响）与 R5.AC7（缓存 + 服务端
 * 记录隔离）的核心运行期保证：双作用域共用同一份 store / engine / storage
 * 实现，但任意一侧的写操作绝不能 “泄漏” 到另一侧的状态、本地缓存与 DOM。
 *
 * 测试设计要点：
 *
 *   1. fast-check 各自独立生成 ADMIN / BUSINESS 两份 `Partial<ThemeProfile>`，
 *      允许 `partial` 选取 18 个字段中的任意非空子集，最大化覆盖 “只改一个
 *      字段也不能串台” 的边界情况；
 *
 *   2. 每次迭代前主动清空 `localStorage` 并新建 Pinia 实例，避免上一次迭代
 *      的状态影响当前不变量；
 *
 *   3. 对 store / localStorage 的快照都使用 `JSON.parse(JSON.stringify(...))`
 *      做深拷贝 + 序列化对比，确保后续 patch 的引用替换不会污染快照；
 *
 *   4. 由于本测试只关注 store / localStorage 的隔离性，不涉及网络与服务端
 *      回填，因此不需要 mock axios；`patch` 内部不会主动调用后端接口
 *      （后台 PUT 由 `useTheme().updateProfile` 在更上层完成）。
 *
 * 关联需求：Requirements 1.3 / 1.4 / 5.7。
 *
 * **Validates: Requirements 1.3, 1.4, 5.7**
 *
 * @vitest-environment jsdom
 */

// @vitest-environment jsdom

import { afterEach, beforeEach, describe, expect, it } from 'vitest';
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
import { useThemeStore } from './theme';

/* ──────────────────────────────────────────────────────
   一、fast-check Arbitrary：生成合法的 Partial<ThemeProfile>
   ──────────────────────────────────────────────────────
   说明：
     - 颜色字段统一生成 `#RRGGBB` 形式（大写）以与后端 `@HexColor` 校验一致；
     - 枚举字段使用 `constantFrom` 在合法取值集合中均匀采样；
     - `partialA` / `partialB` 通过 `fc.record` 的 `requiredKeys: []` 选项
       生成 18 字段中的任意非空子集，从而既覆盖 “全字段写入” 也覆盖 “仅改
       一个字段” 的边界情况；
     - `presetRef` 允许为 `null`，与 ThemeProfile 字段语义保持一致。
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
 * 生成一份 `Partial<ThemeProfile>`：
 *   - 18 个字段每个都可独立缺失或出现（`requiredKeys: []`）；
 *   - 出现时必须是合法值（颜色为 `#RRGGBB`、枚举在合法集合内）。
 *
 * 通过 `filter` 排除完全空的对象，确保至少触发一次写入；
 * 这与本属性 “任意写操作都不应影响另一作用域” 的语义贴合。
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

/* ──────────────────────────────────────────────────────
   二、辅助函数：localStorage / 快照
   ────────────────────────────────────────────────────── */

/** 与 `theme.ts` 内部 `LOCALSTORAGE_KEY_PREFIX` 保持一致。 */
const LOCALSTORAGE_KEY_PREFIX = 'bml-theme-';

/** 将作用域映射为 localStorage 键名（小写拼接）。 */
function lsKey(scope: ThemeScope): string {
    return `${LOCALSTORAGE_KEY_PREFIX}${scope.toLowerCase()}`;
}

/**
 * 对任意可序列化对象做深拷贝快照，避免后续 store.patch 替换引用时
 * 同步污染上一次的快照（store 内部用 `state[scope] = next` 替换引用，
 * 直接保留旧引用本身已经够用，但通过 JSON 化能进一步确保对比稳定）。
 */
function snapshot<T>(value: T): T {
    return JSON.parse(JSON.stringify(value)) as T;
}

/**
 * 强制清理 jsdom 下的 localStorage 与 store 状态。
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
}

/* ──────────────────────────────────────────────────────
   三、属性测试主体
   ────────────────────────────────────────────────────── */

describe('themeStore 属性测试 - Property 1: 作用域隔离 (P_ISOLATION)', () => {
    beforeEach(() => {
        resetEnvironment();
    });

    afterEach(() => {
        resetEnvironment();
    });

    /**
     * 主属性：先写 ADMIN 再写 BUSINESS，ADMIN 的 store 状态与 localStorage
     * 必须完全保持不变。
     *
     * 不变量：
     *   1. `themeStore.admin` 在 `patch('BUSINESS', ...)` 之后逐字段等于
     *      `patch('ADMIN', ...)` 之后的快照；
     *   2. `localStorage['bml-theme-admin']` 在 `patch('BUSINESS', ...)` 之后
     *      与 `patch('ADMIN', ...)` 之后的快照字符串完全相等；
     *   3. 同时 `themeStore.business` 与 `localStorage['bml-theme-business']`
     *      正确反映了 BUSINESS 侧的写入（这是 “隔离” 的对照面：
     *      “互不影响” 不能退化为 “互不写入”）。
     */
    it('先写 ADMIN 再写 BUSINESS 时 ADMIN 状态与 localStorage 完全不变', () => {
        fc.assert(
            fc.property(partialProfileArb, partialProfileArb, (partialA, partialB) => {
                resetEnvironment();
                setActivePinia(createPinia());
                const store = useThemeStore();

                // 步骤 1：ADMIN 侧写入
                store.patch('ADMIN', partialA);
                const adminAfterStep1 = snapshot(store.admin);
                const adminLsAfterStep1 = window.localStorage.getItem(lsKey('ADMIN'));
                // 写入应使 localStorage 命中（patch 内部通过 applyProfileEverywhere 持久化）
                expect(adminLsAfterStep1).not.toBeNull();

                // 步骤 2：BUSINESS 侧写入
                store.patch('BUSINESS', partialB);

                // 不变量 1：ADMIN store 状态保持不变
                expect(store.admin).toEqual(adminAfterStep1);

                // 不变量 2：ADMIN localStorage 保持不变
                expect(window.localStorage.getItem(lsKey('ADMIN'))).toBe(adminLsAfterStep1);

                // 对照：BUSINESS 侧确实被写入并反映了 partialB
                for (const key of Object.keys(partialB) as Array<keyof ThemeProfile>) {
                    expect(store.business[key]).toEqual(partialB[key]);
                }
                expect(window.localStorage.getItem(lsKey('BUSINESS'))).not.toBeNull();
            }),
            { numRuns: 50 },
        );
    });

    /**
     * 反向属性：先写 BUSINESS 再写 ADMIN，BUSINESS 的 store 状态与
     * localStorage 必须完全保持不变。
     *
     * 与正向属性对称设计，确保隔离性是双向成立的不变量；同样校验对照面：
     * ADMIN 侧确实被 partialA 写入。
     */
    it('先写 BUSINESS 再写 ADMIN 时 BUSINESS 状态与 localStorage 完全不变', () => {
        fc.assert(
            fc.property(partialProfileArb, partialProfileArb, (partialB, partialA) => {
                resetEnvironment();
                setActivePinia(createPinia());
                const store = useThemeStore();

                // 步骤 1：BUSINESS 侧写入
                store.patch('BUSINESS', partialB);
                const businessAfterStep1 = snapshot(store.business);
                const businessLsAfterStep1 = window.localStorage.getItem(lsKey('BUSINESS'));
                expect(businessLsAfterStep1).not.toBeNull();

                // 步骤 2：ADMIN 侧写入
                store.patch('ADMIN', partialA);

                // 不变量 1：BUSINESS store 状态保持不变
                expect(store.business).toEqual(businessAfterStep1);

                // 不变量 2：BUSINESS localStorage 保持不变
                expect(window.localStorage.getItem(lsKey('BUSINESS'))).toBe(
                    businessLsAfterStep1,
                );

                // 对照：ADMIN 侧确实被写入并反映了 partialA
                for (const key of Object.keys(partialA) as Array<keyof ThemeProfile>) {
                    expect(store.admin[key]).toEqual(partialA[key]);
                }
                expect(window.localStorage.getItem(lsKey('ADMIN'))).not.toBeNull();
            }),
            { numRuns: 50 },
        );
    });

    /**
     * 强化属性：交错的多次写入序列下作用域依然完全隔离。
     *
     * 在每次迭代中按随机顺序对 ADMIN / BUSINESS 各执行 K 次 patch；
     * 维护一组 “每次写入对应作用域结束时的快照”，断言：
     *   - 每个作用域在自己最后一次 patch 之后，无论对面再发生多少次写入，
     *     状态与 localStorage 都保持不变；
     *   - 跨作用域的写入序列中不会互相覆盖（即没有任意一次 patch 让另一
     *     作用域的状态被回滚或同步到对方的值）。
     */
    it('交错的 ADMIN / BUSINESS patch 序列下两侧状态彼此独立', () => {
        fc.assert(
            fc.property(
                fc.array(
                    fc.tuple(fc.constantFrom<ThemeScope>('ADMIN', 'BUSINESS'), partialProfileArb),
                    { minLength: 2, maxLength: 8 },
                ),
                (sequence) => {
                    resetEnvironment();
                    setActivePinia(createPinia());
                    const store = useThemeStore();

                    /**
                     * 记录每个作用域最近一次写入完成时的快照，
                     * 之后一旦对面再发生 patch，就以此快照断言隔离性。
                     */
                    let lastAdminSnap: ThemeProfile | null = null;
                    let lastAdminLs: string | null = null;
                    let lastBusinessSnap: ThemeProfile | null = null;
                    let lastBusinessLs: string | null = null;

                    for (const [scope, partial] of sequence) {
                        // —— 在执行本次 patch 之前，先验证另一作用域的隔离性 ——
                        if (scope === 'ADMIN' && lastBusinessSnap !== null) {
                            expect(store.business).toEqual(lastBusinessSnap);
                            expect(window.localStorage.getItem(lsKey('BUSINESS'))).toBe(
                                lastBusinessLs,
                            );
                        }
                        if (scope === 'BUSINESS' && lastAdminSnap !== null) {
                            expect(store.admin).toEqual(lastAdminSnap);
                            expect(window.localStorage.getItem(lsKey('ADMIN'))).toBe(
                                lastAdminLs,
                            );
                        }

                        // —— 执行 patch ——
                        store.patch(scope, partial);

                        // —— 更新本作用域的 “最近一次写入快照” ——
                        if (scope === 'ADMIN') {
                            lastAdminSnap = snapshot(store.admin);
                            lastAdminLs = window.localStorage.getItem(lsKey('ADMIN'));
                            expect(lastAdminLs).not.toBeNull();
                        } else {
                            lastBusinessSnap = snapshot(store.business);
                            lastBusinessLs = window.localStorage.getItem(lsKey('BUSINESS'));
                            expect(lastBusinessLs).not.toBeNull();
                        }
                    }

                    // —— 序列末尾再做一次终态校验 ——
                    if (lastAdminSnap !== null) {
                        expect(store.admin).toEqual(lastAdminSnap);
                        expect(window.localStorage.getItem(lsKey('ADMIN'))).toBe(lastAdminLs);
                    }
                    if (lastBusinessSnap !== null) {
                        expect(store.business).toEqual(lastBusinessSnap);
                        expect(window.localStorage.getItem(lsKey('BUSINESS'))).toBe(
                            lastBusinessLs,
                        );
                    }
                },
            ),
            { numRuns: 30 },
        );
    });
});
