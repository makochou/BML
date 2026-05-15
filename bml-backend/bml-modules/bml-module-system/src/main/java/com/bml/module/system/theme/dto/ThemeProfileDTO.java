package com.bml.module.system.theme.dto;

import com.bml.module.system.theme.enums.Density;
import com.bml.module.system.theme.enums.FontScale;
import com.bml.module.system.theme.enums.HeaderStyle;
import com.bml.module.system.theme.enums.RadiusStyle;
import com.bml.module.system.theme.enums.SidebarStyle;
import com.bml.module.system.theme.enums.ThemeMode;
import com.bml.module.system.theme.validator.HexColor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 主题配置数据传输对象（{@code ThemeProfile}）。
 * <p>
 * 用于 {@code PUT /api/theme/me} 与预设新增 / 修改接口的请求体反序列化目标，
 * 字段与设计文档 {@code design.md → ThemeProfile} 表逐项对齐：
 * <ul>
 *   <li>10 个颜色字段全部使用 {@link HexColor} + {@link NotNull} 双重约束；</li>
 *   <li>7 个枚举字段（{@code mode/radius/density/sidebarStyle/sidebarCollapsedStyle/headerStyle/fontScale}）
 *       使用 {@link NotNull} 约束，进一步语义约束（如 {@code sidebarCollapsedStyle}
 *       仅允许 {@code LIGHT/DARK}）由 {@code ThemeProfileValidator} 在 service 入口
 *       全字段校验阶段实施；</li>
 *   <li>{@code presetRef} 为可空字段，记录用户基于哪个预设衍生当前 Profile。</li>
 * </ul>
 * </p>
 * <p>
 * <b>JSR-380 与业务校验的分工</b>：本 DTO 上的注解承担"基本格式 / 必填"校验，
 * 在控制器层通过 {@code @Validated} 触发；{@code ThemeProfileValidator} 则负责
 * <em>全量字段收集</em> 语义（一次扫描所有字段、一次性返回全部非法字段），
 * 二者互补，共同保证错误响应的信息完整度。
 * </p>
 *
 * @author BML Team
 * @see com.bml.module.system.theme.validator.ThemeProfileValidator
 */
@Data
@Accessors(chain = true)
@Schema(description = "主题配置 DTO（ThemeProfile）")
public class ThemeProfileDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主色（{@code #RRGGBB}）。 */
    @Schema(description = "主色，#RRGGBB", example = "#165DFF")
    @NotNull(message = "主色不能为空")
    @HexColor
    private String primaryColor;

    /** 辅助色（{@code #RRGGBB}）。 */
    @Schema(description = "辅助色，#RRGGBB")
    @NotNull(message = "辅助色不能为空")
    @HexColor
    private String secondaryColor;

    /** 点缀色（{@code #RRGGBB}）。 */
    @Schema(description = "点缀色，#RRGGBB")
    @NotNull(message = "点缀色不能为空")
    @HexColor
    private String accentColor;

    /** 成功状态色（{@code #RRGGBB}）。 */
    @Schema(description = "成功状态色，#RRGGBB")
    @NotNull(message = "成功状态色不能为空")
    @HexColor
    private String successColor;

    /** 警告状态色（{@code #RRGGBB}）。 */
    @Schema(description = "警告状态色，#RRGGBB")
    @NotNull(message = "警告状态色不能为空")
    @HexColor
    private String warningColor;

    /** 错误状态色（{@code #RRGGBB}）。 */
    @Schema(description = "错误状态色，#RRGGBB")
    @NotNull(message = "错误状态色不能为空")
    @HexColor
    private String errorColor;

    /** 信息状态色（{@code #RRGGBB}）。 */
    @Schema(description = "信息状态色，#RRGGBB")
    @NotNull(message = "信息状态色不能为空")
    @HexColor
    private String infoColor;

    /** 正文文字色（{@code #RRGGBB}）。 */
    @Schema(description = "正文文字色，#RRGGBB")
    @NotNull(message = "文字色不能为空")
    @HexColor
    private String textColor;

    /** 页面背景色（{@code #RRGGBB}）。 */
    @Schema(description = "页面背景色，#RRGGBB")
    @NotNull(message = "背景色不能为空")
    @HexColor
    private String backgroundColor;

    /** 边框色（{@code #RRGGBB}）。 */
    @Schema(description = "边框色，#RRGGBB")
    @NotNull(message = "边框色不能为空")
    @HexColor
    private String borderColor;

    /** 主题明暗模式：{@code LIGHT} / {@code DARK} / {@code AUTO}。 */
    @Schema(description = "主题明暗模式")
    @NotNull(message = "主题模式不能为空")
    private ThemeMode mode;

    /** 圆角风格档位。 */
    @Schema(description = "圆角风格档位")
    @NotNull(message = "圆角风格不能为空")
    private RadiusStyle radius;

    /** 紧凑度档位。 */
    @Schema(description = "紧凑度档位")
    @NotNull(message = "紧凑度不能为空")
    private Density density;

    /** 侧边栏风格。 */
    @Schema(description = "侧边栏风格")
    @NotNull(message = "侧边栏风格不能为空")
    private SidebarStyle sidebarStyle;

    /**
     * 侧边栏折叠态风格。
     * <p>
     * JSR-380 仅校验非空；进一步约束（仅允许 {@code LIGHT/DARK}）
     * 由 {@code ThemeProfileValidator} 在业务层校验。
     * </p>
     */
    @Schema(description = "侧边栏折叠态风格（业务上仅允许 LIGHT/DARK）")
    @NotNull(message = "侧边栏折叠态风格不能为空")
    private SidebarStyle sidebarCollapsedStyle;

    /** 顶部标题栏风格。 */
    @Schema(description = "顶部标题栏风格")
    @NotNull(message = "顶部栏风格不能为空")
    private HeaderStyle headerStyle;

    /** 字体大小档位。 */
    @Schema(description = "字体大小档位")
    @NotNull(message = "字体档位不能为空")
    private FontScale fontScale;

    /**
     * 引用的预设 ID（可空）。
     * <p>
     * 表示当前 Profile 基于哪个预设衍生；自定义预设被删除后由业务层置 {@code null}。
     * </p>
     */
    @Schema(description = "引用的预设 ID（可空）")
    private String presetRef;
}
