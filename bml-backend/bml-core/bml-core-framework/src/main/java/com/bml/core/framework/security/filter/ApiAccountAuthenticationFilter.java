package com.bml.core.framework.security.filter;

import cn.hutool.core.util.StrUtil;
import com.bml.core.common.support.ApiRegistryPathSupport;
import com.bml.core.framework.interceptor.OpenApiInterceptor;
import com.bml.core.framework.security.model.OpenApiLoginUser;
import com.bml.core.framework.service.model.OpenApiAppAuth;
import com.bml.core.framework.web.request.CachedBodyHttpServletRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * API 账号签名认证过滤器。
 * <p>
 * 设计目标：
 * 1. 让 API 账号签名请求不再局限于 /open/api/**，而是可以覆盖全项目纳管接口；
 * 2. 与后台 JWT 登录并存，前端管理端继续走 JWT，外部系统调用可走 appKey + sign；
 * 3. 认证成功后写入统一 SecurityContext，兼容现有 @PreAuthorize、审计填充与安全工具链。
 * </p>
 */
@Component
public class ApiAccountAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_APP_KEY = "X-Bml-App-Key";

    private final OpenApiInterceptor openApiInterceptor;

    public ApiAccountAuthenticationFilter(OpenApiInterceptor openApiInterceptor) {
        this.openApiInterceptor = openApiInterceptor;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = normalizePath(request);
        if (!ApiRegistryPathSupport.isManagedApiPath(requestPath)) {
            return true;
        }
        return StrUtil.isBlank(request.getHeader(HEADER_APP_KEY));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        HttpServletRequest requestToUse = wrapIfNecessary(request);
        try {
            boolean passed;
            try {
                passed = openApiInterceptor.preHandle(requestToUse, response, this);
            } catch (Exception exception) {
                throw new ServletException("API账号签名认证执行失败", exception);
            }
            if (!passed) {
                return;
            }
            OpenApiAppAuth appAuth = (OpenApiAppAuth) requestToUse.getAttribute(OpenApiInterceptor.REQUEST_ATTR_APP_AUTH);
            if (appAuth != null) {
                OpenApiLoginUser loginUser = new OpenApiLoginUser(appAuth.getAccountId(), requestToUse.getHeader(HEADER_APP_KEY));
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
            filterChain.doFilter(requestToUse, response);
        } finally {
            Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                    ? null
                    : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof OpenApiLoginUser) {
                SecurityContextHolder.clearContext();
            }
        }
    }

    private HttpServletRequest wrapIfNecessary(HttpServletRequest request) throws IOException {
        if (request instanceof CachedBodyHttpServletRequest) {
            return request;
        }
        return new CachedBodyHttpServletRequest(request);
    }

    private String normalizePath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StrUtil.isNotBlank(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }
}
