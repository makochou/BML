package com.bml.app.config;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.Locale;

/**
 * 生产敏感配置启动校验。
 * <p>
 * 防止凭据缺失或使用弱默认值直接启动。
 * </p>
 */
@Component
public class SensitiveConfigValidator {

    private final Environment environment;

    public SensitiveConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        String dbPassword = requireText("spring.datasource.password", "BML_DB_PASSWORD");
        // 数据库密码允许较短，但不允许明显占位值
        rejectWeakValue(dbPassword, "BML_DB_PASSWORD");

        String jwtSecret = requireText("bml.jwt.secret", "BML_JWT_SECRET");
        rejectWeakValue(jwtSecret, "BML_JWT_SECRET");
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("环境变量 BML_JWT_SECRET 长度必须至少 32 个字符");
        }

        String openApiKey = requireText("bml.openapi.secret-encryption-key", "BML_OPENAPI_SECRET_ENCRYPTION_KEY");
        rejectWeakValue(openApiKey, "BML_OPENAPI_SECRET_ENCRYPTION_KEY");
        if (openApiKey.length() < 16) {
            throw new IllegalStateException("环境变量 BML_OPENAPI_SECRET_ENCRYPTION_KEY 长度必须至少 16 个字符");
        }
    }

    private String requireText(String propertyKey, String envName) {
        String value = environment.getProperty(propertyKey);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("缺少必填敏感配置，请设置环境变量 " + envName);
        }
        return value.trim();
    }

    private void rejectWeakValue(String value, String envName) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("change-me")
                || normalized.contains("changeme")
                || normalized.contains("please-set")
                || normalized.contains("example")
                || normalized.contains("local-dev")
                || normalized.contains("local-openapi")) {
            throw new IllegalStateException("环境变量 " + envName + " 仍为占位/弱默认值，请替换为安全值");
        }
    }
}
