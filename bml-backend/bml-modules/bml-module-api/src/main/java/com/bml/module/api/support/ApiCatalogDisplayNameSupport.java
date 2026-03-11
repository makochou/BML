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
        // 核心业务模块展示名：由技术标识映射为专业化中文称谓
        MODULE_DISPLAY_NAMES.put("api", "开放接口核心");
        MODULE_DISPLAY_NAMES.put("system", "系统运维基石");
        MODULE_DISPLAY_NAMES.put("enterprise", "企业运营中心");
        MODULE_DISPLAY_NAMES.put("common", "通用业务基建");
        MODULE_DISPLAY_NAMES.put("openapi", "OpenAPI 枢纽");

        // 基础设施与监控
        MODULE_DISPLAY_NAMES.put("monitor", "系统监控中心");
        MODULE_DISPLAY_NAMES.put("actuate", "运行态在线监测");
        MODULE_DISPLAY_NAMES.put("servlet", "运行态引擎服务");
        MODULE_DISPLAY_NAMES.put("springdoc", "OpenAPI 接口文档");
        MODULE_DISPLAY_NAMES.put("v3", "OpenAPI 协议中心");
        MODULE_DISPLAY_NAMES.put("error", "平台异常中枢");
        MODULE_DISPLAY_NAMES.put("ui", "Swagger UI 门户");

        // 业务控制器展示名：面向管理维度的语义化 rebranding
        CONTROLLER_DISPLAY_NAMES.put("AuthController", "身份认证与授权");
        CONTROLLER_DISPLAY_NAMES.put("SysUserController", "平台账户体系");
        CONTROLLER_DISPLAY_NAMES.put("SysRoleController", "岗位角色定义");
        CONTROLLER_DISPLAY_NAMES.put("SysMenuController", "功能菜单矩阵");
        CONTROLLER_DISPLAY_NAMES.put("SysDeptController", "组织架构树图");
        CONTROLLER_DISPLAY_NAMES.put("SysAlertController", "系统告警终端");
        CONTROLLER_DISPLAY_NAMES.put("SysServerMonitorController", "服务器效能监测");
        
        CONTROLLER_DISPLAY_NAMES.put("SysApiAccountController", "应用账户管理");
        CONTROLLER_DISPLAY_NAMES.put("SysApiAccountAuthorizationController", "资产授权策略");
        CONTROLLER_DISPLAY_NAMES.put("SysApiAccountCallbackController", "数据回调追踪");
        CONTROLLER_DISPLAY_NAMES.put("SysApiListController", "接口资产清单");
        CONTROLLER_DISPLAY_NAMES.put("SysOpenApiRegistryController", "接口目录治理");
        CONTROLLER_DISPLAY_NAMES.put("OpenApiDemoController", "接入示例场景");
        CONTROLLER_DISPLAY_NAMES.put("SysApiAccountCallbackLogController", "回调履约日志");

        // 经营数据大盘
        CONTROLLER_DISPLAY_NAMES.put("OpenEnterpriseDashboardController", "经营数据大盘");
        CONTROLLER_DISPLAY_NAMES.put("OpenEnterpriseCompanyController", "企业主体档案");
        CONTROLLER_DISPLAY_NAMES.put("OpenEnterpriseSystemAccountController", "子系统互联账号");

        // 运行态探针与基础设施 (Actuator & Swagger)
        CONTROLLER_DISPLAY_NAMES.put("HealthEndpoint", "核心健康状态探针");
        CONTROLLER_DISPLAY_NAMES.put("BasicErrorController", "标准异常处理中枢");
        CONTROLLER_DISPLAY_NAMES.put("OpenApiWebMvcResource", "OpenAPI 映射引擎");
        CONTROLLER_DISPLAY_NAMES.put("SwaggerConfigResource", "文档配置热加载");
        CONTROLLER_DISPLAY_NAMES.put("EnvironmentEndpoint", "环境依赖变量探针");
        CONTROLLER_DISPLAY_NAMES.put("BeansEndpoint", "IoC 容器组件审计");
        CONTROLLER_DISPLAY_NAMES.put("InfoEndpoint", "应用元数据快照");
        CONTROLLER_DISPLAY_NAMES.put("LoggersEndpoint", "日志治理实时面板");
        CONTROLLER_DISPLAY_NAMES.put("MappingsEndpoint", "路由终点分布矩阵");
        CONTROLLER_DISPLAY_NAMES.put("MetricsEndpoint", "性能度量指标集");
        CONTROLLER_DISPLAY_NAMES.put("PrometheusScrapableEndpoint", "Prometheus 数据面");
        CONTROLLER_DISPLAY_NAMES.put("RestartEndpoint", "服务热重启指令集");
        CONTROLLER_DISPLAY_NAMES.put("ShutdownEndpoint", "安全有序下线协议");
        CONTROLLER_DISPLAY_NAMES.put("ThreadDumpEndpoint", "线程栈快照诊断");
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
