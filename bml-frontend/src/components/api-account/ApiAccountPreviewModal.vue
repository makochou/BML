<template>
  <a-modal
    :visible="visible"
    width="1160px"
    :footer="false"
    unmount-on-close
    @update:visible="value => emit('update:visible', value)"
  >
    <template #title>
      <div class="account-preview-modal__title">
        <div class="account-preview-modal__heading">
          <p>ACCOUNT DETAIL SNAPSHOT</p>
          <strong>{{ account?.accountName || 'API账号详情' }}</strong>

          <div v-if="account" class="account-preview-modal__meta">
            <span>#{{ account.id }}</span>
            <a-tag :color="account.accountType === 1 ? 'arcoblue' : 'purple'">
              {{ account.accountType === 1 ? '内部账号' : '外部账号' }}
            </a-tag>
            <a-tag :color="account.status === 1 ? 'green' : 'red'">
              {{ account.status === 1 ? '启用' : '停用' }}
            </a-tag>
          </div>
        </div>

        <div class="account-preview-modal__actions">
          <a-button :disabled="!account" @click="emit('edit')">编辑账号</a-button>
          <a-button type="primary" :disabled="!account" @click="emit('authorization')">接口授权</a-button>
          <a-button :disabled="!account" @click="emit('callback')">回调日志</a-button>

          <a-dropdown trigger="click" position="br">
            <a-button :disabled="!account">更多操作</a-button>
            <template #content>
              <a-doption @click="emit('reset-secret')">重置密钥</a-doption>
              <a-doption class="account-preview-modal__danger" @click="emit('delete')">
                删除账号
              </a-doption>
            </template>
          </a-dropdown>
        </div>
      </div>
    </template>

    <div class="account-preview-modal__shell">
      <a-spin :loading="loading" class="account-preview-modal__spin">
        <div v-if="account" class="account-preview-modal__content">
          <GovernanceStatGrid
            class="account-preview-modal__stats"
            card-class="account-preview-modal__stat-card"
            :items="stats"
          />

          <section class="account-preview-modal__section">
            <div class="account-preview-modal__section-head">
              <h4>基础信息</h4>
              <p>账号主体、业务归属和负责人信息统一在这里查看。</p>
            </div>

            <GovernanceFactGrid
              class="account-preview-modal__fact-grid"
              card-class="account-preview-modal__fact-card"
              copy-class="account-preview-modal__copy"
              :items="basicFacts"
            />
          </section>

          <section class="account-preview-modal__section">
            <div class="account-preview-modal__section-head">
              <h4>接入配置</h4>
              <p>环境、客户端、签名版本、回调地址和限流策略集中收口。</p>
            </div>

            <GovernanceFactGrid
              class="account-preview-modal__fact-grid"
              card-class="account-preview-modal__fact-card"
              copy-class="account-preview-modal__copy"
              :items="accessFacts"
            />
          </section>

          <section class="account-preview-modal__section">
            <div class="account-preview-modal__section-head">
              <h4>治理字段</h4>
              <p>低频治理字段放到弹窗中查看，列表只保留高频主信息。</p>
            </div>

            <GovernanceFactGrid
              class="account-preview-modal__fact-grid"
              card-class="account-preview-modal__fact-card"
              copy-class="account-preview-modal__copy"
              :items="governanceFacts"
            />
          </section>
        </div>
      </a-spin>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import type { ApiAccountDetail } from '../../types/apiAccount';
import type { FactCard, WorkbenchStatCard } from '../../types/governance';
import GovernanceFactGrid from '../governance/GovernanceFactGrid.vue';
import GovernanceStatGrid from '../governance/GovernanceStatGrid.vue';

/**
 * API 账号详情预览弹窗。
 * 列表仅展示高频主信息，低频字段统一放入此弹窗查看，并在右上角暴露主操作按钮。
 */
defineProps<{
  visible: boolean;
  loading: boolean;
  account: ApiAccountDetail | null;
  stats: WorkbenchStatCard[];
  basicFacts: FactCard[];
  accessFacts: FactCard[];
  governanceFacts: FactCard[];
}>();

const emit = defineEmits<{
  (event: 'update:visible', value: boolean): void;
  (event: 'edit'): void;
  (event: 'authorization'): void;
  (event: 'callback'): void;
  (event: 'reset-secret'): void;
  (event: 'delete'): void;
}>();
</script>

<style scoped>
.account-preview-modal__title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  width: 100%;
}

.account-preview-modal__heading {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.account-preview-modal__heading p {
  margin: 0;
  color: #7c8aa5;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.account-preview-modal__heading strong {
  color: #0f172a;
  font-size: 28px;
  line-height: 1.12;
}

.account-preview-modal__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  color: #64748b;
  font-size: 12px;
}

.account-preview-modal__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.account-preview-modal__actions :deep(.arco-btn) {
  min-width: 102px;
  height: 38px;
  border-radius: 999px;
  font-weight: 700;
}

.account-preview-modal__actions :deep(.arco-btn-primary) {
  border: 0;
  background: linear-gradient(135deg, #1769ff, #12b8a6);
  box-shadow: 0 14px 28px rgba(23, 105, 255, 0.16);
}

.account-preview-modal__shell {
  min-height: 240px;
}

.account-preview-modal__spin :deep(.arco-spin-children) {
  width: 100%;
}

.account-preview-modal__content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.account-preview-modal__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.account-preview-modal__stat-card {
  padding: 16px 18px;
  border-radius: 22px;
  border: 1px solid rgba(225, 232, 240, 0.94);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(245, 249, 255, 0.96));
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.04);
}

.account-preview-modal__stat-card span,
.account-preview-modal__stat-card small {
  color: #64748b;
  font-size: 12px;
  line-height: 1.65;
}

.account-preview-modal__stat-card strong {
  color: #0f172a;
  font-size: 26px;
  line-height: 1.1;
}

.account-preview-modal__section {
  padding: 18px;
  border-radius: 24px;
  border: 1px solid rgba(225, 232, 240, 0.94);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.98));
}

.account-preview-modal__section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.account-preview-modal__section-head h4 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
}

.account-preview-modal__section-head p {
  margin: 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.7;
}

.account-preview-modal__fact-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.account-preview-modal__fact-card {
  min-height: 118px;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 249, 255, 0.96));
}

.account-preview-modal__fact-card span,
.account-preview-modal__fact-card small {
  color: #64748b;
  font-size: 12px;
  line-height: 1.65;
}

.account-preview-modal__fact-card strong,
.account-preview-modal__copy {
  color: #0f172a;
  font-size: 16px;
  line-height: 1.5;
  font-weight: 700;
  word-break: break-all;
}

.account-preview-modal__danger {
  color: #f53f3f;
}

@media (max-width: 1200px) {
  .account-preview-modal__title,
  .account-preview-modal__section-head {
    flex-direction: column;
  }

  .account-preview-modal__actions {
    justify-content: flex-start;
  }

  .account-preview-modal__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .account-preview-modal__fact-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .account-preview-modal__stats,
  .account-preview-modal__fact-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
