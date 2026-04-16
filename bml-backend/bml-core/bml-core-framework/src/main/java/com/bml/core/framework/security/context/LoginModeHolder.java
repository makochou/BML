package com.bml.core.framework.security.context;

/**
 * 登录模式上下文持有器（基于 ThreadLocal）。
 * <p>
 * 用于区分中台管理平台登录和前台业务系统登录。
 * {@link com.bml.module.system.security.UserDetailsServiceImpl} 根据此上下文
 * 决定走配置管理员认证还是数据库用户认证。
 * </p>
 *
 * <h3>使用方式：</h3>
 * <pre>
 * // 在 Controller 的登录方法中
 * LoginModeHolder.setAdminMode();     // 中台管理平台登录
 * LoginModeHolder.setBusinessMode();  // 前台业务系统登录
 * try {
 *     authenticationManager.authenticate(...);
 * } finally {
 *     LoginModeHolder.clear();  // 必须清理，防止内存泄漏
 * }
 * </pre>
 *
 * <h3>设计要点：</h3>
 * <ul>
 *   <li>使用 {@link ThreadLocal} 保证线程安全</li>
 *   <li>必须在 {@code finally} 块中调用 {@link #clear()} 防止内存泄漏</li>
 *   <li>默认模式为业务模式（{@link LoginMode#BUSINESS}），确保安全性</li>
 * </ul>
 *
 * @author BML Team
 */
public final class LoginModeHolder {

    /**
     * 登录模式枚举
     */
    public enum LoginMode {
        /** 中台管理平台登录 — 仅匹配 application.yml 配置的管理员 */
        ADMIN,
        /** 前台业务系统登录 — 仅查询 sys_user 数据库表 */
        BUSINESS
    }

    /** ThreadLocal 存储当前线程的登录模式 */
    private static final ThreadLocal<LoginMode> MODE_HOLDER = new ThreadLocal<>();

    private LoginModeHolder() {
        // 工具类禁止实例化
    }

    /**
     * 设置为中台管理平台登录模式
     */
    public static void setAdminMode() {
        MODE_HOLDER.set(LoginMode.ADMIN);
    }

    /**
     * 设置为前台业务系统登录模式
     */
    public static void setBusinessMode() {
        MODE_HOLDER.set(LoginMode.BUSINESS);
    }

    /**
     * 获取当前登录模式
     *
     * @return 当前登录模式，未设置时默认返回 {@link LoginMode#BUSINESS}
     */
    public static LoginMode getMode() {
        LoginMode mode = MODE_HOLDER.get();
        return mode != null ? mode : LoginMode.BUSINESS;
    }

    /**
     * 判断当前是否为中台管理平台登录
     *
     * @return {@code true} 表示中台登录
     */
    public static boolean isAdminMode() {
        return LoginMode.ADMIN.equals(MODE_HOLDER.get());
    }

    /**
     * 清除当前线程的登录模式（必须在 finally 中调用）
     */
    public static void clear() {
        MODE_HOLDER.remove();
    }
}
