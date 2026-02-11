package com.bml.core.framework.security.filter;

import com.bml.core.framework.security.model.LoginUser;
import com.bml.core.framework.security.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * <p>
 * 在每次 HTTP 请求到达 Controller 之前执行，负责：
 * <ol>
 * <li>从请求头 {@code Authorization: Bearer {token}} 中提取 Token</li>
 * <li>通过 {@link TokenService} 从 Redis 恢复完整的 {@link LoginUser} 对象</li>
 * <li>将 LoginUser（含完整权限列表）设置到 Spring Security 的 SecurityContext 中</li>
 * <li>检查并自动续期接近过期的 Token 缓存</li>
 * </ol>
 * </p>
 *
 * <h3>与旧实现的区别：</h3>
 * <p>
 * 旧实现直接从 JWT Claims 中读取 username 并构建一个空权限的 {@code User} 对象，
 * 导致 {@code @PreAuthorize("@ss.hasPermi('xxx')")} 始终判定无权限。
 * </p>
 * <p>
 * 新实现通过 Redis 恢复完整的 LoginUser（包含 permissions 集合），
 * 确保 Spring Security 的权限校验链路正常工作。
 * </p>
 *
 * @author BML Team
 * @see TokenService
 * @see LoginUser
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    /**
     * 构造函数注入
     *
     * @param tokenService Token 管理服务
     */
    public JwtAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * 过滤器核心逻辑
     * <p>
     * 执行流程：
     * <ol>
     * <li>通过 TokenService 从请求中提取并验证 Token，恢复 LoginUser</li>
     * <li>如果 LoginUser 不为空，则构建 {@link UsernamePasswordAuthenticationToken}
     * 并设置到 SecurityContext 中</li>
     * <li>触发自动续期机制，延长活跃用户的缓存有效期</li>
     * <li>无论是否认证成功，都继续执行后续过滤器链</li>
     * </ol>
     * </p>
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param chain    过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        // 1. 从请求中获取登录用户信息（Token解析 + Redis恢复）
        LoginUser loginUser = tokenService.getLoginUser(request);

        // 2. 如果 LoginUser 存在且 SecurityContext 尚未设置认证信息
        if (loginUser != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 3. 自动续期：延长活跃用户的 Redis 缓存有效期
            tokenService.refreshTokenExpireTime(loginUser);

            // 4. 构建 Spring Security 认证令牌
            // principal = LoginUser（完整用户信息，含权限列表）
            // credentials = null（Token 认证不需要密码）
            // authorities = LoginUser.getAuthorities()（权限集合）
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser,
                    null, loginUser.getAuthorities());

            // 5. 设置请求详细信息（IP、SessionID等）
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 6. 将认证信息设置到当前请求的 SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 7. 继续执行过滤器链（无论是否认证成功）
        chain.doFilter(request, response);
    }
}
