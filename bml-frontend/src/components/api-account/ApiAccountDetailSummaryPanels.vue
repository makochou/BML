<template>
  <div class="api-account-detail-summary">
    <GovernancePanel class="api-account-detail-summary__panel">
      <div class="api-account-detail-summary__heading">
        <div>
          <h3>核心治理摘要</h3>
          <p>从账号可用性、安全策略、调用边界与回调配置四个维度，快速确认当前账号的治理状态。</p>
        </div>
      </div>
      <GovernanceStatGrid
        class="api-account-detail-summary__grid"
        card-class="api-account-detail-summary__card"
        :items="overviewStats"
      />
    </GovernancePanel>

    <div class="api-account-detail-summary__dual-grid">
      <GovernancePanel class="api-account-detail-summary__panel">
        <div class="api-account-detail-summary__heading api-account-detail-summary__heading--row">
          <div>
            <h3>接口授权现状</h3>
            <p>统一查看当前账号的授权库存、已选接口与启用接口情况，支持一键跳转到授权工作台。</p>
          </div>
          <a-button @click="$emit('openAuthorization')">打开授权工作台</a-button>
        </div>
        <GovernanceStatGrid
          class="api-account-detail-summary__grid"
          card-class="api-account-detail-summary__card"
          :items="authorizationStats"
        />
      </GovernancePanel>

      <GovernancePanel class="api-account-detail-summary__panel">
        <div class="api-account-detail-summary__heading api-account-detail-summary__heading--row">
          <div>
            <h3>回调运行概况</h3>
            <p>查看当前账号的回调投递结果与失败风险，支持跳转到完整日志工作台进行排查和补投。</p>
          </div>
          <a-button @click="$emit('openCallback')">打开回调日志</a-button>
        </div>
        <GovernanceStatGrid
          class="api-account-detail-summary__grid"
          card-class="api-account-detail-summary__card"
          :items="callbackStats"
        />
      </GovernancePanel>
    </div>
  </div>
</template>

<script lang="ts" setup>
import GovernancePanel from '../governance/GovernancePanel.vue';
import GovernanceStatGrid from '../governance/GovernanceStatGrid.vue';
import type { WorkbenchStatCard } from '../../types/governance';

defineProps<{
  overviewStats: WorkbenchStatCard[];
  authorizationStats: WorkbenchStatCard[];
  callbackStats: WorkbenchStatCard[];
}>();

defineEmits<{
  openAuthorization: [];
  openCallback: [];
}>();
</script>

<style scoped>
.api-account-detail-summary {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.api-account-detail-summary__panel {
  border: 1px solid #e5edf6;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(10px);
  padding: 22px;
}

.api-account-detail-summary__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.api-account-detail-summary__heading--row {
  align-items: center;
}

.api-account-detail-summary__heading h3 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.api-account-detail-summary__heading p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.api-account-detail-summary__grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.api-account-detail-summary__card {
  padding: 18px;
  border: 1px solid rgba(226, 232, 240, 0.86);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.05);
}

.api-account-detail-summary__card span,
.api-account-detail-summary__card small {
  color: #64748b;
}

.api-account-detail-summary__card strong {
  color: #0f172a;
}

.api-account-detail-summary__dual-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

@media (max-width: 1280px) {
  .api-account-detail-summary__dual-grid,
  .api-account-detail-summary__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .api-account-detail-summary__panel {
    padding: 18px;
  }

  .api-account-detail-summary__heading {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
