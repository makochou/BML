package com.bml.core.framework.security.filter;

import com.bml.core.framework.interceptor.OpenApiInterceptor;
import com.bml.core.framework.security.model.OpenApiLoginUser;
import com.bml.core.framework.service.model.OpenApiAppAuth;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiAccountAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateSignedManagedRequest() throws Exception {
        OpenApiInterceptor interceptor = Mockito.mock(OpenApiInterceptor.class);
        when(interceptor.preHandle(any(), any(), any())).thenAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            request.setAttribute(OpenApiInterceptor.REQUEST_ATTR_APP_AUTH, OpenApiAppAuth.builder()
                    .accountId(22L)
                    .secretKey("plain-secret")
                    .signVersion("v1")
                    .build());
            return true;
        });

        ApiAccountAuthenticationFilter filter = new ApiAccountAuthenticationFilter(interceptor);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/user");
        request.addHeader("X-Bml-App-Key", "ak-system");
        request.setContentType("application/json");
        request.setContent("{\"username\":\"bml\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        final Authentication[] authenticationHolder = new Authentication[1];
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
                authenticationHolder[0] = SecurityContextHolder.getContext().getAuthentication();
            }
        };

        filter.doFilter(request, response, chain);

        assertInstanceOf(OpenApiLoginUser.class, authenticationHolder[0].getPrincipal());
        OpenApiLoginUser loginUser = (OpenApiLoginUser) authenticationHolder[0].getPrincipal();
        assertEquals(22L, loginUser.getAccountId());
        assertEquals("ak-system", loginUser.getAppKey());
        assertTrue(loginUser.getUserId() < 0);
    }

    @Test
    void shouldSkipWhenNoAppKeyHeader() throws Exception {
        OpenApiInterceptor interceptor = Mockito.mock(OpenApiInterceptor.class);
        ApiAccountAuthenticationFilter filter = new ApiAccountAuthenticationFilter(interceptor);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/system/user/list");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            // no-op
        });

        verify(interceptor, never()).preHandle(any(), any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
