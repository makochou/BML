package com.bml.module.system.security;

import cn.hutool.core.util.StrUtil;
import com.bml.core.framework.security.model.LoginUser;
import com.bml.core.framework.security.model.OpenApiLoginUser;
import com.bml.core.framework.security.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Set;

/**
 * 自定义权限实现，供 `@ss.hasPermi(...)` 调用。
 * <p>
 * 后台 JWT 用户仍按角色权限字符串判断；
 * API 账号签名请求则在过滤器阶段已完成“路径 + 方法”的接口级授权校验，
 * 因此这里需要兼容放行，避免控制器上的方法级注解重复拦截。
 * </p>
 */
@Service("ss")
public class PermissionService {

    private static final String ALL_PERMISSION = "*:*:*";

    /**
     * 校验当前主体是否具备指定权限。
     *
     * @param permission 权限标识
     * @return 是否具备权限
     */
    public boolean hasPermi(String permission) {
        if (StrUtil.isBlank(permission)) {
            return false;
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            return false;
        }
        if (loginUser instanceof OpenApiLoginUser) {
            // API 账号请求的访问控制已由签名过滤器结合 sys_api_permission 完成。
            return true;
        }
        if (CollectionUtils.isEmpty(loginUser.getPermissions())) {
            return false;
        }
        if (Long.valueOf(1L).equals(loginUser.getUserId())) {
            return true;
        }
        return hasPermissions(loginUser.getPermissions(), permission);
    }

    private boolean hasPermissions(Set<String> permissions, String permission) {
        return permissions.contains(ALL_PERMISSION) || permissions.contains(StrUtil.trim(permission));
    }
}
