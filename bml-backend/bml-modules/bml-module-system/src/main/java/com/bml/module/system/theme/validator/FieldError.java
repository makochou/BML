package com.bml.module.system.theme.validator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 字段级错误明细 POJO。
 * <p>
 * 用于在 {@code Theme_Profile} 全字段校验场景下，将每个非法字段的
 * 名称、错误代码与错误提示封装为结构化条目，最终以
 * {@code List<FieldError>} 形式作为
 * {@link com.bml.core.common.exception.BusinessException#getData()}
 * 携带的附加载荷下发到前端，前端可据此在表单上按字段高亮提示。
 * </p>
 * <p>
 * 与 Spring 自带的 {@code org.springframework.validation.FieldError} 不同，
 * 本类是一个面向 API 序列化的轻量数据载体，仅包含三个字符串字段，
 * 不依赖 Spring 校验上下文，便于在控制器响应、单元测试与前端 TypeScript
 * 类型间保持稳定契约。
 * </p>
 *
 * <h3>字段语义</h3>
 * <ul>
 *   <li>{@link #field}：字段路径，例如 {@code primaryColor}、{@code mode}；</li>
 *   <li>{@link #code}：业务错误代码，例如 {@code REQUIRED}、{@code INVALID_HEX_COLOR}、
 *       {@code INVALID_SIDEBAR_COLLAPSED}，方便前端按代码做国际化或定制提示；</li>
 *   <li>{@link #message}：默认中文提示，便于直接展示。</li>
 * </ul>
 *
 * @author BML Team
 * @see ThemeProfileValidator
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "字段级错误明细")
public class FieldError implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 字段路径（如 {@code primaryColor}）。 */
    @Schema(description = "字段路径", example = "primaryColor")
    private String field;

    /** 业务错误代码（如 {@code INVALID_HEX_COLOR}）。 */
    @Schema(description = "业务错误代码", example = "INVALID_HEX_COLOR")
    private String code;

    /** 默认中文错误提示。 */
    @Schema(description = "默认中文错误提示", example = "颜色值必须为 #RRGGBB 6 位十六进制")
    private String message;
}
