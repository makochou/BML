package com.bml.core.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 中台管理员账号配置属性。
 * <p>
 * 中台管理平台不使用数据库用户表，永远只有一个管理员账号。
 * 管理员的用户名和密码通过 {@code application.yml} 配置，支持环境变量覆盖。
 * </p>
 *
 * <h3>配置示例：</h3>
 * <pre>
 * bml:
 *   admin:
 *     username: admin          # 管理员用户名
 *     password: admin123       # 管理员密码（明文，启动时自动 BCrypt 编码比对）
 *     nickname: 系统管理员      # 管理员显示昵称
 * </pre>
 *
 * <h3>环境变量覆盖：</h3>
 * <pre>
 * BML_ADMIN_USERNAME=admin
 * BML_ADMIN_PASSWORD=your-secure-password
 * BML_ADMIN_NICKNAME=管理员
 * </pre>
 *
 * <h3>设计说明：</h3>
 * <ul>
 *   <li>中台管理平台仅供内部运维使用，不需要多用户体系</li>
 *   <li>管理员始终拥有超级管理员权限（{@code *:*:*}），可访问所有功能</li>
 *   <li>管理员使用虚拟 userId（{@link com.bml.core.common.constant.GlobalConstants#ADMIN_USER_ID} = -1L），
 *       不与 sys_user 数据库表中的任何用户冲突</li>
 *   <li>密码以明文配置，登录时通过 BCrypt 编码比对验证</li>
 * </ul>
 *
 * @author BML Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "bml.admin")
public class AdminProperties {

    /**
     * 管理员用户名（必须在 application.yml 中配置）
     */
    private String username;

    /**
     * 管理员密码（明文，必须在 application.yml 中配置）。
     * <p>
     * 登录时通过 BCrypt 编码比对验证。
     * 生产环境建议通过环境变量 {@code BML_ADMIN_PASSWORD} 注入。
     * </p>
     */
    private String password;

    /**
     * 管理员显示昵称（必须在 application.yml 中配置）
     */
    private String nickname;
}
