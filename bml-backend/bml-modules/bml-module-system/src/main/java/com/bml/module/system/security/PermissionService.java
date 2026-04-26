package com.bml.module.system.security;

import cn.hutool.core.util.StrUtil;
import com.bml.core.framework.security.model.LoginUser;
import com.bml.core.framework.security.model.OpenApiLoginUser;
import com.bml.core.framework.security.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Set;

/**
 * 自定义权限校验服务，供 {@code @PreAuthorize("@ss.hasPermi('...')")} 调用。
 *
 * <h3>权限校验流程：</h3>
 * <ol>
 *   <li>权限标识为空 → 直接拒绝</li>
 *   <li>未登录（LoginUser 为 null）→ 直接拒绝</li>
 *   <li>API 账号（{@link OpenApiLoginUser}）→ 签名过滤器已完成接口级授权，直接放行</li>
 *   <li>权限集合为空 → 直接拒绝</li>
 *   <li>权限集合含 {@code *:*:*}（超级管理员）→ 直接放行</li>
 *   <li>权限集合精确包含指定权限标识 → 放行</li>
 *   <li>其他情况 → 拒绝</li>
 * </ol>
 *
 * <h3>超级管理员识别方式：</h3>
 * <p>
 * 超级管理员通过权限集合中是否包含 {@code *:*:*} 通配符来识别，
 * 而非硬编码用户 ID。{@code *:*:*} 由 {@link UserDetailsServiceImpl} 在登录时
 * 根据用户角色的 {@code role_key} 是否为 {@code admin} 来赋予。
 * 这样即使超级管理员的数据库 ID 发生变化，权限逻辑也能正确工作。
 * </p>
 *
 * @author BML Team
 * @see UserDetailsServiceImpl
 */
@Service("ss")
public class PermissionService {

    /**
     * 超级管理员权限通配符。
     * 权限集合中包含此标识的用户可访问所有受保护接口。
     */
    private static final String ALL_PERMISSION = "*:*:*";

    /**
     * 校验当前登录主体是否具备指定权限。
     *
     * @param permission 权限标识，格式为 {@code 模块:资源:操作}，如 {@code system:user:list}
     * @return {@code true} 表示有权限，{@code false} 表示无权限
     */
    public boolean hasPermi(String permission) {
        // 1. 权限标识为空，直接拒绝
        if (StrUtil.isBlank(permission)) {
            return false;
        }

        // 2. 获取当前登录用户
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            return false;
        }

        // 3. API 账号：签名过滤器已完成接口级授权，此处直接放行
        if (loginUser instanceof OpenApiLoginUser) {
            return true;
        }

        // 4. 权限集合为空，直接拒绝
        if (CollectionUtils.isEmpty(loginUser.getPermissions())) {
            return false;
        }

        // 5. 基于权限集合判断：
        //    - 超级管理员：权限集合中含 *:*:* 通配符，直接放行
        //    - 普通用户：精确匹配权限标识
        //
        //    【重要】此处不再使用硬编码的用户 ID（如 1L 或 2L）进行超级管理员判断，
        //    改为通过权限集合中的 *:*:* 标识来识别，确保逻辑的健壮性和可维护性。
        return hasPermissions(loginUser.getPermissions(), permission);
    }

    /**
     * 检查权限集合中是否包含指定权限。
     *
     * <p>支持两种匹配方式：</p>
     * <ul>
     *   <li>通配符匹配：权限集合中含 {@code *:*:*} 时，匹配所有权限</li>
     *   <li>精确匹配：权限集合中含与 {@code permission} 完全相同的字符串</li>
     * </ul>
     *
     * @param permissions 当前用户的权限集合
     * @param permission  待校验的权限标识
     * @return 是否具备权限
     */
    private boolean hasPermissions(Set<String> permissions, String permission) {
        return permissions.contains(ALL_PERMISSION) || permissions.contains(StrUtil.trim(permission));
    }
}
