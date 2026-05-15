package com.bml.module.system.theme.enums;

/**
 * 圆角风格档位枚举。
 * <p>
 * 控制全局 {@code --bml-radius-sm}、{@code --bml-radius-md}、{@code --bml-radius-lg}
 * 三个 Token，使按钮、卡片、输入框、模态框等组件圆角档位同步切换。
 * </p>
 * <p>
 * 对应 {@code Theme_Profile.radius} 字段。
 * </p>
 *
 * @author BML Team
 */
public enum RadiusStyle {

    /** 直角 — 全部圆角归零，呈现现代极简风格。 */
    SHARP,

    /** 小圆角 — 轻微圆角，适合稳重正式的 B 端产品。 */
    SMALL,

    /** 中圆角 — 默认档位，兼顾平衡感与友好度（PRESET_BEST 默认）。 */
    MEDIUM,

    /** 大圆角 — 显著圆角，呈现柔和、活泼的视觉。 */
    LARGE;

    /**
     * 根据名称查找枚举值（大小写不敏感），未匹配返回 {@code null}。
     *
     * @param name 待查找的枚举名称
     * @return 匹配到的 {@link RadiusStyle}；输入为 {@code null} 或未匹配时返回 {@code null}
     */
    public static RadiusStyle fromName(String name) {
        if (name == null) {
            return null;
        }
        for (RadiusStyle style : values()) {
            if (style.name().equalsIgnoreCase(name)) {
                return style;
            }
        }
        return null;
    }
}
