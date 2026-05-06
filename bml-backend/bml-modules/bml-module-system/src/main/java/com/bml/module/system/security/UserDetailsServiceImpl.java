package com.bml.module.system.security;

import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.framework.config.AdminProperties;
import com.bml.core.framework.security.context.LoginModeHolder;
import com.bml.core.framework.security.model.LoginUser;
import com.bml.module.system.entity.SysUser;
import com.bml.module.system.service.SysMenuService;
import com.bml.module.system.service.SysRoleService;
import com.bml.module.system.service.SysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

/**
 * 用户详情服务（双模式认证：配置管理员 + 数据库用户）。
 * <p>
 * 通过 {@link LoginModeHolder} 区分登录来源，分别走不同的认证路径：
 * </p>
 *
 * <h3>认证流程：</h3>
 * <pre>
 * Spring Security AuthenticationManager
 *   └─ UserDetailsServiceImpl.loadUserByUsername()
 *        ├─ LoginMode.ADMIN    → 仅匹配 application.yml 配置的管理员（中台管理平台）
 *        └─ LoginMode.BUSINESS → 仅查询 sys_user 数据库表（前台业务系统）
 * </pre>
 *
 * <h3>设计要点：</h3>
 * <ul>
 *   <li>中台管理平台有且仅有一个管理员，配置在 application.yml，不关联 sys_user 表</li>
 *   <li>前台业务系统用户来自 sys_user 表，权限通过角色-菜单关联动态查询</li>
 *   <li>两种登录模式完全隔离，互不干扰</li>
 *   <li>默认模式为 BUSINESS（安全保障：未设置模式时不会意外匹配配置管理员）</li>
 * </ul>
 *
 * @author BML Team
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    /** 超级管理员权限标识，表示拥有全部权限 */
    private static final Set<String> SUPER_ADMIN_PERMISSIONS = Collections.singleton("*:*:*");

    private final AdminProperties adminProperties;
    private final PasswordEncoder passwordEncoder;
    private final SysUserService userService;
    private final SysMenuService menuService;
    private final SysRoleService roleService;

    public UserDetailsServiceImpl(AdminProperties adminProperties,
                                  PasswordEncoder passwordEncoder,
                                  SysUserService userService,
                                  SysMenuService menuService,
                                  SysRoleService roleService) {
        this.adminProperties = adminProperties;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.menuService = menuService;
        this.roleService = roleService;
    }

    /**
     * 根据账号加载用户详情。
     * <p>
     * 优先匹配配置管理员（中台管理平台），不匹配则查询数据库（前台业务系统）。
     * </p>
     *
     * @param username 登录账号
     * @return LoginUser 对象
     * @throws UsernameNotFoundException 用户不存在或状态异常时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // ── 根据登录模式分流认证路径 ──
        if (LoginModeHolder.isAdminMode()) {
            return loadAdminUser(username);
        }
        return loadBusinessUser(username);
    }

    /**
     * 加载配置管理员（中台管理平台登录）。
     * <p>
     * 仅匹配 application.yml 中配置的管理员账号，不查询数据库。
     * </p>
     *
     * @param username 登录账号
     * @return 配置管理员的 LoginUser
     * @throws UsernameNotFoundException 账号不匹配时抛出
     */
    private UserDetails loadAdminUser(String username) {
        if (adminProperties.getUsername() != null && adminProperties.getUsername().equals(username)) {
            log.debug("加载配置管理员账号：{}", username);
            return new LoginUser(
                    GlobalConstants.ADMIN_USER_ID,   // 使用专用的虚拟 ID，不与数据库用户冲突
                    null,
                    adminProperties.getUsername(),
                    passwordEncoder.encode(adminProperties.getPassword()),
                    1,
                    SUPER_ADMIN_PERMISSIONS
            );
        }
        log.info("中台登录失败：账号 [{}] 不是配置管理员", username);
        throw new UsernameNotFoundException("账号或密码错误");
    }

    /**
     * 加载数据库用户（前台业务系统登录）。
     *
     * <p>仅查询 sys_user 表，不匹配配置管理员。支持用户停用、删除状态校验。</p>
     *
     * <h3>超级管理员权限处理：</h3>
     * <p>
     * 若用户拥有 {@code role_key = 'admin'} 的角色，则直接赋予 {@code *:*:*} 全量权限，
     * 无需查询 sys_role_menu 关联表。这样即使 sys_role_menu 中缺少某些菜单关联，
     * 超级管理员也能正常访问所有接口。
     * </p>
     *
     * @param username 登录账号
     * @return 数据库用户的 LoginUser
     * @throws UsernameNotFoundException 用户不存在或状态异常时抛出
     */
    private UserDetails loadBusinessUser(String username) {
        // ── 1. 查询数据库用户 ──
        SysUser user = userService.selectUserByUserName(username);
        if (user == null) {
            log.info("登录用户：{} 不存在", username);
            throw new UsernameNotFoundException("账号或密码错误");
        }
        if (user.getDeleted() != null && user.getDeleted() == 1) {
            log.info("登录用户：{} 已被删除", username);
            throw new UsernameNotFoundException("对不起，您的账号已被删除");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            log.info("登录用户：{} 已被停用", username);
            throw new UsernameNotFoundException("对不起，您的账号已被停用");
        }

        // ── 2. 判断是否为超级管理员（拥有 role_key='admin' 的角色） ──
        //    超级管理员直接赋予 *:*:* 全量权限，无需查询菜单关联表。
        //    这样即使 sys_role_menu 数据不完整，超级管理员也能正常使用所有功能。
        Set<String> permissions;
        boolean isSuperAdmin = roleService.selectRolesByUserId(user.getId())
                .stream()
                .anyMatch(role -> GlobalConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getRoleCode()));

        if (isSuperAdmin) {
            permissions = SUPER_ADMIN_PERMISSIONS;
            log.debug("加载超级管理员用户：{}，赋予全量权限 *:*:*", username);
        } else {
            // 普通用户：通过角色-菜单关联动态查询权限标识
            permissions = menuService.selectMenuPermsByUserId(user.getId());
            log.debug("加载数据库用户：{}，权限数量：{}", username, permissions.size());
        }

        return new LoginUser(
                user.getId(),
                user.getDeptId(),
                user.getUsername(),
                user.getPassword(),    // 数据库中已是 BCrypt 编码
                user.getStatus(),
                permissions
        );
    }
}
