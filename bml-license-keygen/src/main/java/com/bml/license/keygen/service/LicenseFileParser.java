package com.bml.license.keygen.service;

import com.bml.license.keygen.model.LicensePayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * 许可证文件解析器。
 * <p>
 * 将 {@code .lic} 文件内容解析回 {@link LicensePayload} 对象。
 * 文件格式为 {@code Base64(JSON payload) + "\n---BML-SIG---\n" + Base64(RSA-SHA256 signature)}。
 * </p>
 * <p>
 * 该类是通用工具，不依赖密钥——只做 Base64 解码 + JSON 反序列化，
 * 不验证签名有效性（签名验证由后端 {@code BmlLicenseVerifier} 负责）。
 * </p>
 *
 * @author BML Team
 */
public class LicenseFileParser {

    /** 许可证文件中 payload 与 signature 的分隔符，与 {@link LicenseFileGenerator} 保持一致 */
    private static final String SIGNATURE_SEPARATOR = "\n---BML-SIG---\n";

    private final ObjectMapper objectMapper;

    public LicenseFileParser() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 解析许可证文件，提取 {@link LicensePayload}。
     *
     * @param licenseFile 许可证文件路径（{@code .lic} 文件）
     * @return 解析出的许可证载荷对象
     * @throws LicenseParseException 文件不存在、格式错误或 JSON 反序列化失败时抛出
     */
    public LicensePayload parse(Path licenseFile) throws LicenseParseException {
        // 1. 读取文件内容
        String fileContent;
        try {
            fileContent = Files.readString(licenseFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new LicenseParseException("无法读取许可证文件: " + e.getMessage(), e);
        }

        return parseContent(fileContent);
    }

    /**
     * 解析许可证文件内容字符串，提取 {@link LicensePayload}。
     * <p>
     * 与 {@link #parse(Path)} 功能相同，但接受文件内容字符串，
     * 方便在已获取文件内容的场景下直接调用。
     * </p>
     *
     * @param fileContent 许可证文件的完整内容字符串
     * @return 解析出的许可证载荷对象
     * @throws LicenseParseException 格式错误或 JSON 反序列化失败时抛出
     */
    public LicensePayload parseContent(String fileContent) throws LicenseParseException {
        if (fileContent == null || fileContent.isBlank()) {
            throw new LicenseParseException("许可证文件内容为空");
        }

        // 2. 按分隔符拆分为 payload 和 signature 两部分
        int separatorIndex = fileContent.indexOf(SIGNATURE_SEPARATOR);
        if (separatorIndex < 0) {
            throw new LicenseParseException("许可证文件格式错误：缺少签名分隔符 ---BML-SIG---");
        }

        String payloadBase64 = fileContent.substring(0, separatorIndex).trim();
        if (payloadBase64.isEmpty()) {
            throw new LicenseParseException("许可证文件格式错误：载荷部分为空");
        }

        // 3. Base64 解码 payload
        byte[] payloadBytes;
        try {
            payloadBytes = Base64.getDecoder().decode(payloadBase64);
        } catch (IllegalArgumentException e) {
            throw new LicenseParseException("许可证载荷 Base64 解码失败: " + e.getMessage(), e);
        }

        // 4. JSON 反序列化为 LicensePayload
        String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
        try {
            return objectMapper.readValue(payloadJson, LicensePayload.class);
        } catch (Exception e) {
            throw new LicenseParseException("许可证 JSON 解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 许可证解析异常。
     * <p>
     * 当许可证文件格式不合法、Base64 解码失败或 JSON 反序列化失败时抛出。
     * </p>
     */
    public static class LicenseParseException extends Exception {

        public LicenseParseException(String message) {
            super(message);
        }

        public LicenseParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
