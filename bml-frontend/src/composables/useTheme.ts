/**
 * `useTheme` Composable
 * ──────────────────────────────────────────────────────
 * 主题引擎对外暴露的统一组合式入口。Composable 在双作用域（ADMIN /
 * BUSINESS）主题系统中扮演 “界面与底层引擎之间的薄适配层” 角色：
 *
 *   1. 通过 `scope` 参数（或自动推断结果）锁定本次调用要操作的作用域；
 *   2. 将 {@link useThemeStore} 的响应式状态包装为只读视图（`profile`
 *      / `presets` / `isLoading` / `error`）暴露给上层组件；
 *   3. 提供三个高阶动作：
 *        - {@link UseThemeReturn.updateProfile}：本地 patch → 写
 *          `localStorage` → 跨标签广播 → 后台 `PUT /theme/me` → 服务端
 *          结果回填覆盖本地（与 `requirements.md` R5.AC1 / R5.AC4 对齐）；
 *        - {@link UseThemeReturn.applyPreset}：选中某预设并立即同步到
 *          服务端，失败时仍保留本地变更（R3.AC4 / R5.AC4）；
 *        - {@link UseThemeReturn.restoreDefault}：一键恢复 `PRESET_BEST`，
 *          复用 store 中已实现的 “后端 restore + 失败兜底” 流程（R3.AC5）；
 *   4. 全局 `try/catch` + 平台错误上报通道：任何后台调用失败都不会让
 *      调用方组件崩溃；失败信息通过 `error` 暴露并通过 `Message.warning`
 *      显示一次性提示（R8.AC4 / R13.AC1 / R13.AC4）。
 *
 * 与其它模块的关系：
 *   - 所有写操作的 “本地 + 跨标签 + DOM” 一致性由 `themeStore.patch` /
 *     `themeStore.applyPreset` / `themeStore.restore` 内部保证，
 *     本 Composable 只负责 “后台同步与失败降级”，避免双重写入；
 *   - 服务端调用统一走 `@/api/theme` 中的具名函数，`@/api/theme` 的
 *     {@link parseFieldErrors} 用于在 `THEME_INVALID_PROFILE` 错误时
 *     反序列化字段级错误明细供 UI 高亮；
 *   - 作用域自动推断按当前 `vue-router` 路由布局识别（Admin 嵌 Business
 *     时取外层 Admin），无法识别时抛出 `THEME_SCOPE_UNRESOLVED`。
 *
 * 关联需求：Requirements 3.5 / 5.6 / 8.1 / 8.8 / 13.1 / 13.4。
 */

import { computed, type ComputedRef, type Ref } from 'vue';
import { useRoute } from 'vue-router';
import { Message } from '@arco-design/web-vue';

import { useThemeStore } from '@/store/theme';
import { parseFieldErrors, updateMyProfile } from '@/api/theme';
import type {
    ThemeError,
    ThemePreset,
    ThemeProfile,
    ThemeScope,
} from '@/types/theme';

/* ═══════════════════════════════════════════════════════
   一、常量与公共辅助
   ═══════════════════════════════════════════════════════ */

/**
 * 后端同步失败时统一显示的提示文案。
 *
 * 与 `themeStore` 中的同名常量保持一致，确保 “后端调用失败 → 沿用本地缓存”
 * 这一兜底路径在所有触发点（hydrate / restore / updateProfile / applyPreset）
 * 上展示完全相同的用户提示。R13.AC1 严格要求此提示在网络 / 5xx 异常时无论
 * 本地缓存是否可用都需展示。
 */
const FALLBACK_TOAST_MESSAGE = '主题云端同步失败，已使用本地配置';

/**
 * 作用域无法识别时抛出的错误码。
 *
 * 与 `ThemeError.code` 联合类型中的同名分支以及 `requirements.md` R8.AC8
 * 严格保持一致；外层 UI 可据此区分 “业务可恢复错误” 与 “开发期布局缺失”。
 */
const SCOPE_UNRESOLVED_CODE = 'THEME_SCOPE_UNRESOLVED' as const;

/**
 * `Error` 的轻量扩展类型：附带 `code` 字段供调用方识别错误种类。
 *
 * 使用普通 `Error` + `code` 属性的方式而不是抛出 `ThemeError` 字面量，
 * 是为了在保留语义错误码的同时享受标准异常的堆栈追踪能力。
 */
type ScopeUnresolvedError = Error & { code: typeof SCOPE_UNRESOLVED_CODE };

/**
 * 构造作用域识别失败专用的异常对象。
 */
function buildScopeUnresolvedError(): ScopeUnresolvedError {
    const err = new Error(
        '当前路由无法识别为 ADMIN 或 BUSINESS 主题作用域',
    ) as ScopeUnresolvedError;
    err.code = SCOPE_UNRESOLVED_CODE;
    err.name = SCOPE_UNRESOLVED_CODE;
    return err;
}

/**
 * 上报捕获到的 Composable 异常到平台错误上报通道（R13.AC4）。
 *
 * 当前实现：
 *   1. 通过 `console.error` 输出到控制台；
 *   2. 触发自定义事件 `bml:theme:error`，业务侧可在统一错误上报模块上
 *      监听该事件以聚合 Sentry / 自研日志服务。
 *
 * 该函数永不抛出，避免上报通道异常拖累主题主流程。
 */
function reportError(err: unknown, context: string): void {
    try {
        // eslint-disable-next-line no-console
        console.error(`[BML useTheme] ${context}:`, err);
    } catch {
        /* 静默失败：在 console 不可用时也不应阻断主流程 */
    }
    if (
        typeof window !== 'undefined' &&
        typeof window.dispatchEvent === 'function' &&
        typeof CustomEvent === 'function'
    ) {
        try {
            window.dispatchEvent(
                new CustomEvent('bml:theme:error', {
                    detail: { error: err, context },
                }),
            );
        } catch {
            /* 静默失败：CustomEvent 在极个别老旧浏览器下不可用 */
        }
    }
}

/**
 * 显示一次性云端同步失败提示。
 *
 * 测试 / SSR 环境下 `Message` 可能不可用（无 DOM 容器），此时静默忽略，
 * 保证 Composable 在非浏览器环境也能完成纯逻辑测试。
 */
function showFallbackToast(): void {
    try {
        Message.warning(FALLBACK_TOAST_MESSAGE);
    } catch {
        /* 静默：测试环境忽略 */
    }
}

/**
 * 安全地从 `request` 调用结果中拆出业务数据。
 *
 * `bml-frontend` 的 axios 响应拦截器统一在 `code === 200` 时返回完整的
 * `ApiResponse`（`{ code, message, data, ... }`），因此 `@/api/theme` 中
 * 即便 TypeScript 标注为 `Promise<ThemeProfile>`，运行期实际拿到的也是
 * 包了一层的对象。本函数兼容两种形态，保证 Composable 拿到的是真实
 * 业务负载。
 */
function unwrapResponseData<T>(res: unknown): T {
    if (res && typeof res === 'object') {
        const r = res as { code?: unknown; data?: unknown };
        if ('code' in r && 'data' in r) {
            return r.data as T;
        }
        if (
            r.data &&
            typeof r.data === 'object' &&
            'code' in (r.data as object) &&
            'data' in (r.data as object)
        ) {
            return (r.data as { data: T }).data;
        }
    }
    return res as T;
}

/* ═══════════════════════════════════════════════════════
   二、作用域自动推断
   ═══════════════════════════════════════════════════════ */

/**
 * 已识别为 BUSINESS 作用域的根级路径前缀集合。
 *
 * 与 `src/router/index.ts` 中 BUSINESS 布局下的子路由保持一致；
 * 任何不属于 ADMIN 也不属于该集合的路径都会触发
 * {@link SCOPE_UNRESOLVED_CODE} 抛错。
 */
const BUSINESS_PATH_PREFIXES: ReadonlyArray<string> = [
    '/dashboard',
    '/system',
    '/profile',
    '/login',
];

/**
 * 解析当前路由路径。
 *
 * 优先使用 `vue-router` 的 {@link useRoute}（仅在 setup / 同上下文调用栈中
 * 有效）；当 Composable 在非 setup 路径下被调用（少见但允许）或 vue-router
 * 还未注入时，回退到 `window.location.pathname`。两者皆失败时返回空串
 * 以触发后续的 `THEME_SCOPE_UNRESOLVED` 抛错路径。
 */
function resolveCurrentRoutePath(): string {
    try {
        const route = useRoute();
        if (route && typeof route.path === 'string' && route.path) {
            return route.path;
        }
    } catch {
        /* useRoute 在 setup 之外抛错，回退到 location */
    }
    if (typeof window !== 'undefined' && window.location?.pathname) {
        return window.location.pathname;
    }
    return '';
}

/**
 * 探测 “最外层窗口” 的路径，用于实现 R8.AC8 中
 * “Admin 嵌 Business 时取外层 Admin” 的优先级语义。
 *
 * 跨源 iframe 因同源策略无法读取 `window.top.location`，访问失败时静默
 * 返回空串 —— 此时退化到本窗口路径推断，不影响主流程。
 */
function probeOuterFramePath(): string {
    if (typeof window === 'undefined') {
        return '';
    }
    try {
        const top = window.top;
        if (!top || top === window) {
            return '';
        }
        return top.location?.pathname ?? '';
    } catch {
        return '';
    }
}

/**
 * 基于路径字符串推断 {@link ThemeScope}。
 *
 * @returns 命中 ADMIN 或 BUSINESS 时返回对应作用域；任何已识别布局之外
 *          的路径返回 `null`，由调用方决定是否抛错。
 */
function classifyScope(path: string): ThemeScope | null {
    if (!path) return null;
    if (path.startsWith('/admin')) return 'ADMIN';
    // 业务系统根路径（直接访问 `/`）以及全部根级业务子路径
    if (path === '/' || BUSINESS_PATH_PREFIXES.some((p) => path === p || path.startsWith(p + '/'))) {
        return 'BUSINESS';
    }
    return null;
}

/**
 * 在未传入 `scope` 时，根据当前路由布局自动推断 {@link ThemeScope}。
 *
 * 推断顺序（与 R8.AC8 严格对齐）：
 *   1. 若可探测到外层窗口且其路径属于 ADMIN，则取外层 ADMIN
 *      （“Admin 嵌 Business iframe” 场景的关键分支）；
 *   2. 否则按当前窗口路径分类：`/admin/*` → ADMIN，已知业务前缀 → BUSINESS；
 *   3. 当上述分支均无法识别时，立即抛出 `THEME_SCOPE_UNRESOLVED` 异常，
 *      由调用方捕获或交由全局 errorHandler 处理。
 */
function inferScopeFromLayout(): ThemeScope {
    const outerPath = probeOuterFramePath();
    if (outerPath.startsWith('/admin')) {
        return 'ADMIN';
    }
    const ownPath = resolveCurrentRoutePath();
    const scope = classifyScope(ownPath);
    if (!scope) {
        throw buildScopeUnresolvedError();
    }
    return scope;
}

/* ═══════════════════════════════════════════════════════
   三、对外类型与 Composable 主体
   ═══════════════════════════════════════════════════════ */

/**
 * `useTheme()` 返回值。
 *
 * 所有响应式字段以只读 Ref 形式暴露，强制调用方通过 {@link UseThemeReturn.updateProfile} /
 * {@link UseThemeReturn.applyPreset} / {@link UseThemeReturn.restoreDefault}
 * 这三个动作触发变更，避免业务组件直接修改 store 内部状态。
 */
export interface UseThemeReturn {
    /** 当前作用域只读 ThemeProfile。 */
    profile: Readonly<Ref<ThemeProfile>>;
    /** 共享的预设列表（含 PRESET_BEST 与平台级自定义预设）。 */
    presets: Readonly<Ref<ThemePreset[]>>;
    /** 当前作用域 Profile 加载状态。 */
    isLoading: Readonly<Ref<boolean>>;
    /** 当前作用域最近一次错误（成功后由各动作清空）。 */
    error: Readonly<Ref<ThemeError | null>>;
    /**
     * 增量更新当前作用域 Profile。
     *
     * 流程：本地 patch（同步写 store / DOM / `localStorage` / 跨标签广播）→
     * 后台 `PUT /theme/me` → 服务端结果回填覆盖本地。
     */
    updateProfile: (partial: Partial<ThemeProfile>) => Promise<void>;
    /**
     * 应用指定预设到当前作用域。
     *
     * 内部先调用 `themeStore.applyPreset`（已完成本地三方写入与广播），
     * 然后将最终 Profile 同步到后台；后台失败仅触发兜底提示，不会回滚本地。
     */
    applyPreset: (presetId: string) => Promise<void>;
    /** 一键恢复当前作用域到 PRESET_BEST，等价于 `themeStore.restore(scope)`。 */
    restoreDefault: () => Promise<void>;
    /** 当前作用域（已解析）。 */
    scope: ThemeScope;
}

/**
 * 主题 Composable 主入口。
 *
 * @param scope 显式指定的作用域；省略时根据当前路由布局推断（参见
 *              {@link inferScopeFromLayout}）。
 * @returns {@link UseThemeReturn} 响应式视图与三个高阶动作。
 *
 * @throws {Error & { code: 'THEME_SCOPE_UNRESOLVED' }} 当 `scope` 未传入
 *         且当前路由不属于 Admin / Business 任一被识别布局时抛出。
 *
 * @example
 * ```ts
 * // 在 Admin 布局下（路径 `/admin/...`）显式调用
 * const theme = useTheme('ADMIN');
 * await theme.updateProfile({ primaryColor: '#FF0066' });
 *
 * // 在已挂载布局内省略 scope，自动按路由识别
 * const theme = useTheme();
 * await theme.applyPreset('PRESET_BEST');
 * await theme.restoreDefault();
 * ```
 */
export function useTheme(scope?: ThemeScope): UseThemeReturn {
    /* —— 解析作用域：未传入时按当前路由布局推断，无法识别则抛错 —— */
    const resolvedScope: ThemeScope = scope ?? inferScopeFromLayout();
    /* 用于读写 store 中分项加载 / 错误标志的小写键名。 */
    const stateKey: 'admin' | 'business' = resolvedScope === 'ADMIN' ? 'admin' : 'business';

    const store = useThemeStore();

    /* —— 响应式视图 —— */
    const profile = computed<ThemeProfile>(() =>
        resolvedScope === 'ADMIN' ? store.admin : store.business,
    ) as ComputedRef<ThemeProfile>;
    const presets = computed<ThemePreset[]>(() => store.presets) as ComputedRef<ThemePreset[]>;
    const isLoading = computed<boolean>(() => store.loading[stateKey]) as ComputedRef<boolean>;
    const error = computed<ThemeError | null>(
        () => store.errors[stateKey],
    ) as ComputedRef<ThemeError | null>;

    /* —— updateProfile：本地 → 服务端 → 回填 —— */
    async function updateProfile(partial: Partial<ThemeProfile>): Promise<void> {
        try {
            // 1) 本地 patch：内部已完成 store + DOM + localStorage + 广播
            store.patch(resolvedScope, partial);

            // 2) 后台 PUT：使用本地最终 Profile 作为请求体
            const localProfile = resolvedScope === 'ADMIN' ? store.admin : store.business;
            const res = await updateMyProfile(resolvedScope, localProfile);
            const serverProfile = unwrapResponseData<ThemeProfile>(res);

            // 3) 服务端结果回填覆盖本地（仅当返回合法对象时）。再次 patch
            //    会触发一次广播，让其它标签同步到服务端归一化后的最终 Profile，
            //    与 P_DUAL_WRITE / P_BROADCAST_CONVERGE 属性测试预期一致。
            if (serverProfile && typeof serverProfile === 'object') {
                store.patch(resolvedScope, serverProfile);
            }

            // 4) 同步成功后清空当前作用域的错误标志
            store.errors[stateKey] = null;
        } catch (err) {
            // 字段级校验失败时携带 fieldErrors 以供 Panel 高亮
            const fieldErrors = parseFieldErrors(err);
            const themeError: ThemeError = {
                code:
                    fieldErrors.length > 0
                        ? 'THEME_INVALID_PROFILE'
                        : 'THEME_PERSIST_FAILED',
                message:
                    (err as { message?: unknown })?.message != null
                        ? String((err as { message?: unknown }).message)
                        : '主题更新失败',
                ...(fieldErrors.length > 0 ? { fieldErrors } : {}),
            };
            store.errors[stateKey] = themeError;
            reportError(err, 'updateProfile');
            showFallbackToast();
        }
    }

    /* —— applyPreset：本地选中 → 服务端持久化 —— */
    async function applyPreset(presetId: string): Promise<void> {
        try {
            store.applyPreset(resolvedScope, presetId);
            // store 在预设缺失时会写入 THEME_PRESET_NOT_FOUND，不应继续走 PUT
            const currentErr = store.errors[stateKey];
            if (currentErr && currentErr.code === 'THEME_PRESET_NOT_FOUND') {
                return;
            }

            const localProfile = resolvedScope === 'ADMIN' ? store.admin : store.business;
            const res = await updateMyProfile(resolvedScope, localProfile);
            const serverProfile = unwrapResponseData<ThemeProfile>(res);
            if (serverProfile && typeof serverProfile === 'object') {
                store.patch(resolvedScope, serverProfile);
            }
            store.errors[stateKey] = null;
        } catch (err) {
            store.errors[stateKey] = {
                code: 'THEME_PERSIST_FAILED',
                message:
                    (err as { message?: unknown })?.message != null
                        ? String((err as { message?: unknown }).message)
                        : '应用预设失败',
            };
            reportError(err, 'applyPreset');
            showFallbackToast();
        }
    }

    /* —— restoreDefault：直接复用 store.restore（已含失败兜底） —— */
    async function restoreDefault(): Promise<void> {
        try {
            await store.restore(resolvedScope);
        } catch (err) {
            // `themeStore.restore` 已自行处理网络错误并显示提示，这里仅做兜底
            // 上报，避免任何未预期异常冒泡至上层组件。
            reportError(err, 'restoreDefault');
        }
    }

    return {
        profile,
        presets,
        isLoading,
        error,
        updateProfile,
        applyPreset,
        restoreDefault,
        scope: resolvedScope,
    };
}

export default useTheme;
