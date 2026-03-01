package com.bml.core.framework.config;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 通用配置。
 * <p>
 * 说明：
 * 1. API 账号签名鉴权已经前移到 Spring Security 过滤器链，不再通过 MVC 拦截器按路径单独挂载；
 * 2. 这里仅保留后台在线用户跟踪与统一跨域配置；
 * 3. 这样可以避免接口目录范围扩展后，鉴权规则散落在多个入口点难以维护。
 * </p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired(required = false)
    private HandlerInterceptor activeUserInterceptor;

    @Resource
    private CorsProperties corsProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
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
                .allowedOrigins()
                .allowedOriginPatterns(sanitizedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
