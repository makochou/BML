package com.bml.module.system.theme.vo;

import com.bml.module.system.theme.enums.Density;
import com.bml.module.system.theme.enums.FontScale;
import com.bml.module.system.theme.enums.HeaderStyle;
import com.bml.module.system.theme.enums.RadiusStyle;
import com.bml.module.system.theme.enums.SidebarStyle;
import com.bml.module.system.theme.enums.ThemeMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 主题配置视图对象（{@code ThemeProfile}）。
 * <p>
 * 用于 {@code GET /api/theme/me}、{@code POST /api/theme/restore}、
 * {@code GET /api/theme/default} 与预设接口的响应数据序列化目标，字段与
 * {@link com.bml.module.system.theme.dto.ThemeProfileDTO} 完全对应，但
 * <b>不携带任何 JSR-380 注解</b> —— 视图对象仅负责对外输出，校验工作由
 * 入口侧 DTO 与 {@code ThemeProfileValidator} 承担。
 * </p>
 * <p>
 * 字段语义与设计文档 {@code design.md → ThemeProfile} 表完全一致；颜色字段
 * 输出格式始终为 {@code #RRGGBB} 6 位十六进制，枚举字段以大写名称序列化，
 * 与前端 TypeScript 联合类型保持同形。
 * </p>
 *
 * @author BML Team
 * @see com.bml.module.system.theme.dto.ThemeProfileDTO
 * @see com.bml.module.system.theme.entity.ThemeProfileJson
 */
@Data
@Accessors(chain = true)
@Schema(description = "主题配置 VO（ThemeProfile）")
public class ThemeProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主色（{@code #RRGGBB}）。 */
    @Schema(description = "主色，#RRGGBB", example = "#165DFF")
    private String primaryColor;

    /** 辅助色（{@code #RRGGBB}）。 */
    @Schema(description = "辅助色，#RRGGBB")
    private String secondaryColor;

    /** 点缀色（{@code #RRGGBB}）。 */
    @Schema(description = "点缀色，#RRGGBB")
    private String accentColor;

    /** 成功状态色（{@code #RRGGBB}）。 */
    @Schema(description = "成功状态色，#RRGGBB")
    private String successColor;

    /** 警告状态色（{@code #RRGGBB}）。 */
    @Schema(description = "警告状态色,#RRGGBB")
    private String warningColor;

    /** 错误状态色（{@code #RRGGBB}）。 */
    @Schema(description = "错误状态色，#RRGGBB")
    private String errorColor;

    /** 信息状态色（{@code #RRGGBB}）。 */
    @Schema(description = "信息状态色，#RRGGBB")
    private String infoColor;

    /** 正文文字色（{@code #RRGGBB}）。 */
    @Schema(description = "正文文字色，#RRGGBB")
    private String textColor;

    /** 页面背景色（{@code #RRGGBB}）。 */
    @Schema(description = "页面背景色，#RRGGBB")
    private String backgroundColor;

    /** 边框色（{@code #RRGGBB}）。 */
    @Schema(description = "边框色，#RRGGBB")
    private String borderColor;

    /** 主题明暗模式：{@code LIGHT} / {@code DARK} / {@code AUTO}。 */
    @Schema(description = "主题明暗模式")
    private ThemeMode mode;

    /** 圆角风格档位。 */
    @Schema(description = "圆角风格档位")
    private RadiusStyle radius;

    /** 紧凑度档位。 */
    @Schema(description = "紧凑度档位")
    private Density density;

    /** 侧边栏风格。 */
    @Schema(description = "侧边栏风格")
    private SidebarStyle sidebarStyle;

    /** 侧边栏折叠态风格（业务上仅使用 {@code LIGHT}/{@code DARK}）。 */
    @Schema(description = "侧边栏折叠态风格（业务上仅 LIGHT/DARK）")
    private SidebarStyle sidebarCollapsedStyle;

    /** 顶部标题栏风格。 */
    @Schema(description = "顶部标题栏风格")
    private HeaderStyle headerStyle;

    /** 字体大小档位。 */
    @Schema(description = "字体大小档位")
    private FontScale fontScale;

    /**
     * 引用的预设 ID（可空）。
     * <p>
     * 自定义预设被删除后，业务层将该字段置 {@code null}，但 Profile 其余
     * Token 字段保持原值。
     * </p>
     */
    @Schema(description = "引用的预设 ID（可空）")
    private String presetRef;
}
