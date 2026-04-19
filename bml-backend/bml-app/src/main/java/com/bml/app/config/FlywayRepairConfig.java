package com.bml.app.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway 自动修复配置类
 * 
 * <p>用于解决开发环境下由于换行符（CRLF/LF）或文件微调导致的 Checksum mismatch 报错。</p>
 * <p>该策略会在项目启动执行 migrate 之前先执行一次 repair，自动更新数据库中的校验码记录。</p>
 */
@Configuration
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // 在执行迁移前自动修复校验码，解决文件换行符或空格导致的 Checksum mismatch
            // 也会清理之前执行失败的残留记录
            flyway.repair();
            
            // 继续执行正常的数据库迁移
            flyway.migrate();
        };
    }
}
