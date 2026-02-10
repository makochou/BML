package com.bml.module.system.controller;

import com.bml.core.common.result.Result;
import com.bml.core.framework.security.utils.SecurityUtils;
import com.bml.module.system.dto.LoginBody;
import com.bml.module.system.entity.SysMenu;
import com.bml.module.system.entity.SysUser;
import com.bml.core.framework.security.model.LoginUser;
import com.bml.module.system.service.SysLoginService;
import com.bml.module.system.service.SysMenuService;
import com.bml.module.system.service.SysRoleService;
import com.bml.module.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 认证控制器
 *
 * @author BML Team
 */
@Tag(name = "认证中心")
@RestController
public class AuthController {

    @Resource
    private SysLoginService loginService;
    
    @Resource
    private SysMenuService menuService;
    
    @Resource
    private SysRoleService roleService;

    @Resource
    private SysUserService userService;

    @Operation(summary = "登录方法")
    @PostMapping("/auth/login")
    public Result<Map<String, String>> login(@RequestBody LoginBody loginBody) {
        String token = loginService.login(loginBody.getUsername(), loginBody.getPassword());
        Map<String, String> map = new HashMap<>();
        map.put("token", token);
        return Result.ok(map);
    }
    
    @Operation(summary = "获取用户信息")
    @GetMapping("/system/user/getInfo")
    public Result<Map<String, Object>> getInfo() {
        Long userId = SecurityUtils.getUserId();
        SysUser user = userService.getById(userId);
        // LoginUser loginUser = SecurityUtils.getLoginUser(); // Generic T
        // Set<String> permissions = ((LoginUser)loginUser).getPermissions(); // Need cast
        
        // But better fetch permissions from Service or just use LoginUser if available
        // Let's use LoginUser for permissions as it is cached in token/security context
        LoginUser loginUser = (LoginUser) SecurityUtils.getLoginUser();
        Set<String> permissions = loginUser.getPermissions();
        
        // Roles from Service
        Set<String> roles = roleService.selectRolePermissionByUserId(userId);
        
        Map<String, Object> map = new HashMap<>();
        map.put("user", user);
        map.put("roles", roles);
        map.put("permissions", permissions);
        return Result.ok(map);
    }
    
    @Operation(summary = "获取路由信息")
    @GetMapping("/system/menu/getRouters")
    public Result<List<SysMenu>> getRouters() {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return Result.ok(menuService.buildMenus(menus));
    }
}
