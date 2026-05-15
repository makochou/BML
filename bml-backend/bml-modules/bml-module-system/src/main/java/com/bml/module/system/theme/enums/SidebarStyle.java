package com.bml.module.system.theme.enums;

/**
 * 侧边栏风格枚举。
 * <p>
 * 控制侧边栏背景、文字、激活项、分组标题、悬停态、折叠态对应的
 * Design Token 集合，且与顶部栏、内容区互不耦合（切换时仅更新侧边栏相关 Token）。
 * </p>
 * <p>
 * 对应 {@code Theme_Profile.sidebarStyle} 字段。
 * </p>
 *
 * @author BML Team
 */
public enum SidebarStyle {

    /** 浅色侧边栏 — 白底、深色文字，与浅色内容区融合。 */
    LIGHT,

    /** 深色侧边栏 — 深底、浅色文字，呈现强对比的导航区。 */
    DARK,

    /** 透明侧边栏 — 半透明叠层，适配毛玻璃质感（如 PRESET_BEST）。 */
    TRANSPARENT,

    /** 主色侧边栏 — 使用主题主色作为侧边栏背景，强调品牌识别。 */
    PRIMARY;

    /**
     * 根据名称查找枚举值（大小写不敏感），未匹配返回 {@code null}。
     *
     * @param name 待查找的枚举名称
     * @return 匹配到的 {@link SidebarStyle}；输入为 {@code null} 或未匹配时返回 {@code null}
     */
    public static SidebarStyle fromName(String name) {
        if (name == null) {
            return null;
        }
        for (SidebarStyle style : values()) {
            if (style.name().equalsIgnoreCase(name)) {
                return style;
            }
        }
        return null;
    }
}
