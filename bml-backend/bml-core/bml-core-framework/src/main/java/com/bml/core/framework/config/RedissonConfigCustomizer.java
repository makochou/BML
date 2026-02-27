package com.bml.core.framework.config;

import org.redisson.config.BaseConfig;
import org.redisson.config.Config;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

@Configuration
public class RedissonConfigCustomizer {

    @Bean
    public RedissonAutoConfigurationCustomizer sanitizeRedisCredentialsCustomizer() {
        return config -> {
            clearBlankCredentials(config, "getSingleServerConfig");
            clearBlankCredentials(config, "getClusterServersConfig");
            clearBlankCredentials(config, "getReplicatedServersConfig");
            clearBlankCredentials(config, "getSentinelServersConfig");
            clearBlankCredentials(config, "getMasterSlaveServersConfig");
        };
    }

    private void clearBlankCredentials(Config config, String getterName) {
        try {
            Method getter = Config.class.getDeclaredMethod(getterName);
            getter.setAccessible(true);
            Object serverConfig = getter.invoke(config);
            if (serverConfig instanceof BaseConfig<?> baseConfig) {
                if (!StringUtils.hasText(baseConfig.getPassword())) {
                    baseConfig.setPassword(null);
                }
                if (!StringUtils.hasText(baseConfig.getUsername())) {
                    baseConfig.setUsername(null);
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to sanitize Redisson config via " + getterName, ex);
        }
    }
}
