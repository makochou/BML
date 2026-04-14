package com.bml.core.framework.license;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 许可证功能模块常量定义。
 * <p>
 * 集中管理所有功能模块标识码，与离线签发工具 {@code bml-license-keygen} 中的
 * {@code IssueLicensePanel.FEATURE_DEFS} 保持一致。
 * </p>
 * <p>
 * 同时维护「前端菜单组件路径 → 功能模块」映射关系，
 * 供 {@link com.bml.module.system.service.impl.SysMenuServiceImpl} 在构建路由时过滤未授权菜单。
 * </p>
 *
 * @author BML Team
 */
public final class LicenseFeatureConstants {

    private LicenseFeatureConstants() {
    }

    // ── 功能模块标识码（与 keygen 一致） ──

    /** API 网关 / 授权治理 */
    public static final String API_GATEWAY = "api_gateway";

    /** 企业管理 */
    public static final String ENTERPRISE = "enterprise";

    /** 系统管理（用户 / 角色 / 菜单 / 部门） */
    public static final String SYSTEM = "system";

    /** 系统监控（服务器监控） */
    public static final String MONITOR = "monitor";

    /** 告警通知 */
    public static final String ALERT = "alert";

    // ── 功能模块中文标签 ──

    /** 功能模块标识 → 中文名称，有序 */
    private static final Map<String, String> FEATURE_LABELS;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(API_GATEWAY, "API 网关");
        map.put(ENTERPRISE, "企业管理");
        map.put(SYSTEM, "系统管理");
        map.put(MONITOR, "系统监控");
        map.put(ALERT, "告警通知");
        FEATURE_LABELS = Collections.unmodifiableMap(map);
    }

    // ── 前端菜单组件路径 → 所需功能模块 ──

    /**
     * 菜单 Vue 组件路径 → 功能模块标识码。
     * <p>
     * 未在此映射中的组件视为无特性要求（如 Dashboard），始终可见。
     * </p>
     */
    private static final Map<String, String> COMPONENT_FEATURE_MAP;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        // API 网关相关页面
        map.put("api/ApiList", API_GATEWAY);
        map.put("api/ApiAccountManage", API_GATEWAY);
        map.put("api/ApiAccountDetail", API_GATEWAY);
        // 系统管理相关页面
        map.put("system/user/index", SYSTEM);
        map.put("system/role/index", SYSTEM);
        map.put("system/menu/index", SYSTEM);
        map.put("system/dept/index", SYSTEM);
        // 系统监控相关页面
        map.put("monitor/server/index", MONITOR);
        // 告警通知相关页面
        map.put("monitor/alert/index", ALERT);
        // 企业管理相关页面
        map.put("enterprise/index", ENTERPRISE);
        COMPONENT_FEATURE_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * 根据菜单的 Vue 组件路径查询所需的功能模块标识码。
     *
     * @param component 菜单组件路径（如 {@code "monitor/alert/index"}）
     * @return 功能模块标识码，无要求时返回 {@code null}
     */
    public static String getRequiredFeature(String component) {
        if (component == null || component.isEmpty()) {
            return null;
        }
        return COMPONENT_FEATURE_MAP.get(component);
    }

    /**
     * 获取功能模块的中文显示名称。
     *
     * @param featureCode 功能模块标识码
     * @return 中文名称，未知模块返回标识码本身
     */
    public static String getFeatureLabel(String featureCode) {
        return FEATURE_LABELS.getOrDefault(featureCode, featureCode);
    }

    /**
     * 获取全部功能模块标签映射（有序）。
     *
     * @return 不可变映射
     */
    public static Map<String, String> getAllFeatureLabels() {
        return FEATURE_LABELS;
    }
}
