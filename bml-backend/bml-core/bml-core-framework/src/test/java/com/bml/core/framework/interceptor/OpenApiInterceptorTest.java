package com.bml.core.framework.interceptor;

import com.bml.core.framework.security.utils.OpenApiSignatureUtils;
import com.bml.core.framework.service.OpenApiAuthService;
import com.bml.core.framework.service.model.OpenApiAppAuth;
import com.bml.core.framework.web.request.CachedBodyHttpServletRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class OpenApiInterceptorTest {

    @Test
    void shouldAcceptValidSignedRequest() throws Exception {
        OpenApiAuthService authService = Mockito.mock(OpenApiAuthService.class);
        RedisTemplate<String, Object> redisTemplate = mockRedisTemplate();
        ValueOperations<String, Object> valueOperations = mockValueOperations();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), ArgumentMatchers.any(TimeUnit.class))).thenReturn(true);
        when(authService.getAppAuth("ak-test")).thenReturn(OpenApiAppAuth.builder()
                .accountId(1L)
                .secretKey("plain-secret")
                .build());
        when(authService.isApiAuthorized(1L, "/open/api/demo", "POST")).thenReturn(true);

        OpenApiInterceptor interceptor = buildInterceptor(authService, redisTemplate);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = "nonce-1";
        String body = "{\"name\":\"bml\"}";
        String bodySha256 = OpenApiSignatureUtils.sha256Hex(body.getBytes(StandardCharsets.UTF_8));
        String payload = OpenApiSignatureUtils.buildSignPayload(
                "ak-test",
                timestamp,
                nonce,
                "POST",
                "/open/api/demo",
                "page=1",
                bodySha256);
        String signature = OpenApiSignatureUtils.sign(payload, "plain-secret");

        MockHttpServletRequest rawRequest = new MockHttpServletRequest("POST", "/api/open/api/demo");
        rawRequest.setContextPath("/api");
        rawRequest.setContentType("application/json");
        rawRequest.setQueryString("page=1");
        rawRequest.addParameter("page", "1");
        rawRequest.setContent(body.getBytes(StandardCharsets.UTF_8));
        rawRequest.addHeader("X-Bml-App-Key", "ak-test");
        rawRequest.addHeader("X-Bml-Timestamp", timestamp);
        rawRequest.addHeader("X-Bml-Nonce", nonce);
        rawRequest.addHeader("X-Bml-Sign", signature);

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean passed = interceptor.preHandle(new CachedBodyHttpServletRequest(rawRequest), response, new Object());

        assertTrue(passed);
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldRejectReplayRequest() throws Exception {
        OpenApiAuthService authService = Mockito.mock(OpenApiAuthService.class);
        RedisTemplate<String, Object> redisTemplate = mockRedisTemplate();
        ValueOperations<String, Object> valueOperations = mockValueOperations();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), ArgumentMatchers.any(TimeUnit.class))).thenReturn(false);
        when(authService.getAppAuth("ak-test")).thenReturn(OpenApiAppAuth.builder()
                .accountId(1L)
                .secretKey("plain-secret")
                .build());
        when(authService.isApiAuthorized(1L, "/open/api/demo", "GET")).thenReturn(true);

        OpenApiInterceptor interceptor = buildInterceptor(authService, redisTemplate);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String payload = OpenApiSignatureUtils.buildSignPayload(
                "ak-test",
                timestamp,
                "nonce-1",
                "GET",
                "/open/api/demo",
                "",
                OpenApiSignatureUtils.sha256Hex(new byte[0]));
        String signature = OpenApiSignatureUtils.sign(payload, "plain-secret");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/open/api/demo");
        request.setContextPath("/api");
        request.addHeader("X-Bml-App-Key", "ak-test");
        request.addHeader("X-Bml-Timestamp", timestamp);
        request.addHeader("X-Bml-Nonce", "nonce-1");
        request.addHeader("X-Bml-Sign", signature);

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean passed = interceptor.preHandle(request, response, new Object());

        assertFalse(passed);
        assertEquals(409, response.getStatus());
        JsonNode jsonNode = new ObjectMapper().readTree(response.getContentAsString());
        assertEquals(2106, jsonNode.get("code").asInt());
        assertEquals("重复请求已被拒绝", jsonNode.get("message").asText());
        assertTrue(jsonNode.has("timestamp"));
    }

    private OpenApiInterceptor buildInterceptor(OpenApiAuthService authService, RedisTemplate<String, Object> redisTemplate) {
        OpenApiInterceptor interceptor = new OpenApiInterceptor();
        ReflectionTestUtils.setField(interceptor, "openApiAuthService", authService);
        ReflectionTestUtils.setField(interceptor, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(interceptor, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(interceptor, "nonceTtlMs", 300000L);
        return interceptor;
    }

    @SuppressWarnings("unchecked")
    private ValueOperations<String, Object> mockValueOperations() {
        return Mockito.mock(ValueOperations.class);
    }

    @SuppressWarnings("unchecked")
    private RedisTemplate<String, Object> mockRedisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }
}
