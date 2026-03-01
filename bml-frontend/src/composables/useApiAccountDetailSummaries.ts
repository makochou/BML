import { computed, unref, type Ref } from 'vue';
import type {
  ApiAccountDetail,
  ApiAuthorizationSnapshot,
  ApiCallbackLogSummary
} from '../types/apiAccount';
import type { WorkbenchStatCard } from '../types/governance';

type SummarySource<T> = Ref<T> | T;

/**
 * API账号详情页摘要指标组合式模型。
 * 统一收口 Hero、授权摘要和回调摘要的指标映射，避免多个详情页重复维护同一套卡片结构。
 */
export function useApiAccountDetailSummaries(options: {
  detail: Ref<ApiAccountDetail | null>;
  authorizationSnapshot: Ref<ApiAuthorizationSnapshot | null>;
  callbackSummary: SummarySource<ApiCallbackLogSummary>;
}) {
  const resolveCallbackSummary = () => unref(options.callbackSummary);

  const detailHeroStats = computed<WorkbenchStatCard[]>(() => {
    const detail = options.detail.value;
    const callbackSummary = resolveCallbackSummary();
    if (!detail) return [];
    return [
      { label: '授权接口', value: detail.authorizedApiCount || 0, hint: '当前账号累计授权的接口条目数', tone: 'blue' },
      { label: '启用授权', value: detail.enabledAuthorizedApiCount || 0, hint: '授权结果中当前处于启用状态的接口数', tone: 'green' },
      { label: '回调失败', value: callbackSummary.failedCount, hint: '累计失败的业务回调记录数', tone: 'teal' },
      { label: '回调重试中', value: callbackSummary.retryingCount, hint: '系统仍在自动重试的回调记录数', tone: 'gold' }
    ];
  });

  const detailAuthorizationStats = computed<WorkbenchStatCard[]>(() => {
    const summary = options.authorizationSnapshot.value?.summary;
    if (!summary) return [];
    return [
      { label: '目录总接口', value: summary.totalApiCount, hint: '该账号可参与授权配置的接口库存', tone: 'blue' },
      { label: '目录启用接口', value: summary.enabledApiCount, hint: '当前目录中状态为启用的接口数', tone: 'green' },
      { label: '已保存授权', value: summary.selectedApiCount, hint: '上一次保存后的授权接口数量', tone: 'teal' },
      { label: '已保存启用授权', value: summary.selectedEnabledApiCount, hint: '授权结果中仍处于启用状态的接口数', tone: 'gold' }
    ];
  });

  const detailCallbackStats = computed<WorkbenchStatCard[]>(() => {
    const callbackSummary = resolveCallbackSummary();
    return [
      { label: '累计投递', value: callbackSummary.totalCount, hint: '当前账号累计触发的回调记录总数', tone: 'blue' },
      { label: '成功记录', value: callbackSummary.successCount, hint: '收到 2xx 且业务回调成功完成的次数', tone: 'green' },
      { label: '重试中', value: callbackSummary.retryingCount, hint: '当前仍在系统自动重试中的记录数', tone: 'teal' },
      { label: '失败记录', value: callbackSummary.failedCount, hint: '建议尽快进入日志工作台做排查或补投', tone: 'gold' }
    ];
  });

  return {
    detailHeroStats,
    detailAuthorizationStats,
    detailCallbackStats
  };
}
