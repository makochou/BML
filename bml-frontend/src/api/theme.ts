/**
 * 主题设置 API 接口层
 * <p>
 * 封装 BML 平台 “中台管理 / 业务系统” 双作用域主题引擎的全部 RESTful 接口。
 * 所有接口均通过项目共享的 axios 实例（{@link request}）发起，
 * 由请求拦截器统一附加 `Authorization`，由响应拦截器统一拆包 `Result<T>` 协议。
 * </p>
 *
 * <h3>错误响应中的 FieldError[] 反序列化</h3>
 * 当后端 {@code ThemeProfileValidator} 校验失败时会抛出
 * {@code BusinessException(THEME_INVALID_PROFILE, List<FieldError>)}，
 * 经全局异常处理器后形成 {@code Result.fail(code, message, data=List<FieldError>)}。
 * 项目共享的 {@link request} 拦截器会在 reject 时构造 {@link ApiError}，
 * 并将 `code` / `data` / `traceId` 携带到该错误对象上。
 * 因此调用方只需在 `catch (e)` 中调用 {@link parseFieldErrors} 即可拿到结构化的
 * {@link ThemeFieldError} 数组（非 `THEME_INVALID_PROFILE` 时返回空数组）。
 *
 * <h3>接口列表</h3>
 * - {@link getPresets}：列出全部主题预设（含内置与平台级自定义）
 * - {@link getMyProfile}：查询当前用户某作用域 Profile
 * - {@link updateMyProfile}：更新当前用户某作用域 Profile
 * - {@link restore}：将当前用户某作用域 Profile 重置为 PRESET_BEST
 * - {@link getDefault}：查询 PRESET_BEST 在指定作用域的变体（公开接口，未登录可访问）
 * - {@link createPreset}：新增自定义预设（需 `system:theme:manage` 权限）
 * - {@link updatePreset}：修改自定义预设（需 `system:theme:manage` 权限）
 * - {@link deletePreset}：删除自定义预设（需 `system:theme:manage` 权限）
 *
 * @module api/theme
 */
import request, { type ApiError } from '../utils/request';
import type {
    ThemeFieldError,
    ThemePreset,
    ThemeProfile,
    ThemeScope,
} from '@/types/theme';

/**
 * 主题预设新增 / 修改请求体（DTO）。
 *
 * 与后端 {@code ThemePresetUpsertDTO} 字段一一对应：同一预设需提供
 * `profileAdmin` 与 `profileBusiness` 两个作用域变体，以便在不同作用域下
 * 应用同一预设时获得各自的最佳呈现。
 */
export interface ThemePresetUpsertDTO {
    /** 预设展示名（必填，长度 ≤ 64）。 */
    name: string;
    /** 预设描述（可选，长度 ≤ 255）。 */
    description?: string;
    /** 排序权重，列表按升序展示；不传由后端兜底为 0。 */
    sortOrder?: number;
    /** ADMIN 作用域下的 Profile 变体（必填）。 */
    profileAdmin: ThemeProfile;
    /** BUSINESS 作用域下的 Profile 变体（必填）。 */
    profileBusiness: ThemeProfile;
}

/**
 * 从请求层抛出的错误对象中解析后端字段级错误明细。
 *
 * 仅当后端错误码为 `THEME_INVALID_PROFILE` 时（约定为 40010），其
 * `Result.data` 字段会承载 `List<FieldError>`；其余错误码下统一返回空数组。
 *
 * @param error 来自 axios 拦截器的异常对象（{@link ApiError} 或普通 `Error`）
 * @returns 字段级错误数组；无明细时为空数组
 */
export function parseFieldErrors(error: unknown): ThemeFieldError[] {
    if (!error || typeof error !== 'object') {
        return [];
    }
    const data = (error as ApiError).data;
    if (!Array.isArray(data)) {
        return [];
    }
    // 容错：保留满足 FieldError 形状的元素，剔除异常项，避免上层崩溃
    return data.filter((item: unknown): item is ThemeFieldError => {
        if (!item || typeof item !== 'object') return false;
        const candidate = item as Record<string, unknown>;
        return typeof candidate.field === 'string'
            && typeof candidate.code === 'string'
            && typeof candidate.message === 'string';
    });
}

/* ═══════════════════════════════════════════════════════════
   主题预设 - 查询
   ═══════════════════════════════════════════════════════════ */

/**
 * 查询全部主题预设（包含系统内置与平台级自定义），按 `sortOrder` 升序返回。
 *
 * 已登录用户即可访问。
 *
 * @returns 预设列表
 */
export const getPresets = () =>
    request.get<ThemePreset[]>('/theme/presets');

/* ═══════════════════════════════════════════════════════════
   当前用户 Profile
   ═══════════════════════════════════════════════════════════ */

/**
 * 查询当前登录用户在指定作用域下的主题 Profile。
 *
 * 若该用户在该作用域下尚无任何记录，后端会回退返回 `PRESET_BEST`
 * 对应作用域的变体（详见后端 `ThemeService#getMyProfile` 实现）。
 *
 * @param scope 主题作用域（`ADMIN` 或 `BUSINESS`）
 * @returns 当前用户在该作用域下的 Profile
 */
export const getMyProfile = (scope: ThemeScope) =>
    request.get<ThemeProfile>('/theme/me', { params: { scope } });

/**
 * 更新（upsert）当前登录用户在指定作用域下的主题 Profile。
 *
 * 当请求体中的字段不合法（颜色格式 / 枚举值 / 必填等）时，后端将以
 * `THEME_INVALID_PROFILE` 错误码返回结构化字段错误，调用方可通过
 * {@link parseFieldErrors} 在 `catch` 中获取明细并高亮 UI 表单。
 *
 * @param scope 主题作用域
 * @param dto   完整的 ThemeProfile 对象
 * @returns 服务端持久化后的最终 Profile（用于本地回填）
 */
export const updateMyProfile = (scope: ThemeScope, dto: ThemeProfile) =>
    request.put<ThemeProfile>('/theme/me', dto, { params: { scope } });

/**
 * 将当前登录用户在指定作用域下的主题 Profile 一键恢复为 `PRESET_BEST`
 * 对应作用域的变体。
 *
 * @param scope 主题作用域
 * @returns 重置后的 Profile
 */
export const restore = (scope: ThemeScope) =>
    request.post<ThemeProfile>('/theme/restore', null, { params: { scope } });

/**
 * 查询 `PRESET_BEST` 在指定作用域下的变体。
 *
 * <p>
 * 该接口属于平台白名单，未登录用户也可访问，主要用于登录前页面的首屏渲染、
 * 防 FOUC 兜底以及客户端缓存损坏时的恢复。请求层不会附加 `Authorization`
 * 时也不会被 401 拦截器拦截。
 * </p>
 *
 * @param scope 主题作用域
 * @returns `PRESET_BEST` 在该作用域下的 Profile
 */
export const getDefault = (scope: ThemeScope) =>
    request.get<ThemeProfile>('/theme/default', {
        params: { scope },
        // 未登录场景下不应触发 token 刷新与登录页跳转
        _skipAuthRefresh: true,
    } as unknown as Parameters<typeof request.get>[1]);

/* ═══════════════════════════════════════════════════════════
   主题预设 - 受保护的写操作（system:theme:manage）
   ═══════════════════════════════════════════════════════════ */

/**
 * 新增自定义主题预设（仅平台管理员，需 `system:theme:manage` 权限）。
 *
 * @param dto 新增预设请求体
 * @returns 新增后的预设完整 VO
 */
export const createPreset = (dto: ThemePresetUpsertDTO) =>
    request.post<ThemePreset>('/theme/presets', dto);

/**
 * 修改自定义主题预设（仅平台管理员，需 `system:theme:manage` 权限）。
 *
 * <p>
 * 对内置预设（`isBuiltIn=true`，含 `PRESET_BEST`）调用本接口，后端将返回
 * `THEME_BUILTIN_NOT_MUTABLE` 错误码并保持数据库行不变。
 * </p>
 *
 * @param id  预设 ID
 * @param dto 更新预设请求体
 * @returns 更新后的预设完整 VO
 */
export const updatePreset = (id: string, dto: ThemePresetUpsertDTO) =>
    request.put<ThemePreset>(`/theme/presets/${encodeURIComponent(id)}`, dto);

/**
 * 删除自定义主题预设（仅平台管理员，需 `system:theme:manage` 权限）。
 *
 * <p>
 * 对内置预设调用本接口，后端将返回 `THEME_BUILTIN_NOT_MUTABLE`。
 * 删除自定义预设后，后端会自动将所有引用该预设的 `bml_theme_user_setting`
 * 行的 `preset_ref` 字段置空，但保留各自 `profile` 字段不变（解引用机制）。
 * </p>
 *
 * @param id 预设 ID
 */
export const deletePreset = (id: string) =>
    request.delete<void>(`/theme/presets/${encodeURIComponent(id)}`);
