package com.bml.core.framework.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI（Swagger UI）配置。
 *
 * <p>仅在 {@code bml.swagger.enabled=true} 时生效，生产环境默认关闭。</p>
 *
 * <h3>访问地址：</h3>
 * <ul>
 *     <li>Swagger UI：<a href="http://localhost:8080/api/swagger-ui.html">http://localhost:8080/api/swagger-ui.html</a></li>
 *     <li>OpenAPI JSON：<a href="http://localhost:8080/api/v3/api-docs">http://localhost:8080/api/v3/api-docs</a></li>
 * </ul>
 *
 * <h3>认证说明：</h3>
 * <p>已配置 Bearer Token 认证方案，在 Swagger UI 右上角点击「Authorize」，
 * 输入 {@code Bearer {accessToken}} 即可对需要认证的接口进行调试。</p>
 *
 * @author BML Team
 */
@Configuration
@ConditionalOnProperty(name = "bml.swagger.enabled", havingValue = "true")
public class SpringDocConfig {

    /** Bearer Token 认证方案名称，与 SecurityRequirement 中的名称保持一致 */
    private static final String BEARER_AUTH_SCHEME = "BearerAuth";

    /**
     * 配置 OpenAPI 文档基本信息与全局安全方案。
     *
     * @return OpenAPI 配置对象
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // ── 文档基本信息 ──
                .info(new Info()
                        .title("BML 企业中台管理系统 API")
                        .description("""
                                BML 企业中台管理系统后端 API 文档。
                                
                                **认证方式：**
                                - 管理后台接口：Bearer JWT Token（在 Authorize 中输入 `Bearer {accessToken}`）
                                - OpenAPI 接口：HmacSHA256 签名认证（参见 OpenAPI 接入文档）
                                
                                **接口规范：**
                                - 所有接口统一返回 `Result<T>` 格式
                                - 业务状态码 200 表示成功，其他表示失败
                                - 时间格式：`yyyy-MM-dd HH:mm:ss`（Asia/Shanghai 时区）
                                """)
                        .version("v2.0.0")
                        .contact(new Contact()
                                .name("BML Team")
                                .email("support@bml.com")))
                // ── 全局安全方案：Bearer JWT ──
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 访问令牌，登录后从 /auth/login 接口获取，格式：Bearer {accessToken}")))
                // ── 全局应用安全方案（所有接口默认需要认证） ──
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
    }
}
