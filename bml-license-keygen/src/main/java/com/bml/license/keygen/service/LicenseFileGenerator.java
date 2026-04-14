package com.bml.license.keygen.service;

import com.bml.license.keygen.model.LicensePayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

/**
 * 许可证文件生成器。
 * <p>
 * 将 {@link LicensePayload} 序列化为 JSON，使用 RSA-SHA256 私钥签名，
 * 生成格式为 {@code Base64(payload) + 分隔符 + Base64(signature)} 的 .lic 文件。
 * </p>
 *
 * @author BML Team
 */
public class LicenseFileGenerator {

    private static final String SIGNATURE_SEPARATOR = "\n---BML-SIG---\n";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private final ObjectMapper objectMapper;
    private final PrivateKey privateKey;

    public LicenseFileGenerator(PrivateKey privateKey) {
        this.privateKey = privateKey;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 生成许可证文件。
     *
     * @param payload    许可证载荷
     * @param outputPath 输出文件路径
     * @return 生成的文件路径
     * @throws Exception 生成过程异常
     */
    public Path generate(LicensePayload payload, Path outputPath) throws Exception {
        String payloadJson = objectMapper.writeValueAsString(payload);
        byte[] payloadBytes = payloadJson.getBytes(StandardCharsets.UTF_8);

        String payloadBase64 = Base64.getEncoder().encodeToString(payloadBytes);

        Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
        sig.initSign(privateKey);
        sig.update(payloadBytes);
        byte[] signatureBytes = sig.sign();
        String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);

        String fileContent = payloadBase64 + SIGNATURE_SEPARATOR + signatureBase64;

        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, fileContent, StandardCharsets.UTF_8);
        return outputPath;
    }

    /**
     * 将 payload 序列化为格式化 JSON（用于控制台预览）。
     */
    public String toFormattedJson(LicensePayload payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }
}
