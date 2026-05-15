package com.bml.module.system.theme.enums;

/**
 * 顶部标题栏 / 导航栏风格枚举。
 * <p>
 * 控制顶部标题栏背景、文字、图标、用户菜单等区域的 Design Token，
 * 与侧边栏、内容区相互独立。
 * </p>
 * <p>
 * 对应 {@code Theme_Profile.headerStyle} 字段。
 * </p>
 *
 * @author BML Team
 */
public enum HeaderStyle {

    /** 浅色顶栏 — 白底、深色文字，与浅色内容区一致。 */
    LIGHT,

    /** 深色顶栏 — 深底、浅色文字，呈现稳重的导航区。 */
    DARK,

    /** 主色顶栏 — 使用主题主色作为顶栏背景，强调品牌识别。 */
    PRIMARY,

    /** 透明顶栏 — 半透明叠层，适配沉浸式背景或毛玻璃质感。 */
    TRANSPARENT;

    /**
     * 根据名称查找枚举值（大小写不敏感），未匹配返回 {@code null}。
     *
     * @param name 待查找的枚举名称
     * @return 匹配到的 {@link HeaderStyle}；输入为 {@code null} 或未匹配时返回 {@code null}
     */
    public static HeaderStyle fromName(String name) {
        if (name == null) {
            return null;
        }
        for (HeaderStyle style : values()) {
            if (style.name().equalsIgnoreCase(name)) {
                return style;
            }
        }
        return null;
    }
}
