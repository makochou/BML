package com.bml.module.system.theme.enums;

import com.bml.core.common.result.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 主题模块错误码定义。
 *
 * <p>实现 {@link ErrorCode} 接口，与平台统一异常 {@code BusinessException} 与
 * 统一响应 {@code Result<T>} 配合使用。错误码遵循设计文档约定的分段：</p>
 * <ul>
 *     <li>{@code 40_010 / 40_011}：客户端请求参数非法（对齐 HTTP 400 语义）；</li>
 *     <li>{@code 40_310}：客户端请求被拒绝（对齐 HTTP 403 语义，针对内置预设保护）；</li>
 *     <li>{@code 40_410}：资源不存在（对齐 HTTP 404 语义）；</li>
 *     <li>{@code 50_010}：服务端持久化失败（对齐 HTTP 500 语义）。</li>
 * </ul>
 *
 * <p>错误码与设计文档 {@code design.md → ThemeErrorCode} 章节一一对应，
 * 修改前请同步更新该文档与 {@code ThemeErrorCodeTest} 测试。</p>
 *
 * @author BML Team
 */
@Getter
@AllArgsConstructor
public enum ThemeErrorCode implements ErrorCode {

    /**
     * 主题配置存在非法字段。
     * <p>
     * 在 {@code Theme_Profile} 提交（{@code PUT /api/theme/me}）或预设新增 / 修改
     * （{@code POST/PUT /api/theme/presets}）阶段触发；错误响应的 {@code data}
     * 字段必须返回全部非法字段（{@code List<FieldError>}），不允许首字段失败即提前返回。
     * </p>
     */
    THEME_INVALID_PROFILE(40_010, "主题配置存在非法字段"),

    /**
     * 非法作用域参数。
     * <p>
     * 当请求中的 {@code scope} 既不是 {@code ADMIN} 也不是 {@code BUSINESS}（或缺失）时返回。
     * 由 JSR-380 校验或控制器层兜底抛出。
     * </p>
     */
    THEME_SCOPE_INVALID(40_011, "非法作用域参数"),

    /**
     * 主题预设不存在。
     * <p>
     * 当根据 {@code presetId} 查询、修改或删除主题预设而未命中时返回。
     * </p>
     */
    THEME_PRESET_NOT_FOUND(40_410, "主题预设不存在"),

    /**
     * 系统内置预设不可修改或删除。
     * <p>
     * 当尝试对 {@code is_built_in=true} 的预设（包含 {@code PRESET_BEST}）执行
     * {@code PUT} 或 {@code DELETE} 时返回；保证内置预设在数据库中保持原样。
     * </p>
     */
    THEME_BUILTIN_NOT_MUTABLE(40_310, "系统内置预设不可修改或删除"),

    /**
     * 主题持久化失败。
     * <p>
     * 当 {@code bml_theme_user_setting} 或 {@code bml_theme_preset} 的写入操作
     * 因数据库异常或 JSON 序列化失败而无法完成时返回。
     * </p>
     */
    THEME_PERSIST_FAILED(50_010, "主题持久化失败");

    /** 业务状态码 */
    private final int code;
    /** 可读错误消息 */
    private final String message;
}
