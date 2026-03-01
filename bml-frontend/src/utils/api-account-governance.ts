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
