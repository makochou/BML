package com.bml.module.system.theme.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @HexColor} —— 6 位十六进制颜色字符串 JSR-380 校验注解。
 * <p>
 * 用于 {@code ThemeProfile} 中所有颜色字段（{@code primaryColor}、{@code textColor}
 * 等），约束取值必须匹配
 * {@link com.bml.module.system.theme.constant.ThemeConstants#HEX_COLOR_REGEX}
 * （即 {@code ^#[0-9A-Fa-f]{6}$}）。本注解仅做格式校验，
 * <b>不做非空校验</b>：值为 {@code null} 时视为校验通过，
 * 调用方需自行使用 {@link jakarta.validation.constraints.NotNull} 或
 * {@link jakarta.validation.constraints.NotBlank} 强制非空。
 * </p>
 * <p>
 * 该注解可作用于字段（如 DTO 上的属性）与方法参数（如 Controller 中的
 * 单值参数），由 {@link HexColorValidator} 实施实际校验逻辑。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * public class ThemeProfileDTO {
 *     @NotNull(message = "主色不能为空")
 *     @HexColor
 *     private String primaryColor;
 * }
 * }</pre>
 *
 * @author BML Team
 * @see HexColorValidator
 * @see com.bml.module.system.theme.constant.ThemeConstants#HEX_COLOR_REGEX
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HexColorValidator.class)
public @interface HexColor {

    /**
     * 校验失败时的提示消息。
     *
     * @return 默认中文提示，可在使用处覆盖
     */
    String message() default "颜色值必须为合法的 #RRGGBB 6 位十六进制格式";

    /**
     * JSR-380 分组校验所需的 {@code groups()} 元数据。
     *
     * @return 默认空数组（不参与分组）
     */
    Class<?>[] groups() default {};

    /**
     * JSR-380 元数据扩展所需的 {@code payload()}。
     *
     * @return 默认空数组
     */
    Class<? extends Payload>[] payload() default {};
}
