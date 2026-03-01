package com.bml.core.common.support;

import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * 项目接口纳管路径规则工具。
 * <p>
 * 设计目标：
 * 1. 将“哪些接口需要进入统一授权目录”收口到一个公共位置，避免前端、鉴权层、目录同步层各写一套规则。
 * 2. 默认纳管项目内所有业务接口，自动排除登录、文档、健康检查、错误页等基础设施路径。
 * 3. 后续若新增公共排除规则，仅需修改本类即可同步影响接口自动发现与授权控制。
 * </p>
 */
public final class ApiRegistryPathSupport {

    /**
     * Spring Boot / 平台基础设施路径前缀。
     * <p>
     * 这些路径不属于业务接口授权目录，不应出现在 API 账号授权工作台中。
     * </p>
     */
    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/auth/",
            "/actuator/",
            "/v3/api-docs",
            "/swagger-ui",
            "/error",
            "/open/api/test/");

    /**
     * 需要精确排除的单一路径。
     */
    private static final List<String> EXCLUDED_EXACT_PATHS = List.of(
            "/auth/login",
            "/auth/refresh",
            "/auth/register",
            "/captchaImage",
            "/swagger-ui.html",
            "/favicon.ico",
            "/error");

    private ApiRegistryPathSupport() {
    }

    /**
     * 判断路径是否应纳入统一授权目录。
     *
     * @param rawPath 请求路径
     * @return {@code true} 表示需要纳管；{@code false} 表示属于基础设施或显式排除路径
     */
    public static boolean isManagedApiPath(String rawPath) {
        String normalizedPath = normalizePath(rawPath);
        if (StrUtil.isBlank(normalizedPath) || !StrUtil.startWith(normalizedPath, "/")) {
            return false;
        }
        if (EXCLUDED_EXACT_PATHS.contains(normalizedPath)) {
            return false;
        }
        return EXCLUDED_PREFIXES.stream().noneMatch(normalizedPath::startsWith);
    }

    /**
     * 判断控制器包名是否属于项目自身代码。
     *
     * @param packageName 控制器包名
     * @return {@code true} 表示属于 BML 自身控制器
     */
    public static boolean isProjectControllerPackage(String packageName) {
        return StrUtil.startWith(StrUtil.blankToDefault(packageName, StrUtil.EMPTY), "com.bml.");
    }

    /**
     * 统一规范化路径，确保比较逻辑不会被上下文路径或缺失斜杠干扰。
     *
     * @param rawPath 原始路径
     * @return 规范化后的路径
     */
    public static String normalizePath(String rawPath) {
        if (StrUtil.isBlank(rawPath)) {
            return StrUtil.EMPTY;
        }
        String normalizedPath = rawPath.trim();
        return StrUtil.startWith(normalizedPath, "/") ? normalizedPath : "/" + normalizedPath;
    }
}
