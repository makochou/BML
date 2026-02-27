package com.bml.core.framework.config;

import com.bml.core.framework.interceptor.OpenApiInterceptor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private OpenApiInterceptor openApiInterceptor;

    @Autowired(required = false)
    private HandlerInterceptor activeUserInterceptor;

    @Resource
    private CorsProperties corsProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(openApiInterceptor)
                .addPathPatterns("/open/api/**")
                .excludePathPatterns("/open/api/test/**");

        if (activeUserInterceptor != null) {
            registry.addInterceptor(activeUserInterceptor)
                    .addPathPatterns("/**")
                    .excludePathPatterns("/login", "/captchaImage", "/**/*.html", "/**/*.js", "/**/*.css");
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] sanitizedOrigins = corsProperties.getAllowedOriginPatterns().stream()
                .filter(origin -> origin != null && !origin.isBlank())
                .toArray(String[]::new);

        registry.addMapping("/**")
                // CorsRegistration comes with permit-default-values, which preset
                // allowedOrigins="*". Clear it before using explicit origin patterns.
                .allowedOrigins()
                .allowedOriginPatterns(sanitizedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
