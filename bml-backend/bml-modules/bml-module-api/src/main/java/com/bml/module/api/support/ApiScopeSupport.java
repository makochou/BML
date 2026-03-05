package com.bml.module.api.support;

import cn.hutool.core.util.StrUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * API 账号授权范围（Scope）支持工具。
 * <p>
 * 对标 OAuth2 scope 概念，为 API 账号提供粗粒度的授权范围标签管理。
 * Scope 标签用于鉴权链路前置快速判断请求是否属于账号允许的业务范围，
 * 不替代已有的细粒度 API 接口授权功能。
 * </p>
 * <p>
 * 数据库存储格式：逗号分隔的小写字符串，例如 "read,write,admin"。
 * 工具类提供序列化 / 反序列化 / 标准化方法，格式与 {@link ApiClientTypeSupport} 保持一致。
 * </p>
 *
 * <h3>预置范围说明</h3>
 * <ul>
 * <li>{@code read} — 只读访问，仅允许查询类接口</li>
 * <li>{@code write} — 读写访问，允许查询和变更类接口</li>
 * <li>{@code admin} — 管理访问，允许管理类接口（如配置变更、用户管理等）</li>
 * <li>{@code export} — 导出访问，允许数据导出类接口</li>
 * <li>{@code callback} — 回调访问，允许回调通知类接口</li>
 * </ul>
 * <p>
 * 如需扩展新范围，在 {@link #SUPPORTED_SCOPES} 列表中追加即可。
 * </p>
 */
public final class ApiScopeSupport {

    /** 只读访问 */
    public static final String READ = "read";

    /** 读写访问 */
    public static final String WRITE = "write";

    /** 管理访问 */
    public static final String ADMIN = "admin";

    /** 导出访问 */
    public static final String EXPORT = "export";

    /** 回调访问 */
    public static final String CALLBACK = "callback";

    /**
     * 当前支持的授权范围列表，顺序决定序列化后的排列顺序。
     * 后续扩展新范围只需在此追加即可。
     */
    private static final List<String> SUPPORTED_SCOPES = List.of(
            READ,
            WRITE,
            ADMIN,
            EXPORT,
            CALLBACK);

    private ApiScopeSupport() {
    }

    /**
     * 获取当前支持的所有授权范围列表（不可变）。
     *
     * @return 支持的授权范围列表
     */
    public static List<String> getSupportedScopes() {
        return SUPPORTED_SCOPES;
    }

    /**
     * 标准化单个 scope 值：去空白、转小写并校验合法性。
     *
     * @param scope 原始 scope 值
     * @return 标准化后的 scope 值，如果输入为空则返回 null
     */
    public static String normalizeSingleScope(String scope) {
        String normalized = StrUtil.trimToNull(scope);
        if (normalized == null) {
            return null;
        }
        String code = normalized.toLowerCase();
        if (!SUPPORTED_SCOPES.contains(code)) {
            // 不抛异常，允许自定义 scope，但预置范围优先排序
            return code;
        }
        return code;
    }

    /**
     * 标准化 scope 集合：去重、排序（预置范围优先，自定义范围追尾）。
     *
     * @param scopes 原始 scope 集合
     * @return 标准化后的 scope 列表
     */
    public static List<String> normalizeScopes(Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String scope : scopes) {
            String code = normalizeSingleScope(scope);
            if (code != null) {
                normalized.add(code);
            }
        }
        // 预置范围按定义顺序排在前面，自定义范围追尾
        List<String> ordered = new java.util.ArrayList<>(
                SUPPORTED_SCOPES.stream().filter(normalized::contains).toList());
        normalized.stream()
                .filter(s -> !SUPPORTED_SCOPES.contains(s))
                .forEach(ordered::add);
        return Collections.unmodifiableList(ordered);
    }

    /**
     * 将 scope 集合序列化为逗号分隔字符串，用于数据库存储。
     *
     * @param scopes scope 集合
     * @return 逗号分隔字符串，如果集合为空则返回 null
     */
    public static String serializeScopes(Collection<String> scopes) {
        List<String> normalized = normalizeScopes(scopes);
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    /**
     * 将逗号分隔字符串反序列化为 scope 列表。
     *
     * @param serialized 数据库中存储的逗号分隔字符串
     * @return scope 列表
     */
    public static List<String> deserializeScopes(String serialized) {
        if (StrUtil.isBlank(serialized)) {
            return Collections.emptyList();
        }
        return normalizeScopes(StrUtil.splitTrim(serialized, ','));
    }
}
