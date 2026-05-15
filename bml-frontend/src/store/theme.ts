/**
 * 主题状态管理（`themeStore`）
 * ──────────────────────────────────────────────────────
 * 双作用域（ADMIN / BUSINESS）主题运行期状态的唯一可信来源。
 *
 * 职责：
 *   1. 持有两套作用域的 {@link ThemeProfile} 状态（互不读取、互不覆盖），
 *      以及共享的预设列表 / 加载与错误标志；
 *   2. 提供统一的写操作入口（`patch` / `applyPreset` / `restore`），
 *      所有写操作内部 “三步原子化” 完成：
 *        a. 写入 store 响应式状态；
 *        b. 通过 {@link applyTokens} 写入 DOM CSS 自定义属性与 `<body>` 主题属性；
 *        c. 写入 `localStorage[bml-theme-{scope}]` 并通过 {@link themeBroadcast}
 *           跨标签广播；
 *   3. 提供 `hydrate(scope)` 完成 “本地优先 → 服务端覆盖 → 失败兜底 PRESET_BEST”
 *      的初始化流程；
 *   4. 提供 `onBroadcast(msg)` 用于消化来自其它标签的同步消息：仅更新 store
 *      与 DOM，**不再次写 localStorage、不再次发布广播**，避免回环。
 *
 * 与其它模块的关系：
 *   - `useTheme(scope)` Composable 只通过本 store 与外界交互；
 *   - `themeEngine.applyTokens` 是 DOM 写入的唯一通道；
 *   - `themeBroadcast` 用于跨标签同步；接收方法 {@link onBroadcast} 不再次广播；
 *   - 服务端接口当前以 `request` 模块的轻封装直接调用，待 `src/api/theme.ts`
 *     （任务 14.1）落地后会切换为该模块的具名 API；前端外部行为保持不变。
 *
 * 关联需求：Requirements 1.3 / 1.4 / 5.2 / 5.7 / 6.1 / 8.6 / 13.1 / 13.2。
 */

import { defineStore } from 'pinia';
import { Message } from '@arco-design/web-vue';

import { applyTokens } from '@/utils/themeEngine';
import { themeBroadcast, type ThemeBroadcastInput } from '@/utils/themeBroadcast';
import request from '@/utils/request';
import type {
    ThemeBroadcastMessage,
    ThemeError,
    ThemePreset,
    ThemeProfile,
    ThemeScope,
} from '@/types/theme';

/* ═══════════════════════════════════════════════════════
   一、常量
   ═══════════════════════════════════════════════════════ */

/**
 * `localStorage` 键前缀；完整键名按作用域转小写拼接得到，
 * 与 `themeBootstrap.THEME_LOCALSTORAGE_KEY_PREFIX` 保持一致。
 */
const LOCALSTORAGE_KEY_PREFIX = 'bml-theme-';

/**
 * 后端主题接口前缀；与设计文档接口契约保持一致：
 *   - GET    `/theme/presets`
 *   - GET    `/theme/me?scope=...`
 *   - PUT    `/theme/me?scope=...`
 *   - POST   `/theme/restore?scope=...`
 *
 * 注：`request` 实例 `baseURL` 已包含 `/api`，因此此处不需要再次加前缀。
 */
const THEME_API_PREFIX = '/theme';

/**
 * 一次性云端同步失败提示的内容。
 *
 * 与 R13.AC1 严格对齐：网络 / 5xx 异常发生时，无论本地缓存是否可用都需弹出
 * 该提示，让用户知晓 “已使用本地配置”，并在网络恢复后自行重试。
 */
const FALLBACK_TOAST_MESSAGE = '主题云端同步失败，已使用本地配置';

/* ═══════════════════════════════════════════════════════
   二、PRESET_BEST 兜底 Profile 常量
   ═══════════════════════════════════════════════════════
   说明：
     - 当 `localStorage` 缺失 / 解析失败、且服务端不可用时，作为最终兜底
       的内置默认主题方案；
     - 字段值与 `tokens.preset-best.scss` 中 `$preset-best-light-{admin,business}`
       SCSS Map 一一对应（颜色、状态色、文字、背景、边框）；
     - 后续若需扩展暗色变体兜底，可在此处新增 DARK 常量并在 mode=AUTO 时按
       `prefers-color-scheme` 选择 —— 但当前 `applyTokens` 已通过 {@link resolveMode}
       在 DOM 层完成 AUTO 解析，因此 store 只需维护亮色基线即可保证对比度合格。
   ═══════════════════════════════════════════════════════ */

/**
 * `PRESET_BEST` 在 ADMIN 作用域的兜底 ThemeProfile（亮色基线）。
 *
 * 维护约束：与 `src/styles/tokens.preset-best.scss` 中
 * `$preset-best-light-admin` / `themeBootstrap.ts` 的 `LIGHT_ADMIN` 字段保持一致。
 */
const PRESET_BEST_PROFILE_ADMIN: ThemeProfile = Object.freeze({
    primaryColor: '#165DFF',
    secondaryColor: '#4080FF',
    accentColor: '#722ED1',
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
}) as ThemeProfile;

/**
 * `PRESET_BEST` 在 BUSINESS 作用域的兜底 ThemeProfile（亮色基线）。
 *
 * 与 ADMIN 仅在 `backgroundColor` 一处不同（业务侧使用更柔和的灰白底色）。
 */
const PRESET_BEST_PROFILE_BUSINESS: ThemeProfile = Object.freeze({
    ...PRESET_BEST_PROFILE_ADMIN,
    backgroundColor: '#F7F8FA',
}) as ThemeProfile;

/**
 * 按作用域获取 `PRESET_BEST` 兜底 Profile 的浅拷贝。
 *
 * 返回浅拷贝是因为 store 状态会被原地修改（`patch` 合并），
 * 直接返回 `Object.freeze` 后的常量会在严格模式下抛出 TypeError。
 */
function clonePresetBestProfile(scope: ThemeScope): ThemeProfile {
    const base = scope === 'ADMIN' ? PRESET_BEST_PROFILE_ADMIN : PRESET_BEST_PROFILE_BUSINESS;
    return { ...base };
}

/* ═══════════════════════════════════════════════════════
   三、localStorage 工具
   ═══════════════════════════════════════════════════════ */

/**
 * 计算指定作用域对应的 `localStorage` 键名。
 *
 * 将 `ThemeScope` 转为小写以便与 `themeBootstrap` 引导脚本约定一致。
 *
 * @param scope 主题作用域（`ADMIN` / `BUSINESS`）。
 * @returns `bml-theme-admin` 或 `bml-theme-business`。
 */
function buildLocalStorageKey(scope: ThemeScope): string {
    return `${LOCALSTORAGE_KEY_PREFIX}${scope.toLowerCase()}`;
}

/**
 * 从 `localStorage` 读取指定作用域的 ThemeProfile。
 *
 * 任意失败（缺失、JSON 解析异常、字段非法）都返回 `null`，由调用方决定兜底；
 * 函数本身不抛出异常，且在解析失败时静默清除该键，避免长期存留损坏数据
 * （与 R13.AC2 保持一致）。
 */
function readProfileFromLocalStorage(scope: ThemeScope): ThemeProfile | null {
    if (typeof window === 'undefined' || typeof window.localStorage === 'undefined') {
        return null;
    }
    const key = buildLocalStorageKey(scope);
    try {
        const raw = window.localStorage.getItem(key);
        if (!raw) {
            return null;
        }
        const parsed = JSON.parse(raw);
        if (!parsed || typeof parsed !== 'object') {
            window.localStorage.removeItem(key);
            return null;
        }
        return parsed as ThemeProfile;
    } catch {
        try {
            window.localStorage.removeItem(key);
        } catch {
            /* 静默失败 */
        }
        return null;
    }
}

/**
 * 将指定作用域的 ThemeProfile 写入 `localStorage`。
 *
 * 写入失败（如隐私模式、超额）只在控制台输出 `console.warn`，不影响主流程。
 */
function writeProfileToLocalStorage(scope: ThemeScope, profile: ThemeProfile): void {
    if (typeof window === 'undefined' || typeof window.localStorage === 'undefined') {
        return;
    }
    try {
        window.localStorage.setItem(buildLocalStorageKey(scope), JSON.stringify(profile));
    } catch (err) {
        console.warn('[BML ThemeStore] 写入 localStorage 失败:', err);
    }
}

/* ═══════════════════════════════════════════════════════
   四、HTTP 调用辅助
   ═══════════════════════════════════════════════════════
   说明：
     - `request` 模块的响应拦截器在 `code === 200` 时返回完整的统一响应体
       `{ code, message, data, ... }`，因此此处通过 `unwrapResponse` 取出
       业务数据；
     - 待任务 14.1 创建 `src/api/theme.ts` 后，本节调用将切换为该模块的
       具名 API（外部行为保持不变）。
   ═══════════════════════════════════════════════════════ */

/** 后端统一响应体在拦截器透传后保留的最小字段集。 */
interface UnifiedApiResponse<T> {
    code: number;
    message: string;
    data: T;
}

/**
 * 从 `request` 调用的返回值中安全取出业务数据。
 *
 * 兼容两种返回形态：
 *   - 拦截器直接返回的统一响应体（`{ code, message, data }`）；
 *   - 极少数失败路径下退化的 `AxiosResponse`（`{ data: { code, message, data } }`）。
 */
function unwrapResponse<T>(res: unknown): T {
    if (res && typeof res === 'object') {
        const r = res as { code?: unknown; data?: unknown };
        if ('code' in r && 'data' in r) {
            return r.data as T;
        }
        if (r.data && typeof r.data === 'object' && 'code' in (r.data as object)) {
            return (r.data as unknown as UnifiedApiResponse<T>).data;
        }
    }
    return res as T;
}

/**
 * 调用后端 `GET /theme/me?scope=...`。
 *
 * @returns 服务端持久化或回退的 ThemeProfile。
 */
async function apiGetMyProfile(scope: ThemeScope): Promise<ThemeProfile> {
    const res = await request.get<ThemeProfile>(`${THEME_API_PREFIX}/me`, {
        params: { scope },
    });
    return unwrapResponse<ThemeProfile>(res);
}

/**
 * 调用后端 `POST /theme/restore?scope=...`，重置为 PRESET_BEST 并返回新 Profile。
 *
 * 注：与 `PUT /theme/me` 不同，本端点的调用属于 store 自身的写操作（restore 动作），
 * 因此被保留在 store 内部；而普通 `updateProfile` 的服务端 PUT 由 `useTheme`
 * Composable（任务 14.2）在更上一层完成，store 不直接调用。
 */
async function apiRestoreProfile(scope: ThemeScope): Promise<ThemeProfile> {
    const res = await request.post<ThemeProfile>(`${THEME_API_PREFIX}/restore`, undefined, {
        params: { scope },
    });
    return unwrapResponse<ThemeProfile>(res);
}

/**
 * 调用后端 `GET /theme/presets`，列出全部预设。
 */
async function apiListPresets(): Promise<ThemePreset[]> {
    const res = await request.get<ThemePreset[]>(`${THEME_API_PREFIX}/presets`);
    return unwrapResponse<ThemePreset[]>(res) ?? [];
}

/* ═══════════════════════════════════════════════════════
   五、Store 类型定义
   ═══════════════════════════════════════════════════════ */

/**
 * 主题状态分项加载标志。
 *
 * 三项相互独立，便于 UI 在 “某个作用域 Profile 加载中” 与
 * “预设列表加载中” 之间精细化区分骨架 / spinner。
 */
export interface ThemeLoadingFlags {
    /** ADMIN 作用域 Profile 加载状态。 */
    admin: boolean;
    /** BUSINESS 作用域 Profile 加载状态。 */
    business: boolean;
    /** 预设列表加载状态。 */
    presets: boolean;
}

/**
 * 主题状态分项错误对象（最近一次失败）。
 *
 * 与 {@link ThemeLoadingFlags} 同维度，保证 UI 能针对性高亮失败区域。
 */
export interface ThemeErrorBag {
    admin: ThemeError | null;
    business: ThemeError | null;
    presets: ThemeError | null;
}

/**
 * `themeStore` 的整体 State。
 *
 * 由 {@link ThemeStoreState.admin} / {@link ThemeStoreState.business} 维持
 * 双作用域响应式 Profile；`presets` 全局共享。
 */
export interface ThemeStoreState {
    /** ADMIN 作用域当前生效 Profile。 */
    admin: ThemeProfile;
    /** BUSINESS 作用域当前生效 Profile。 */
    business: ThemeProfile;
    /** 全部预设（包含 PRESET_BEST 与自定义预设）。 */
    presets: ThemePreset[];
    /** 分项加载状态。 */
    loading: ThemeLoadingFlags;
    /** 分项最近一次错误。 */
    errors: ThemeErrorBag;
}

/* ═══════════════════════════════════════════════════════
   六、内部工具：写操作三步走
   ═══════════════════════════════════════════════════════ */

/**
 * 将 Profile 一致地写入 “store + DOM + localStorage”，并按需广播。
 *
 * @param state         store state 引用，用于直接 `state[scope] = profile` 写入。
 * @param scope         作用域。
 * @param profile       要应用的最终 Profile（已合并完成）。
 * @param broadcastMsg  广播消息体（不含 `senderId`，由 `themeBroadcast.publish`
 *                      自动注入）；为 `null` 时跳过广播（用于 {@link onBroadcast}
 *                      接收远端消息后的本地同步路径，避免回环触发）。
 */
function applyProfileEverywhere(
    state: ThemeStoreState,
    scope: ThemeScope,
    profile: ThemeProfile,
    broadcastMsg: ThemeBroadcastInput | null,
): void {
    // 1) 状态写入：直接替换引用，让 Pinia 监听到顶层变化即可
    state[scope === 'ADMIN' ? 'admin' : 'business'] = profile;
    // 2) DOM 写入：CSS 自定义属性 + body 属性 + 过渡 class
    applyTokens(profile, scope);
    // 3) 持久化写入：localStorage 按作用域分键保存
    writeProfileToLocalStorage(scope, profile);
    // 4) 跨标签广播（接收远端消息时跳过以避免回环）
    if (broadcastMsg) {
        themeBroadcast.publish(broadcastMsg);
    }
}

/**
 * 将网络 / 业务异常归一化为 {@link ThemeError}。
 *
 * `request` 拦截器在拒绝时通常以 `Error` 抛出，附带 `message`；这里仅在 store
 * 层做轻量包装，具体字段错误高亮由 `useTheme()` 与 UI 层进一步处理。
 */
function toThemeError(err: unknown, fallbackCode: ThemeError['code']): ThemeError {
    if (err && typeof err === 'object' && 'message' in err) {
        const msg = String((err as { message?: unknown }).message ?? '');
        return { code: fallbackCode, message: msg || '主题操作失败' };
    }
    return { code: fallbackCode, message: '主题操作失败' };
}

/**
 * 显示一次性云端同步失败提示。
 *
 * 与 R13.AC1 保持一致：无论本地缓存是否可用都需提示用户
 * “已在本地恢复 / 使用本地配置”。封装为函数便于在多处调用且保持文案统一。
 */
function showFallbackToast(): void {
    try {
        Message.warning(FALLBACK_TOAST_MESSAGE);
    } catch {
        /* 测试 / SSR 环境下 Message 可能不可用，忽略即可 */
    }
}

/* ═══════════════════════════════════════════════════════
   七、Store 定义
   ═══════════════════════════════════════════════════════ */

/**
 * 主题 Pinia Store。
 *
 * 对外 API（actions）：
 *   - {@link hydrate}        ：作用域级初始化（本地优先 → 服务端覆盖 → 兜底）。
 *   - {@link patch}          ：合并部分字段，触发三方写入与广播。
 *   - {@link applyPreset}    ：根据预设 ID 应用对应作用域变体。
 *   - {@link restore}        ：调后端 restore，失败时本地兜底 PRESET_BEST。
 *   - {@link fetchPresets}   ：拉取预设列表。
 *   - {@link onBroadcast}    ：消化跨标签广播消息（不二次广播）。
 */
export const useThemeStore = defineStore('theme', {
    state: (): ThemeStoreState => ({
        // 状态初始化：先尝试 localStorage，缺失或损坏时回退到 PRESET_BEST。
        // 这样即使 `hydrate(scope)` 还未被调用，组件读到的也是合法 Profile。
        admin: readProfileFromLocalStorage('ADMIN') ?? clonePresetBestProfile('ADMIN'),
        business: readProfileFromLocalStorage('BUSINESS') ?? clonePresetBestProfile('BUSINESS'),
        presets: [],
        loading: { admin: false, business: false, presets: false },
        errors: { admin: null, business: null, presets: null },
    }),

    actions: {
        /**
         * 初始化指定作用域的主题。
         *
         * 流程（与设计文档「主题数据流」一致）：
         *   1. 本地优先：从 `localStorage` 读取，命中即立刻调 `applyTokens`
         *      渲染（避免后续异步等待期间出现样式抖动）；
         *   2. 服务端覆盖：调用 `GET /theme/me?scope=...`，成功后服务端 Profile
         *      胜出，重新写入 store / DOM / localStorage（注意：无需广播，
         *      初始化阶段属于本标签自身行为，没有跨标签同步语义）；
         *   3. 失败兜底：服务端不可用且本地缓存也缺失时，使用 PRESET_BEST 兜底
         *      并显示一次性提示；缓存可用时仅提示不替换。
         *
         * 该方法是幂等的，可在多处（路由 hook、页面 onMounted）安全调用。
         *
         * @param scope 要初始化的作用域。
         */
        async hydrate(scope: ThemeScope): Promise<void> {
            const loadingKey = scope === 'ADMIN' ? 'admin' : 'business';
            this.loading[loadingKey] = true;
            this.errors[loadingKey] = null;

            // 步骤 1：本地优先（如果 state 与 localStorage 已经一致则只补一次 applyTokens）
            const localProfile = readProfileFromLocalStorage(scope);
            if (localProfile) {
                applyProfileEverywhere(this.$state, scope, localProfile, null);
            }

            try {
                // 步骤 2：服务端覆盖
                const remoteProfile = await apiGetMyProfile(scope);
                if (remoteProfile && typeof remoteProfile === 'object') {
                    applyProfileEverywhere(this.$state, scope, remoteProfile, null);
                }
            } catch (err) {
                // 步骤 3：失败兜底
                this.errors[loadingKey] = toThemeError(err, 'THEME_PERSIST_FAILED');
                if (!localProfile) {
                    const fallback = clonePresetBestProfile(scope);
                    applyProfileEverywhere(this.$state, scope, fallback, null);
                }
                showFallbackToast();
            } finally {
                this.loading[loadingKey] = false;
            }
        },

        /**
         * 合并指定作用域的部分字段并完成三方写入。
         *
         * 注意：本方法**仅维护本地一致性 + 跨标签广播**，不会主动调用
         * 后台 PUT 接口。后台写入由 `useTheme().updateProfile` 在更上一层
         * 完成（见任务 14.2），本 store 保持单一职责。
         *
         * @param scope   作用域。
         * @param partial 要合并的部分 Profile 字段。
         */
        patch(scope: ThemeScope, partial: Partial<ThemeProfile>): void {
            const current = scope === 'ADMIN' ? this.admin : this.business;
            const next: ThemeProfile = { ...current, ...partial };
            applyProfileEverywhere(this.$state, scope, next, {
                kind: 'profile-changed',
                scope,
                profile: next,
            });
        },

        /**
         * 根据预设 ID 应用对应作用域的变体到当前作用域。
         *
         * 找不到指定预设时设置 `errors[scope] = THEME_PRESET_NOT_FOUND` 并直接返回，
         * 不会抛出异常以避免 UI 崩溃；调用方可据此显示具体错误提示。
         *
         * @param scope    作用域。
         * @param presetId 预设 ID（如 `PRESET_BEST`）。
         */
        applyPreset(scope: ThemeScope, presetId: string): void {
            const loadingKey = scope === 'ADMIN' ? 'admin' : 'business';
            const preset = this.presets.find((p) => p.id === presetId);
            if (!preset) {
                this.errors[loadingKey] = {
                    code: 'THEME_PRESET_NOT_FOUND',
                    message: `预设 ${presetId} 不存在`,
                };
                return;
            }
            const variant = scope === 'ADMIN' ? preset.profileAdmin : preset.profileBusiness;
            const next: ThemeProfile = { ...variant, presetRef: preset.id };
            this.errors[loadingKey] = null;
            applyProfileEverywhere(this.$state, scope, next, {
                kind: 'preset-applied',
                scope,
                presetId: preset.id,
            });
        },

        /**
         * 一键恢复指定作用域到 PRESET_BEST。
         *
         * 优先调用后端 `POST /theme/restore?scope=...`，成功时使用服务端返回的
         * Profile 作为最终值；失败时回退到本地 {@link clonePresetBestProfile}
         * 并显示一次性提示，与 R3.AC5 / R13.AC1 对齐。
         *
         * 无论成功或失败，最终都会向所有标签发布 `restored` 广播，确保跨标签
         * 视觉一致。
         *
         * @param scope 作用域。
         */
        async restore(scope: ThemeScope): Promise<void> {
            const loadingKey = scope === 'ADMIN' ? 'admin' : 'business';
            this.loading[loadingKey] = true;
            this.errors[loadingKey] = null;

            let finalProfile: ThemeProfile;
            try {
                finalProfile = await apiRestoreProfile(scope);
                if (!finalProfile || typeof finalProfile !== 'object') {
                    // 后端返回为空时按本地兜底处理，避免后续 applyTokens 收到非法对象
                    finalProfile = clonePresetBestProfile(scope);
                }
            } catch (err) {
                this.errors[loadingKey] = toThemeError(err, 'THEME_PERSIST_FAILED');
                finalProfile = clonePresetBestProfile(scope);
                showFallbackToast();
            } finally {
                this.loading[loadingKey] = false;
            }

            applyProfileEverywhere(this.$state, scope, finalProfile, {
                kind: 'restored',
                scope,
            });
        },

        /**
         * 拉取并缓存预设列表。
         *
         * 失败时保留旧 `presets` 引用（不清空）以避免界面骤然空白；
         * `errors.presets` 同步记录最近一次错误供 UI 展示。
         */
        async fetchPresets(): Promise<void> {
            this.loading.presets = true;
            this.errors.presets = null;
            try {
                const list = await apiListPresets();
                this.presets = Array.isArray(list) ? list : [];
            } catch (err) {
                this.errors.presets = toThemeError(err, 'THEME_PERSIST_FAILED');
            } finally {
                this.loading.presets = false;
            }
        },

        /**
         * 处理来自其它标签的主题广播消息。
         *
         * 行为约定：
         *   - 仅同步本地 store 与 DOM；
         *   - **不再次写入 localStorage**：广播发起方自身已写入，远端无需重写；
         *   - **不再次发布广播**：避免广播回环（`themeBroadcast` 已通过
         *     `senderId` 过滤自身消息，但作为多重防线 store 层也明确禁止）；
         *   - `restored` 消息使用本地 PRESET_BEST 兜底 Profile，与发起方端最终
         *     调用 `applyTokens` 后的视觉等价。
         *
         * @param msg 广播消息体。
         */
        onBroadcast(msg: ThemeBroadcastMessage): void {
            if (!msg || typeof msg !== 'object') {
                return;
            }
            switch (msg.kind) {
                case 'profile-changed': {
                    if (msg.profile && typeof msg.profile === 'object') {
                        // 仅更新 state + DOM；不写 localStorage、不再次广播
                        this[msg.scope === 'ADMIN' ? 'admin' : 'business'] = msg.profile;
                        applyTokens(msg.profile, msg.scope);
                    }
                    break;
                }
                case 'preset-applied': {
                    const preset = this.presets.find((p) => p.id === msg.presetId);
                    if (preset) {
                        const variant =
                            msg.scope === 'ADMIN' ? preset.profileAdmin : preset.profileBusiness;
                        const next: ThemeProfile = { ...variant, presetRef: preset.id };
                        this[msg.scope === 'ADMIN' ? 'admin' : 'business'] = next;
                        applyTokens(next, msg.scope);
                    }
                    break;
                }
                case 'restored': {
                    const next = clonePresetBestProfile(msg.scope);
                    this[msg.scope === 'ADMIN' ? 'admin' : 'business'] = next;
                    applyTokens(next, msg.scope);
                    break;
                }
                default:
                    /* 未识别的 kind 直接忽略，向后兼容预留 */
                    break;
            }
        },
    },
});

export default useThemeStore;
