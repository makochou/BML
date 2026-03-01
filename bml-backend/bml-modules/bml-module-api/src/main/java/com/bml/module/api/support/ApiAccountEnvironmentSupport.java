package com.bml.module.api.support;

import cn.hutool.core.util.StrUtil;
import com.bml.core.common.exception.BusinessException;

import java.util.List;

/**
 * API 账号接入环境支持工具。
 * <p>
 * 接入环境会贯穿账号开通、授权、联调和生产运维，因此需要统一环境代码，
 * 避免前后端和运维文档各自维护不同枚举造成理解偏差。
 * </p>
 */
public final class ApiAccountEnvironmentSupport {

    public static final String TEST = "test";
    public static final String STAGING = "staging";
    public static final String PRODUCTION = "production";

    private static final List<String> SUPPORTED_ENVIRONMENTS = List.of(
            TEST,
            STAGING,
            PRODUCTION
    );

    private ApiAccountEnvironmentSupport() {
    }

    public static List<String> getSupportedEnvironments() {
        return SUPPORTED_ENVIRONMENTS;
    }

    public static String normalizeEnvironment(String environment) {
        String normalized = StrUtil.trimToNull(environment);
        if (normalized == null) {
            return null;
        }
        String code = normalized.toLowerCase();
        validateSupported(code);
        return code;
    }

    private static void validateSupported(String code) {
        if (!SUPPORTED_ENVIRONMENTS.contains(code)) {
            throw new BusinessException("存在不支持的接入环境: " + code);
        }
    }
}
