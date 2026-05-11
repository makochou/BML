package com.bml.core.framework.license;

import com.bml.core.framework.runtime.BmlRuntimePaths;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * BML 许可证持有者（全局单例）。
 * <p>
 * 职责：
 * <ol>
 *     <li>应用启动时从磁盘加载许可证文件并验证签名；</li>
 *     <li>缓存解析后的 {@link BmlLicense} 对象供全局使用；</li>
 *     <li>定时重新加载许可证文件，确保管理员上传新文件后自动生效；</li>
 *     <li>提供许可证状态查询方法，供拦截器和控制器调用。</li>
 * </ol>
 * </p>
 * <p>
 * 许可证文件路径由 {@link LicenseProperties#getStorageDir()} 和
 * {@link LicenseProperties#getFileName()} 决定，默认落盘到 {@code bml-app/License/bml-license.lic}。
 * </p>
 *
 * @author BML Team
 */
@Slf4j
@Component
public class BmlLicenseHolder {

    private final LicenseProperties licenseProperties;
    private final ObjectMapper objectMapper;

    /** 许可证签名验证器（公钥不为空时初始化） */
    private volatile BmlLicenseVerifier verifier;

    /** 当前生效的许可证（通过签名验证后缓存） */
    @Getter
    private volatile BmlLicense currentLicense;

    /** 最近一次加载的错误消息 */
    @Getter
    private volatile String lastError;

    public BmlLicenseHolder(LicenseProperties licenseProperties) {
        this.licenseProperties = licenseProperties;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /** 旧版本许可证存储目录（用于自动迁移） */
    private static final String[] LEGACY_STORAGE_DIRS = {"./License", "./data/license", "./config/license"};

    @PostConstruct
    public void init() {
        if (!licenseProperties.isEnabled()) {
            log.info("[License] 许可证校验已关闭 (bml.license.enabled=false)");
            return;
        }
        migrateLegacyLicenseFile();
        initVerifier();
        // 确保许可证存储目录存在，避免用户找不到路径
        Path licensePath = resolveLicensePath();
        try {
            Files.createDirectories(licensePath.getParent());
        } catch (IOException ex) {
            log.warn("[License] 创建许可证存储目录失败: {}", ex.getMessage());
        }
        log.info("[License] 许可证文件路径: {}", licensePath.toAbsolutePath());
        reload();
    }

    /**
     * 自动迁移旧版本许可证文件。
     * <p>
     * 遍历所有旧路径，如果旧路径下存在许可证文件而新路径不存在，
     * 则自动复制到新路径，并保留旧文件作为备份。
     * </p>
     */
    private void migrateLegacyLicenseFile() {
        Path newPath = resolveLicensePath();
        if (Files.exists(newPath)) {
            return;
        }
        for (String legacyDir : LEGACY_STORAGE_DIRS) {
            Path legacyPath = Path.of(legacyDir, licenseProperties.getFileName());
            if (Files.exists(legacyPath)) {
                try {
                    Files.createDirectories(newPath.getParent());
                    Files.copy(legacyPath, newPath, StandardCopyOption.COPY_ATTRIBUTES);
                    log.info("[License] 已将许可证文件从 {} 迁移到 {}", legacyPath, newPath);
                    return;
                } catch (IOException ex) {
                    log.warn("[License] 许可证文件迁移失败 ({}): {}", legacyPath, ex.getMessage());
                }
            }
        }
    }

    /**
     * 定时重新加载许可证文件。
     * <p>
     * 间隔由 {@code bml.license.refresh-interval-ms} 控制，默认 5 分钟。
     * </p>
     */
    @Scheduled(fixedDelayString = "${bml.license.refresh-interval-ms:300000}",
            initialDelayString = "${bml.license.refresh-interval-ms:300000}")
    public void scheduledReload() {
        if (licenseProperties.isEnabled()) {
            reload();
        }
    }

    /**
     * 手动触发许可证重新加载（上传新许可证后调用）。
     */
    public synchronized void reload() {
        Path licensePath = resolveLicensePath();
        if (!Files.exists(licensePath)) {
            this.currentLicense = null;
            this.lastError = "许可证文件不存在: " + licensePath;
            log.warn("[License] {}", lastError);
            return;
        }

        try {
            String fileContent = Files.readString(licensePath, StandardCharsets.UTF_8);
            loadFromContent(fileContent);
        } catch (IOException ex) {
            this.currentLicense = null;
            this.lastError = "读取许可证文件失败: " + ex.getMessage();
            log.error("[License] {}", lastError, ex);
        }
    }

    /**
     * 从许可证文件内容加载并验证。
     *
     * @param fileContent 许可证文件完整内容
     * @return 加载是否成功
     */
    public synchronized boolean loadFromContent(String fileContent) {
        if (verifier == null) {
            this.lastError = "许可证公钥未配置，无法验证";
            log.error("[License] {}", lastError);
            return false;
        }

        BmlLicenseVerifier.VerifyResult result = verifier.verify(fileContent);
        if (!result.valid()) {
            this.currentLicense = null;
            this.lastError = result.errorMessage();
            log.error("[License] 签名验证失败: {}", lastError);
            return false;
        }

        try {
            BmlLicense license = objectMapper.readValue(result.payloadJson(), BmlLicense.class);
            if (license.isExpired()) {
                this.currentLicense = license;
                this.lastError = "许可证已过期，到期日: " + license.getExpireDate();
                log.warn("[License] {}", lastError);
                return true;
            }
            this.currentLicense = license;
            this.lastError = null;
            log.info("[License] 许可证加载成功 — 客户: {}, 有效期至: {}",
                    license.getCustomerName(), license.getExpireDate());
            return true;
        } catch (Exception ex) {
            this.currentLicense = null;
            this.lastError = "许可证 JSON 解析失败: " + ex.getMessage();
            log.error("[License] {}", lastError, ex);
            return false;
        }
    }

    /**
     * 预览许可证文件内容（仅验证和解析，不替换当前许可证）。
     * <p>
     * 用于「更新许可证」前的预览对比，避免直接覆盖当前许可证。
     * </p>
     *
     * @param fileContent 许可证文件完整内容
     * @return 解析后的许可证对象
     * @throws com.bml.core.common.exception.BusinessException 验证或解析失败时抛出
     */
    public BmlLicense previewLicense(String fileContent) {
        if (verifier == null) {
            throw new com.bml.core.common.exception.BusinessException(
                    com.bml.core.common.enums.GlobalErrorCode.LICENSE_INVALID.getCode(),
                    "许可证公钥未配置，无法验证");
        }
        BmlLicenseVerifier.VerifyResult result = verifier.verify(fileContent);
        if (!result.valid()) {
            throw new com.bml.core.common.exception.BusinessException(
                    com.bml.core.common.enums.GlobalErrorCode.LICENSE_INVALID.getCode(),
                    result.errorMessage() != null ? result.errorMessage() : "许可证签名验证失败");
        }
        try {
            return objectMapper.readValue(result.payloadJson(), BmlLicense.class);
        } catch (Exception ex) {
            throw new com.bml.core.common.exception.BusinessException(
                    com.bml.core.common.enums.GlobalErrorCode.LICENSE_PARSE_ERROR.getCode(),
                    "许可证解析失败: " + ex.getMessage());
        }
    }

    /**
     * 判断当前是否持有有效许可证（存在且未过期）。
     *
     * @return 有效返回 {@code true}
     */
    public boolean isLicenseValid() {
        if (!licenseProperties.isEnabled()) {
            return true;
        }
        BmlLicense license = this.currentLicense;
        return license != null && !license.isExpired();
    }

    /**
     * 判断是否启用了许可证校验。
     *
     * @return 启用返回 {@code true}
     */
    public boolean isEnabled() {
        return licenseProperties.isEnabled();
    }

    /**
     * 获取许可证文件的完整路径。
     *
     * @return 许可证文件路径
     */
    public Path resolveLicensePath() {
        return BmlRuntimePaths.resolveRuntimePath(licenseProperties.getStorageDir())
                .resolve(licenseProperties.getFileName())
                .normalize();
    }

    /** 备份文件时间戳格式 */
    private static final DateTimeFormatter BACKUP_TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 备份当前许可证文件。
     * <p>
     * 将当前许可证文件复制为 {@code bml-license_20260415213000.lic.bak} 格式的备份文件。
     * 如果当前许可证文件不存在则跳过。
     * </p>
     *
     * @return 备份文件路径，许可证文件不存在时返回 {@code null}
     * @throws IOException 文件操作失败时抛出
     */
    public Path backupCurrentLicense() throws IOException {
        Path licensePath = resolveLicensePath();
        if (!Files.exists(licensePath)) {
            log.info("[License] 许可证文件不存在，无需备份");
            return null;
        }
        String baseName = licenseProperties.getFileName().replace(".lic", "");
        String timestamp = LocalDateTime.now().format(BACKUP_TS_FMT);
        String backupName = baseName + "_" + timestamp + ".lic.bak";
        Path backupPath = licensePath.getParent().resolve(backupName);
        Files.copy(licensePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("[License] 许可证文件已备份: {}", backupPath.toAbsolutePath());
        return backupPath;
    }

    /**
     * 初始化 RSA 公钥验证器。
     */
    private void initVerifier() {
        String publicKey = licenseProperties.getPublicKey();
        if (publicKey == null || publicKey.isBlank()) {
            log.warn("[License] 许可证公钥未配置 (bml.license.public-key)，许可证验证将无法工作");
            return;
        }
        try {
            this.verifier = new BmlLicenseVerifier(publicKey);
            log.info("[License] RSA 公钥验证器初始化成功");
        } catch (Exception ex) {
            log.error("[License] RSA 公钥初始化失败", ex);
        }
    }
}
