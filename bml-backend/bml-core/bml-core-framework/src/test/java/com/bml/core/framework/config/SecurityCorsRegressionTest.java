package com.bml.core.framework.config;

import com.bml.core.framework.interceptor.OpenApiInterceptor;
import com.bml.core.framework.security.filter.ApiAccountAuthenticationFilter;
import com.bml.core.framework.security.filter.JwtAuthenticationFilter;
import com.bml.core.framework.security.handle.AccessDeniedHandlerImpl;
import com.bml.core.framework.security.handle.AuthenticationEntryPointImpl;
import com.bml.core.framework.security.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SecurityCorsRegressionTest.TestApp.class)
@AutoConfigureMockMvc
class SecurityCorsRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private OpenApiInterceptor openApiInterceptor;

    @Test
    void shouldAllowOptionsPreflightWithoutAuthentication() throws Exception {
        mockMvc.perform(options("/system/protected"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUnsignedManagedApiPathWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/open/api/ping"))
                .andExpect(status().isUnauthorized());
    }

    @RestController
    static class DummyOptionsController {
        @RequestMapping(value = "/system/protected", method = RequestMethod.OPTIONS)
        public void options() {
            // no-op
        }

        @GetMapping("/open/api/ping")
        public String openApiPing() {
            return "ok";
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
            org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
            org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class
    })
    @Import({ SecurityConfig.class, TestBeans.class, DummyOptionsController.class })
    static class TestApp {
    }

    @Configuration
    static class TestBeans {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        AuthenticationEntryPointImpl authenticationEntryPointImpl(ObjectMapper objectMapper) {
            return new AuthenticationEntryPointImpl(objectMapper);
        }

        @Bean
        AccessDeniedHandlerImpl accessDeniedHandlerImpl(ObjectMapper objectMapper) {
            return new AccessDeniedHandlerImpl(objectMapper);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(TokenService tokenService) {
            return new JwtAuthenticationFilter(tokenService);
        }

        @Bean
        ApiAccountAuthenticationFilter apiAccountAuthenticationFilter(OpenApiInterceptor openApiInterceptor) {
            return new ApiAccountAuthenticationFilter(openApiInterceptor);
        }
    }
}
