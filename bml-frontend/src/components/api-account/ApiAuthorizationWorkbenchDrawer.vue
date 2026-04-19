<template>
  <a-drawer
    :visible="visible"
    width="1280px"
    unmount-on-close
    render-to-body
    class="authorization-drawer"
    @update:visible="$emit('update:visible', $event)"
  >
    <template #title>
      <div class="drawer-header-content">
        <h2 class="title">接口授权</h2>
      </div>
    </template>
    <template #extra>
      <div class="drawer-header-actions">
        <a-button @click="$emit('update:visible', false)">
          <template #icon><icon-close /></template>
          取消
        </a-button>
        <a-button type="primary" :loading="saving" @click="$emit('save')">
          提交
        </a-button>
      </div>
    </template>
    <template #footer>
      <div class="drawer-footer-actions">
        <a-button @click="$emit('update:visible', false)">
          取消
        </a-button>
        <a-button type="primary" :loading="saving" @click="$emit('save')">
          提交
        </a-button>
      </div>
    </template>
    <div class="drawer-body-wrapper">
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
          hide-hero
        >

          <template #aside>
            <GovernancePanel class="authorization-panel authorization-panel--identity" title="账号画像">
              <GovernanceFactGrid class="authorization-fact-grid" card-class="authorization-fact-card" copy-class="authorization-fact-card__copy" :items="accountFacts" />
            </GovernancePanel>

            <GovernancePanel class="authorization-panel authorization-panel--summary" title="授权数据概览">
              <GovernanceStatGrid class="authorization-summary-grid" card-class="authorization-summary-card" :items="filteredSummaryCards" />
            </GovernancePanel>


          </template>



          <section class="authorization-panel authorization-panel--tree">
            <!-- 头部筛选工具栏 -->
            <div class="authorization-toolbar">
              <a-form :model="filters" layout="inline" class="authorization-filter-form">
                <a-form-item field="keyword" label="搜索">
                  <a-input-search v-model="filters.keyword" allow-clear placeholder="模块/控制器/路径" style="width: 240px" />
                </a-form-item>
                <a-form-item field="moduleName" label="模块">
                  <a-select v-model="filters.moduleName" allow-clear placeholder="所有模块" :options="moduleOptions" style="width: 160px" />
                </a-form-item>
                <a-form-item field="method" label="方法">
                  <a-select v-model="filters.method" allow-clear placeholder="所有方法" :options="methodOptions" style="width: 120px" />
                </a-form-item>
              </a-form>
            </div>

            <div class="section-heading section-heading--row">
              <div>
                <h3>授权树</h3>
              </div>
              <div class="authorization-tree-actions">
                <a-button @click="$emit('selectVisible')">全选当前视图</a-button>
                <a-button @click="$emit('clearVisible')">清空当前视图</a-button>
              </div>
            </div>
            <div class="authorization-tree-shell">
              <ApiCatalogTree
                v-if="treeData.length"
                :nodes="treeData"
                checkable
                show-full-api-info
                v-model:checked-keys="checkedKeysModel"
                v-model:expanded-module-ids="expandedModuleIds"
                v-model:expanded-resource-ids="expandedResourceIds"
                show-method
              />
              <a-empty v-else description="当前筛选条件下没有可授权接口" />
            </div>
          </section>
        </GovernanceWorkbenchShell>
      </template>
    </a-spin>
    </div>
  </a-drawer>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue';
import { IconClose } from '@arco-design/web-vue/es/icon';
import GovernanceFactGrid from '../governance/GovernanceFactGrid.vue';
import GovernancePanel from '../governance/GovernancePanel.vue';
import GovernanceStatGrid from '../governance/GovernanceStatGrid.vue';
import GovernanceWorkbenchShell from '../governance/GovernanceWorkbenchShell.vue';
import ApiCatalogTree from '../api/ApiCatalogTree.vue';
import type { ApiAuthorizationSnapshot } from '../../types/apiAccount';
import type { ApiCatalogTreeNode } from '../../types/apiList';
import type { FactCard, WorkbenchStatCard } from '../../types/governance';
import { methodOptions } from '../../utils/api-account-governance';

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
  treeData: ApiCatalogTreeNode[];
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

// --- 树展开状态管理 ---
const expandedModuleIds = ref<Set<string>>(new Set());
const expandedResourceIds = ref<Set<string>>(new Set());

// 当数据变化时，默认展开所有（模拟 a-tree default-expand-all）
watch(() => props.treeData, (newNodes) => {
  if (newNodes.length > 0) {
    const modIds = new Set<string>();
    const resIds = new Set<string>();
    newNodes.forEach(mod => {
      modIds.add(mod.id);
      if (mod.children) {
        mod.children.forEach(res => {
          resIds.add(res.id);
        });
      }
    });
    expandedModuleIds.value = modIds;
    expandedResourceIds.value = resIds;
  }
}, { immediate: true });
const filteredSummaryCards = computed(() => {
  return props.summaryCards.filter(card => 
    card.label === '已选接口' || card.label === '目录总接口'
  );
});
</script>

<style scoped>
/* 最外层包装器 - 强制高度约束 */
.drawer-body-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  height: 100%;
}

/* 抽屉整体高度锁定与布局增强 - 强制覆盖所有默认样式 */
:deep(.authorization-drawer .arco-drawer-content) {
  display: flex !important;
  flex-direction: column !important;
  overflow: hidden !important;
  height: 100% !important;
}

:deep(.authorization-drawer .arco-drawer-body) {
  padding: 0 !important;
  flex: 1 !important;
  overflow: hidden !important;
  display: flex !important;
  flex-direction: column !important;
  min-height: 0 !important;
  max-height: 100% !important;
}

/* 强制 a-spin 组件也参与 flex 布局 */
.drawer-body {
  flex: 1 !important;
  display: flex !important;
  flex-direction: column !important;
  min-height: 0 !important;
  overflow: hidden !important;
  padding: 24px 24px 0; /* 移除底部 padding，让内容完全填充 */
}

:deep(.drawer-body .arco-spin-children) {
  flex: 1 !important;
  display: flex !important;
  flex-direction: column !important;
  min-height: 0 !important;
  overflow: hidden !important;
  height: 100% !important;
}

.authorization-workbench {
  flex: 1 !important;
  display: flex !important;
  flex-direction: column !important;
  min-height: 0 !important;
  gap: 0; /* 移除 gap，让内容紧密排列 */
  overflow: hidden !important;
  height: 100% !important;
  max-height: 100% !important;
  padding-bottom: 0; /* 移除底部 padding */
}

/* 确保 GovernanceWorkbenchShell 的布局填满整个空间 */
:deep(.authorization-workbench .governance-workbench__layout) {
  flex: 1 !important;
  min-height: 0 !important;
  height: 100% !important;
  max-height: 100% !important;
  display: grid !important;
  overflow: hidden !important;
  align-items: stretch !important;
  grid-template-rows: 1fr !important;
  gap: 20px; /* Grid 的列间距 */
  padding-bottom: 0; /* 移除底部间距，完全铺满 */
}

/* 侧边栏样式优化 - 确保内容垂直分布并填满 */
:deep(.authorization-workbench .governance-workbench__aside) {
  overflow: visible !important;
  min-height: 0 !important;
  max-height: 100% !important;
  height: 100% !important;
  display: flex !important;
  flex-direction: column !important;
  gap: 20px !important;
  justify-content: space-between !important; /* 让两个卡片上下分布 */
}

/* 主内容区域样式优化 */
:deep(.authorization-workbench .governance-workbench__main) {
  display: flex !important;
  flex-direction: column !important;
  min-height: 0 !important;
  height: 100% !important;
  max-height: 100% !important;
  overflow: hidden !important;
}

/* 抽屉头部增强 */
:deep(.authorization-drawer .arco-drawer-header) {
  height: 64px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #f2f3f5;
  display: flex;
  align-items: center;
}

.drawer-header-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.drawer-header-content .eyebrow {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #86909c;
  font-weight: 500;
}

.drawer-header-content .title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1d2129;
}

.drawer-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.drawer-header-actions :deep(.arco-btn) {
  border-radius: 10px;
  font-weight: 500;
}

/**
 * 确保抽屉头部按钮样式清晰可见
 * 修复主题色变量导致的按钮显示问题
 */
.drawer-header-actions :deep(.arco-btn-primary) {
  background: var(--color-primary, #165dff) !important;
  border-color: var(--color-primary, #165dff) !important;
  color: #fff !important;
}

.drawer-header-actions :deep(.arco-btn-primary:hover) {
  background: var(--color-primary-light-4, #4080ff) !important;
  border-color: var(--color-primary-light-4, #4080ff) !important;
  color: #fff !important;
}

.drawer-header-actions :deep(.arco-btn-primary:active) {
  background: var(--color-primary-dark-1, #0e42d2) !important;
  border-color: var(--color-primary-dark-1, #0e42d2) !important;
  color: #fff !important;
}

.drawer-header-actions :deep(.arco-btn:not(.arco-btn-primary)) {
  background: #fff !important;
  border-color: #e5e6eb !important;
  color: #4e5969 !important;
}

.drawer-header-actions :deep(.arco-btn:not(.arco-btn-primary):hover) {
  background: #f7f8fa !important;
  border-color: #c9cdd4 !important;
  color: #1d2129 !important;
}

.drawer-header-actions :deep(.arco-btn .arco-icon) {
  color: inherit !important;
}

/**
 * 抽屉底部按钮样式
 * 确保底部操作按钮清晰可见
 */
.drawer-footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  width: 100%;
}

.drawer-footer-actions :deep(.arco-btn-primary) {
  background: var(--color-primary, #165dff) !important;
  border-color: var(--color-primary, #165dff) !important;
  color: #fff !important;
  border-radius: 10px;
  font-weight: 500;
  padding: 0 20px;
  height: 36px;
}

.drawer-footer-actions :deep(.arco-btn-primary:hover) {
  background: var(--color-primary-light-4, #4080ff) !important;
  border-color: var(--color-primary-light-4, #4080ff) !important;
  color: #fff !important;
}

.drawer-footer-actions :deep(.arco-btn-primary:active) {
  background: var(--color-primary-dark-1, #0e42d2) !important;
  border-color: var(--color-primary-dark-1, #0e42d2) !important;
  color: #fff !important;
}

.drawer-footer-actions :deep(.arco-btn:not(.arco-btn-primary)) {
  background: #fff !important;
  border-color: #e5e6eb !important;
  color: #4e5969 !important;
  border-radius: 10px;
  font-weight: 500;
  padding: 0 20px;
  height: 36px;
}

.drawer-footer-actions :deep(.arco-btn:not(.arco-btn-primary):hover) {
  background: #f7f8fa !important;
  border-color: #c9cdd4 !important;
  color: #1d2129 !important;
}

.drawer-footer-actions :deep(.arco-btn .arco-icon) {
  color: inherit !important;
}

/**
 * 抽屉底部区域样式优化
 * 确保底部按钮区域美观且功能清晰
 */
:deep(.authorization-drawer .arco-drawer-footer) {
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #f2f3f5;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.03);
}

.authorization-panel {
  position: relative;
  border: 1px solid #e5edf6;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(10px);
  padding: 20px;
}

/* 侧边栏面板保持 overflow: hidden */
.authorization-panel--identity,
.authorization-panel--summary {
  overflow: hidden;
}

/* 主内容面板 - 完全填充父容器 */
.authorization-panel--tree {
  display: flex !important;
  flex-direction: column !important;
  min-height: 0 !important;
  flex: 1 !important;
  overflow: hidden !important;
  max-height: 100% !important;
  height: 100% !important;
  padding: 0 !important; /* 移除 padding */
  background: transparent !important; /* 背景透明 */
  border: none !important; /* 移除边框 */
  box-shadow: none !important; /* 移除阴影 */
}

.authorization-panel--identity {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(238, 248, 255, 0.96));
  flex: 0 0 auto; /* 不拉伸，保持内容高度 */
}

.authorization-fact-grid,
.authorization-summary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.authorization-panel--summary {
  background: linear-gradient(180deg, #f0f7ff 0%, #ffffff 100%);
  border: 1px solid #e1eaff;
  flex: 1; /* 让这个卡片拉伸填充剩余空间 */
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.authorization-summary-grid {
  display: grid !important;
  grid-template-columns: repeat(2, 1fr) !important;
  gap: 12px;
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

.authorization-toolbar {
  margin-bottom: 20px;
  padding: 20px 20px 20px 20px; /* 添加左右内边距 */
  border-bottom: 1px solid #f1f5f9;
  flex-shrink: 0 !important;
}

.authorization-filter-form :deep(.arco-form-item) {
  margin-bottom: 0;
  margin-right: 24px;
}

.authorization-filter-form :deep(.arco-input-wrapper),
.authorization-filter-form :deep(.arco-select-view) {
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.96);
  border: 1px solid #e2e8f0;
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
  padding: 0 20px; /* 添加左右内边距 */
  flex-shrink: 0 !important;
}

.section-heading h3 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.section-heading p {
  display: none;
}

.authorization-tree-shell {
  flex: 1 !important;
  min-height: 0 !important;
  max-height: 100% !important;
  margin: 0 20px 0 20px; /* 移除底部外边距，让卡片完全填充到底部 */
  padding: 20px; /* 内边距，让树形结构更舒适 */
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(255, 255, 255, 0.98));
  border: 1px solid rgba(226, 232, 240, 0.86);
  overflow-y: auto !important;
  overflow-x: hidden !important;
}

/* 自定义滚动条样式 - 现代化设计 */
.authorization-tree-shell::-webkit-scrollbar {
  width: 8px;
}

.authorization-tree-shell::-webkit-scrollbar-track {
  background: rgba(241, 245, 249, 0.6);
  border-radius: 10px;
  margin: 8px 0;
}

.authorization-tree-shell::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #cbd5e1 0%, #94a3b8 100%);
  border-radius: 10px;
  transition: background 0.3s ease;
}

.authorization-tree-shell::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #94a3b8 0%, #64748b 100%);
}

.authorization-tree-shell::-webkit-scrollbar-thumb:active {
  background: linear-gradient(180deg, #64748b 0%, #475569 100%);
}

/* Firefox 滚动条样式 */
.authorization-tree-shell {
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 rgba(241, 245, 249, 0.6);
}

.authorization-tree-shell :deep(.api-catalog-tree) {
  /* 确保树组件不会撑开容器 */
  max-height: 100%;
  overflow: visible;
}

.authorization-tree-shell :deep(.arco-tree) {
  padding: 0;
}

.authorization-tree-shell :deep(.arco-empty) {
  padding: 40px 0;
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

/**
 * 授权树操作按钮样式修复
 * 确保所有按钮在任何主题下都清晰可见
 */
.authorization-tree-actions :deep(.arco-btn-primary) {
  background: var(--color-primary, #165dff) !important;
  border-color: var(--color-primary, #165dff) !important;
  color: #fff !important;
}

.authorization-tree-actions :deep(.arco-btn-primary:hover) {
  background: var(--color-primary-light-4, #4080ff) !important;
  border-color: var(--color-primary-light-4, #4080ff) !important;
  color: #fff !important;
}

.authorization-tree-actions :deep(.arco-btn-primary:active) {
  background: var(--color-primary-dark-1, #0e42d2) !important;
  border-color: var(--color-primary-dark-1, #0e42d2) !important;
  color: #fff !important;
}

.authorization-tree-actions :deep(.arco-btn:not(.arco-btn-primary)) {
  background: #fff !important;
  border-color: #e5e6eb !important;
  color: #4e5969 !important;
}

.authorization-tree-actions :deep(.arco-btn:not(.arco-btn-primary):hover) {
  background: #f7f8fa !important;
  border-color: #c9cdd4 !important;
  color: #1d2129 !important;
}

.authorization-tree-actions :deep(.arco-btn .arco-icon) {
  color: inherit !important;
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
