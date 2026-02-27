package com.bml.core.framework.security.handle;

import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.framework.web.util.ServletResponseUtils;
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
 * 权限不足处理器。
 * <p>
 * 当用户已经完成认证，但访问了无权限资源时，由 Spring Security 调用本处理器，
 * 并返回统一的 403 JSON 响应。
 * </p>
 *
 * @author BML Team
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(AccessDeniedHandlerImpl.class);

    private final ObjectMapper objectMapper;

    public AccessDeniedHandlerImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        log.warn("权限不足，请求URI: {}, 异常信息: {}", request.getRequestURI(), accessDeniedException.getMessage());
        ServletResponseUtils.writeFailure(
                response,
                objectMapper,
                HttpServletResponse.SC_FORBIDDEN,
                GlobalErrorCode.FORBIDDEN);
    }
}
