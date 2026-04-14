package com.bml.license.keygen.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 密钥对管理器。
 * <p>
 * 职责：
 * <ol>
 *     <li>生成 RSA-2048 密钥对并持久化到本地文件；</li>
 *     <li>从文件加载已有的密钥对；</li>
 *     <li>导出公钥 Base64 字符串，便于配置到后端 {@code application.yml}。</li>
 * </ol>
 * </p>
 * <p>
 * 密钥文件默认存储在 {@code ./keys/} 目录下：
 * <ul>
 *     <li>{@code bml-license-private.key} — 私钥（PKCS#8 DER，Base64 编码）</li>
 *     <li>{@code bml-license-public.key} — 公钥（X.509 DER，Base64 编码）</li>
 * </ul>
 * </p>
 *
 * @author BML Team
 */
public class RsaKeyPairManager {

    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String PRIVATE_KEY_FILE = "bml-license-private.key";
    private static final String PUBLIC_KEY_FILE = "bml-license-public.key";

    private final Path keysDir;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    /**
     * 构造管理器。
     *
     * @param keysDir 密钥文件存储目录
     */
    public RsaKeyPairManager(Path keysDir) {
        this.keysDir = keysDir;
    }

    /**
     * 初始化密钥对：若文件已存在则加载，否则自动生成。
     *
     * @throws Exception 密钥生成或加载异常
     */
    public void init() throws Exception {
        Path privateKeyPath = keysDir.resolve(PRIVATE_KEY_FILE);
        Path publicKeyPath = keysDir.resolve(PUBLIC_KEY_FILE);

        if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
            loadKeys(privateKeyPath, publicKeyPath);
            System.out.println("  [密钥] 已加载现有 RSA 密钥对");
        } else {
            generateKeys(privateKeyPath, publicKeyPath);
            System.out.println("  [密钥] 已生成新的 RSA-2048 密钥对");
        }
    }

    /**
     * 获取私钥（用于签名许可证）。
     */
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * 获取公钥（用于配置到后端验证许可证）。
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * 获取公钥的 Base64 编码字符串（无换行，可直接配置到 application.yml）。
     */
    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * 判断密钥对是否已就绪。
     */
    public boolean isReady() {
        return privateKey != null && publicKey != null;
    }

    /**
     * 生成新的 RSA 密钥对并保存到文件。
     */
    private void generateKeys(Path privateKeyPath, Path publicKeyPath) throws Exception {
        Files.createDirectories(keysDir);
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        generator.initialize(KEY_SIZE, new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();

        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();

        // 保存私钥（PKCS#8 DER，Base64）
        String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        Files.writeString(privateKeyPath, privateKeyBase64, StandardCharsets.UTF_8);

        // 保存公钥（X.509 DER，Base64）
        String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        Files.writeString(publicKeyPath, publicKeyBase64, StandardCharsets.UTF_8);
    }

    /**
     * 从文件加载已有密钥对。
     */
    private void loadKeys(Path privateKeyPath, Path publicKeyPath) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);

        String privateKeyBase64 = Files.readString(privateKeyPath, StandardCharsets.UTF_8).trim();
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
        this.privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

        String publicKeyBase64 = Files.readString(publicKeyPath, StandardCharsets.UTF_8).trim();
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        this.publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
    }
}
