package com.bml.core.framework.interceptor;

import cn.hutool.core.util.StrUtil;
import com.bml.core.common.exception.BusinessException;
import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.common.support.ApiIpWhitelistSupport;
import com.bml.core.common.support.ApiSignatureVersionSupport;
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
 * OpenAPI 请求统一鉴权拦截器。
 * <p>
 * 这里集中处理开放接口调用的公共安全能力，避免每个控制器重复拼接认证逻辑。
 * 当前统一校验请求头、时间戳、签名版本、IP 白名单、接口授权、nonce 防重放和签名值。
 * </p>
 */
@Slf4j
@Component
public class OpenApiInterceptor implements HandlerInterceptor {

    /**
     * 请求作用域内的 API 账号鉴权结果。
     * <p>
     * API 账号过滤器在验签通过后会从该属性中读取账号上下文，并写入 SecurityContext。
     * </p>
     */
    public static final String REQUEST_ATTR_APP_AUTH = OpenApiInterceptor.class.getName() + ".APP_AUTH";

    private static final String HEADER_APP_KEY = "X-Bml-App-Key";
    private static final String HEADER_TIMESTAMP = "X-Bml-Timestamp";
    private static final String HEADER_NONCE = "X-Bml-Nonce";
    private static final String HEADER_SIGN = "X-Bml-Sign";
    private static final String HEADER_SIGN_VERSION = "X-Bml-Sign-Version";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_REAL_IP = "X-Real-IP";
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
        String signVersion;
        try {
            signVersion = StrUtil.blankToDefault(
                    ApiSignatureVersionSupport.normalizeVersion(request.getHeader(HEADER_SIGN_VERSION)),
                    ApiSignatureVersionSupport.defaultVersion());
        } catch (BusinessException exception) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, GlobalErrorCode.OPEN_API_SIGN_VERSION_INVALID);
            return false;
        }

        if (StrUtil.hasBlank(appKey, timestamp, nonce, sign)) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, GlobalErrorCode.OPEN_API_HEADER_MISSING);
            return false;
        }

        long clientTimestamp;
        try {
            clientTimestamp = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, GlobalErrorCode.OPEN_API_TIMESTAMP_INVALID);
            return false;
        }

        if (Math.abs(System.currentTimeMillis() - clientTimestamp) > nonceTtlMs) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, GlobalErrorCode.OPEN_API_REQUEST_EXPIRED);
            return false;
        }

        OpenApiAppAuth appAuth = openApiAuthService.getAppAuth(appKey);
        if (appAuth == null || StrUtil.isBlank(appAuth.getSecretKey())) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, GlobalErrorCode.OPEN_API_APP_INVALID);
            return false;
        }

        String accountSignVersion = StrUtil.blankToDefault(
                ApiSignatureVersionSupport.normalizeVersion(appAuth.getSignVersion()),
                ApiSignatureVersionSupport.defaultVersion());
        if (!StrUtil.equals(signVersion, accountSignVersion)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, GlobalErrorCode.OPEN_API_SIGN_VERSION_INVALID);
            return false;
        }

        String clientIp = resolveClientIp(request);
        if (!ApiIpWhitelistSupport.isAllowed(clientIp, appAuth.getIpWhitelist())) {
            log.warn("OpenAPI IP 白名单校验失败，accountId={}, clientIp={}", appAuth.getAccountId(), clientIp);
            writeError(response, HttpServletResponse.SC_FORBIDDEN, GlobalErrorCode.OPEN_API_IP_FORBIDDEN);
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
            log.warn("OpenAPI 签名校验失败，requestPath={}, expectedSign={}, actualSign={}",
                    requestPath,
                    calculatedSign,
                    sign);
            redisTemplate.delete(nonceRedisKey);
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, GlobalErrorCode.OPEN_API_SIGNATURE_INVALID);
            return false;
        }

        request.setAttribute(REQUEST_ATTR_APP_AUTH, appAuth);

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

    /**
     * 生产环境中开放接口通常经由网关或反向代理转发，请优先信任代理透传的真实来源 IP。
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedIp = firstValidIpToken(request.getHeader(HEADER_FORWARDED_FOR));
        if (StrUtil.isNotBlank(forwardedIp)) {
            return forwardedIp;
        }
        String realIp = normalizeSingleHeaderIp(request.getHeader(HEADER_REAL_IP));
        if (StrUtil.isNotBlank(realIp)) {
            return realIp;
        }
        return normalizeSingleHeaderIp(request.getRemoteAddr());
    }

    private String firstValidIpToken(String headerValue) {
        if (StrUtil.isBlank(headerValue)) {
            return null;
        }
        String[] segments = headerValue.split(",");
        for (String segment : segments) {
            String ip = normalizeSingleHeaderIp(segment);
            if (StrUtil.isNotBlank(ip)) {
                return ip;
            }
        }
        return null;
    }

    private String normalizeSingleHeaderIp(String source) {
        String normalized = StrUtil.trimToNull(source);
        if (normalized == null || "unknown".equalsIgnoreCase(normalized)) {
            return null;
        }
        if (normalized.startsWith("[") && normalized.contains("]")) {
            return normalized.substring(1, normalized.indexOf(']'));
        }
        if (normalized.contains(".") && normalized.chars().filter(ch -> ch == ':').count() == 1) {
            return normalized.substring(0, normalized.lastIndexOf(':'));
        }
        return normalized;
    }

    private void writeError(HttpServletResponse response, int httpStatus, GlobalErrorCode errorCode) {
        try {
            ServletResponseUtils.writeFailure(response, objectMapper, httpStatus, errorCode);
        } catch (Exception exception) {
            log.error("写出 OpenAPI 失败响应时发生异常", exception);
        }
    }
}
