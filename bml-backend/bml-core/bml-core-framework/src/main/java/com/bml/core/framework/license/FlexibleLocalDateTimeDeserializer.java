package com.bml.core.framework.license;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 许可证日期时间灵活反序列化器。
 * <p>
 * 同时兼容两种日期格式，确保新旧版本许可证文件都能正确解析：
 * <ul>
 *     <li>{@code yyyy-MM-dd HH:mm:ss} — 新版本许可证使用的完整日期时间格式</li>
 *     <li>{@code yyyy-MM-dd} — 旧版本许可证使用的仅日期格式（自动补充为当天 00:00:00）</li>
 * </ul>
 * Jackson 内置的 {@code JavaTimeModule} 默认序列化 {@code LocalDateTime} 为 ISO 格式
 * （如 {@code 2026-04-20T14:30:00}），此反序列化器额外兼容空格分隔和仅日期两种输入。
 * </p>
 *
 * @author BML Team
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    /** 新版本许可证日期时间格式：yyyy-MM-dd HH:mm:ss */
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 旧版本许可证仅日期格式：yyyy-MM-dd */
    private static final DateTimeFormatter DATE_ONLY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText().trim();
        if (text.isEmpty()) {
            return null;
        }

        // 1. 尝试 ISO 标准格式（Jackson JavaTimeModule 默认输出的格式，含 'T' 分隔符）
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            // 继续尝试其他格式
        }

        // 2. 尝试空格分隔的完整日期时间格式：yyyy-MM-dd HH:mm:ss
        try {
            return LocalDateTime.parse(text, DATETIME_FMT);
        } catch (DateTimeParseException ignored) {
            // 继续尝试仅日期格式
        }

        // 3. 回退到仅日期格式：yyyy-MM-dd → 补充为当天 00:00:00
        try {
            return LocalDate.parse(text, DATE_ONLY_FMT).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new IOException(
                    String.format("无法解析许可证日期字段 '%s'，支持的格式: yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd", text), e);
        }
    }
}
