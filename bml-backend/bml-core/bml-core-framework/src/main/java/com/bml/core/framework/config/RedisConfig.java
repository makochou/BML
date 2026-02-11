package com.bml.core.framework.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置
 * <p>
 * 配置 {@link RedisTemplate} 的序列化策略：
 * </p>
 * <ul>
 * <li><b>Key</b> — 使用 {@link StringRedisSerializer}，确保 Key 在 Redis 中以纯字符串形式存储，
 * 方便命令行和 Redis 管理工具查看</li>
 * <li><b>Value</b> — 使用 {@link GenericJackson2JsonRedisSerializer}，将对象序列化为
 * JSON，
 * 并写入 {@code @class} 类型信息以支持反序列化时的多态还原</li>
 * </ul>
 *
 * <h3>与 JacksonConfig 的关系：</h3>
 * <p>
 * 本配置注入 Spring 容器中统一管理的 {@code ObjectMapper}（已包含 {@code JacksonConfig} 中的
 * Java 8 时间格式配置），然后在此基础上创建一份副本并激活 {@code DefaultTyping}，
 * 确保 Redis 序列化和 HTTP 响应序列化共享相同的时间格式。
 * </p>
 *
 * <h3>为什么需要 DefaultTyping？</h3>
 * <p>
 * Redis 中存储的是 JSON 字符串，反序列化时需要知道目标类型。
 * {@code DefaultTyping.NON_FINAL} 会在 JSON 中追加 {@code @class} 字段，
 * 告诉 Jackson 应当还原为哪个 Java 类。
 * </p>
 *
 * @author BML Team
 * @see JacksonConfig
 */
@Configuration
public class RedisConfig {

    /**
     * 配置 RedisTemplate
     * <p>
     * 注入 Spring 容器管理的 {@code ObjectMapper}，在此基础上创建 Redis 专用的序列化器，
     * 确保 Java 8 时间类型（{@code LocalDateTime}、{@code LocalDate} 等）在 Redis 中
     * 的序列化格式与 HTTP 响应一致。
     * </p>
     *
     * @param connectionFactory Redis 连接工厂（由 Spring Boot 自动配置）
     * @param objectMapper      Spring 容器中的 ObjectMapper（已包含 JacksonConfig 自定义配置）
     * @return 配置好序列化策略的 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 序列化：纯字符串，方便 Redis CLI 查看
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // Value 序列化：JSON + 类型信息
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = createRedisSerializer(objectMapper);
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 创建 Redis 专用 JSON 序列化器
     * <p>
     * 基于 Spring 的 {@code ObjectMapper} 创建副本，激活 {@code DefaultTyping}
     * 以支持多态类型的反序列化。
     * </p>
     *
     * <h4>安全策略：</h4>
     * <p>
     * 使用 {@link PolymorphicTypeValidator} 限制可反序列化的类型范围，
     * 防止反序列化漏洞（如 Jackson Gadget Chain 攻击）。
     * 仅允许 {@code com.bml} 包下的类型和 JDK 标准库类型。
     * </p>
     *
     * @param objectMapper Spring 管理的 ObjectMapper
     * @return Redis 专用序列化器
     */
    private GenericJackson2JsonRedisSerializer createRedisSerializer(ObjectMapper objectMapper) {
        // 1. 复制 Spring ObjectMapper 的全部配置（含 JacksonConfig 的时间格式化）
        ObjectMapper redisMapper = objectMapper.copy();

        // 2. 配置多态类型验证器（安全白名单）
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class) // 基础类型
                .allowIfSubType("com.bml") // 项目内部类型
                .allowIfSubType("java.") // JDK 标准库
                .allowIfSubType("javax.") // Jakarta / javax 扩展
                .allowIfSubType("org.springframework.security") // Spring Security 类型
                .build();

        // 3. 激活默认类型推断（NON_FINAL = 对非 final 类型写入 @class）
        redisMapper.activateDefaultTyping(ptv, DefaultTyping.NON_FINAL);

        return new GenericJackson2JsonRedisSerializer(redisMapper);
    }
}
