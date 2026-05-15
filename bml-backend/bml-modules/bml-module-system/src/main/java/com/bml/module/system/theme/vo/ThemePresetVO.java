package com.bml.module.system.theme.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 主题预设视图对象。
 * <p>
 * 用于 {@code GET /api/theme/presets}、预设新增 / 修改接口响应等场景，
 * 序列化主题预设的元信息与 {@code ADMIN} / {@code BUSINESS} 两个作用域
 * 的完整 {@link ThemeProfileVO} 变体。
 * </p>
 * <p>
 * 与实体 {@link com.bml.module.system.theme.entity.ThemePreset} 字段一一对应，
 * 通过 {@code ThemeProfileConverter}（MapStruct）完成转换；
 * {@link #isBuiltIn} 与 {@link #isDefault} 由数据库 {@code is_built_in} /
 * {@code is_default} 列直接映射而来，前端据此分别渲染"系统内置"标识、
 * 默认预设徽章。
 * </p>
 *
 * @author BML Team
 * @see ThemeProfileVO
 * @see com.bml.module.system.theme.entity.ThemePreset
 */
@Data
@Accessors(chain = true)
@Schema(description = "主题预设 VO")
public class ThemePresetVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 预设 ID（语义化字符串，如 {@code PRESET_BEST}）。 */
    @Schema(description = "预设 ID", example = "PRESET_BEST")
    private String id;

    /** 预设名称。 */
    @Schema(description = "预设名称")
    private String name;

    /** 预设描述（可空）。 */
    @Schema(description = "预设描述")
    private String description;

    /** 是否系统内置。 */
    @Schema(description = "是否系统内置（true 不可改/不可删）")
    private Boolean isBuiltIn;

    /** 是否系统默认预设（仅 {@code PRESET_BEST} 为 {@code true}）。 */
    @Schema(description = "是否系统默认预设")
    private Boolean isDefault;

    /** 排序权重，数字越小越靠前。 */
    @Schema(description = "排序权重，数字越小越靠前")
    private Integer sortOrder;

    /** {@code ADMIN} 作用域变体（完整 ThemeProfile）。 */
    @Schema(description = "ADMIN 作用域变体")
    private ThemeProfileVO profileAdmin;

    /** {@code BUSINESS} 作用域变体（完整 ThemeProfile）。 */
    @Schema(description = "BUSINESS 作用域变体")
    private ThemeProfileVO profileBusiness;

    /** 创建时间。 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
