package com.bml.module.system.theme.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 主题预设实体。
 * <p>
 * 对应数据表 {@code bml_theme_preset}，承载系统内置预设（如
 * {@code PRESET_BEST}、{@code PRESET_OCEAN}）以及平台级自定义预设。每条预设
 * 同时存储 {@code ADMIN} 与 {@code BUSINESS} 两个作用域的完整 {@code ThemeProfile}
 * 变体（以 {@link ThemeProfileJson} 形式映射 JSON 列）。
 * </p>
 * <p>
 * <b>关键约束</b>：
 * <ul>
 *   <li>{@code id} 为 {@link String} 类型主键；内置预设使用语义 ID（如
 *       {@code PRESET_BEST}），自定义预设由业务层生成并保证全局唯一。</li>
 *   <li>{@link #isBuiltIn} 为 {@code true} 的预设不可被修改或删除，由
 *       {@code ThemeService} 统一拒绝（错误码 {@code THEME_BUILTIN_NOT_MUTABLE}）。</li>
 *   <li>{@link #isDefault} 仅 {@code PRESET_BEST} 为 {@code true}，作为系统默认预设。</li>
 *   <li>{@link #profileAdmin} / {@link #profileBusiness} 通过
 *       {@link JacksonTypeHandler} 与数据库 {@code JSON} 列双向转换；启用
 *       {@link TableName#autoResultMap()} 后 MyBatis-Plus 自动生成 ResultMap。</li>
 * </ul>
 * </p>
 *
 * @author BML Team
 * @see ThemeProfileJson
 * @see ThemeUserSetting
 */
@Data
@Accessors(chain = true)
@TableName(value = "bml_theme_preset", autoResultMap = true)
@Schema(description = "主题预设（内置 + 平台级自定义）")
public class ThemePreset implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 预设 ID（内置预设使用语义 ID，例如 {@code PRESET_BEST}）。 */
    @Schema(description = "预设 ID（语义化字符串）", example = "PRESET_BEST")
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /** 预设名称（人类可读）。 */
    @Schema(description = "预设名称")
    private String name;

    /** 预设描述（可空）。 */
    @Schema(description = "预设描述")
    private String description;

    /** 是否系统内置：{@code true} 表示不可修改/删除。 */
    @Schema(description = "是否系统内置（true 不可改/不可删）")
    private Boolean isBuiltIn;

    /** 是否系统默认预设：仅 {@code PRESET_BEST} 为 {@code true}。 */
    @Schema(description = "是否系统默认预设（仅 PRESET_BEST=true）")
    private Boolean isDefault;

    /** 排序权重，数字越小越靠前。 */
    @Schema(description = "排序权重，数字越小越靠前")
    private Integer sortOrder;

    /** {@code ADMIN} 作用域变体的完整 ThemeProfile（JSON 列）。 */
    @Schema(description = "ADMIN 作用域变体（JSON）")
    @TableField(value = "profile_admin", typeHandler = JacksonTypeHandler.class)
    private ThemeProfileJson profileAdmin;

    /** {@code BUSINESS} 作用域变体的完整 ThemeProfile（JSON 列）。 */
    @Schema(description = "BUSINESS 作用域变体（JSON）")
    @TableField(value = "profile_business", typeHandler = JacksonTypeHandler.class)
    private ThemeProfileJson profileBusiness;

    /** 创建时间（数据库 {@code DEFAULT CURRENT_TIMESTAMP}）。 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间（数据库 {@code ON UPDATE CURRENT_TIMESTAMP}）。 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
