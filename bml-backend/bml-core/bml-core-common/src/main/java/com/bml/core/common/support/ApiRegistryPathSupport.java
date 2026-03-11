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
     * 响应用户要求，现已允许纳管登录、监控、健康检查等路径。
     * </p>
     */
    private static final List<String> EXCLUDED_PREFIXES = List.of();

    /**
     * 需要精确排除的单一路径。
     */
    private static final List<String> EXCLUDED_EXACT_PATHS = List.of(
            "/favicon.ico");

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
        if (EXCLUDED_PREFIXES.isEmpty()) {
            return true;
        }
        return EXCLUDED_PREFIXES.stream().noneMatch(normalizedPath::startsWith);
    }

    /**
     * 判断控制器包名是否属于纳管范围（项目代码或指定的基础设施代码）。
     *
     * @param packageName 控制器包名
     * @return {@code true} 表示纳入扫描范围
     */
    public static boolean isProjectControllerPackage(String packageName) {
        if (StrUtil.isBlank(packageName)) {
            return false;
        }
        return packageName.startsWith("com.bml.") 
                || packageName.startsWith("org.springframework.boot.actuate")
                || packageName.startsWith("org.springframework.boot.autoconfigure.web")
                || packageName.startsWith("org.springframework.boot.web")
                || packageName.startsWith("org.springdoc.")
                || packageName.startsWith("org.springframework.web.servlet.resource");
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
