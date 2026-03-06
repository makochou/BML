<template>
  <div class="api-list-page">
    <!-- 筛选与统计栏（无顶部大标题块） -->
    <div class="api-list-toolbar">
      <div class="toolbar-left">
        <a-input-search
          v-model="filterKeyword"
          placeholder="搜索接口名称、路径或描述"
          allow-clear
          style="width: 280px"
          @search="loadTree"
        />
        <a-select
          v-model="filterMethod"
          placeholder="HTTP 方法"
          allow-clear
          style="width: 120px"
          :options="methodOptions"
          @change="loadTree"
        />
        <a-button type="primary" :loading="loading" @click="loadTree">
          <template #icon><icon-refresh /></template>
          刷新
        </a-button>
      </div>
      <div class="toolbar-right">
        <span class="stat-text" v-if="catalogTree.length">
          共 <strong>{{ moduleCount }}</strong> 个模块，
          <strong>{{ resourceCount }}</strong> 个业务分组，
          <strong>{{ apiCount }}</strong> 个接口
        </span>
      </div>
    </div>

    <!-- 两栏：模块（含展开的业务分组）| 接口列表栏（选中分组后展示该分组下的接口） -->
    <div class="api-list-body">
      <a-spin :loading="loading" class="api-list-spin">
        <div class="hierarchy-layout" v-if="catalogTree.length">
          <!-- 左栏：模块，展开后显示业务分组（如 API 管理 > OpenAPI 示例、API 账号管理等） -->
          <div class="hierarchy-col hierarchy-col-modules">
            <div class="col-title">模块</div>
            <div class="col-list">
              <template v-for="mod in catalogTree" :key="mod.id">
                <div
                  class="col-item col-item-module"
                  :class="{ expanded: expandedModuleIds.has(mod.id) }"
                  @click="toggleModuleExpand(mod.id)"
                >
                  <span class="col-item-arrow">
                    <icon-right />
                  </span>
                  <span class="col-item-icon module-icon"><icon-apps /></span>
                  <span class="col-item-label">{{ mod.label }}</span>
                  <span class="col-item-meta">{{ (mod.children?.length) ?? 0 }} 个分组</span>
                </div>
                <template v-if="expandedModuleIds.has(mod.id) && mod.children?.length">
                  <div
                    v-for="res in mod.children"
                    :key="res.id"
                    class="col-item col-item-resource"
                    :class="{ active: selectedResource?.id === res.id }"
                    @click.stop="selectResource(mod, res)"
                  >
                    <span class="col-item-indent"></span>
                    <span class="col-item-icon resource-icon"><icon-folder /></span>
                    <span class="col-item-label">{{ res.label }}</span>
                    <span class="col-item-meta">{{ (res.children?.length) ?? 0 }} 个接口</span>
                  </div>
                </template>
              </template>
            </div>
          </div>

          <!-- 中栏：接口列表栏（选中左侧业务分组后，在此栏展示该分组下的接口列表） -->
          <div class="hierarchy-col hierarchy-col-api-list">
            <div class="col-title">接口列表栏</div>
            <div class="col-list col-list-apis" v-if="apiList.length">
              <div
                v-for="api in apiList"
                :key="api.id"
                class="api-row"
              >
                <a-tag
                  size="small"
                  :color="getMethodTagColor(api.httpMethod)"
                  class="api-method"
                >
                  {{ api.httpMethod || '-' }}
                </a-tag>
                <span class="api-path">{{ api.apiUrl || '-' }}</span>
                <span class="api-desc" v-if="api.description">{{ api.description }}</span>
                <a-tag v-if="api.status === 0" size="small" color="gray">已停用</a-tag>
              </div>
            </div>
            <div class="col-empty" v-else>
              <span>{{ selectedResource ? '该分组下暂无接口' : '请先选择左侧业务分组' }}</span>
            </div>
          </div>
        </div>
        <a-empty
          v-else-if="!loading"
          description="暂无接口数据，请先在「开放接口目录」中同步接口"
          class="empty-block"
        />
      </a-spin>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconRefresh, IconApps, IconFolder, IconRight } from '@arco-design/web-vue/es/icon';
import { fetchApiListTree } from '../../api/apiList';
import type { ApiCatalogTreeNode } from '../../types/apiList';
import { methodOptions, getMethodTagColor } from '../../utils/api-account-governance';

/**
 * 当前选中的模块（一级）
 */
const selectedModule = ref<ApiCatalogTreeNode | null>(null);

/**
 * 当前选中的业务分组（二级），用于右栏展示该分组下的接口列表
 */
const selectedResource = ref<ApiCatalogTreeNode | null>(null);

/** 已展开的模块 id 集合，用于在模块栏内展示其下的业务分组 */
const expandedModuleIds = ref<Set<string>>(new Set());

/**
 * 切换模块展开/收起，展开后在模块栏内显示该模块下的业务分组
 */
function toggleModuleExpand(moduleId: string) {
  const next = new Set(expandedModuleIds.value);
  if (next.has(moduleId)) {
    next.delete(moduleId);
  } else {
    next.add(moduleId);
  }
  expandedModuleIds.value = next;
}

/**
 * 选中某一业务分组，用于在右栏展示该分组下的接口列表
 */
function selectResource(module: ApiCatalogTreeNode, resource: ApiCatalogTreeNode) {
  selectedModule.value = module;
  selectedResource.value = resource;
}

const loading = ref(false);
const catalogTree = ref<ApiCatalogTreeNode[]>([]);
const filterKeyword = ref('');
const filterMethod = ref<string | undefined>(undefined);

/** 当前选中的业务分组下的接口列表（三级，type=API 的节点） */
const apiList = computed(() => {
  const res = selectedResource.value;
  if (!res?.children?.length) return [];
  return res.children.filter((n) => n.type === 'API');
});

const moduleCount = computed(() => catalogTree.value.length);

const resourceCount = computed(() => {
  return catalogTree.value.reduce(
    (sum, m) => sum + (m.children?.length ?? 0),
    0
  );
});

const apiCount = computed(() => {
  function countApis(nodes: ApiCatalogTreeNode[]): number {
    return nodes.reduce((acc, n) => {
      if (n.type === 'API') return acc + 1;
      if (n.children?.length) return acc + countApis(n.children);
      return acc;
    }, 0);
  }
  return countApis(catalogTree.value);
});

async function loadTree() {
  loading.value = true;
  selectedModule.value = null;
  selectedResource.value = null;
  expandedModuleIds.value = new Set();
  try {
    const { data } = await fetchApiListTree({
      keyword: filterKeyword.value || undefined,
      method: filterMethod.value,
    });
    catalogTree.value = Array.isArray(data) ? data : [];
  } catch (e) {
    Message.error((e as Error)?.message || '加载接口列表失败');
    catalogTree.value = [];
  } finally {
    loading.value = false;
  }
}

onMounted(() => loadTree());
</script>

<style scoped>
/**
 * 整页铺满：作为 Layout .page-container 的 flex 子项，占满除标签栏外的可用高度；
 * 一页显示不全时，在「模块 / 业务分组 / 接口列表」各列内分别上下滚动。
 */
.api-list-page {
  display: flex;
  flex-direction: column;
  flex: 1 1 0;
  min-height: 0;
  padding: 0 24px 24px;
  background: linear-gradient(180deg, #f2f5fa 0%, #e8eef5 100%);
}

.api-list-toolbar {
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-right .stat-text {
  font-size: 13px;
  color: #4e5969;
}

.toolbar-right strong {
  color: #165dff;
  font-weight: 600;
}

/* 卡片区域：占据剩余高度，内容超出时在列内上下滚动 */
.api-list-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  padding: 0;
  overflow: hidden;
}

.api-list-spin {
  width: 100%;
  height: 100%;
  min-height: 200px;
  display: flex;
  flex-direction: column;
}

.api-list-spin :deep(.arco-spin) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.api-list-spin :deep(.arco-spin .arco-spin-children) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 左右三栏层级布局：铺满 body，列内 overflow-y: auto 实现上下滚动 */
.hierarchy-layout {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.hierarchy-col {
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e5e6eb;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.hierarchy-col:last-child {
  border-right: none;
}

/* 左栏：模块 + 展开后的业务分组，略宽以容纳缩进 */
.hierarchy-col-modules {
  width: 260px;
  flex-shrink: 0;
}

/* 中栏：接口列表栏（选中业务分组后展示该分组下的接口，占满剩余宽度） */
.hierarchy-col-api-list {
  flex: 1;
  min-width: 280px;
}

.col-title {
  padding: 14px 16px;
  font-size: 12px;
  font-weight: 600;
  color: #86909c;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  background: #f7f8fa;
  border-bottom: 1px solid #e5e6eb;
}

/* 列内列表：flex:1 占满列高，overflow-y:auto 一页显示不了时上下滚动 */
.col-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 0;
  -webkit-overflow-scrolling: touch;
}

.col-list-apis {
  padding: 12px 16px;
}

.col-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.col-item:hover {
  background: #f7f8fa;
}

.col-item.active {
  background: #e8f3ff;
  color: #165dff;
}

.col-item.active .col-item-label {
  color: #165dff;
  font-weight: 500;
}

/**
 * 模块行（上层）：靠左显示，仅保留箭头与图标所需的最小内边距，便于与下层区分
 */
.col-item-module {
  padding-left: 14px;
  padding-right: 12px;
}

.col-item-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  font-size: 12px;
  color: #86909c;
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.col-item-module.expanded .col-item-arrow {
  transform: rotate(90deg);
}

/**
 * 业务分组行（下层）：左侧增加缩进块 + 竖线装饰，与模块行形成明显层级关系
 */
.col-item-resource {
  padding-left: 14px;
  padding-right: 12px;
  border-left: 2px solid transparent;
  margin-left: 20px;
  border-radius: 0 6px 6px 0;
}

.col-item-resource:hover {
  border-left-color: #e5e6eb;
}

.col-item-resource.active {
  border-left-color: #165dff;
}

.col-item-indent {
  width: 24px;
  flex-shrink: 0;
  min-width: 24px;
}

.col-item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  flex-shrink: 0;
}

.module-icon {
  background: linear-gradient(135deg, #e8f3ff 0%, #bedaff 100%);
  color: #165dff;
}

.resource-icon {
  background: #f2f3f5;
  color: #86909c;
}

.col-item.active .resource-icon {
  background: #e8f3ff;
  color: #165dff;
}

.col-item-label {
  flex: 1;
  font-size: 13px;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-item-meta {
  font-size: 11px;
  color: #86909c;
  flex-shrink: 0;
}

.col-empty {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  font-size: 13px;
  color: #86909c;
}

/* 右栏接口行 */
.api-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  transition: background 0.15s ease;
}

.api-row:hover {
  background: #f7f8fa;
}

.api-method {
  flex-shrink: 0;
  min-width: 52px;
  text-align: center;
}

.api-path {
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  color: #1d2129;
  background: #f2f3f5;
  padding: 4px 10px;
  border-radius: 6px;
  flex-shrink: 0;
}

.api-desc {
  font-size: 12px;
  color: #86909c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.empty-block {
  padding: 60px 0;
}
</style>
