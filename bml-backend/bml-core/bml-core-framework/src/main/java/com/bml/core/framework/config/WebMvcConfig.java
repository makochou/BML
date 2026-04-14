package com.bml.core.framework.config;

import com.bml.core.framework.license.LicenseCheckInterceptor;
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
 * 3. 许可证校验拦截器优先级最高，在所有业务处理前检查系统是否已激活；
 * 4. 这样可以避免接口目录范围扩展后，鉴权规则散落在多个入口点难以维护。
 * </p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired(required = false)
    private HandlerInterceptor activeUserInterceptor;

    @Resource
    private CorsProperties corsProperties;

    @Resource
    private LicenseCheckInterceptor licenseCheckInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 许可证校验拦截器 — 最高优先级，未激活系统时拒绝一切非白名单请求
        registry.addInterceptor(licenseCheckInterceptor)
                .addPathPatterns("/**")
                .order(0);

        if (activeUserInterceptor != null) {
            registry.addInterceptor(activeUserInterceptor)
                    .addPathPatterns("/**")
                    .excludePathPatterns("/login", "/captchaImage", "/**/*.html", "/**/*.js", "/**/*.css")
                    .order(1);
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
