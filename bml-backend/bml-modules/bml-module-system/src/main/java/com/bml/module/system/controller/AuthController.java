package com.bml.module.system.controller;

import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.common.result.Result;
import com.bml.core.framework.config.AdminProperties;
import com.bml.core.framework.security.context.LoginModeHolder;
import com.bml.core.framework.security.model.LoginUser;
import com.bml.core.framework.security.model.TokenVO;
import com.bml.core.framework.security.service.TokenService;
import com.bml.core.framework.security.utils.SecurityUtils;
import com.bml.module.system.converter.UserConverter;
import com.bml.module.system.dto.LoginBody;
import com.bml.module.system.dto.RefreshTokenDTO;
import com.bml.module.system.entity.SysMenu;
import com.bml.module.system.entity.SysUser;
import com.bml.module.system.service.CaptchaService;
import com.bml.module.system.service.SysConfigService;
import com.bml.module.system.service.SysLoginService;
import com.bml.module.system.service.SysMenuService;
import com.bml.module.system.service.SysRoleService;
import com.bml.module.system.service.SysUserService;
import com.bml.module.system.vo.RouterVO;
import com.bml.module.system.vo.SysUserVO;
import com.bml.module.system.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 认证控制器
 * <p>
 * 提供用户认证相关的 API 接口，包括登录、登出、刷新Token、获取用户信息和路由信息。
 * 所有认证接口的路径前缀为 {@code /auth/}。
 * </p>
 *
 * <h3>接口列表：</h3>
 * <table>
 * <tr>
 * <th>方法</th>
 * <th>路径</th>
 * <th>说明</th>
 * <th>认证要求</th>
 * </tr>
 * <tr>
 * <td>POST</td>
 * <td>/auth/login</td>
 * <td>用户登录</td>
 * <td>无需认证</td>
 * </tr>
 * <tr>
 * <td>POST</td>
 * <td>/auth/refresh</td>
 * <td>刷新Token</td>
 * <td>无需认证</td>
 * </tr>
 * <tr>
 * <td>POST</td>
 * <td>/auth/logout</td>
 * <td>用户登出</td>
 * <td>需要认证</td>
 * </tr>
 * <tr>
 * <td>GET</td>
 * <td>/auth/info</td>
 * <td>获取当前用户信息</td>
 * <td>需要认证</td>
 * </tr>
 * <tr>
 * <td>GET</td>
 * <td>/auth/routers</td>
 * <td>获取路由菜单</td>
 * <td>需要认证</td>
 * </tr>
 * </table>
 *
 * @author BML Team
 */
@Tag(name = "认证中心", description = "登录、登出、Token刷新、用户信息获取")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private SysLoginService loginService;

    @Resource
    private SysMenuService menuService;

    @Resource
    private TokenService tokenService;

    @Resource
    private AdminProperties adminProperties;

    @Resource
    private SysUserService userService;

    @Resource
    private SysRoleService roleService;

    @Resource
    private CaptchaService captchaService;

    @Resource
    private SysConfigService configService;

    /**
     * 用户登录
     * <p>
     * 验证用户名和密码，认证通过后返回双令牌（AccessToken + RefreshToken）。
     * </p>
     *
     * <h4>请求示例：</h4>
     * 
     * <pre>
     * POST /api/auth/login
     * Content-Type: application/json
     *
     * {
     *   "username": "your-admin-user",
     *   "password": "your-admin-password"
     * }
     * </pre>
     *
     * <h4>响应示例：</h4>
     * 
     * <pre>
     * {
     *   "code": 200,
     *   "message": "操作成功",
     *   "data": {
     *     "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
     *     "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
     *     "expiresIn": 7200
     *   }
     * }
     * </pre>
     *
     * @param loginBody 登录参数（用户名、密码）
     * @return 包含双令牌的统一响应
     */
    @Operation(summary = "前台业务系统登录", description = "使用数据库用户（sys_user 表）进行认证，返回 AccessToken 和 RefreshToken")
    @PostMapping("/login")
    public Result<TokenVO> login(@RequestBody LoginBody loginBody) {
        // 验证码校验（仅当系统开启验证码功能时生效）
        String captchaEnabled = configService.getConfigValue("sys.login.captchaEnabled", "false");
        if ("true".equalsIgnoreCase(captchaEnabled)) {
            if (!StringUtils.hasText(loginBody.getCaptchaKey()) || !StringUtils.hasText(loginBody.getCaptchaCode())) {
                return Result.badRequest("请输入验证码");
            }
            if (!captchaService.validateCaptcha(loginBody.getCaptchaKey(), loginBody.getCaptchaCode())) {
                return Result.badRequest("验证码错误或已过期");
            }
        }

        // 设置为业务系统登录模式 → UserDetailsServiceImpl 仅查询 sys_user 表
        LoginModeHolder.setBusinessMode();
        try {
            TokenVO tokenVO = loginService.login(loginBody.getUsername(), loginBody.getPassword());
            return Result.ok(tokenVO);
        } finally {
            LoginModeHolder.clear();
        }
    }

    /**
     * 获取图形验证码
     * <p>
     * 返回验证码图片（Base64 编码）和唯一标识 key。
     * 前端在登录时将 key 和用户输入的验证码一起提交。
     * </p>
     *
     * @return captchaKey + captchaImage（Base64 PNG）
     */
    @Operation(summary = "获取图形验证码", description = "生成验证码图片，返回 Base64 编码的 PNG 和唯一标识 key")
    @GetMapping("/captcha")
    public Result<java.util.Map<String, String>> captcha() {
        return Result.ok(captchaService.generateCaptcha());
    }

    /**
     * 获取登录页及品牌相关配置
     * <p>
     * 无需认证即可调用，用于前端登录页动态展示验证码、背景图，
     * 以及业务系统布局加载侧边栏 Logo 等品牌配置。
     * 同时返回 {@code sys.login.*} 和 {@code sys.sidebar.*} 前缀的所有配置项。
     * </p>
     */
    @Operation(summary = "获取登录页配置", description = "返回验证码开关、登录页背景图、侧边栏 Logo 等配置")
    @GetMapping("/login/config")
    public Result<java.util.Map<String, String>> loginConfig() {
        java.util.Map<String, String> config = new java.util.HashMap<>(configService.getConfigsByPrefix("sys.login."));
        config.putAll(configService.getConfigsByPrefix("sys.sidebar."));
        return Result.ok(config);
    }

    /**
     * 中台管理平台登录
     * <p>
     * 仅匹配 application.yml 中配置的管理员账号，不查询数据库。
     * 中台管理平台有且仅有一个管理员用户。
     * </p>
     *
     * @param loginBody 登录参数（用户名、密码）
     * @return 包含双令牌的统一响应
     */
    @Operation(summary = "中台管理平台登录", description = "使用配置管理员（application.yml）进行认证，返回 AccessToken 和 RefreshToken")
    @PostMapping("/admin/login")
    public Result<TokenVO> adminLogin(@RequestBody LoginBody loginBody) {
        // 设置为中台管理平台登录模式 → UserDetailsServiceImpl 仅匹配配置管理员
        LoginModeHolder.setAdminMode();
        try {
            TokenVO tokenVO = loginService.login(loginBody.getUsername(), loginBody.getPassword());
            return Result.ok(tokenVO);
        } finally {
            LoginModeHolder.clear();
        }
    }

    /**
     * 刷新 AccessToken
     * <p>
     * 当 AccessToken 过期时，前端使用 RefreshToken 获取新的 AccessToken。
     * RefreshToken 本身在有效期内不会更新，过期后需要重新登录。
     * </p>
     *
     * <h4>请求示例：</h4>
     * 
     * <pre>
     * POST /api/auth/refresh
     * Content-Type: application/json
     *
     * {
     *   "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
     * }
     * </pre>
     *
     * @param dto 包含 refreshToken 的请求体（使用 {@link RefreshTokenDTO} 替代原始 Map）
     * @return 包含新 AccessToken 的统一响应
     */
    @Operation(summary = "刷新Token", description = "使用RefreshToken获取新的AccessToken")
    @PostMapping("/refresh")
    public Result<TokenVO> refresh(@Validated @RequestBody RefreshTokenDTO dto) {
        // 调用 TokenService 刷新 AccessToken
        TokenVO tokenVO = tokenService.refreshAccessToken(dto.getRefreshToken());
        return Result.ok(tokenVO);
    }

    /**
     * 用户登出
     * <p>
     * 清除 Redis 中的用户会话缓存，使当前用户的所有 Token 立即失效。
     * </p>
     *
     * <h4>请求示例：</h4>
     * 
     * <pre>
     * POST /api/auth/logout
     * Authorization: Bearer {accessToken}
     * </pre>
     *
     * @param request HTTP 请求（从中提取当前用户的 Token）
     * @return 统一响应
     */
    @Operation(summary = "用户登出", description = "清除登录状态，使Token失效")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        // 1. 从请求中获取当前登录用户
        LoginUser loginUser = tokenService.getLoginUser(request);

        // 2. 如果用户存在，删除 Redis 中的缓存
        if (loginUser != null && StringUtils.hasText(loginUser.getUserKey())) {
            tokenService.deleteLoginUser(loginUser.getUserKey());
        }

        return Result.ok();
    }

    /**
     * 获取当前登录用户信息
     * <p>
     * 返回当前用户的基本信息、角色列表和权限标识列表。
     * 前端可据此渲染用户界面和控制按钮权限。
     * </p>
     *
     * <h4>响应示例：</h4>
     * 
     * <pre>
     * {
     *   "code": 200,
     *   "data": {
     *     "user": { "id": 1, "username": "admin", ... },
     *     "roles": ["admin", "common"],
     *     "permissions": ["*:*:*"]
     *   }
     * }
     * </pre>
     *
     * @return 包含用户信息、角色和权限的 {@link UserInfoVO}
     */
    @Operation(summary = "获取当前用户信息", description = "返回用户信息、角色列表和权限标识列表")
    @GetMapping("/info")
    public Result<UserInfoVO> getInfo() {
        // 1. 从 SecurityContext 中获取当前登录用户（已在登录时缓存到 Redis）
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Set<String> permissions = loginUser.getPermissions();

        SysUserVO userVO;
        Set<String> roles;

        // 2. 判断是否为配置管理员（中台管理平台使用）
        if (GlobalConstants.SYSTEM_USER_ID.equals(loginUser.getUserId())
                && adminProperties.getUsername() != null
                && adminProperties.getUsername().equals(loginUser.getUsername())) {
            // 配置管理员：从 AdminProperties 构建用户信息
            userVO = new SysUserVO();
            userVO.setId(loginUser.getUserId());
            userVO.setUsername(loginUser.getUsername());
            userVO.setNickname(adminProperties.getNickname());
            userVO.setStatus(1);
            roles = Collections.singleton("admin");
        } else {
            // 数据库用户：查询完整信息（前台业务系统使用）
            SysUser user = userService.getById(loginUser.getUserId());
            userVO = UserConverter.INSTANCE.toVO(user);
            roles = roleService.selectRolePermissionByUserId(loginUser.getUserId());
        }

        // 3. 组装返回数据
        UserInfoVO userInfoVO = UserInfoVO.builder()
                .user(userVO)
                .roles(roles)
                .permissions(permissions)
                .build();

        return Result.ok(userInfoVO);
    }

    /**
     * 获取路由菜单信息
     * <p>
     * 返回当前用户有权限访问的菜单树，前端据此生成导航路由。
     * </p>
     *
     * @return 菜单树列表
     */
    @Operation(summary = "获取路由菜单", description = "返回当前用户的菜单树，用于前端路由生成")
    @GetMapping("/routers")
    public Result<List<RouterVO>> getRouters() {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return Result.ok(menuService.buildMenus(menus));
    }
}
