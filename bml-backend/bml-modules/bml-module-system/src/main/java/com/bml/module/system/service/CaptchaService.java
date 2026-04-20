package com.bml.module.system.service;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务
 * <p>
 * 基于 JDK 原生 AWT 绘制验证码图片，不依赖任何第三方验证码库。
 * 验证码文本存储在 Redis 中，TTL 默认 5 分钟。
 * </p>
 *
 * <h3>使用流程：</h3>
 * <ol>
 *   <li>前端调用 {@code GET /auth/captcha} 获取验证码图片（Base64）和唯一 key</li>
 *   <li>用户输入验证码后随登录请求一起提交 key + code</li>
 *   <li>后端调用 {@link #validateCaptcha(String, String)} 校验并销毁验证码</li>
 * </ol>
 *
 * @author BML Team
 */
@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    /** Redis 键前缀 */
    private static final String CAPTCHA_KEY_PREFIX = "bml:captcha:";
    /** 验证码有效期（分钟） */
    private static final long CAPTCHA_TTL_MINUTES = 5;
    /** 验证码字符集（去除容易混淆的 0/O/1/I/l） */
    private static final String CHAR_POOL = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";
    /** 验证码长度 */
    private static final int CODE_LENGTH = 4;
    /** 图片宽度 */
    private static final int IMG_WIDTH = 160;
    /** 图片高度 */
    private static final int IMG_HEIGHT = 48;

    private final SecureRandom random = new SecureRandom();

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成验证码。
     *
     * @return Map 包含 captchaKey（唯一标识）和 captchaImage（Base64 PNG 图片，含 data:image/png;base64, 前缀）
     */
    public Map<String, String> generateCaptcha() {
        // 1. 生成随机验证码文本
        String code = randomCode();

        // 2. 生成唯一 key 并存入 Redis
        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(
                CAPTCHA_KEY_PREFIX + captchaKey,
                code.toLowerCase(),
                CAPTCHA_TTL_MINUTES, TimeUnit.MINUTES);

        // 3. 绘制验证码图片
        String base64Image = drawCaptchaImage(code);

        log.debug("[Captcha] 生成验证码: key={}, code={}", captchaKey, code);

        return Map.of(
                "captchaKey", captchaKey,
                "captchaImage", "data:image/png;base64," + base64Image);
    }

    /**
     * 校验验证码。
     * <p>
     * 校验成功后自动删除 Redis 中的验证码记录（一次性有效）。
     * </p>
     *
     * @param captchaKey  验证码唯一标识
     * @param captchaCode 用户输入的验证码
     * @return true 校验通过，false 校验失败
     */
    public boolean validateCaptcha(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaCode == null) {
            return false;
        }
        String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (storedCode == null) {
            return false;
        }
        // 校验后立即删除（一次性使用）
        stringRedisTemplate.delete(redisKey);
        return storedCode.equalsIgnoreCase(captchaCode.trim());
    }

    /**
     * 生成随机验证码文本。
     */
    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHAR_POOL.charAt(random.nextInt(CHAR_POOL.length())));
        }
        return sb.toString();
    }

    /**
     * 绘制验证码图片并返回 Base64 编码。
     * <p>
     * 包含随机颜色字符、旋转变换、干扰线条和噪点，有效防止 OCR 识别。
     * </p>
     */
    private String drawCaptchaImage(String code) {
        BufferedImage image = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();

        // 开启抗锯齿
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        // 填充背景（浅色渐变）
        GradientPaint bgGradient = new GradientPaint(
                0, 0, new Color(245, 247, 250),
                IMG_WIDTH, IMG_HEIGHT, new Color(235, 238, 245));
        g2.setPaint(bgGradient);
        g2.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);

        // 绘制干扰线
        for (int i = 0; i < 5; i++) {
            g2.setColor(randomLightColor());
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawLine(
                    random.nextInt(IMG_WIDTH), random.nextInt(IMG_HEIGHT),
                    random.nextInt(IMG_WIDTH), random.nextInt(IMG_HEIGHT));
        }

        // 绘制干扰噪点
        for (int i = 0; i < 30; i++) {
            g2.setColor(randomLightColor());
            g2.fillOval(random.nextInt(IMG_WIDTH), random.nextInt(IMG_HEIGHT), 2, 2);
        }

        // 绘制验证码字符
        Font font = new Font("SansSerif", Font.BOLD, 30);
        g2.setFont(font);
        int charSpacing = (IMG_WIDTH - 20) / CODE_LENGTH;
        for (int i = 0; i < code.length(); i++) {
            AffineTransform transform = new AffineTransform();
            double angle = (random.nextDouble() - 0.5) * 0.5; // -15° ~ +15°
            int x = 10 + i * charSpacing + random.nextInt(6);
            int y = 32 + random.nextInt(6);
            transform.setToRotation(angle, x, y);
            g2.setTransform(transform);
            g2.setColor(randomDarkColor());
            g2.drawString(String.valueOf(code.charAt(i)), x, y);
        }

        g2.dispose();

        // 编码为 Base64
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            log.error("[Captcha] 验证码图片编码失败", e);
            return "";
        }
    }

    /** 生成随机深色（用于字符） */
    private Color randomDarkColor() {
        return new Color(
                random.nextInt(80) + 20,
                random.nextInt(80) + 20,
                random.nextInt(80) + 80);
    }

    /** 生成随机浅色（用于干扰元素） */
    private Color randomLightColor() {
        return new Color(
                random.nextInt(60) + 180,
                random.nextInt(60) + 180,
                random.nextInt(60) + 180);
    }
}
