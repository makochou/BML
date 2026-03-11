import type {
  AccessEnvironment,
  ApiAccountDetail,
  ApiAccountItem,
  ApiCredentialPayload
} from '../types/apiAccount';

export const accountTypeOptions = [
  { label: '内部账号', value: 1 },
  { label: '外部账号', value: 2 }
];

export const clientTypeOptions = [
  { label: 'Web前端', value: 'web' },
  { label: 'H5页面', value: 'h5' },
  { label: 'APP', value: 'app' },
  { label: '小程序', value: 'mini_program' },
  { label: '服务端', value: 'server' },
  { label: '第三方系统', value: 'third_party' },
  { label: '其他客户端', value: 'other' }
];

export const environmentOptions: { label: string; value: AccessEnvironment }[] = [
  { label: '测试环境', value: 'test' },
  { label: '预发环境', value: 'staging' },
  { label: '生产环境', value: 'production' }
];

export const signVersionOptions = [{ label: 'v1（当前正式版）', value: 'v1' }];
export const statusOptions = [{ label: '启用', value: 1 }, { label: '停用', value: 0 }];
export const methodOptions = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].map(item => ({ label: item, value: item }));
export const callbackStatusOptions = [{ label: '待执行', value: 0 }, { label: '重试中', value: 1 }, { label: '成功', value: 2 }, { label: '失败', value: 3 }];

/**
 * 统一处理 API 账号治理页中常用的标签与状态文案，避免多个页面各自维护一套映射。
 */
export function getAccountTypeLabel(value: number) {
  return accountTypeOptions.find(item => item.value === value)?.label || '未知类型';
}

export function getClientTypeLabels(values?: string[]) {
  return (values || []).map(value => clientTypeOptions.find(item => item.value === value)?.label || value);
}

export function getAccessEnvironmentLabel(value?: string | null) {
  return environmentOptions.find(item => item.value === value)?.label || '未设置环境';
}

export function getEnvironmentTagColor(value?: string | null) {
  return ({ test: 'arcoblue', staging: 'orange', production: 'green' } as Record<string, string>)[value || ''] || 'gray';
}

export function getMethodTagColor(method?: string) {
  return ({ GET: 'arcoblue', POST: 'green', PUT: 'orange', DELETE: 'red', PATCH: 'purple' } as Record<string, string>)[method || ''] || 'gray';
}

export function getCallbackStatusLabel(status: number) {
  return ({ 0: '待执行', 1: '重试中', 2: '成功', 3: '失败' } as Record<number, string>)[status] || '未知状态';
}

export function getCallbackStatusColor(status: number) {
  return ({ 0: 'gray', 1: 'arcoblue', 2: 'green', 3: 'red' } as Record<number, string>)[status] || 'gray';
}

export function isCallbackRetryable(status: number) {
  return status === 1 || status === 3;
}

export function formatDisplayDateTime(value?: string | null, fallback = '-') {
  return value || fallback;
}

type GovernanceWhitelistPayload = Pick<
  ApiAccountItem | ApiAccountDetail | ApiCredentialPayload,
  'environmentIpWhitelist' | 'accessEnvironment' | 'ipWhitelist'
>;

export function getWhitelistByEnvironment(payload: GovernanceWhitelistPayload, environment: AccessEnvironment) {
  const values = payload.environmentIpWhitelist?.[environment];
  if (values?.length) return values;
  if (payload.accessEnvironment === environment) return payload.ipWhitelist || [];
  return [];
}

export function getEffectiveWhitelist(record: Pick<ApiAccountItem, 'accessEnvironment' | 'environmentIpWhitelist' | 'ipWhitelist'>) {
  const env = (record.accessEnvironment || 'production') as AccessEnvironment;
  const whitelist = record.environmentIpWhitelist?.[env];
  return whitelist?.length ? whitelist : (record.ipWhitelist || []);
}

/**
 * 模块名称中文映射表
 * 与后端 ApiCatalogDisplayNameSupport 保持一致
 */
const MODULE_DISPLAY_NAMES: Record<string, string> = {
  // 业务核心
  'api': '开放接口核心',
  'system': '系统运维基石',
  'enterprise': '企业运营中心',
  'common': '通用业务基建',
  'openapi': 'OpenAPI 枢纽',
  
  // 基础设施与监控
  'monitor': '系统监控中心',
  'actuate': '运行态在线监测',
  'servlet': '运行态引擎服务',
  'springdoc': 'OpenAPI 接口文档',
  'v3': 'OpenAPI 协议中心',
  'error': '平台异常中枢',
  'ui': 'Swagger UI 门户'
};

/**
 * 控制器名称中文映射表
 * 与后端 ApiCatalogDisplayNameSupport 保持一致
 */
const CONTROLLER_DISPLAY_NAMES: Record<string, string> = {
  // 核心认证与管理
  'AuthController': '身份认证与授权',
  'SysUserController': '平台账户体系',
  'SysRoleController': '岗位角色定义',
  'SysMenuController': '功能菜单矩阵',
  'SysDeptController': '组织架构树图',
  'SysAlertController': '系统告警终端',
  'SysServerMonitorController': '服务器效能监测',
  
  // 资产治理与 API 运维
  'SysApiAccountController': '应用账户管理',
  'SysApiAccountAuthorizationController': '资产授权策略',
  'SysApiAccountCallbackController': '数据回调追踪',
  'SysApiListController': '接口资产清单',
  'SysOpenApiRegistryController': '接口目录治理',
  'OpenApiDemoController': '接入示例场景',
  'SysApiAccountCallbackLogController': '回调履约日志', // 补充
  
  // 经营数据大盘
  'OpenEnterpriseDashboardController': '经营数据大盘',
  'OpenEnterpriseCompanyController': '企业主体档案',
  'OpenEnterpriseSystemAccountController': '子系统互联账号',

  // 运行态探针与基础设施 (Actuator & Swagger)
  'HealthEndpoint': '核心健康状态探针',
  'BasicErrorController': '标准异常处理中枢',
  'OpenApiWebMvcResource': 'OpenAPI 映射引擎',
  'SwaggerConfigResource': '文档配置热加载',
  'EnvironmentEndpoint': '环境依赖变量探针',
  'BeansEndpoint': 'IoC 容器组件审计',
  'InfoEndpoint': '应用元数据快照',
  'LoggersEndpoint': '日志治理实时面板',
  'MappingsEndpoint': '路由终点分布矩阵',
  'MetricsEndpoint': '性能度量指标集',
  'PrometheusScrapableEndpoint': 'Prometheus 数据面',
  'RestartEndpoint': '服务热重启指令集',
  'ShutdownEndpoint': '安全有序下线协议',
  'ThreadDumpEndpoint': '线程栈快照诊断'
};

/**
 * 获取模块的中文显示名称
 * @param moduleName 模块名称（英文）
 * @returns 中文显示名称，未配置时返回原始名称
 */
export function getModuleDisplayName(moduleName: string): string {
  if (!moduleName) return '未分类';
  return MODULE_DISPLAY_NAMES[moduleName] || moduleName;
}

/**
 * 获取控制器的中文显示名称
 * @param controllerName 控制器名称（英文）
 * @returns 中文显示名称，未配置时返回原始名称
 */
export function getControllerDisplayName(controllerName: string): string {
  if (!controllerName) return '未命名';
  return CONTROLLER_DISPLAY_NAMES[controllerName] || controllerName;
}
