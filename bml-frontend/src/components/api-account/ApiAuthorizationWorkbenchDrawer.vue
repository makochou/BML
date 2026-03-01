<template>
  <a-drawer
    :visible="visible"
    width="1240px"
    :header="false"
    :footer="false"
    render-to-body
    unmount-on-close
    @update:visible="$emit('update:visible', $event)"
  >
    <a-spin :loading="loading" class="drawer-body">
      <template v-if="snapshot">
        <GovernanceWorkbenchShell
          class="drawer authorization-workbench"
          eyebrow="Authorization Governance Studio"
          title="接口授权"
          description="为单个 API 账号按模块、控制器和接口维度配置调用权限，保存后即刻影响该账号的实际可访问能力。"
          :tags="heroTags"
          :stats="heroStats"
          theme="ocean"
          sticky-hero
        >
          <template #heroActions>
            <a-button @click="$emit('update:visible', false)">
              <template #icon><icon-close /></template>
              关闭工作台
            </a-button>
            <a-button type="primary" :loading="saving" @click="$emit('save')">保存授权</a-button>
          </template>

          <template #aside>
            <GovernancePanel class="authorization-panel authorization-panel--identity" title="账号画像">
              <GovernanceFactGrid class="authorization-fact-grid" card-class="authorization-fact-card" copy-class="authorization-fact-card__copy" :items="accountFacts" />
            </GovernancePanel>

            <GovernancePanel class="authorization-panel" title="筛选器">
              <a-form layout="vertical" class="authorization-filter-form">
                <a-form-item field="keyword" label="关键字搜索">
                  <a-input-search v-model="filters.keyword" allow-clear placeholder="搜索模块、控制器、接口名或路径" />
                </a-form-item>
                <a-form-item field="moduleName" label="模块筛选">
                  <a-select v-model="filters.moduleName" allow-clear placeholder="筛选模块" :options="moduleOptions" />
                </a-form-item>
                <a-form-item field="method" label="请求方法">
                  <a-select v-model="filters.method" allow-clear placeholder="筛选请求方法" :options="methodOptions" />
                </a-form-item>
              </a-form>
            </GovernancePanel>

            <GovernancePanel class="authorization-panel authorization-panel--guide" title="授权建议">
              <ul class="authorization-guide-list">
                <li v-for="item in guideItems" :key="item">{{ item }}</li>
              </ul>
            </GovernancePanel>

            <GovernancePanel class="authorization-panel authorization-panel--modules scroll" title="模块批量授权">
              <div class="authorization-module-list">
                <article v-for="moduleCard in moduleCards" :key="moduleCard.moduleName" class="authorization-module-card">
                  <div class="authorization-module-card__top">
                    <div>
                      <strong>{{ moduleCard.moduleName }}</strong>
                      <small>{{ moduleCard.apiCount }} 个接口 / {{ moduleCard.controllerCount }} 个控制器</small>
                    </div>
                    <a-tag color="green">{{ moduleCard.selectedCount }} 已选</a-tag>
                  </div>
                  <div class="authorization-module-card__actions">
                    <a-button size="mini" @click="$emit('selectModule', moduleCard.moduleName)">全选模块</a-button>
                    <a-button size="mini" @click="$emit('clearModule', moduleCard.moduleName)">清空模块</a-button>
                  </div>
                </article>
              </div>
            </GovernancePanel>
          </template>

          <GovernancePanel class="authorization-panel">
            <GovernanceStatGrid class="authorization-summary-grid" card-class="authorization-summary-card" :items="summaryCards" />
          </GovernancePanel>

          <section class="authorization-panel authorization-panel--tree grow">
            <div class="section-heading section-heading--row">
              <div>
                <h3>授权树</h3>
                <p>当前视图共命中 {{ visibleApiCount }} 个接口、{{ visibleControllerCount }} 个控制器，可直接批量勾选并保存。</p>
              </div>
              <div class="authorization-tree-actions">
                <a-button @click="$emit('selectVisible')">全选当前视图</a-button>
                <a-button @click="$emit('clearVisible')">清空当前视图</a-button>
                <a-button type="primary" :loading="saving" @click="$emit('save')">保存授权</a-button>
              </div>
            </div>
            <div class="authorization-tree-shell scroll">
              <a-tree
                v-if="treeData.length"
                v-model:checked-keys="checkedKeysModel"
                checkable
                block-node
                :data="treeData"
                :default-expand-all="true"
              >
                <template #title="nodeData">
                  <div v-if="nodeData.nodeType === 'api'" class="node authorization-node">
                    <strong>{{ nodeData.title }}</strong>
                    <div class="node-meta">
                      <a-tag :color="getMethodTagColor(nodeData.httpMethod)">{{ nodeData.httpMethod }}</a-tag>
                      <span>{{ nodeData.apiUrl }}</span>
                    </div>
                  </div>
                  <div v-else class="node authorization-node">
                    <strong>{{ nodeData.title }}</strong>
                    <small>{{ nodeData.description }}</small>
                  </div>
                </template>
              </a-tree>
              <a-empty v-else description="当前筛选条件下没有可授权接口" />
            </div>
          </section>
        </GovernanceWorkbenchShell>
      </template>
    </a-spin>
  </a-drawer>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import { IconClose } from '@arco-design/web-vue/es/icon';
import GovernanceFactGrid from '../governance/GovernanceFactGrid.vue';
import GovernancePanel from '../governance/GovernancePanel.vue';
import GovernanceStatGrid from '../governance/GovernanceStatGrid.vue';
import GovernanceWorkbenchShell from '../governance/GovernanceWorkbenchShell.vue';
import type { ApiAuthorizationSnapshot } from '../../types/apiAccount';
import type { FactCard, WorkbenchStatCard } from '../../types/governance';
import { methodOptions, getMethodTagColor } from '../../utils/api-account-governance';

type AuthorizationFilterModel = {
  keyword: string;
  moduleName: string;
  method: string;
};

type AuthorizationModuleCard = {
  moduleName: string;
  apiCount: number;
  controllerCount: number;
  selectedCount: number;
};

type AuthorizationTreeNode = {
  key: string;
  title: string;
  nodeType: 'module' | 'controller' | 'api';
  description?: string;
  httpMethod?: string;
  apiUrl?: string;
  children?: AuthorizationTreeNode[];
};

const props = defineProps<{
  visible: boolean;
  loading: boolean;
  saving: boolean;
  snapshot: ApiAuthorizationSnapshot | null;
  filters: AuthorizationFilterModel;
  heroTags: string[];
  heroStats: WorkbenchStatCard[];
  accountFacts: FactCard[];
  guideItems: string[];
  moduleCards: AuthorizationModuleCard[];
  summaryCards: WorkbenchStatCard[];
  visibleApiCount: number;
  visibleControllerCount: number;
  moduleOptions: { label: string; value: string }[];
  treeData: AuthorizationTreeNode[];
  checkedKeys: string[];
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  'update:checkedKeys': [value: string[]];
  save: [];
  selectVisible: [];
  clearVisible: [];
  selectModule: [moduleName: string];
  clearModule: [moduleName: string];
}>();

const checkedKeysModel = computed({
  get: () => props.checkedKeys,
  set: value => emit('update:checkedKeys', value)
});
</script>

<style scoped>
.authorization-workbench {
  gap: 20px;
}

.authorization-panel {
  position: relative;
  overflow: hidden;
  border: 1px solid #e5edf6;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(10px);
  padding: 20px;
}

.authorization-panel--identity {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(238, 248, 255, 0.96));
}

.authorization-fact-grid,
.authorization-summary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.authorization-fact-card,
.authorization-module-card,
.authorization-summary-card {
  border: 1px solid rgba(226, 232, 240, 0.86);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
}

.authorization-fact-card,
.authorization-summary-card {
  padding: 18px;
}

.authorization-fact-card span,
.authorization-fact-card small,
.authorization-module-card small,
.authorization-summary-card span,
.authorization-summary-card small {
  color: #64748b;
}

.authorization-fact-card strong,
.authorization-module-card strong,
.authorization-summary-card strong {
  color: #0f172a;
}

.authorization-fact-card__copy {
  font-weight: 700;
}

.authorization-filter-form :deep(.arco-form-item-label-col > label) {
  color: #334155;
  font-weight: 600;
}

.authorization-filter-form :deep(.arco-input-wrapper),
.authorization-filter-form :deep(.arco-select-view) {
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.96);
}

.authorization-guide-list {
  margin: 0;
  padding-left: 18px;
  color: #64748b;
  line-height: 1.8;
}

.authorization-module-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.authorization-module-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
}

.authorization-module-card__top,
.authorization-module-card__actions,
.authorization-tree-actions,
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-heading {
  margin-bottom: 14px;
}

.section-heading h3 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.section-heading p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.authorization-tree-shell {
  min-height: 520px;
  padding: 6px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(255, 255, 255, 0.98));
  border: 1px solid rgba(226, 232, 240, 0.86);
}

.authorization-tree-shell :deep(.arco-tree-node-title) {
  width: 100%;
  border-radius: 16px;
  padding: 10px 12px;
}

.authorization-tree-shell :deep(.arco-tree-node-title:hover) {
  background: rgba(239, 246, 255, 0.96);
}

.authorization-node {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.authorization-node .node-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: #64748b;
}

.authorization-tree-actions {
  flex-wrap: wrap;
}

@media (max-width: 1280px) {
  .authorization-fact-grid,
  .authorization-summary-grid {
    grid-template-columns: 1fr;
  }

  .authorization-tree-actions,
  .section-heading {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
