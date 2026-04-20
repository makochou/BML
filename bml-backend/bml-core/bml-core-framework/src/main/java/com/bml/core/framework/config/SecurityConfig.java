package com.bml.core.framework.config;

import com.bml.core.framework.security.filter.ApiAccountAuthenticationFilter;
import com.bml.core.framework.security.filter.JwtAuthenticationFilter;
import com.bml.core.framework.security.handle.AccessDeniedHandlerImpl;
import com.bml.core.framework.security.handle.AuthenticationEntryPointImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 统一安全配置。
 * <p>
 * 当前系统同时支持两条认证链路：
 * 1. 管理后台通过 JWT 访问内部管理接口；
 * 2. 外部系统通过 API 账号签名访问受管项目接口。
 * </p>
 * <p>
 * 这样可以在不破坏现有后台登录体系的前提下，将接口级授权统一收口到
 * {@code sys_api_registry + sys_api_permission}，并保证后续新增接口能够自动进入授权目录。
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final AuthenticationEntryPointImpl authenticationEntryPoint;

    private final AccessDeniedHandlerImpl accessDeniedHandler;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final ApiAccountAuthenticationFilter apiAccountAuthenticationFilter;

    @Value("${bml.swagger.enabled:false}")
    private Boolean swaggerEnabled;

    public SecurityConfig(AuthenticationEntryPointImpl authenticationEntryPoint,
            AccessDeniedHandlerImpl accessDeniedHandler,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ApiAccountAuthenticationFilter apiAccountAuthenticationFilter) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.apiAccountAuthenticationFilter = apiAccountAuthenticationFilter;
    }

    /**
     * 构建统一的安全过滤器链。
     * <p>
     * 白名单只保留登录、令牌刷新、健康检查和文档接口，其余项目接口必须先完成认证。
     * 认证优先级如下：
     * 1. API 账号签名认证；
     * 2. 后台 JWT 认证。
     * </p>
     *
     * @param http HttpSecurity 构建器
     * @return 安全过滤器链
     * @throws Exception 安全组件初始化异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    auth.requestMatchers(
                                    "/auth/login",
                                    "/auth/admin/login",
                                    "/auth/refresh",
                                    "/auth/register",
                                    "/auth/captcha",
                                    "/auth/login/config",
                                    "/system/config/branding/**",
                                    "/actuator/health",
                                    "/system/license/status",
                                    "/system/license/preview",
                                    "/system/license/upload",
                                    "/system/license/update",
                                    "/system/license/reset")
                            .permitAll();

                    if (Boolean.TRUE.equals(swaggerEnabled)) {
                        auth.requestMatchers(
                                        "/v3/api-docs/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html")
                                .permitAll();
                    }

                    auth.anyRequest().authenticated();
                })
                // 先执行 API 账号签名认证，再执行后台 JWT 认证。
                .addFilterBefore(apiAccountAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, ApiAccountAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 禁止 JWT 过滤器以普通 Servlet 过滤器方式重复注册。
     * <p>
     * 该过滤器仅应通过 Spring Security 过滤器链生效，否则会出现一次请求被执行两遍的问题。
     * </p>
     *
     * @param filter JWT 过滤器
     * @return 关闭自动注册的过滤器注册器
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registrationBean = new FilterRegistrationBean<>(filter);
        registrationBean.setEnabled(false);
        return registrationBean;
    }

    /**
     * 禁止 API 账号过滤器以普通 Servlet 过滤器方式重复注册。
     *
     * @param filter API 账号认证过滤器
     * @return 关闭自动注册的过滤器注册器
     */
    @Bean
    public FilterRegistrationBean<ApiAccountAuthenticationFilter> apiAccountAuthenticationFilterRegistration(
            ApiAccountAuthenticationFilter filter) {
        FilterRegistrationBean<ApiAccountAuthenticationFilter> registrationBean =
                new FilterRegistrationBean<>(filter);
        registrationBean.setEnabled(false);
        return registrationBean;
    }

    /**
     * 认证管理器。
     *
     * @param config Spring Security 认证配置
     * @return 认证管理器
     * @throws Exception 认证组件初始化异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 密码编码器。
     *
     * @return BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
