package com.bml.core.framework.license;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * BML 许可证配置属性。
 * <p>
 * 对应 {@code application.yml} 中 {@code bml.license.*} 前缀的配置项。
 * </p>
 *
 * @author BML Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "bml.license")
public class LicenseProperties {

    /**
     * 是否启用许可证校验。
     * <p>
     * 开发环境可设为 {@code false} 跳过许可证检查，生产环境必须为 {@code true}。
     * </p>
     */
    private boolean enabled = true;

    /**
     * 许可证文件存储目录。
     * <p>
     * 上传的 {@code .lic} 文件将保存在此目录下，文件名固定为 {@code bml-license.lic}。
     * </p>
     */
    private String storageDir = "License";

    /**
     * 许可证文件名。
     */
    private String fileName = "bml-license.lic";

    /**
     * RSA 公钥（Base64 编码，X.509 格式，不含 PEM 头尾标记）。
     * <p>
     * 该公钥由离线签发工具生成，用于验证许可证文件的签名。
     * 生产环境必须通过环境变量 {@code BML_LICENSE_PUBLIC_KEY} 注入。
     * </p>
     */
    private String publicKey;

    /**
     * 许可证刷新间隔（毫秒），默认 5 分钟。
     * <p>
     * 系统定时重新加载许可证文件，确保管理员上传新许可证后能自动生效。
     * </p>
     */
    private long refreshIntervalMs = 300000;
}
