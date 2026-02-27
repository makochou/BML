package com.bml.core.framework.interceptor;

import cn.hutool.core.util.StrUtil;
import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.framework.security.utils.OpenApiSignatureUtils;
import com.bml.core.framework.service.OpenApiAuthService;
import com.bml.core.framework.service.model.OpenApiAppAuth;
import com.bml.core.framework.web.request.CachedBodyHttpServletRequest;
import com.bml.core.framework.web.util.ServletResponseUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * OpenAPI 签名校验拦截器。
 * <p>
 * 所有开放接口统一在该拦截器中完成：
 * </p>
 * <ul>
 *     <li>请求头完整性校验</li>
 *     <li>时间戳校验</li>
 *     <li>应用凭证校验</li>
 *     <li>接口授权校验</li>
 *     <li>nonce 防重放校验</li>
 *     <li>签名串计算与校验</li>
 * </ul>
 *
 * @author BML Team
 */
@Slf4j
@Component
public class OpenApiInterceptor implements HandlerInterceptor {

    private static final String HEADER_APP_KEY = "X-Bml-App-Key";
    private static final String HEADER_TIMESTAMP = "X-Bml-Timestamp";
    private static final String HEADER_NONCE = "X-Bml-Nonce";
    private static final String HEADER_SIGN = "X-Bml-Sign";
    private static final String NONCE_KEY_PREFIX = "openapi:nonce:";

    @Resource
    private OpenApiAuthService openApiAuthService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${bml.openapi.nonce-ttl-ms:300000}")
    private long nonceTtlMs;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String appKey = request.getHeader(HEADER_APP_KEY);
        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);
        String sign = request.getHeader(HEADER_SIGN);

        if (StrUtil.hasBlank(appKey, timestamp, nonce, sign)) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, GlobalErrorCode.OPEN_API_HEADER_MISSING);
            return false;
        }

        long clientTimestamp;
        try {
            clientTimestamp = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, GlobalErrorCode.OPEN_API_TIMESTAMP_INVALID);
            return false;
        }

        long currentTimestamp = System.currentTimeMillis();
        if (Math.abs(currentTimestamp - clientTimestamp) > nonceTtlMs) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, GlobalErrorCode.OPEN_API_REQUEST_EXPIRED);
            return false;
        }

        OpenApiAppAuth appAuth = openApiAuthService.getAppAuth(appKey);
        if (appAuth == null || StrUtil.isBlank(appAuth.getSecretKey())) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, GlobalErrorCode.OPEN_API_APP_INVALID);
            return false;
        }

        String requestPath = normalizePath(request);
        if (!openApiAuthService.isApiAuthorized(appAuth.getAccountId(), requestPath, request.getMethod())) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, GlobalErrorCode.OPEN_API_FORBIDDEN);
            return false;
        }

        String nonceRedisKey = NONCE_KEY_PREFIX + appKey + ":" + nonce;
        Boolean stored = redisTemplate.opsForValue().setIfAbsent(
                nonceRedisKey,
                "1",
                nonceTtlMs,
                TimeUnit.MILLISECONDS);
        if (!Boolean.TRUE.equals(stored)) {
            writeError(response, HttpServletResponse.SC_CONFLICT, GlobalErrorCode.OPEN_API_REPLAY_REQUEST);
            return false;
        }

        String canonicalQuery = OpenApiSignatureUtils.buildCanonicalQuery(request.getParameterMap());
        String bodySha256 = buildBodySha256(request);
        String payload = OpenApiSignatureUtils.buildSignPayload(
                appKey,
                timestamp,
                nonce,
                request.getMethod(),
                requestPath,
                canonicalQuery,
                bodySha256);
        String calculatedSign = OpenApiSignatureUtils.sign(payload, appAuth.getSecretKey());

        if (!StrUtil.equals(sign, calculatedSign)) {
            log.warn("OpenAPI 签名校验失败，请求路径: {}, 期望签名: {}, 实际签名: {}", requestPath, calculatedSign, sign);
            redisTemplate.delete(nonceRedisKey);
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, GlobalErrorCode.OPEN_API_SIGNATURE_INVALID);
            return false;
        }

        return true;
    }

    private String buildBodySha256(HttpServletRequest request) {
        if (HttpMethod.GET.matches(request.getMethod())) {
            return OpenApiSignatureUtils.sha256Hex(new byte[0]);
        }
        if (request instanceof CachedBodyHttpServletRequest cachedRequest) {
            return OpenApiSignatureUtils.sha256Hex(cachedRequest.getCachedBody());
        }
        return OpenApiSignatureUtils.sha256Hex(new byte[0]);
    }

    private String normalizePath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StrUtil.isNotBlank(contextPath) && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private void writeError(HttpServletResponse response, int httpStatus, GlobalErrorCode errorCode) {
        try {
            ServletResponseUtils.writeFailure(response, objectMapper, httpStatus, errorCode);
        } catch (Exception ex) {
            log.error("写出 OpenAPI 失败响应时发生异常", ex);
        }
    }
}
