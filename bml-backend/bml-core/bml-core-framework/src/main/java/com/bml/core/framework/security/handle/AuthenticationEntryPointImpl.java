package com.bml.core.framework.security.handle;

import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.framework.web.util.ServletResponseUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未认证访问处理器。
 * <p>
 * 当匿名用户或失效登录态访问受保护资源时，由 Spring Security 调用本处理器，
 * 并返回统一的 401 JSON 响应。
 * </p>
 *
 * @author BML Team
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationEntryPointImpl.class);

    private final ObjectMapper objectMapper;

    public AuthenticationEntryPointImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        log.warn("认证失败，请求URI: {}, 异常信息: {}", request.getRequestURI(), authException.getMessage());
        ServletResponseUtils.writeFailure(
                response,
                objectMapper,
                HttpServletResponse.SC_UNAUTHORIZED,
                GlobalErrorCode.UNAUTHORIZED);
    }
}
