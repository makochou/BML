import request from '../utils/request';

/** 综合监控面板聚合数据 */
export interface DashboardSummary {
  license: {
    valid: boolean;
    clientName: string;
    expireDate: string;
    daysLeft: number;
  };
  monitor: {
    cpuPercent: number;
    memPercent: number;
    diskPercent: number;
    os: string;
  };
  alert: {
    todayTotal: number;
    unresolved: number;
  };
  apiAccount: {
    total: number;
    enabled: number;
  };
  apiRegistry: {
    total: number;
  };
  recentActivities: Array<{
    text: string;
    time: string;
    color: string;
  }>;
}

/** 获取工作台综合监控数据 */
export function getDashboardSummary() {
  return request.get<DashboardSummary>('/system/dashboard/summary');
}
