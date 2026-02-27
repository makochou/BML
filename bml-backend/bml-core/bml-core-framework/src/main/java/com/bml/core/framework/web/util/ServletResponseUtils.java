package com.bml.core.framework.web.util;

import com.bml.core.common.result.ErrorCode;
import com.bml.core.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Servlet 响应写出工具。
 * <p>
 * 统一处理过滤器、拦截器和 Spring Security 处理器中的 JSON 响应写出逻辑，
 * 避免这些组件各自拼装状态码、编码和响应体格式。
 * </p>
 *
 * @author BML Team
 */
public final class ServletResponseUtils {

    private ServletResponseUtils() {
    }

    /**
     * 以统一响应模型写出失败结果。
     *
     * @param response     HTTP 响应
     * @param objectMapper JSON 序列化工具
     * @param httpStatus   HTTP 状态码
     * @param errorCode    业务错误码
     * @throws IOException IO 异常
     */
    public static void writeFailure(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            int httpStatus,
            ErrorCode errorCode) throws IOException {
        writeJson(response, objectMapper, httpStatus, Result.fail(errorCode));
    }

    /**
     * 以统一响应模型写出失败结果，并允许覆盖默认消息。
     *
     * @param response     HTTP 响应
     * @param objectMapper JSON 序列化工具
     * @param httpStatus   HTTP 状态码
     * @param code         业务错误码
     * @param message      响应消息
     * @throws IOException IO 异常
     */
    public static void writeFailure(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            int httpStatus,
            int code,
            String message) throws IOException {
        writeJson(response, objectMapper, httpStatus, Result.fail(code, message));
    }

    /**
     * 写出统一 JSON 响应。
     *
     * @param response     HTTP 响应
     * @param objectMapper JSON 序列化工具
     * @param httpStatus   HTTP 状态码
     * @param result       统一响应对象
     * @throws IOException IO 异常
     */
    public static void writeJson(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            int httpStatus,
            Result<?> result) throws IOException {
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
