package com.bml.core.framework.config;

import com.bml.core.framework.license.LicenseCheckInterceptor;
import com.bml.core.framework.security.interceptor.AdminSessionTimeoutInterceptor;
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
 * 2. 这里仅保留后台在线用户跟踪、中台管理员会话超时检查与统一跨域配置；
 * 3. 许可证校验拦截器优先级最高，在所有业务处理前检查系统是否已激活；
 * 4. 中台管理员会话超时拦截器次之，确保管理员空闲超时后自动登出；
 * 5. 这样可以避免接口目录范围扩展后，鉴权规则散落在多个入口点难以维护。
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

    @Resource
    private AdminSessionTimeoutInterceptor adminSessionTimeoutInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 许可证校验拦截器 — 最高优先级，未激活系统时拒绝一切非白名单请求
        registry.addInterceptor(licenseCheckInterceptor)
                .addPathPatterns("/**")
                .order(0);

        // 中台管理员会话超时拦截器 — 次高优先级，检查管理员空闲超时
        registry.addInterceptor(adminSessionTimeoutInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/admin/login",           // 登录接口不检查
                        "/auth/login",                 // 业务登录接口不检查
                        "/auth/login/config",          // 登录配置接口不检查
                        "/auth/refresh",               // 令牌刷新接口不检查
                        "/auth/register",              // 注册接口不检查
                        "/auth/captcha",               // 验证码接口不检查
                        "/system/license/**",          // 许可证管理接口不检查（允许未登录访问）
                        "/system/config/branding/**",  // 品牌图片接口不检查
                        "/actuator/health",            // 健康检查接口不检查
                        "/**/*.html",                  // 静态资源不检查
                        "/**/*.js",
                        "/**/*.css"
                )
                .order(1);

        if (activeUserInterceptor != null) {
            registry.addInterceptor(activeUserInterceptor)
                    .addPathPatterns("/**")
                    .excludePathPatterns("/login", "/captchaImage", "/auth/captcha", "/auth/login/config",
                            "/system/license/**", "/system/config/branding/**",
                            "/actuator/health", "/**/*.html", "/**/*.js", "/**/*.css")
                    .order(2);
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
