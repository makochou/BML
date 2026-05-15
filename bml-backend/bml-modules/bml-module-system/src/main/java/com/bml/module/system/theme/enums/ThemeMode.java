package com.bml.module.system.theme.enums;

/**
 * 主题明暗模式枚举。
 * <p>
 * 控制 {@code Theme_Engine} 在亮色变体、暗色变体与跟随系统之间切换。
 * 当取值为 {@link #AUTO} 时，前端会订阅浏览器
 * {@code prefers-color-scheme} 媒体查询，并在系统明暗模式变更时
 * 自动在 {@link #LIGHT} / {@link #DARK} 之间切换。
 * </p>
 * <p>
 * 该枚举对应 {@code Theme_Profile.mode} 字段，并最终映射为
 * {@code <body>} 元素的 {@code arco-theme} 属性。
 * </p>
 *
 * @author BML Team
 */
public enum ThemeMode {

    /** 亮色模式 — 使用 PRESET_BEST 亮色变体或对应自定义亮色 Token。 */
    LIGHT,

    /** 暗色模式 — 使用 PRESET_BEST 暗色变体或对应自定义暗色 Token。 */
    DARK,

    /**
     * 跟随系统 — 根据浏览器 {@code prefers-color-scheme} 自动选择
     * {@link #LIGHT} 或 {@link #DARK} 变体。
     */
    AUTO;

    /**
     * 根据名称查找枚举值（大小写不敏感），未匹配返回 {@code null}。
     *
     * @param name 待查找的枚举名称
     * @return 匹配到的 {@link ThemeMode}；输入为 {@code null} 或未匹配时返回 {@code null}
     */
    public static ThemeMode fromName(String name) {
        if (name == null) {
            return null;
        }
        for (ThemeMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return null;
    }
}
