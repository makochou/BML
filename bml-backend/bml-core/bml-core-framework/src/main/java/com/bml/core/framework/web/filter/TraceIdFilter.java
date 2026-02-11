package com.bml.core.framework.web.filter;

import com.bml.core.common.constant.GlobalConstants;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 全链路追踪过滤器 (TraceId Filter)
 * <p>
 * 在请求进入系统时生成或透传 TraceId，并写入 SLF4J MDC (Mapped Diagnostic Context)。
 * 确保后续日志（包括 {@code Result} 响应对象）能够获取到该链路 ID。
 * </p>
 *
 * <h3>处理逻辑：</h3>
 * <ol>
 * <li>检查请求头 `X-Trace-Id`，若存在则透传，否则生成新 UUID。</li>
 * <li>将 TraceId 放入 MDC。</li>
 * <li>将 TraceId 放入响应头 `X-Trace-Id`，方便前端调试。</li>
 * <li>请求结束后清理 MDC，防止线程池复用导致的 TraceId 污染。</li>
 * </ol>
 *
 * @author BML Team
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class TraceIdFilter implements Filter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        try {
            // 写入 MDC
            MDC.put(GlobalConstants.TRACE_ID, traceId);

            // 写入响应头
            httpResponse.setHeader(TRACE_ID_HEADER, traceId);

            chain.doFilter(request, response);
        } finally {
            // 清理 MDC
            MDC.remove(GlobalConstants.TRACE_ID);
        }
    }
}
