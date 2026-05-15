package com.bml.module.system.theme.validator;

import com.bml.module.system.theme.constant.ThemeConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * {@link HexColor} 注解的 JSR-380 校验实现。
 * <p>
 * 校验目标字符串是否符合
 * {@link ThemeConstants#HEX_COLOR_REGEX}（{@code ^#[0-9A-Fa-f]{6}$}），
 * 即以 {@code #} 开头、紧跟 6 位十六进制字符的颜色值。
 * </p>
 * <p>
 * <b>{@code null} 处理策略</b>：与 Bean Validation 规范保持一致，
 * 当被校验值为 {@code null} 时返回 {@code true}，将"是否非空"语义交由
 * {@link jakarta.validation.constraints.NotNull} / {@link jakarta.validation.constraints.NotBlank}
 * 等专门注解处理；这样可让本注解在可选字段上自然组合使用。
 * </p>
 * <p>
 * 正则在类加载阶段编译为 {@link Pattern} 单例并通过 {@code static final} 持有，
 * 避免每次校验重复编译，确保高频校验场景（如 {@code ThemeProfileValidator}
 * 全字段扫描）的性能可控。
 * </p>
 *
 * @author BML Team
 * @see HexColor
 */
public class HexColorValidator implements ConstraintValidator<HexColor, String> {

    /**
     * 预编译的颜色匹配正则表达式。
     * <p>
     * 引用 {@link ThemeConstants#HEX_COLOR_REGEX} 作为唯一来源，避免
     * 在校验器与常量类之间出现正则漂移。
     * </p>
     */
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile(ThemeConstants.HEX_COLOR_REGEX);

    /**
     * 执行校验逻辑。
     *
     * @param value   待校验的颜色字符串，允许为 {@code null}
     * @param context JSR-380 校验上下文（本实现不使用，保留默认行为）
     * @return 当 {@code value} 为 {@code null} 或匹配 {@link #HEX_COLOR_PATTERN} 时返回 {@code true}；否则 {@code false}
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            // 允许 null，由 @NotNull / @NotBlank 控制是否必填
            return true;
        }
        return HEX_COLOR_PATTERN.matcher(value).matches();
    }
}
