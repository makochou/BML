package com.bml.module.system.theme.entity;

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
 * 主题配置 JSON 载体（共享字段）。
 * <p>
 * 该 POJO 与设计文档 {@code Theme_Profile} 字段表逐字段对齐，作为：
 * <ul>
 *   <li>{@link ThemePreset#getProfileAdmin()} / {@link ThemePreset#getProfileBusiness()}
 *       两个 JSON 列的 Java 载体；</li>
 *   <li>{@link ThemeUserSetting#getProfile()} JSON 列的 Java 载体。</li>
 * </ul>
 * 通过 MyBatis-Plus {@code JacksonTypeHandler} 与数据库 {@code JSON} 列双向转换。
 * </p>
 * <p>
 * <b>字段语义</b>：颜色字段统一采用 {@code #RRGGBB} 6 位十六进制格式（具体校验由
 * {@code ThemeProfileValidator} / {@code @HexColor} 注解承担，本 POJO 不施加约束注解）；
 * 枚举字段在 JSON 中以枚举名称（大写形式）出现，与数据库 JSON 内容、前端 TypeScript
 * 联合类型保持一致；{@code presetRef} 为可选预设引用，自定义预设被删除后由业务层置 {@code null}
 * （参见 R12.AC4，预设删除解引用）。
 * </p>
 * <p>
 * <b>序列化要求</b>：使用 Jackson 序列化时枚举默认输出为 {@code name()}，与 SQL
 * 种子数据中存放的字符串完全一致；不要为本类启用其它枚举序列化策略。
 * </p>
 *
 * @author BML Team
 * @see ThemePreset
 * @see ThemeUserSetting
 */
@Data
@Accessors(chain = true)
@Schema(description = "主题配置 JSON 载体（ThemeProfile 共享字段）")
public class ThemeProfileJson implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主色（{@code #RRGGBB}），驱动 {@code --bml-color-primary} 与 {@code --arcoblue-*} 色阶。 */
    @Schema(description = "主色，#RRGGBB", example = "#165DFF")
    private String primaryColor;

    /** 辅助色（{@code #RRGGBB}），用于次要按钮、强调链接等场景。 */
    @Schema(description = "辅助色，#RRGGBB")
    private String secondaryColor;

    /** 点缀色（{@code #RRGGBB}），用于高亮提示与品牌点缀。 */
    @Schema(description = "点缀色，#RRGGBB")
    private String accentColor;

    /** 成功状态色（{@code #RRGGBB}），如成功提示、通过标识等。 */
    @Schema(description = "成功状态色，#RRGGBB")
    private String successColor;

    /** 警告状态色（{@code #RRGGBB}），如风险提示、警示标识等。 */
    @Schema(description = "警告状态色，#RRGGBB")
    private String warningColor;

    /** 错误状态色（{@code #RRGGBB}），如错误提示、危险操作等。 */
    @Schema(description = "错误状态色，#RRGGBB")
    private String errorColor;

    /** 信息状态色（{@code #RRGGBB}），如普通信息提示、占位标识等。 */
    @Schema(description = "信息状态色，#RRGGBB")
    private String infoColor;

    /** 正文文字色（{@code #RRGGBB}），与 {@link #backgroundColor} 共同决定可读对比度。 */
    @Schema(description = "正文文字色，#RRGGBB")
    private String textColor;

    /** 页面背景色（{@code #RRGGBB}），驱动 {@code --bml-color-bg-*} 派生层级背景。 */
    @Schema(description = "页面背景色，#RRGGBB")
    private String backgroundColor;

    /** 边框色（{@code #RRGGBB}），驱动卡片、表格、表单的分隔线。 */
    @Schema(description = "边框色，#RRGGBB")
    private String borderColor;

    /** 主题明暗模式：{@code LIGHT} / {@code DARK} / {@code AUTO}（跟随系统）。 */
    @Schema(description = "主题明暗模式")
    private ThemeMode mode;

    /** 圆角风格档位：{@code SHARP} / {@code SMALL} / {@code MEDIUM} / {@code LARGE}。 */
    @Schema(description = "圆角风格档位")
    private RadiusStyle radius;

    /** 紧凑度档位：{@code COMPACT} / {@code DEFAULT} / {@code LOOSE}。 */
    @Schema(description = "紧凑度档位")
    private Density density;

    /** 侧边栏风格：{@code LIGHT} / {@code DARK} / {@code TRANSPARENT} / {@code PRIMARY}。 */
    @Schema(description = "侧边栏风格")
    private SidebarStyle sidebarStyle;

    /**
     * 侧边栏折叠态风格。
     * <p>
     * 仅 {@link SidebarStyle#LIGHT} 与 {@link SidebarStyle#DARK} 在折叠态下有合法语义，
     * 进一步约束由 {@code ThemeProfileValidator} 在校验阶段检查。
     * </p>
     */
    @Schema(description = "侧边栏折叠态风格（建议 LIGHT/DARK）")
    private SidebarStyle sidebarCollapsedStyle;

    /** 顶部标题栏风格：{@code LIGHT} / {@code DARK} / {@code PRIMARY} / {@code TRANSPARENT}。 */
    @Schema(description = "顶部标题栏风格")
    private HeaderStyle headerStyle;

    /** 字体大小档位：{@code SMALL} / {@code DEFAULT} / {@code LARGE} / {@code XLARGE}。 */
    @Schema(description = "字体大小档位")
    private FontScale fontScale;

    /**
     * 引用的预设 ID（可空）。
     * <p>
     * 当用户基于某预设衍生自定义 Profile 时记录其来源；自定义预设被删除后，
     * 业务层会将该字段置为 {@code null}，但保留其它字段的具体 Token 值
     * （参见需求 R12.AC4）。
     * </p>
     */
    @Schema(description = "引用的预设 ID（可空，预设删除后置 null）")
    private String presetRef;
}
