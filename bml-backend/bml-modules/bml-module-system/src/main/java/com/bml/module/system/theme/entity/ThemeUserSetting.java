package com.bml.module.system.theme.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.bml.module.system.theme.enums.ThemeScope;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户主题设置实体。
 * <p>
 * 对应数据表 {@code bml_theme_user_setting}，每个用户在每个 {@link ThemeScope}
 * 下唯一保存一条记录（数据库唯一键 {@code uk_user_scope (user_id, scope)}）。
 * 完整的 {@code ThemeProfile} 字段以 {@link ThemeProfileJson} 形式映射到
 * {@code profile} JSON 列；{@link #presetRef} 记录该 Profile 来源预设，
 * 自定义预设被删除后由业务层置 {@code null}（参见需求 R12.AC4）。
 * </p>
 * <p>
 * <b>关键约束</b>：
 * <ul>
 *   <li>{@code id} 为自增主键（{@link IdType#AUTO}），与数据库 DDL
 *       {@code AUTO_INCREMENT} 对齐。</li>
 *   <li>{@link #scope} 在数据库中以字符串形式存储（{@code ADMIN} / {@code BUSINESS}），
 *       通过 MyBatis-Plus 默认枚举处理由 {@link ThemeScope#name()} 转换。</li>
 *   <li>{@link #profile} 通过 {@link JacksonTypeHandler} 与 JSON 列双向转换；
 *       启用 {@link TableName#autoResultMap()} 后 MyBatis-Plus 自动生成 ResultMap。</li>
 * </ul>
 * </p>
 *
 * @author BML Team
 * @see ThemeProfileJson
 * @see ThemePreset
 */
@Data
@Accessors(chain = true)
@TableName(value = "bml_theme_user_setting", autoResultMap = true)
@Schema(description = "用户主题设置（每用户每作用域唯一）")
public class ThemeUserSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 ID（数据库自增）。 */
    @Schema(description = "主键 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 ID（关联 {@code sys_user.id}）。 */
    @Schema(description = "用户 ID")
    private Long userId;

    /** 作用域：{@link ThemeScope#ADMIN} 或 {@link ThemeScope#BUSINESS}。 */
    @Schema(description = "作用域：ADMIN | BUSINESS")
    private ThemeScope scope;

    /**
     * 引用的预设 ID（可空）。
     * <p>
     * 指向 {@link ThemePreset#getId()}；当被引用的自定义预设被删除时，业务层
     * 将该字段置 {@code null}，但 {@link #profile} 中的具体 Token 值保持不变。
     * </p>
     */
    @Schema(description = "引用的预设 ID（解引用后置 null）")
    private String presetRef;

    /** 完整 ThemeProfile 字段（JSON 列）。 */
    @Schema(description = "完整 ThemeProfile（JSON）")
    @TableField(value = "profile", typeHandler = JacksonTypeHandler.class)
    private ThemeProfileJson profile;

    /** 更新时间（数据库 {@code ON UPDATE CURRENT_TIMESTAMP}）。 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
