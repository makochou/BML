package com.bml.core.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * <p>
 * 配置全局 CORS（跨域资源共享）策略。
 * </p>
 *
 * <h3>CORS 配置说明：</h3>
 * <table>
 * <tr>
 * <th>配置项</th>
 * <th>值</th>
 * <th>说明</th>
 * </tr>
 * <tr>
 * <td>allowedOriginPatterns</td>
 * <td>*</td>
 * <td>允许所有来源（开发环境适用）</td>
 * </tr>
 * <tr>
 * <td>allowedMethods</td>
 * <td>GET, POST, PUT, DELETE, OPTIONS</td>
 * <td>允许的请求方法</td>
 * </tr>
 * <tr>
 * <td>allowedHeaders</td>
 * <td>*</td>
 * <td>允许所有请求头</td>
 * </tr>
 * <tr>
 * <td>allowCredentials</td>
 * <td>true</td>
 * <td>允许携带 Cookie</td>
 * </tr>
 * <tr>
 * <td>maxAge</td>
 * <td>3600</td>
 * <td>预检请求缓存时间（秒）</td>
 * </tr>
 * </table>
 *
 * <h3>⚠ 生产环境安全建议：</h3>
 * <p>
 * 当前配置 {@code allowedOriginPatterns("*")} 允许所有来源访问，仅适用于开发/测试环境。
 * <b>生产环境务必修改为明确的域名白名单</b>，示例：
 * </p>
 * 
 * <pre>
 * // 生产环境推荐配置：
 * registry.addMapping("/api/**")
 *         .allowedOrigins("https://www.example.com", "https://admin.example.com")
 *         .allowedMethods("GET", "POST", "PUT", "DELETE")
 *         .allowedHeaders("Authorization", "Content-Type", "X-Requested-With")
 *         .allowCredentials(true)
 *         .maxAge(3600);
 * </pre>
 * <p>
 * 也可以将允许的来源配置在 {@code application.yml} 中，通过 {@code @Value} 注入：
 * </p>
 * 
 * <pre>
 * # application.yml
 * cors:
 *   allowed-origins: https://www.example.com,https://admin.example.com
 * </pre>
 *
 * @author BML Team
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @jakarta.annotation.Resource
    private com.bml.core.framework.interceptor.OpenApiInterceptor openApiInterceptor;

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        // 开放平台接口签名认证：/open/api/**
        registry.addInterceptor(openApiInterceptor)
                .addPathPatterns("/open/api/**")
                .excludePathPatterns("/open/api/test/**"); // 排除测试接口
    }

    /**
     * 配置跨域访问策略
     * <p>
     * TODO: 生产环境部署前，务必将 {@code allowedOriginPatterns("*")} 修改为明确的域名白名单，
     * 防止 CSRF 等跨站攻击风险。
     * </p>
     *
     * @param registry CORS 注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
