package com.bml.module.system.theme.enums;

/**
 * 紧凑度档位枚举。
 * <p>
 * 控制全局 {@code --bml-spacing-*} 与组件高度相关 Token，使表格行高、
 * 表单控件高度、按钮高度按档位比例缩放。
 * </p>
 * <p>
 * 对应 {@code Theme_Profile.density} 字段。
 * </p>
 *
 * @author BML Team
 */
public enum Density {

    /** 紧凑 — 缩减间距与控件高度，适合数据密集型管理后台。 */
    COMPACT,

    /** 默认 — 推荐档位，兼顾信息密度与可读性（PRESET_BEST 默认）。 */
    DEFAULT,

    /** 宽松 — 放大间距与控件高度，适合面向终端用户的展示型页面。 */
    LOOSE;

    /**
     * 根据名称查找枚举值（大小写不敏感），未匹配返回 {@code null}。
     *
     * @param name 待查找的枚举名称
     * @return 匹配到的 {@link Density}；输入为 {@code null} 或未匹配时返回 {@code null}
     */
    public static Density fromName(String name) {
        if (name == null) {
            return null;
        }
        for (Density density : values()) {
            if (density.name().equalsIgnoreCase(name)) {
                return density;
            }
        }
        return null;
    }
}
