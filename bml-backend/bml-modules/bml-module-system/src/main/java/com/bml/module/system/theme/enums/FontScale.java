package com.bml.module.system.theme.enums;

/**
 * 字体大小档位枚举。
 * <p>
 * 控制全局 {@code --bml-font-size-base} Token，所有具体字号均通过
 * 相对单位（rem）或基于该 Token 的派生变量推导，从而保证档位切换时
 * 整体字号比例一致。
 * </p>
 * <p>
 * 对应 {@code Theme_Profile.fontScale} 字段。
 * </p>
 *
 * @author BML Team
 */
public enum FontScale {

    /** 小号字体 — 缩小基准字号，适合屏幕分辨率较高或信息密集的场景。 */
    SMALL,

    /** 默认字体 — 推荐档位，平衡可读性与信息密度（PRESET_BEST 默认）。 */
    DEFAULT,

    /** 大号字体 — 放大基准字号，提升可读性，适合长时间阅读。 */
    LARGE,

    /** 超大字体 — 显著放大基准字号，提供高度可访问的阅读体验。 */
    XLARGE;

    /**
     * 根据名称查找枚举值（大小写不敏感），未匹配返回 {@code null}。
     *
     * @param name 待查找的枚举名称
     * @return 匹配到的 {@link FontScale}；输入为 {@code null} 或未匹配时返回 {@code null}
     */
    public static FontScale fromName(String name) {
        if (name == null) {
            return null;
        }
        for (FontScale scale : values()) {
            if (scale.name().equalsIgnoreCase(name)) {
                return scale;
            }
        }
        return null;
    }
}
