package com.bml.module.api.support;

import cn.hutool.core.util.StrUtil;
import com.bml.core.common.exception.BusinessException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * API 账号客户端类型支持工具。
 * <p>
 * 客户端类型作为 API 账号的重要运营维度，需要兼顾三个目标：
 * 1. 前端可多选，满足同一账号服务多个终端渠道的场景。
 * 2. 数据库存储保持轻量，便于在当前阶段快速接入现有账号模型。
 * 3. 对外暴露统一代码值，避免后续前后端各自维护不一致的枚举。
 * </p>
 */
public final class ApiClientTypeSupport {

    public static final String WEB = "web";
    public static final String H5 = "h5";
    public static final String APP = "app";
    public static final String MINI_PROGRAM = "mini_program";
    public static final String SERVER = "server";
    public static final String THIRD_PARTY = "third_party";
    public static final String OTHER = "other";

    private static final List<String> SUPPORTED_CLIENT_TYPES = List.of(
            WEB,
            H5,
            APP,
            MINI_PROGRAM,
            SERVER,
            THIRD_PARTY,
            OTHER
    );

    private ApiClientTypeSupport() {
    }

    public static List<String> getSupportedClientTypes() {
        return SUPPORTED_CLIENT_TYPES;
    }

    public static String normalizeSingleClientType(String clientType) {
        String normalized = StrUtil.trimToNull(clientType);
        if (normalized == null) {
            return null;
        }
        String code = normalized.toLowerCase();
        validateSupported(code);
        return code;
    }

    public static List<String> normalizeClientTypes(Collection<String> clientTypes) {
        if (clientTypes == null || clientTypes.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String clientType : clientTypes) {
            String code = normalizeSingleClientType(clientType);
            if (code != null) {
                normalized.add(code);
            }
        }
        return SUPPORTED_CLIENT_TYPES.stream()
                .filter(normalized::contains)
                .toList();
    }

    public static String serializeClientTypes(Collection<String> clientTypes) {
        List<String> normalized = normalizeClientTypes(clientTypes);
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    public static List<String> deserializeClientTypes(String serialized) {
        if (StrUtil.isBlank(serialized)) {
            return Collections.emptyList();
        }
        return normalizeClientTypes(StrUtil.splitTrim(serialized, ','));
    }

    private static void validateSupported(String code) {
        if (!SUPPORTED_CLIENT_TYPES.contains(code)) {
            throw new BusinessException("存在不支持的客户端类型: " + code);
        }
    }
}
