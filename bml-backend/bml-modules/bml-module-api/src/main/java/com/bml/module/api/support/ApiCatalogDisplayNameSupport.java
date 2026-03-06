package com.bml.module.api.support;

import cn.hutool.core.util.StrUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 接口目录展示名解析支持。
 * <p>
 * 将纳管接口的 {@code moduleName}、{@code controllerName} 解析为前端展示用的中文名称，
 * 用于「API 接口列表」页的树形展示（如：系统管理 > 用户管理 > 用户列表/新增用户 等）。
 * 采用「默认映射 + 可扩展」设计，后续新增模块或控制器时在此处补充映射即可。
 * </p>
 *
 * <h3>扩展说明：</h3>
 * <ul>
 * <li>模块展示名：通过 {@link #registerModuleDisplayName(String, String)} 注册</li>
 * <li>控制器展示名：通过 {@link #registerControllerDisplayName(String, String)} 注册</li>
 * <li>未注册的项将回退为原始名称（便于开发期识别）</li>
 * </ul>
 *
 * @author BML Team
 */
public final class ApiCatalogDisplayNameSupport {

    /** 树节点类型：模块（一级） */
    public static final String TYPE_MODULE = "MODULE";
    /** 树节点类型：业务/资源（二级，如用户管理、角色管理） */
    public static final String TYPE_RESOURCE = "RESOURCE";
    /** 树节点类型：具体接口（叶子） */
    public static final String TYPE_API = "API";

    private static final Map<String, String> MODULE_DISPLAY_NAMES = new ConcurrentHashMap<>();
    private static final Map<String, String> CONTROLLER_DISPLAY_NAMES = new ConcurrentHashMap<>();

    static {
        // 模块展示名：与 bml-module-* 及 resolveModuleName 产出保持一致
        MODULE_DISPLAY_NAMES.put("system", "系统管理");
        MODULE_DISPLAY_NAMES.put("api", "API 管理");
        MODULE_DISPLAY_NAMES.put("enterprise", "企业管理");
        MODULE_DISPLAY_NAMES.put("common", "通用");

        // 控制器展示名：与现有 Controller 类名对应，便于「业务模块 > 增删改查」层级展示
        CONTROLLER_DISPLAY_NAMES.put("AuthController", "认证中心");
        CONTROLLER_DISPLAY_NAMES.put("SysUserController", "用户管理");
        CONTROLLER_DISPLAY_NAMES.put("SysRoleController", "角色管理");
        CONTROLLER_DISPLAY_NAMES.put("SysMenuController", "菜单管理");
        CONTROLLER_DISPLAY_NAMES.put("SysDeptController", "部门管理");
        CONTROLLER_DISPLAY_NAMES.put("SysAlertController", "告警中心");
        CONTROLLER_DISPLAY_NAMES.put("SysServerMonitorController", "服务器监控");
        CONTROLLER_DISPLAY_NAMES.put("SysApiAccountController", "API 账号管理");
        CONTROLLER_DISPLAY_NAMES.put("SysApiAccountAuthorizationController", "API 账号授权");
        CONTROLLER_DISPLAY_NAMES.put("SysApiAccountCallbackController", "API 回调日志");
        CONTROLLER_DISPLAY_NAMES.put("SysOpenApiRegistryController", "开放接口目录");
        CONTROLLER_DISPLAY_NAMES.put("OpenApiDemoController", "OpenAPI 示例");
        CONTROLLER_DISPLAY_NAMES.put("OpenEnterpriseDashboardController", "企业概览");
        CONTROLLER_DISPLAY_NAMES.put("OpenEnterpriseCompanyController", "企业档案");
        CONTROLLER_DISPLAY_NAMES.put("OpenEnterpriseSystemAccountController", "企业系统账号");
    }

    private ApiCatalogDisplayNameSupport() {
    }

    /**
     * 解析模块展示名。
     *
     * @param moduleName 模块名（如 system、api）
     * @return 中文展示名，未配置时返回原始 moduleName
     */
    public static String getModuleDisplayName(String moduleName) {
        if (StrUtil.isBlank(moduleName)) {
            return "未分类";
        }
        return MODULE_DISPLAY_NAMES.getOrDefault(moduleName.trim(), moduleName);
    }

    /**
     * 解析控制器展示名。
     *
     * @param controllerName 控制器类名（如 SysUserController）
     * @return 中文展示名，未配置时返回 controllerName
     */
    public static String getControllerDisplayName(String controllerName) {
        if (StrUtil.isBlank(controllerName)) {
            return "未命名";
        }
        return CONTROLLER_DISPLAY_NAMES.getOrDefault(controllerName.trim(), controllerName);
    }

    /**
     * 注册模块展示名（供后续扩展或配置化使用）。
     *
     * @param moduleName    模块名
     * @param displayName   展示名
     */
    public static void registerModuleDisplayName(String moduleName, String displayName) {
        if (StrUtil.isNotBlank(moduleName) && StrUtil.isNotBlank(displayName)) {
            MODULE_DISPLAY_NAMES.put(moduleName.trim(), displayName.trim());
        }
    }

    /**
     * 注册控制器展示名（供后续扩展或配置化使用）。
     *
     * @param controllerName 控制器类名
     * @param displayName    展示名
     */
    public static void registerControllerDisplayName(String controllerName, String displayName) {
        if (StrUtil.isNotBlank(controllerName) && StrUtil.isNotBlank(displayName)) {
            CONTROLLER_DISPLAY_NAMES.put(controllerName.trim(), displayName.trim());
        }
    }
}
