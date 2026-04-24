<template>
  <div class="page detail-page">
    <a-spin :loading="loading" class="detail-spin">
      <GovernanceWorkbenchShell
        v-if="detail"
        class="detail-workbench"
        eyebrow="Account Governance Insight"
        title="账号详情"
        description="统一查看当前 API 账号的归属信息、授权现状、环境白名单与回调运行状态，并可一键进入编辑、授权和日志治理动作。"
        :tags="detailHeroTags"
        :stats="detailHeroStats"
        theme="emerald"
      >
        <template #titleBadge>
          <a-tag :color="detail.status === 1 ? 'green' : 'red'">{{ detail.status === 1 ? '启用中' : '已停用' }}</a-tag>
        </template>

        <template #heroActions>
          <a-button @click="goBackToManage">返回列表</a-button>
          <a-button @click="goToManageAction('edit')">编辑账号</a-button>
          <a-button @click="goToManageAction('copy')">复制新建</a-button>
          <a-button @click="goToManageAction('authorization')">接口授权</a-button>
          <a-button @click="goToManageAction('callback')">回调日志</a-button>
          <a-button type="primary" @click="goToManageAction('reset-secret')">重置并查看新凭证</a-button>
        </template>

        <template #aside>
          <GovernancePanel class="detail-panel detail-panel--identity" title="账号画像">
            <GovernanceFactGrid class="detail-fact-grid" card-class="detail-fact-card" copy-class="detail-fact-card__copy" :items="detailFactCards" />
          </GovernancePanel>

          <GovernancePanel class="detail-panel detail-panel--guide" title="治理建议">
            <ul class="detail-guide-list">
              <li v-for="item in detailGuideItems" :key="item">{{ item }}</li>
            </ul>
          </GovernancePanel>
        </template>

        <ApiAccountDetailSummaryPanels
          :overview-stats="detailOverviewStats"
          :authorization-stats="detailAuthorizationStats"
          :callback-stats="detailCallbackStats"
          @open-authorization="goToManageAction('authorization')"
          @open-callback="goToManageAction('callback')"
        />

        <GovernancePanel class="detail-panel">
          <div class="section-heading section-heading--row">
            <div>
              <h3>环境白名单</h3>
              <p>测试、预发、生产三套白名单独立治理，系统会按当前接入环境自动选择生效清单。</p>
            </div>
            <a-tag :color="getEnvironmentTagColor(detail.accessEnvironment)">当前生效：{{ getAccessEnvironmentLabel(detail.accessEnvironment) }}</a-tag>
          </div>
          <ApiEnvironmentWhitelistCards :payload="detail" />
        </GovernancePanel>

        <GovernancePanel class="detail-panel detail-panel--logs">
          <div class="section-heading section-heading--row">
            <div>
              <h3>最近回调日志</h3>
              <p>展示最近 5 条投递日志，便于在详情页快速判断最近联调和异常情况。</p>
            </div>
            <a-button type="primary" @click="goToManageAction('callback')">查看完整日志</a-button>
          </div>
          <ApiCallbackLogTable :data="recentCallbackLogs" :scroll-x="1280" mode="preview" />
        </GovernancePanel>
      </GovernanceWorkbenchShell>

      <a-empty v-else description="未找到对应账号信息" />
    </a-spin>
  </div>
</template>

<script lang="ts" setup>
/**
 * API 账号详情页面
 *
 * 重要说明：
 *   defineOptions({ name: 'ApiAccountDetail' }) 是 keep-alive 缓存的关键。
 *   组件 name 必须与路由配置中的 name 字段保持一致，
 *   否则 <keep-alive :include="cachedViews"> 无法匹配到该组件，
 *   导致切换标签页后页面内容被销毁、重新加载。
 */
defineOptions({ name: 'ApiAccountDetail' });

import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ApiAccountDetailSummaryPanels from '../../components/api-account/ApiAccountDetailSummaryPanels.vue';
import ApiCallbackLogTable from '../../components/api-account/ApiCallbackLogTable.vue';
import ApiEnvironmentWhitelistCards from '../../components/api-account/ApiEnvironmentWhitelistCards.vue';
import GovernanceFactGrid from '../../components/governance/GovernanceFactGrid.vue';
import GovernancePanel from '../../components/governance/GovernancePanel.vue';
import GovernanceWorkbenchShell from '../../components/governance/GovernanceWorkbenchShell.vue';
import { fetchApiAccountDetail, fetchApiCallbackLogs, fetchAuthorizationSnapshot } from '../../api/apiAccount';
import { useApiAccountDetailSummaries } from '../../composables/useApiAccountDetailSummaries';
import type {
  ApiAccountDetail,
  ApiAuthorizationSnapshot,
  ApiCallbackLogItem,
  ApiCallbackLogSummary
} from '../../types/apiAccount';
import type { FactCard, WorkbenchStatCard } from '../../types/governance';
import {
  getAccessEnvironmentLabel,
  getAccountTypeLabel,
  getClientTypeLabels,
  getEnvironmentTagColor,
  formatDisplayDateTime
} from '../../utils/api-account-governance';

type ManageRouteAction = 'edit' | 'copy' | 'authorization' | 'callback' | 'reset-secret';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const detail = ref<ApiAccountDetail | null>(null);
const authorizationSnapshot = ref<ApiAuthorizationSnapshot | null>(null);
const recentCallbackLogs = ref<ApiCallbackLogItem[]>([]);
const callbackSummary = reactive<ApiCallbackLogSummary>({ totalCount: 0, pendingCount: 0, retryingCount: 0, successCount: 0, failedCount: 0 });

// 详情页聚焦“查看 + 跳转处理”，Hero 指标重点突出账号当前生效能力与运维状态。
const detailHeroTags = ['详情查看与治理闭环', '授权库存一屏掌握', '回调状态即时判断', '支持跳转到具体工作台'];
const detailGuideItems = ['如需修改账号主体、环境或白名单，请直接点击“编辑账号”，避免在详情页中做分散配置。', '接口授权与回调日志工作台会沿用同一套治理视觉语言，适合继续做问题排查与批量处理。', 'SecretKey 无法直接回显，如需重新交付，请点击“重置并查看新凭证”，系统会生成新密钥并使旧值失效。'];

const detailFactCards = computed<FactCard[]>(() => {
  if (!detail.value) return [];
  return [
    { label: '账号名称', value: detail.value.accountName, hint: '当前治理对象的账号主体' },
    { label: '系统账号ID', value: `#${detail.value.id}`, hint: '可用于日志检索、工单排查和问题追踪', copyable: true },
    { label: '账号类型', value: getAccountTypeLabel(detail.value.accountType), hint: '用于区分内外部接入主体' },
    { label: '业务系统', value: detail.value.systemName || '未维护', hint: detail.value.systemCode || '未维护系统编码' },
    { label: '调用客户端', value: detail.value.clientTypes?.length ? getClientTypeLabels(detail.value.clientTypes).join('、') : '未维护', hint: '声明当前账号允许接入的终端形态' },
    { label: '负责人', value: detail.value.ownerName || '未维护', hint: detail.value.ownerContact || '未维护联系方式' },
    { label: 'AccessKey', value: detail.value.accessKey, hint: '调用方公开身份标识', copyable: true }
  ];
});

const detailOverviewStats = computed<WorkbenchStatCard[]>(() => {
  if (!detail.value) return [];
  return [
    { label: '状态', value: detail.value.status === 1 ? '启用' : '停用', hint: '决定账号是否仍可访问已授权接口', tone: 'blue' },
    { label: '接入环境', value: getAccessEnvironmentLabel(detail.value.accessEnvironment), hint: '决定当前默认命中的环境白名单', tone: 'green' },
    { label: '限流阈值', value: `${detail.value.rateLimit || 0} / min`, hint: '每分钟允许的请求上限', tone: 'teal' },
    { label: '到期时间', value: formatDisplayDateTime(detail.value.expireTime, '永久有效'), hint: '为空表示账号不会自动过期', tone: 'gold' }
  ];
});

// 详情页中的授权摘要、回调摘要与 Hero 指标统一交给组合式函数生成，后续其他详情页可直接复用同一套卡片映射。
const { detailHeroStats, detailAuthorizationStats, detailCallbackStats } = useApiAccountDetailSummaries({
  detail,
  authorizationSnapshot,
  callbackSummary
});

const currentAccountId = computed(() => Number(route.params.id));

async function loadDetailPage() {
  if (!Number.isFinite(currentAccountId.value) || currentAccountId.value <= 0) return;
  loading.value = true;
  try {
    const [{ data: accountDetail }, { data: snapshot }, { data: callbackPayload }] = await Promise.all([
      fetchApiAccountDetail(currentAccountId.value),
      fetchAuthorizationSnapshot(currentAccountId.value),
      fetchApiCallbackLogs(currentAccountId.value, { pageNum: 1, pageSize: 5 })
    ]);
    detail.value = accountDetail;
    authorizationSnapshot.value = snapshot;
    recentCallbackLogs.value = callbackPayload.page.records || [];
    Object.assign(callbackSummary, callbackPayload.summary || createCallbackSummary());
  } finally {
    loading.value = false;
  }
}

function goBackToManage() {
  router.push('/admin/api/account');
}

function goToManageAction(action: ManageRouteAction) {
  router.push({
    path: '/admin/api/account',
    query: { action, accountId: String(currentAccountId.value) }
  });
}

function createCallbackSummary(): ApiCallbackLogSummary {
  return { totalCount: 0, pendingCount: 0, retryingCount: 0, successCount: 0, failedCount: 0 };
}

onMounted(loadDetailPage);
watch(() => route.params.id, loadDetailPage);
</script>

<style scoped>
.detail-page {
  --page-bg-start: #f4f7fb;
  --page-bg-end: #f8fafc;
  --surface-color: #ffffff;
  --surface-border: #e5edf6;
  --text-primary: #0f172a;
  --text-secondary: #64748b;
  min-height: 100%;
  padding: 24px;
  background: linear-gradient(180deg, var(--page-bg-start), var(--page-bg-end));
}

.detail-spin {
  width: 100%;
}

.detail-workbench {
  gap: 20px;
}

.detail-panel {
  border: 1px solid var(--surface-border);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(10px);
  padding: 22px;
}

.detail-panel--guide {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(238, 249, 245, 0.96));
}

.detail-fact-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-fact-card {
  border: 1px solid rgba(226, 232, 240, 0.86);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.05);
}

.detail-fact-card {
  padding: 18px;
}

.detail-fact-card span,
.detail-fact-card small {
  color: var(--text-secondary);
}

.detail-fact-card strong {
  color: var(--text-primary);
}

.detail-fact-card__copy {
  font-weight: 700;
}

.detail-guide-list {
  margin: 0;
  padding-left: 18px;
  color: var(--text-secondary);
  line-height: 1.8;
}

.stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stack small {
  color: var(--text-secondary);
}

.ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 1280px) {
  .detail-fact-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .detail-page {
    padding: 16px;
  }

  .detail-panel {
    padding: 18px;
  }
}
</style>
