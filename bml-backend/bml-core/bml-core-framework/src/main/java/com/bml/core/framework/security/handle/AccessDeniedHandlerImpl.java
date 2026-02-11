package com.bml.core.framework.security.handle;

import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 权限不足处理器
 * <p>
 * 当已认证的用户尝试访问其无权限的资源时触发。
 * 返回统一的 {@link Result} 格式响应，HTTP 状态码为 403。
 * </p>
 *
 * <h3>触发场景：</h3>
 * <ul>
 * <li>{@code @PreAuthorize("@ss.hasPermi('xxx')")} 校验不通过</li>
 * <li>用户已登录但角色权限不足</li>
 * </ul>
 *
 * @author BML Team
 * @see AuthenticationEntryPointImpl 未认证（401）场景的处理器
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(AccessDeniedHandlerImpl.class);

    /** 使用 Spring 容器管理的 ObjectMapper，确保序列化配置一致 */
    private final ObjectMapper objectMapper;

    public AccessDeniedHandlerImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 处理权限不足异常
     *
     * @param request               HTTP 请求
     * @param response              HTTP 响应
     * @param accessDeniedException 权限不足异常
     * @throws IOException IO 异常
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        log.warn("权限不足，请求URI: {}, 异常信息: {}", request.getRequestURI(), accessDeniedException.getMessage());

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        Result<Void> result = Result.fail(GlobalErrorCode.FORBIDDEN);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
