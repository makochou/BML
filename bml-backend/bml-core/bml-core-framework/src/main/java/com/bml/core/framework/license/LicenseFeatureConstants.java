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
 * 许可证授权控制的是前台业务端模块（非中台管理），中台管理始终包含全部功能。
 * 前台业务模块尚未开发，待开发后在此补充模块定义。
 * </p>
 *
 * @author BML Team
 */
public final class LicenseFeatureConstants {

    private LicenseFeatureConstants() {
    }

    // ── 功能模块标识码 ──
    // 许可证授权控制的是前台业务端模块（非中台管理），
    // 前台业务模块尚未开发，待开发后在此补充常量定义。

    // ── 功能模块中文标签 ──

    /** 功能模块标识 → 中文名称，有序。前台业务模块开发后在此注册。 */
    private static final Map<String, String> FEATURE_LABELS;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        // 前台业务模块尚未开发，待补充
        FEATURE_LABELS = Collections.unmodifiableMap(map);
    }

    // ── 前端菜单组件路径 → 所需功能模块 ──

    /**
     * 菜单 Vue 组件路径 → 功能模块标识码。
     * <p>
     * 中台管理端不做许可证功能过滤，此映射仅保留结构供未来前台业务使用。
     * 未在此映射中的组件视为无特性要求，始终可见。
     * </p>
     */
    private static final Map<String, String> COMPONENT_FEATURE_MAP;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        // 前台业务模块尚未开发，待补充
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
