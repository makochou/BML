package com.bml.core.framework.license;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * BML 许可证签名验证器。
 * <p>
 * 使用 RSA-SHA256 算法验证许可证文件的完整性与真实性。
 * 公钥编译进后端 JAR 包，私钥仅保留在离线签发工具中，确保客户无法伪造许可证。
 * </p>
 * <p>
 * 许可证文件格式：
 * <pre>
 *   Base64(JSON payload)
 *   ---BML-SIG---
 *   Base64(RSA-SHA256 signature)
 * </pre>
 * </p>
 *
 * @author BML Team
 */
@Slf4j
public class BmlLicenseVerifier {

    /** 许可证文件中 payload 与 signature 的分隔符 */
    public static final String SIGNATURE_SEPARATOR = "\n---BML-SIG---\n";

    /** 签名算法 */
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /** 密钥算法 */
    private static final String KEY_ALGORITHM = "RSA";

    private final PublicKey publicKey;

    /**
     * 构造验证器。
     *
     * @param publicKeyBase64 Base64 编码的 RSA 公钥（X.509 格式，不含 PEM 头尾标记）
     * @throws IllegalStateException 公钥格式异常
     */
    public BmlLicenseVerifier(String publicKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(
                    publicKeyBase64.replaceAll("\\s+", ""));
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            this.publicKey = keyFactory.generatePublic(keySpec);
        } catch (Exception ex) {
            throw new IllegalStateException("初始化许可证公钥失败", ex);
        }
    }

    /**
     * 解析并验证许可证文件内容。
     *
     * @param licenseFileContent 许可证文件完整内容
     * @return 验证结果，包含解析后的 payload 或错误信息
     */
    public VerifyResult verify(String licenseFileContent) {
        if (licenseFileContent == null || licenseFileContent.isBlank()) {
            return VerifyResult.failure("许可证文件内容为空");
        }

        // 分离 payload 与 signature
        int separatorIndex = licenseFileContent.indexOf(SIGNATURE_SEPARATOR);
        if (separatorIndex < 0) {
            return VerifyResult.failure("许可证文件格式错误，缺少签名分隔符");
        }

        String payloadBase64 = licenseFileContent.substring(0, separatorIndex).trim();
        String signatureBase64 = licenseFileContent.substring(
                separatorIndex + SIGNATURE_SEPARATOR.length()).trim();

        if (payloadBase64.isEmpty() || signatureBase64.isEmpty()) {
            return VerifyResult.failure("许可证文件格式错误，payload 或签名为空");
        }

        // Base64 解码 payload
        byte[] payloadBytes;
        try {
            payloadBytes = Base64.getDecoder().decode(payloadBase64);
        } catch (IllegalArgumentException ex) {
            return VerifyResult.failure("许可证 payload Base64 解码失败");
        }

        // Base64 解码 signature
        byte[] signatureBytes;
        try {
            signatureBytes = Base64.getDecoder().decode(signatureBase64);
        } catch (IllegalArgumentException ex) {
            return VerifyResult.failure("许可证签名 Base64 解码失败");
        }

        // RSA-SHA256 验签
        try {
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(payloadBytes);
            if (!sig.verify(signatureBytes)) {
                return VerifyResult.failure("许可证签名校验失败，文件可能被篡改");
            }
        } catch (Exception ex) {
            log.error("许可证签名验证异常", ex);
            return VerifyResult.failure("许可证签名验证过程异常");
        }

        // 返回 JSON payload 原文
        String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
        return VerifyResult.success(payloadJson);
    }

    /**
     * 验证结果封装。
     */
    public record VerifyResult(boolean valid, String payloadJson, String errorMessage) {

        public static VerifyResult success(String payloadJson) {
            return new VerifyResult(true, payloadJson, null);
        }

        public static VerifyResult failure(String errorMessage) {
            return new VerifyResult(false, null, errorMessage);
        }
    }
}
