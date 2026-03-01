package com.bml.core.common.support;

import cn.hutool.core.util.StrUtil;
import com.bml.core.common.exception.BusinessException;

import java.util.List;

/**
 * API 账号签名算法版本支持工具。
 */
public final class ApiSignatureVersionSupport {

    public static final String V1 = "v1";

    private static final List<String> SUPPORTED_VERSIONS = List.of(V1);

    private ApiSignatureVersionSupport() {
    }

    public static List<String> getSupportedVersions() {
        return SUPPORTED_VERSIONS;
    }

    public static String defaultVersion() {
        return V1;
    }

    public static String normalizeVersion(String version) {
        String normalized = StrUtil.trimToNull(version);
        if (normalized == null) {
            return null;
        }
        String lowerCaseVersion = normalized.toLowerCase();
        if (!SUPPORTED_VERSIONS.contains(lowerCaseVersion)) {
            throw new BusinessException("存在不支持的签名算法版本: " + lowerCaseVersion);
        }
        return lowerCaseVersion;
    }
}
