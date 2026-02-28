package com.bml.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SensitiveConfigValidatorTest {

    @Test
    void shouldFailWhenSensitiveConfigMissing() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.password", "")
                .withProperty("bml.jwt.secret", "")
                .withProperty("bml.openapi.secret-encryption-key", "");

        SensitiveConfigValidator validator = new SensitiveConfigValidator(env);
        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void shouldFailWhenSensitiveConfigUsesWeakPlaceholder() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.password", "please-set-db-password")
                .withProperty("bml.jwt.secret", "local-dev-jwt-secret-change-me-please")
                .withProperty("bml.openapi.secret-encryption-key", "local-openapi-encryption-key-change-me");

        SensitiveConfigValidator validator = new SensitiveConfigValidator(env);
        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void shouldPassWhenSensitiveConfigIsStrongEnough() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.password", "S3cure_Db_Pwd_2026!")
                .withProperty("bml.jwt.secret", "JWT-SECRET-IS-VERY-STRONG-AND-LONG-1234567890")
                .withProperty("bml.openapi.secret-encryption-key", "OPENAPI-ENCRYPTION-KEY-2026");

        SensitiveConfigValidator validator = new SensitiveConfigValidator(env);
        assertDoesNotThrow(validator::validate);
    }
}
