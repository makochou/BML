package com.bml.module.system.theme.validator;

import com.bml.core.common.exception.BusinessException;
import com.bml.module.system.theme.constant.ThemeConstants;
import com.bml.module.system.theme.dto.ThemeProfileDTO;
import com.bml.module.system.theme.enums.SidebarStyle;
import com.bml.module.system.theme.enums.ThemeErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 主题配置 {@link ThemeProfileDTO} 业务级校验器。
 *
 * <h3>核心语义：全量字段收集</h3>
 * <p>
 * 与 JSR-380 默认的"快速失败"行为不同，本校验器一次性遍历 Profile 的全部字段，
 * 将所有非法项汇总到 {@link FieldError} 列表后再决定是否抛出异常；这样可以
 * 满足 R4.AC8 / R7.4：错误响应中必须包含全部非法字段，而不会因首字段失败提前
 * 返回（参见设计文档 Property 6 — {@code P_VALIDATE_ALL}）。
 * </p>
 *
 * <h3>校验规则</h3>
 * <ul>
 *   <li>10 个颜色字段：必填 + 必须匹配
 *       {@link ThemeConstants#HEX_COLOR_REGEX}（{@code ^#[0-9A-Fa-f]{6}$}）；</li>
 *   <li>7 个枚举字段：必填（{@code mode/radius/density/sidebarStyle/
 *       sidebarCollapsedStyle/headerStyle/fontScale}）；</li>
 *   <li>{@code sidebarCollapsedStyle} 业务规则：仅允许 {@link SidebarStyle#LIGHT}
 *       与 {@link SidebarStyle#DARK}；其它取值即使非空也视为非法。</li>
 * </ul>
 *
 * <h3>错误抛出</h3>
 * <p>
 * 校验失败时抛出
 * {@code BusinessException(ThemeErrorCode.THEME_INVALID_PROFILE, List<FieldError>)}，
 * {@link com.bml.core.framework.exception.GlobalExceptionHandler}
 * 会将携带的字段列表写入 {@code Result.data}，前端可按字段在面板上高亮提示。
 * </p>
 *
 * <h3>调用约定</h3>
 * <p>
 * 控制器层 JSR-380 注解会先于本校验器触发并以 {@code Result.badRequest}
 * 返回首条非法消息；本校验器作为 service 入口的 <em>第二道关卡</em>，
 * 主要用于：
 * <ul>
 *   <li>JSR-380 注解被绕过的内部调用（如 service 直接调用 {@code upsertMyProfile}）；</li>
 *   <li>需要"全量返回"语义的接口（PUT 主题配置、新增 / 修改预设）。</li>
 * </ul>
 * 当传入的 DTO 为 {@code null} 时直接返回，避免空指针；该场景应由调用方的
 * {@code @NotNull} 或控制器层 {@code @Validated @RequestBody} 保证。
 * </p>
 *
 * @author BML Team
 */
@Component
public class ThemeProfileValidator {

    /** 业务错误码：必填字段缺失。 */
    private static final String CODE_REQUIRED = "REQUIRED";

    /** 业务错误码：颜色格式非法（非 {@code #RRGGBB}）。 */
    private static final String CODE_INVALID_HEX_COLOR = "INVALID_HEX_COLOR";

    /** 业务错误码：侧边栏折叠态风格非法（不在 {@code LIGHT/DARK}）。 */
    private static final String CODE_INVALID_SIDEBAR_COLLAPSED = "INVALID_SIDEBAR_COLLAPSED";

    /** 默认必填提示。 */
    private static final String MSG_REQUIRED = "必填字段";

    /** 默认颜色格式错误提示。 */
    private static final String MSG_INVALID_HEX_COLOR = "颜色值必须为 #RRGGBB 6 位十六进制";

    /** 默认折叠态错误提示。 */
    private static final String MSG_INVALID_SIDEBAR_COLLAPSED = "侧边栏折叠态仅支持 LIGHT / DARK";

    /**
     * 颜色匹配正则的预编译实例。
     * <p>
     * 引用 {@link ThemeConstants#HEX_COLOR_REGEX} 作为唯一来源，避免正则在多处复制造成漂移；
     * 通过 {@code static final} 持有以减少重复编译开销。
     * </p>
     */
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile(ThemeConstants.HEX_COLOR_REGEX);

    /**
     * 执行全量字段收集校验。
     * <p>
     * 算法概述：
     * <ol>
     *   <li>初始化空的 {@code errors} 列表；</li>
     *   <li>顺序遍历全部 10 个颜色字段，每个字段独立判定（缺失或非法仅记录到列表）；</li>
     *   <li>顺序遍历全部 7 个枚举字段，缺失即记录；</li>
     *   <li>对 {@code sidebarCollapsedStyle} 追加业务子规则校验；</li>
     *   <li>若 {@code errors} 非空则一次性抛出
     *       {@link BusinessException}，否则正常返回。</li>
     * </ol>
     * </p>
     *
     * @param dto 待校验的主题配置 DTO；当为 {@code null} 时直接返回
     * @throws BusinessException 当存在任何非法字段时抛出，错误码
     *                           {@link ThemeErrorCode#THEME_INVALID_PROFILE}，
     *                           载荷为完整的 {@code List<FieldError>}
     */
    public void validate(ThemeProfileDTO dto) {
        if (dto == null) {
            // 入参为 null 时由 JSR-380 @NotNull 或上层兜底，此处直接返回，避免在
            // 空对象上访问 getter 触发 NPE 而吞掉真正的错误信息。
            return;
        }

        List<FieldError> errors = new ArrayList<>();

        // ── 颜色字段：10 个 ────────────────────────────────────────────────
        validateHexColor(dto.getPrimaryColor(), "primaryColor", errors);
        validateHexColor(dto.getSecondaryColor(), "secondaryColor", errors);
        validateHexColor(dto.getAccentColor(), "accentColor", errors);
        validateHexColor(dto.getSuccessColor(), "successColor", errors);
        validateHexColor(dto.getWarningColor(), "warningColor", errors);
        validateHexColor(dto.getErrorColor(), "errorColor", errors);
        validateHexColor(dto.getInfoColor(), "infoColor", errors);
        validateHexColor(dto.getTextColor(), "textColor", errors);
        validateHexColor(dto.getBackgroundColor(), "backgroundColor", errors);
        validateHexColor(dto.getBorderColor(), "borderColor", errors);

        // ── 枚举字段：7 个 ────────────────────────────────────────────────
        validateRequired(dto.getMode(), "mode", errors);
        validateRequired(dto.getRadius(), "radius", errors);
        validateRequired(dto.getDensity(), "density", errors);
        validateRequired(dto.getSidebarStyle(), "sidebarStyle", errors);
        validateRequired(dto.getSidebarCollapsedStyle(), "sidebarCollapsedStyle", errors);
        validateRequired(dto.getHeaderStyle(), "headerStyle", errors);
        validateRequired(dto.getFontScale(), "fontScale", errors);

        // ── 业务子规则：折叠态仅允许 LIGHT/DARK ────────────────────────────
        SidebarStyle collapsed = dto.getSidebarCollapsedStyle();
        if (collapsed != null && collapsed != SidebarStyle.LIGHT && collapsed != SidebarStyle.DARK) {
            errors.add(new FieldError(
                    "sidebarCollapsedStyle",
                    CODE_INVALID_SIDEBAR_COLLAPSED,
                    MSG_INVALID_SIDEBAR_COLLAPSED));
        }

        // 全部字段遍历完成后再决定是否抛出，确保非法字段全量返回
        if (!errors.isEmpty()) {
            throw new BusinessException(ThemeErrorCode.THEME_INVALID_PROFILE, errors);
        }
    }

    /**
     * 校验单个颜色字段：必填 + 6 位十六进制格式。
     *
     * @param value  字段当前值
     * @param field  字段名（用于错误条目）
     * @param errors 错误累积列表（{@code in/out} 参数）
     */
    private void validateHexColor(String value, String field, List<FieldError> errors) {
        if (value == null) {
            errors.add(new FieldError(field, CODE_REQUIRED, MSG_REQUIRED));
            return;
        }
        if (!HEX_COLOR_PATTERN.matcher(value).matches()) {
            errors.add(new FieldError(field, CODE_INVALID_HEX_COLOR, MSG_INVALID_HEX_COLOR));
        }
    }

    /**
     * 校验单个引用类型字段（通常为枚举）必填。
     *
     * @param value  字段当前值
     * @param field  字段名
     * @param errors 错误累积列表（{@code in/out} 参数）
     */
    private void validateRequired(Object value, String field, List<FieldError> errors) {
        if (value == null) {
            errors.add(new FieldError(field, CODE_REQUIRED, MSG_REQUIRED));
        }
    }
}
