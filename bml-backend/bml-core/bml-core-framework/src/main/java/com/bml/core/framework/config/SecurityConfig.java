package com.bml.core.framework.config;

import com.bml.core.framework.security.filter.JwtAuthenticationFilter;
import com.bml.core.framework.security.handle.AccessDeniedHandlerImpl;
import com.bml.core.framework.security.handle.AuthenticationEntryPointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全框架配置
 * <p>
 * 核心安全配置类，定义了认证和授权的完整策略：
 * </p>
 * <ul>
 * <li><b>无状态会话</b> — 不使用 HttpSession，完全依赖 JWT + Redis 管理会话</li>
 * <li><b>JWT 过滤器</b> — 在 {@link UsernamePasswordAuthenticationFilter} 之前执行</li>
 * <li><b>白名单路径</b> — 登录、刷新Token、API文档等路径允许匿名访问</li>
 * <li><b>异常处理</b> — 统一的401（未认证）和403（无权限）响应</li>
 * <li><b>方法级权限</b> — 通过 {@code @EnableMethodSecurity} 支持
 * {@code @PreAuthorize}</li>
 * </ul>
 *
 * <h3>白名单路径说明：</h3>
 * <table>
 * <tr>
 * <th>路径</th>
 * <th>说明</th>
 * </tr>
 * <tr>
 * <td>{@code /auth/login}</td>
 * <td>用户登录接口</td>
 * </tr>
 * <tr>
 * <td>{@code /auth/refresh}</td>
 * <td>刷新 AccessToken 接口</td>
 * </tr>
 * <tr>
 * <td>{@code /auth/register}</td>
 * <td>用户注册接口（预留）</td>
 * </tr>
 * <tr>
 * <td>{@code /v3/api-docs/**}</td>
 * <td>OpenAPI 文档接口</td>
 * </tr>
 * <tr>
 * <td>{@code /swagger-ui/**}</td>
 * <td>Swagger UI 页面</td>
 * </tr>
 * </table>
 *
 * @author BML Team
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        /** 未认证处理器（401） */
        private final AuthenticationEntryPointImpl authenticationEntryPoint;

        /** 权限不足处理器（403） */
        private final AccessDeniedHandlerImpl accessDeniedHandler;

        /** JWT 认证过滤器 */
        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @org.springframework.beans.factory.annotation.Value("${bml.swagger.enabled:false}")
        private Boolean swaggerEnabled;

        public SecurityConfig(AuthenticationEntryPointImpl authenticationEntryPoint,
                        AccessDeniedHandlerImpl accessDeniedHandler,
                        JwtAuthenticationFilter jwtAuthenticationFilter) {
                this.authenticationEntryPoint = authenticationEntryPoint;
                this.accessDeniedHandler = accessDeniedHandler;
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        /**
         * 安全过滤器链配置
         * <p>
         * 配置了完整的 HTTP 安全策略，包括 CSRF 禁用、CORS 委托、
         * 会话策略、异常处理、请求授权规则和 JWT 过滤器注册。
         * </p>
         *
         * @param http HttpSecurity 构建器
         * @return 构建完成的 SecurityFilterChain
         * @throws Exception 配置异常
         */
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // 禁用 CSRF（无状态 Token 认证不需要 CSRF 保护）
                                .csrf(AbstractHttpConfigurer::disable)
                                // 启用 CORS，让 Security 层与 WebMvcConfig 的跨域规则保持一致，
                                // 避免预检请求在 Security 层被提前拦截。
                                .cors(Customizer.withDefaults())
                                // 异常处理：未认证(401) + 权限不足(403)
                                .exceptionHandling(handling -> handling
                                                .authenticationEntryPoint(authenticationEntryPoint)
                                                .accessDeniedHandler(accessDeniedHandler))
                                // 无状态会话（不创建 HttpSession）
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // 请求授权规则
                                .authorizeHttpRequests(auth -> {
                                        // 白名单：始终允许匿名访问的路径
                                        auth.requestMatchers(
                                                        HttpMethod.OPTIONS, "/**" // 预检请求放行
                                        ).permitAll();

                                        auth.requestMatchers(
                                                        "/auth/login", // 登录
                                                        "/auth/refresh", // 刷新Token
                                                        "/auth/register", // 注册（预留）
                                                        "/actuator/health", // 健康检查(无需认证，供云原生探针或反向代理网关侦测)
                                                        "/open/api/**" // OpenAPI 走签名鉴权，不走 JWT 鉴权
                                        ).permitAll();

                                        // 仅在配置开启时允许 Swagger 文档访问
                                        if (Boolean.TRUE.equals(swaggerEnabled)) {
                                                auth.requestMatchers(
                                                                "/v3/api-docs/**", // OpenAPI 文档
                                                                "/swagger-ui/**", // Swagger UI
                                                                "/swagger-ui.html" // Swagger UI 入口
                                                ).permitAll();
                                        }

                                        // 其他所有请求需要认证
                                        auth.anyRequest().authenticated();
                                })
                                // 在 UsernamePasswordAuthenticationFilter 之前插入 JWT 过滤器
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        /**
         * 认证管理器
         * <p>
         * 由 Spring Security 自动配置，用于 {@code SysLoginService} 中
         * 调用 {@code authenticationManager.authenticate()} 进行密码验证。
         * </p>
         *
         * @param config 认证配置
         * @return AuthenticationManager 实例
         * @throws Exception 配置异常
         */
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        /**
         * 密码编码器
         * <p>
         * 使用 BCrypt 算法加密密码，强度默认为 10（Spring Security 默认值）。
         * BCrypt 是目前推荐的密码存储算法，具有自动加盐和可调节计算成本的特点。
         * </p>
         *
         * @return BCryptPasswordEncoder 实例
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
