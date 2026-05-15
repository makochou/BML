package com.bml.module.system.theme.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 主题预设新增 / 修改数据传输对象。
 * <p>
 * 用于受保护接口 {@code POST /api/theme/presets} 与 {@code PUT /api/theme/presets/{id}}
 * 的请求体反序列化目标，承载平台级自定义预设的新建与修改字段。系统内置预设
 * （{@code is_built_in=true}，如 {@link com.bml.module.system.theme.constant.ThemeConstants#PRESET_BEST_ID}）
 * 不允许通过本 DTO 修改，由 {@code ThemeService} 在业务层抛出
 * {@code THEME_BUILTIN_NOT_MUTABLE} 拒绝。
 * </p>
 * <p>
 * <b>字段构成</b>：
 * <ul>
 *   <li>{@link #name} — 预设名称（必填，长度 1..64）；</li>
 *   <li>{@link #description} — 预设描述（可选，最长 255 字符）；</li>
 *   <li>{@link #sortOrder} — 排序权重（可选，缺省由业务层置 {@code 0}）；</li>
 *   <li>{@link #profileAdmin} / {@link #profileBusiness} — 两个作用域的完整
 *       {@link ThemeProfileDTO}，必填且需通过级联校验
 *       （由 {@code @Valid} 触发字段级 JSR-380 校验）。</li>
 * </ul>
 * </p>
 * <p>
 * <b>JSR-380 与业务校验的分工</b>：本 DTO 注解负责"基本格式 / 必填"校验，
 * 通过控制器层的 {@code @Validated} 触发；嵌套 {@link ThemeProfileDTO} 的
 * 颜色 / 枚举字段由其自身注解承担。{@code ThemeProfileValidator} 在 service
 * 入口对两个 Profile 执行全字段收集校验，确保非法字段全量返回（参见 R4.AC8 / R7.AC4）。
 * </p>
 *
 * @author BML Team
 * @see ThemeProfileDTO
 * @see com.bml.module.system.theme.validator.ThemeProfileValidator
 */
@Data
@Accessors(chain = true)
@Schema(description = "主题预设新增 / 修改 DTO")
public class ThemePresetUpsertDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 预设名称（必填）。
     * <p>
     * 长度限制 1..64 字符，与数据库 {@code bml_theme_preset.name} 列保持一致；
     * 内容由前端用户输入，建议在 UI 层做去前后空白处理。
     * </p>
     */
    @Schema(description = "预设名称", example = "海洋蓝")
    @NotBlank(message = "预设名称不能为空")
    @Size(max = 64, message = "预设名称长度不能超过 64 字符")
    private String name;

    /**
     * 预设描述（可选）。
     * <p>
     * 长度限制 0..255 字符，与数据库 {@code bml_theme_preset.description} 列对齐。
     * </p>
     */
    @Schema(description = "预设描述")
    @Size(max = 255, message = "预设描述长度不能超过 255 字符")
    private String description;

    /**
     * 排序权重（可选）。
     * <p>
     * 数字越小越靠前，缺省由业务层置 {@code 0}；与数据库
     * {@code bml_theme_preset.sort_order} 列对齐。
     * </p>
     */
    @Schema(description = "排序权重，数字越小越靠前", example = "100")
    private Integer sortOrder;

    /**
     * {@code ADMIN} 作用域变体（必填）。
     * <p>
     * 通过 {@link Valid} 启用字段级级联校验，确保 10 个颜色字段格式合法、
     * 7 个枚举字段非空。
     * </p>
     */
    @Schema(description = "ADMIN 作用域变体（完整 ThemeProfile）")
    @NotNull(message = "ADMIN 作用域变体不能为空")
    @Valid
    private ThemeProfileDTO profileAdmin;

    /**
     * {@code BUSINESS} 作用域变体（必填）。
     * <p>
     * 通过 {@link Valid} 启用字段级级联校验。
     * </p>
     */
    @Schema(description = "BUSINESS 作用域变体（完整 ThemeProfile）")
    @NotNull(message = "BUSINESS 作用域变体不能为空")
    @Valid
    private ThemeProfileDTO profileBusiness;
}
