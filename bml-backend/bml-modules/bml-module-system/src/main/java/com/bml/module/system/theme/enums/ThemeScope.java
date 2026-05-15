package com.bml.module.system.theme.enums;

/**
 * 主题作用域枚举。
 * <p>
 * 中台管理（{@link #ADMIN}）与业务系统（{@link #BUSINESS}）拥有彼此隔离的
 * 主题状态与持久化数据，但共享同一套主题引擎、组件与 API。该枚举用于
 * 标识当前主题数据所属的作用域，是 {@code Theme_API}、{@code Theme_Repository}
 * 与前端 {@code useTheme(scope)} 的关键路由参数。
 * </p>
 * <p>
 * 与数据库列 {@code bml_theme_user_setting.scope} 以及 {@code bml_theme_preset}
 * 中两个 JSON 列（{@code profile_admin} / {@code profile_business}）一一对应。
 * </p>
 *
 * @author BML Team
 */
public enum ThemeScope {

    /**
     * 中台管理作用域。
     * <p>
     * 平台管理员视角下的所有页面、布局、组件均使用该作用域的 {@code Theme_Profile}。
     * </p>
     */
    ADMIN,

    /**
     * 业务系统作用域。
     * <p>
     * 业务前台用户视角下的所有页面、布局、组件均使用该作用域的 {@code Theme_Profile}。
     * </p>
     */
    BUSINESS;

    /**
     * 根据名称查找枚举值（大小写不敏感），未匹配返回 {@code null}。
     *
     * @param name 待查找的枚举名称（如 {@code "ADMIN"} / {@code "admin"}）
     * @return 匹配到的 {@link ThemeScope}；输入为 {@code null} 或未匹配时返回 {@code null}
     */
    public static ThemeScope fromName(String name) {
        if (name == null) {
            return null;
        }
        for (ThemeScope scope : values()) {
            if (scope.name().equalsIgnoreCase(name)) {
                return scope;
            }
        }
        return null;
    }
}
