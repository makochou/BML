package com.bml.module.api.support;

import cn.hutool.core.util.StrUtil;
import com.bml.core.common.support.ApiIpWhitelistSupport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API 账号按环境独立维护白名单的统一支持工具。
 * <p>
 * 该工具负责把数据库中的三个环境白名单字段收敛为统一 Map，并在运行时根据当前账号接入环境
 * 解析出实际生效的白名单。这样可以保持表结构清晰，也能让前后端继续使用统一的数据模型。
 * </p>
 */
public final class ApiAccountEnvironmentWhitelistSupport {

    public static final String TEST = "test";
    public static final String STAGING = "staging";
    public static final String PRODUCTION = "production";

    private ApiAccountEnvironmentWhitelistSupport() {
    }

    public static Map<String, List<String>> buildEnvironmentWhitelistMap(
            String testIpWhitelist,
            String stagingIpWhitelist,
            String productionIpWhitelist) {
        Map<String, List<String>> whitelistMap = new LinkedHashMap<>();
        whitelistMap.put(TEST, ApiIpWhitelistSupport.deserializeEntries(testIpWhitelist));
        whitelistMap.put(STAGING, ApiIpWhitelistSupport.deserializeEntries(stagingIpWhitelist));
        whitelistMap.put(PRODUCTION, ApiIpWhitelistSupport.deserializeEntries(productionIpWhitelist));
        return whitelistMap;
    }

    public static Map<String, List<String>> normalizeEnvironmentWhitelistMap(Map<String, List<String>> whitelistMap) {
        Map<String, List<String>> normalizedMap = new LinkedHashMap<>();
        normalizedMap.put(TEST, ApiIpWhitelistSupport.normalizeEntries(whitelistMap == null ? null : whitelistMap.get(TEST)));
        normalizedMap.put(STAGING,
                ApiIpWhitelistSupport.normalizeEntries(whitelistMap == null ? null : whitelistMap.get(STAGING)));
        normalizedMap.put(PRODUCTION,
                ApiIpWhitelistSupport.normalizeEntries(whitelistMap == null ? null : whitelistMap.get(PRODUCTION)));
        return normalizedMap;
    }

    public static List<String> resolveEffectiveWhitelist(String accessEnvironment,
            Map<String, List<String>> whitelistMap,
            List<String> fallbackWhitelist) {
        String normalizedEnvironment = ApiAccountEnvironmentSupport.normalizeEnvironment(accessEnvironment);
        if (StrUtil.isNotBlank(normalizedEnvironment) && whitelistMap != null) {
            List<String> environmentWhitelist = whitelistMap.get(normalizedEnvironment);
            if (environmentWhitelist != null && !environmentWhitelist.isEmpty()) {
                return ApiIpWhitelistSupport.normalizeEntries(environmentWhitelist);
            }
        }
        return ApiIpWhitelistSupport.normalizeEntries(fallbackWhitelist);
    }
}
