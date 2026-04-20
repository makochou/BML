package com.bml.core.framework.license;

import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 许可证校验拦截器。
 * <p>
 * 在所有业务请求处理前检查系统是否持有有效许可证。
 * 若许可证不存在或已过期，直接返回对应错误码，前端根据错误码跳转到许可证上传页面。
 * </p>
 * <p>
 * 白名单路径（无需许可证即可访问）：
 * <ul>
 *     <li>{@code /auth/login}、{@code /auth/refresh} — 登录与令牌刷新</li>
 *     <li>{@code /system/license/**} — 许可证上传与查询</li>
 *     <li>{@code /actuator/health} — 健康检查</li>
 * </ul>
 * </p>
 *
 * @author BML Team
 */
@Slf4j
@Component
public class LicenseCheckInterceptor implements HandlerInterceptor {

    /** 无需许可证即可访问的路径前缀 */
    private static final Set<String> WHITELIST_PREFIXES = Set.of(
            "/auth/login",
            "/auth/refresh",
            "/auth/register",
            "/auth/captcha",
            "/system/license",
            "/system/config/branding",
            "/actuator/health"
    );

    private final BmlLicenseHolder licenseHolder;
    private final ObjectMapper objectMapper;

    public LicenseCheckInterceptor(BmlLicenseHolder licenseHolder, ObjectMapper objectMapper) {
        this.licenseHolder = licenseHolder;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler) throws Exception {

        // 许可证校验未启用，直接放行
        if (!licenseHolder.isEnabled()) {
            return true;
        }

        // 白名单路径放行
        String requestUri = request.getRequestURI();
        // 去除 context-path 前缀（如 /api）
        String contextPath = request.getContextPath();
        String path = requestUri;
        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            path = requestUri.substring(contextPath.length());
        }

        if (isWhitelisted(path)) {
            return true;
        }

        // 校验许可证状态
        BmlLicense license = licenseHolder.getCurrentLicense();
        if (license == null) {
            writeError(response, GlobalErrorCode.LICENSE_NOT_FOUND);
            return false;
        }
        if (license.isExpired()) {
            writeError(response, GlobalErrorCode.LICENSE_EXPIRED);
            return false;
        }

        return true;
    }

    /**
     * 判断请求路径是否在白名单中。
     *
     * @param path 去除 context-path 后的请求路径
     * @return 在白名单中返回 {@code true}
     */
    private boolean isWhitelisted(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        for (String prefix : WHITELIST_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 向客户端写入错误响应。
     *
     * @param response HTTP 响应
     * @param errorCode 错误码
     * @throws IOException IO 异常
     */
    private void writeError(HttpServletResponse response, GlobalErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> result = Result.fail(errorCode);
        response.getWriter().write(objectMapper.writeValueAsString(result));
        response.getWriter().flush();
    }
}
