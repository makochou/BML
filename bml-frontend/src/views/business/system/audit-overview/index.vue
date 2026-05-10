<template>
  <div class="page-wrapper audit-overview-page">
    <GovernanceCompactQueryPanel
      eyebrow="AUDIT CENTER"
      title="日志审计总览"
      description="统一汇总登录、操作、异常、风险告警与归档策略状态，帮助管理员快速掌握系统安全态势。"
      density="ultra"
      theme="aurora"
      :meta-items="metaItems"
    >
      <template #actions>
        <a-button type="primary" :loading="loading" @click="loadOverview">
          <template #icon><icon-refresh /></template>
          刷新态势
        </a-button>
      </template>
    </GovernanceCompactQueryPanel>

    <div class="audit-overview-grid">
      <a-card v-for="item in cards" :key="item.key" class="audit-stat-card" :bordered="false">
        <div class="audit-stat-card__icon" :class="`audit-stat-card__icon--${item.tone}`">
          <component :is="item.icon" />
        </div>
        <div class="audit-stat-card__body">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </div>
      </a-card>
    </div>

    <a-card class="audit-overview-panel" :bordered="false">
      <template #title>审计能力矩阵</template>
      <a-row :gutter="16">
        <a-col v-for="item in capabilityItems" :key="item.title" :xs="24" :md="12" :xl="8">
          <div class="capability-item">
            <div class="capability-item__title">{{ item.title }}</div>
            <div class="capability-item__desc">{{ item.desc }}</div>
            <a-tag :color="item.color">{{ item.status }}</a-tag>
          </div>
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconBug, IconHistory, IconLock, IconNotification, IconRefresh } from '@arco-design/web-vue/es/icon';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import { fetchAuditOverview, type AuditOverviewVO } from '../../../../api/system';

const loading = ref(false);
const overview = ref<AuditOverviewVO>({
  operationTotal: 0,
  operationErrorTotal: 0,
  loginTotal: 0,
  loginFailureTotal: 0,
  alertTotal: 0,
  unreadAlertTotal: 0,
  onlineRetentionDays: 180,
  archiveEnabled: false,
});

const metaItems = computed(() => [
  { label: '登录日志', value: overview.value.loginTotal, tone: 'blue' as const },
  { label: '操作日志', value: overview.value.operationTotal, tone: 'teal' as const },
  { label: '异常记录', value: overview.value.operationErrorTotal, tone: 'gold' as const },
  { label: '未读告警', value: overview.value.unreadAlertTotal, tone: 'violet' as const },
]);

const cards = computed(() => [
  { key: 'login', label: '认证事件', value: overview.value.loginTotal, hint: `失败 ${overview.value.loginFailureTotal} 次`, tone: 'blue', icon: IconLock },
  { key: 'operation', label: '业务操作', value: overview.value.operationTotal, hint: `异常 ${overview.value.operationErrorTotal} 条`, tone: 'teal', icon: IconHistory },
  { key: 'exception', label: '异常日志', value: overview.value.operationErrorTotal, hint: '来自操作日志异常记录', tone: 'gold', icon: IconBug },
  { key: 'alert', label: '风险告警', value: overview.value.alertTotal, hint: `未读 ${overview.value.unreadAlertTotal} 条`, tone: 'violet', icon: IconNotification },
]);

const capabilityItems = computed(() => [
  { title: '多维度复合查询', desc: '支持账号、模块、IP、状态、时间范围等维度快速筛选。', status: '已接入', color: 'green' },
  { title: '详情穿透查看', desc: '操作日志和登录日志支持抽屉查看请求参数、响应和异常摘要。', status: '已接入', color: 'green' },
  { title: '数据脱敏显示', desc: '后端操作日志切面已对密码、密钥、Token 等敏感字段脱敏。', status: '已接入', color: 'green' },
  { title: '安全导出', desc: '登录、操作、异常日志导出动作自身会记录到操作审计。', status: '已接入', color: 'blue' },
  { title: '生命周期管理', desc: `在线保留 ${overview.value.onlineRetentionDays} 天，自动归档${overview.value.archiveEnabled ? '已启用' : '未启用'}。`, status: '策略化', color: 'arcoblue' },
  { title: '数据 Diff 视图', desc: '关键业务修改前后对比预留扩展能力，后续按业务表逐步接入。', status: '规划中', color: 'orange' },
]);

const loadOverview = async () => {
  loading.value = true;
  try {
    const res = await fetchAuditOverview() as any;
    overview.value = res.data || overview.value;
  } catch (error) {
    Message.error('加载审计概览失败');
  } finally {
    loading.value = false;
  }
};

onMounted(loadOverview);
</script>

<style scoped>
.audit-overview-page {
  min-height: 100%;
}

.audit-overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.audit-stat-card {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
}

.audit-stat-card :deep(.arco-card-body) {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 18px;
}

.audit-stat-card__icon {
  display: inline-flex;
  width: 46px;
  height: 46px;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  font-size: 22px;
}

.audit-stat-card__icon--blue { color: #165dff; background: rgba(22, 93, 255, 0.12); }
.audit-stat-card__icon--teal { color: #0fc6c2; background: rgba(15, 198, 194, 0.12); }
.audit-stat-card__icon--gold { color: #ff7d00; background: rgba(255, 125, 0, 0.12); }
.audit-stat-card__icon--violet { color: #722ed1; background: rgba(114, 46, 209, 0.12); }

.audit-stat-card__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.audit-stat-card__body span {
  color: var(--color-text-2);
  font-size: 13px;
}

.audit-stat-card__body strong {
  color: var(--color-text-1);
  font-size: 28px;
  line-height: 1;
}

.audit-stat-card__body small {
  color: var(--color-text-3);
}

.audit-overview-panel {
  margin-top: 16px;
  border-radius: 18px;
}

.capability-item {
  min-height: 128px;
  margin-bottom: 16px;
  padding: 18px;
  border: 1px solid var(--color-border-2);
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(255,255,255,0.94), rgba(245,248,255,0.9));
}

.capability-item__title {
  margin-bottom: 8px;
  color: var(--color-text-1);
  font-size: 16px;
  font-weight: 700;
}

.capability-item__desc {
  min-height: 42px;
  margin-bottom: 12px;
  color: var(--color-text-2);
  line-height: 1.6;
}

@media (max-width: 1200px) {
  .audit-overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .audit-overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
