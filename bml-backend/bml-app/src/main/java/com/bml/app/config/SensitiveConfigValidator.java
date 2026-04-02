package com.bml.app.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 敏感配置启动校验器。
 *
 * <p>在应用启动时（{@link PostConstruct} 阶段）对关键安全配置进行强制校验，
 * 防止以下高危场景进入生产环境：</p>
 * <ul>
 *     <li>配置项缺失（未设置环境变量）</li>
 *     <li>使用弱默认值或占位符（如 change-me、example 等）</li>
 *     <li>密钥长度不足（JWT 密钥 &lt; 32 位，OpenAPI 加密密钥 &lt; 16 位）</li>
 * </ul>
 *
 * <p><b>注意：</b>本校验器仅在非本地开发环境（非 local profile）下强制执行。
 * 本地开发时允许使用默认值，方便快速启动。</p>
 *
 * <h3>环境变量清单：</h3>
 * <pre>
 * BML_DB_PASSWORD                    — 数据库密码
 * BML_JWT_SECRET                     — JWT 签名密钥（≥32 位）
 * BML_OPENAPI_SECRET_ENCRYPTION_KEY  — OpenAPI SecretKey 加密密钥（≥16 位）
 * </pre>
 *
 * @author BML Team
 */
@Component
public class SensitiveConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(SensitiveConfigValidator.class);

    /**
     * 弱默认值关键词列表，包含这些关键词的配置值将被拒绝。
     */
    private static final List<String> WEAK_VALUE_KEYWORDS = Arrays.asList(
            "change-me", "changeme", "please-set", "example",
            "local-dev", "local-openapi", "your-secret", "todo",
            "placeholder", "default-key"
    );

    private final Environment environment;

    public SensitiveConfigValidator(Environment environment) {
        this.environment = environment;
    }

    /**
     * 执行敏感配置校验。
     *
     * <p>本地开发环境（active profile 包含 local 或 dev）跳过强制校验，
     * 仅打印警告日志，方便开发者快速启动。</p>
     */
    @PostConstruct
    public void validate() {
        // 判断是否为本地开发环境
        boolean isLocalDev = isLocalDevEnvironment();

        if (isLocalDev) {
            log.warn("========================================================");
            log.warn("  [安全警告] 当前为本地开发环境，跳过敏感配置强制校验。");
            log.warn("  生产部署前请确保所有敏感配置已通过环境变量正确注入！");
            log.warn("========================================================");
            return;
        }

        log.info("[安全校验] 开始校验敏感配置...");

        // 校验数据库密码
        String dbPassword = requireText("spring.datasource.password", "BML_DB_PASSWORD");
        rejectWeakValue(dbPassword, "BML_DB_PASSWORD");

        // 校验 JWT 密钥
        String jwtSecret = requireText("bml.jwt.secret", "BML_JWT_SECRET");
        rejectWeakValue(jwtSecret, "BML_JWT_SECRET");
        requireMinLength(jwtSecret, 32, "BML_JWT_SECRET");

        // 校验 OpenAPI 加密密钥
        String openApiKey = requireText("bml.openapi.secret-encryption-key", "BML_OPENAPI_SECRET_ENCRYPTION_KEY");
        rejectWeakValue(openApiKey, "BML_OPENAPI_SECRET_ENCRYPTION_KEY");
        requireMinLength(openApiKey, 16, "BML_OPENAPI_SECRET_ENCRYPTION_KEY");

        log.info("[安全校验] 敏感配置校验通过。");
    }

    /**
     * 判断当前是否为本地开发环境。
     *
     * @return 如果 active profiles 包含 local 或 dev，返回 true
     */
    private boolean isLocalDevEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("local".equalsIgnoreCase(profile) || "dev".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 要求配置项存在且不为空。
     *
     * @param propertyKey Spring 配置键名
     * @param envName     对应的环境变量名（用于错误提示）
     * @return 配置值（已 trim）
     * @throws IllegalStateException 配置项缺失时抛出
     */
    private String requireText(String propertyKey, String envName) {
        String value = environment.getProperty(propertyKey);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                    String.format("[安全校验失败] 缺少必填敏感配置，请设置环境变量：%s", envName));
        }
        return value.trim();
    }

    /**
     * 拒绝弱默认值。
     *
     * @param value   配置值
     * @param envName 环境变量名（用于错误提示）
     * @throws IllegalStateException 配置值为弱默认值时抛出
     */
    private void rejectWeakValue(String value, String envName) {
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String keyword : WEAK_VALUE_KEYWORDS) {
            if (normalized.contains(keyword)) {
                throw new IllegalStateException(
                        String.format("[安全校验失败] 环境变量 %s 仍为占位/弱默认值（包含关键词：%s），请替换为安全值。",
                                envName, keyword));
            }
        }
    }

    /**
     * 要求配置值长度不低于指定最小长度。
     *
     * @param value     配置值
     * @param minLength 最小长度
     * @param envName   环境变量名（用于错误提示）
     * @throws IllegalStateException 长度不足时抛出
     */
    private void requireMinLength(String value, int minLength, String envName) {
        if (value.length() < minLength) {
            throw new IllegalStateException(
                    String.format("[安全校验失败] 环境变量 %s 长度必须至少 %d 个字符，当前长度：%d。",
                            envName, minLength, value.length()));
        }
    }
}
