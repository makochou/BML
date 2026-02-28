package com.bml.core.framework.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * Jackson 全局序列化配置
 * <p>
 * 使用 {@link Jackson2ObjectMapperBuilderCustomizer} 扩展 Spring Boot 自动配置的
 * {@code ObjectMapper}，
 * 而非手动创建新的 {@code ObjectMapper} Bean（避免覆盖 Spring Boot 默认的模块注册和属性绑定）。
 * </p>
 *
 * <h3>配置内容：</h3>
 * <ul>
 * <li>{@code LocalDateTime} → {@code yyyy-MM-dd HH:mm:ss}</li>
 * <li>{@code LocalDate} → {@code yyyy-MM-dd}</li>
 * <li>{@code LocalTime} → {@code HH:mm:ss}</li>
 * <li>禁用 {@code WRITE_DATES_AS_TIMESTAMPS}（日期不序列化为时间戳）</li>
 * <li>禁用 {@code FAIL_ON_UNKNOWN_PROPERTIES}（忽略未知属性，提高兼容性）</li>
 * </ul>
 *
 * <h3>与原方案的区别：</h3>
 * <table>
 * <tr>
 * <th>原方案</th>
 * <th>新方案</th>
 * </tr>
 * <tr>
 * <td>{@code @Bean ObjectMapper} + {@code @Primary}</td>
 * <td>{@code @Bean Jackson2ObjectMapperBuilderCustomizer}</td>
 * </tr>
 * <tr>
 * <td>覆盖 Spring Boot 自动配置</td>
 * <td>扩展 Spring Boot 自动配置</td>
 * </tr>
 * <tr>
 * <td>需手动注册所有模块</td>
 * <td>Spring Boot 自动注册 + 自定义追加</td>
 * </tr>
 * </table>
 *
 * @author BML Team
 * @see org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
 */
@Configuration
public class JacksonConfig {

    /** 日期时间格式：yyyy-MM-dd HH:mm:ss */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 日期格式：yyyy-MM-dd */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /** 时间格式：HH:mm:ss */
    private static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 自定义 Jackson ObjectMapper 构建器
     * <p>
     * Spring Boot 会在创建 {@code ObjectMapper} 时自动调用所有
     * {@code Jackson2ObjectMapperBuilderCustomizer}
     * Bean，按 {@code @Order} 排序。这确保了我们的自定义配置与 Spring Boot 的默认配置（如
     * {@code ParameterNamesModule}、{@code Jdk8Module} 等）共存。
     * </p>
     *
     * @return 自定义构建器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // Java 8+ 时间类型序列化格式
            builder.serializers(
                    new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)),
                    new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)),
                    new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));

            // Java 8+ 时间类型反序列化格式
            builder.deserializers(
                    new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)),
                    new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_PATTERN)),
                    new LocalTimeDeserializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));

            // 统一将 Long 序列化为字符串，避免前端 JS Number 精度丢失
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);

            // 禁用日期时间戳格式（输出人类可读的字符串格式）
            builder.featuresToDisable(
                    com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // 忽略未知属性（提高前后端兼容性，避免因字段不匹配导致反序列化失败）
            builder.featuresToDisable(
                    com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        };
    }
}
