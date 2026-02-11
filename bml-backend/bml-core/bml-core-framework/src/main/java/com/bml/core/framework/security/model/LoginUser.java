package com.bml.core.framework.security.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录用户身份权限模型
 * <p>
 * 实现 Spring Security 的 {@link UserDetails} 接口，封装当前登录用户的完整信息。
 * 该对象在登录成功后会被序列化存储到 Redis 中，后续每次请求通过 Token 中的
 * {@code userKey}（UUID）从 Redis 恢复。
 * </p>
 *
 * <h3>字段说明：</h3>
 * <ul>
 * <li>{@code userKey} — Redis 中的唯一标识（UUID），登录时生成</li>
 * <li>{@code userId} — 数据库用户主键ID</li>
 * <li>{@code deptId} — 所属部门ID</li>
 * <li>{@code username} — 登录账号</li>
 * <li>{@code password} — 密码（仅认证时使用，序列化时忽略）</li>
 * <li>{@code permissions} — 权限标识集合（如 {@code system:user:list}）</li>
 * <li>{@code loginTime} — 登录时间戳（毫秒）</li>
 * <li>{@code expireTime} — Redis 缓存过期时间戳（毫秒）</li>
 * </ul>
 *
 * @author BML Team
 */
public class LoginUser implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一标识（UUID）
     * <p>
     * 登录时由 TokenService 生成，作为 Redis Key 的后缀。
     * 同时会写入 JWT Token 的 claims 中，用于后续从 Redis 恢复用户信息。
     * </p>
     */
    private String userKey;

    /**
     * 用户ID（数据库主键）
     */
    private Long userId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 用户名（登录账号）
     */
    private String username;

    /**
     * 密码（仅认证时使用）
     */
    @JsonIgnore
    private String password;

    /**
     * 用户头像URL
     */
    private String avatar;

    /**
     * 帐号状态（0=停用, 1=正常）
     */
    private Integer status;

    /**
     * 权限标识集合
     * <p>
     * 存储用户拥有的所有权限字符串，格式为 {@code 模块:资源:操作}，
     * 例如 {@code system:user:list}。超级管理员的权限集合中包含 {@code *:*:*}。
     * </p>
     */
    private Set<String> permissions;

    /**
     * 登录时间戳（毫秒）
     */
    private Long loginTime;

    /**
     * Redis 缓存过期时间戳（毫秒）
     * <p>
     * 当当前时间接近此时间时，TokenService 会自动续期
     * </p>
     */
    private Long expireTime;

    /**
     * 默认构造函数（JSON反序列化需要）
     */
    public LoginUser() {
    }

    /**
     * 全参构造函数
     *
     * @param userId      用户ID
     * @param deptId      部门ID
     * @param username    用户名
     * @param password    密码
     * @param status      状态
     * @param permissions 权限集合
     */
    public LoginUser(Long userId, Long deptId, String username, String password, Integer status,
            Set<String> permissions) {
        this.userId = userId;
        this.deptId = deptId;
        this.username = username;
        this.password = password;
        this.status = status;
        this.permissions = permissions;
    }

    /**
     * 获取 Spring Security 权限集合
     * <p>
     * 将权限标识字符串转换为 {@link SimpleGrantedAuthority} 对象集合，
     * 供 {@code @PreAuthorize} 等注解使用。
     * </p>
     *
     * @return 权限集合，如果 permissions 为 null 则返回空集合
     */
    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (permissions == null) {
            return Collections.emptyList();
        }
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /**
     * 账户是否未过期（始终返回 true，过期逻辑由 Token 机制控制）
     */
    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账户是否未锁定（始终返回 true，锁定逻辑在业务层处理）
     */
    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 凭证是否未过期（始终返回 true）
     */
    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 账户是否启用
     * <p>
     * 状态值说明：1=正常, 0=停用。
     * 当 status 为 null 或 非1 时视为禁用。
     * </p>
     */
    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }

    // ======================== Getter / Setter ========================

    public String getUserKey() {
        return userKey;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Long loginTime) {
        this.loginTime = loginTime;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }
}
