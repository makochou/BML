<template>
  <div class="api-catalog-tree">
    <template v-for="mod in nodes" :key="mod.id">
      <!-- 模块级 (Level 1) -->
      <div
        class="tree-item tree-item-module"
        :class="{ expanded: isExpanded(mod.id, 'MODULE') }"
        @click="toggleExpand(mod.id, 'MODULE')"
      >
        <div class="item-main">
          <span class="item-toggle">
            <component :is="isExpanded(mod.id, 'MODULE') ? IconMinus : IconPlus" />
          </span>
          <a-checkbox
            v-if="checkable"
            :model-value="isModuleChecked(mod)"
            :indeterminate="isModuleIndeterminate(mod)"
            @click.stop
            @change="(val: boolean | string | number) => handleModuleCheck(mod, !!val)"
          />
          <span class="item-label">{{ mod.label }}</span>
          <span class="item-meta">{{ mod.children?.length ?? 0 }} 个分组</span>
        </div>
      </div>

      <!-- 子节点：业务分组 (Level 2) -->
      <template v-if="isExpanded(mod.id, 'MODULE') && mod.children?.length">
        <div class="module-children">
          <template v-for="res in mod.children" :key="res.id">
            <div
              class="tree-item tree-item-resource"
              :class="{ expanded: isExpanded(res.id, 'RESOURCE') }"
              @click.stop="toggleExpand(res.id, 'RESOURCE')"
            >
              <div class="item-main">
                <span class="item-toggle toggle-resource">
                  <component :is="isExpanded(res.id, 'RESOURCE') ? IconMinus : IconPlus" />
                </span>
                <a-checkbox
                  v-if="checkable"
                  :model-value="isResourceChecked(res)"
                  :indeterminate="isResourceIndeterminate(res)"
                  @click.stop
                  @change="(val: boolean | string | number) => handleResourceCheck(res, !!val)"
                />
                <span class="item-label">{{ res.label }}</span>
                <span class="item-meta">{{ res.children?.length ?? 0 }} 个接口</span>
              </div>
            </div>

            <!-- 子节点：API (Level 3 / Leaf) -->
            <template v-if="isExpanded(res.id, 'RESOURCE') && res.children?.length">
              <div class="resource-children">
                <div
                  v-for="api in getApiNodes(res.children)"
                  :key="api.id"
                  class="tree-item tree-item-api"
                  :class="{ 
                    active: selectedApiId === api.id,
                    'is-checked': checkedKeys.includes(api.id)
                  }"
                  @click.stop="handleApiClick(res, api)"
                >
                  <div class="item-main">
                    <a-checkbox
                      v-if="checkable"
                      :model-value="checkedKeys.includes(api.id)"
                      @click.stop
                      @change="(val: boolean | string | number) => handleApiCheck(api.id, !!val)"
                    />
                    <!-- 完整接口信息：方法 + 路径 + 名称（用于授权抽屉） -->
                    <div v-if="showFullApiInfo" class="api-full-info" :title="getApiTitle(api)">
                      <a-tag 
                        v-if="api.httpMethod" 
                        size="small" 
                        :color="getMethodColor(api.httpMethod)"
                        class="api-method-tag"
                      >
                        {{ api.httpMethod }}
                      </a-tag>
                      <span class="api-path">{{ api.apiUrl || '/未知路径' }}</span>
                      <span class="api-name-separator">-</span>
                      <span class="api-friendly">{{ getApiLabel(api) }}</span>
                    </div>
                    <!-- 简洁接口信息：仅名称（用于接口列表页） -->
                    <span v-else class="api-friendly" :title="getApiTitle(api)">
                      {{ getApiLabel(api) }}
                    </span>
                    <!-- 状态标签 -->
                    <div class="api-node-meta">
                      <a-tag v-if="api.status === 0" size="small" color="gray" class="api-status-tag">已停用</a-tag>
                      <a-tag v-if="showMethod && api.httpMethod && !showFullApiInfo" size="small" :color="getMethodColor(api.httpMethod)">{{ api.httpMethod }}</a-tag>
                    </div>
                    <!-- 悬浮指示器 (非勾选模式下显示) -->
                    <div v-if="!checkable" class="api-item-hover-indicator">
                      <icon-right />
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </template>
        </div>
      </template>
    </template>
  </div>
</template>

<script lang="ts" setup>
import { 
  IconPlus, 
  IconMinus, 
  IconRight 
} from '@arco-design/web-vue/es/icon';
import type { ApiCatalogTreeNode } from '../../types/apiList';
import { getMethodTagColor } from '../../utils/api-account-governance';

const props = withDefaults(defineProps<{
  /** 树节点数据 */
  nodes: ApiCatalogTreeNode[];
  /** 是否启用勾选模式 */
  checkable?: boolean;
  /** 已勾选的 Key (双向绑定) */
  checkedKeys?: string[];
  /** 列表模式下选中的 ID */
  selectedApiId?: string;
  /** 已展开的模块 ID 集合 */
  expandedModuleIds?: Set<string>;
  /** 已展开的业务分组 ID 集合 */
  expandedResourceIds?: Set<string>;
  /** 是否显示 HTTP 方法标签 */
  showMethod?: boolean;
  /** 是否显示完整接口信息（方法+路径+名称） */
  showFullApiInfo?: boolean;
  /** 获取 API 显示名称的函数 */
  apiLabelFormatter?: (api: ApiCatalogTreeNode) => string;
}>(), {
  checkable: false,
  checkedKeys: () => [],
  showMethod: false,
  showFullApiInfo: false,
  expandedModuleIds: () => new Set(),
  expandedResourceIds: () => new Set()
});

const emit = defineEmits<{
  'update:checkedKeys': [keys: string[]];
  'update:expandedModuleIds': [ids: Set<string>];
  'update:expandedResourceIds': [ids: Set<string>];
  'api-click': [resource: ApiCatalogTreeNode, api: ApiCatalogTreeNode];
}>();

// --- 展开/收起逻辑 ---

function isExpanded(id: string, type: 'MODULE' | 'RESOURCE') {
  return type === 'MODULE' 
    ? props.expandedModuleIds.has(id) 
    : props.expandedResourceIds.has(id);
}

function toggleExpand(id: string, type: 'MODULE' | 'RESOURCE') {
  if (type === 'MODULE') {
    const next = new Set(props.expandedModuleIds);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    emit('update:expandedModuleIds', next);
  } else {
    const next = new Set(props.expandedResourceIds);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    emit('update:expandedResourceIds', next);
  }
}

// --- 勾选逻辑 ---

/**
 * 递归获取节点下所有的 API ID
 */
function getAllApiIds(node: ApiCatalogTreeNode): string[] {
  if (node.type === 'API') return [node.id];
  let ids: string[] = [];
  if (node.children) {
    node.children.forEach(child => {
      ids = ids.concat(getAllApiIds(child));
    });
  }
  return ids;
}

function isModuleChecked(mod: ApiCatalogTreeNode) {
  const ids = getAllApiIds(mod);
  return ids.length > 0 && ids.every(id => props.checkedKeys.includes(id));
}

function isModuleIndeterminate(mod: ApiCatalogTreeNode) {
  const ids = getAllApiIds(mod);
  const checkedCount = ids.filter(id => props.checkedKeys.includes(id)).length;
  return checkedCount > 0 && checkedCount < ids.length;
}

function isResourceChecked(res: ApiCatalogTreeNode) {
  const ids = getAllApiIds(res);
  return ids.length > 0 && ids.every(id => props.checkedKeys.includes(id));
}

function isResourceIndeterminate(res: ApiCatalogTreeNode) {
  const ids = getAllApiIds(res);
  const checkedCount = ids.filter(id => props.checkedKeys.includes(id)).length;
  return checkedCount > 0 && checkedCount < ids.length;
}

function handleApiCheck(id: string, checked: boolean) {
  const next = [...props.checkedKeys];
  if (checked) {
    if (!next.includes(id)) next.push(id);
  } else {
    const idx = next.indexOf(id);
    if (idx > -1) next.splice(idx, 1);
  }
  emit('update:checkedKeys', next);
}

function handleResourceCheck(res: ApiCatalogTreeNode, checked: boolean) {
  const ids = getAllApiIds(res);
  let next = [...props.checkedKeys];
  if (checked) {
    ids.forEach(id => {
      if (!next.includes(id)) next.push(id);
    });
  } else {
    next = next.filter(id => !ids.includes(id));
  }
  emit('update:checkedKeys', next);
}

function handleModuleCheck(mod: ApiCatalogTreeNode, checked: boolean) {
  const ids = getAllApiIds(mod);
  let next = [...props.checkedKeys];
  if (checked) {
    ids.forEach(id => {
      if (!next.includes(id)) next.push(id);
    });
  } else {
    next = next.filter(id => !ids.includes(id));
  }
  emit('update:checkedKeys', next);
}

// --- 辅助函数 ---

function getApiNodes(children?: ApiCatalogTreeNode[]) {
  return (children || []).filter(n => n.type === 'API');
}

function handleApiClick(resource: ApiCatalogTreeNode, api: ApiCatalogTreeNode) {
  if (props.checkable) {
    handleApiCheck(api.id, !props.checkedKeys.includes(api.id));
  }
  emit('api-click', resource, api);
}

function getApiLabel(api: ApiCatalogTreeNode) {
  return props.apiLabelFormatter ? props.apiLabelFormatter(api) : (api.label || api.apiUrl || '未命名接口');
}

function getApiTitle(api: ApiCatalogTreeNode) {
  return `${api.httpMethod || ''} ${api.apiUrl || ''}\n${api.description || ''}`;
}

function getMethodColor(method?: string) {
  return getMethodTagColor(method);
}
</script>

<style scoped>
.api-catalog-tree {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  /* 滚动由父容器 .col-list 控制 */
}

.tree-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.item-main {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-width: 0;
}

/* 切换按钮（+/-）样式 */
.item-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  font-size: 10px;
  color: #86909c;
  background: #fff;
  transition: all 0.2s ease;
  flex-shrink: 0;
  z-index: 2;
}

.tree-item:hover .item-toggle {
  border-color: #165dff;
  color: #165dff;
}

/* 模块行样式 */
.tree-item-module {
  font-weight: 600;
  padding-left: 16px;
}

.module-children {
  position: relative;
  margin-left: 25px; /* 对齐父级展开按钮中心 */
}

/* 垂直引导线：模块 -> 业务分组 */
.module-children::before {
  content: "";
  position: absolute;
  left: 0;
  top: -8px;
  bottom: 0;
  width: 1px;
  background: #e5e6eb;
}

/* 业务分组行样式 */
.tree-item-resource {
  padding-left: 16px;
}

/* 水平引导线：指向业务分组 */
.tree-item-resource::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  width: 12px;
  height: 1px;
  background: #e5e6eb;
}

.resource-children {
  position: relative;
  margin-left: 25px;
}

/* 垂直引导线：业务分组 -> 接口 */
.resource-children::before {
  content: "";
  position: absolute;
  left: 0;
  top: -8px;
  bottom: 0;
  width: 1px;
  background: #e5e6eb;
}

/* 接口行样式 */
.tree-item-api {
  padding-left: 16px;
  border-left: 3px solid transparent;
  border-radius: 0 8px 8px 0;
  min-height: 40px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 水平引导线：指向接口 */
.tree-item-api::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  width: 12px;
  height: 1px;
  background: #e5e6eb;
}

.tree-item-api:hover {
  background: #f5f7fa;
  border-left-color: #f0f2f5;
  transform: translateX(4px);
}

.tree-item-api.active {
  background: linear-gradient(90deg, #e8f3ff 0%, rgba(232, 243, 255, 0.2) 100%);
  border-left-color: #165dff;
  box-shadow: 0 2px 8px rgba(22, 93, 255, 0.08);
}

/**
 * 勾选状态样式
 * 使用主题色的浅色背景，让选中的接口更明显
 */
.tree-item-api.is-checked {
  background: linear-gradient(90deg, 
    var(--color-primary-light-1, rgba(22, 93, 255, 0.1)) 0%, 
    rgba(255, 255, 255, 0.5) 100%
  );
  border-left-color: var(--color-primary, #165dff);
  box-shadow: 0 2px 8px var(--bml-shadow, rgba(22, 93, 255, 0.08));
}

.tree-item-api.is-checked:hover {
  background: linear-gradient(90deg, 
    var(--color-primary-light-2, rgba(22, 93, 255, 0.15)) 0%, 
    rgba(255, 255, 255, 0.3) 100%
  );
  border-left-color: var(--color-primary, #165dff);
  box-shadow: 0 3px 10px var(--bml-shadow, rgba(22, 93, 255, 0.12));
}

.tree-item-api.is-checked .api-friendly {
  color: var(--color-primary, #165dff);
  font-weight: 600;
}

.tree-item-api.is-checked .api-path {
  color: var(--color-primary-dark-1, #0e42d2);
}

.tree-item-api.active::before,
.tree-item-api.is-checked::before {
  background: var(--color-primary, #165dff);
}

.item-label {
  flex: 1;
  font-size: 13px;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta {
  font-size: 11px;
  color: #86909c;
  flex-shrink: 0;
}

/* 接口完整信息容器 */
.api-full-info {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  padding-left: 15px; /* 为引导线留出空间 */
}

/* 请求方法标签 */
.api-method-tag {
  flex-shrink: 0;
  font-weight: 600;
  font-size: 11px;
  min-width: 48px;
  text-align: center;
}

/* 接口路径 */
.api-path {
  flex-shrink: 0;
  font-size: 12px;
  color: #86909c;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-weight: 500;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 分隔符 */
.api-name-separator {
  flex-shrink: 0;
  color: #c9cdd4;
  font-weight: 300;
}

/* 接口业务名称 */
.api-friendly {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: #4e5969;
  font-weight: 500;
  transition: color 0.25s ease;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-left: 0; /* 移除之前的 padding */
}

.tree-item-api:hover .api-friendly {
  color: #1d2129;
}

.tree-item-api.active .api-friendly {
  color: #165dff;
  font-weight: 600;
}

.tree-item-api:hover .api-path {
  color: #4e5969;
}

.api-node-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.api-status-tag {
  opacity: 0.8;
}

/**
 * 悬浮指示器 */
.api-item-hover-indicator {
  opacity: 0;
  font-size: 12px;
  color: #165dff;
  transition: all 0.25s ease;
  transform: translateX(-4px);
}

.tree-item-api:hover .api-item-hover-indicator {
  opacity: 1;
  transform: translateX(0);
}

.tree-item-api.active .api-item-hover-indicator {
  display: none;
}

/**
 * Checkbox 样式优化
 * 确保复选框与主题色一致
 */
.tree-item :deep(.arco-checkbox-checked .arco-checkbox-icon) {
  background-color: var(--color-primary, #165dff) !important;
  border-color: var(--color-primary, #165dff) !important;
}

.tree-item :deep(.arco-checkbox:hover .arco-checkbox-icon) {
  border-color: var(--color-primary, #165dff) !important;
}

.tree-item :deep(.arco-checkbox-indeterminate .arco-checkbox-icon) {
  background-color: var(--color-primary, #165dff) !important;
  border-color: var(--color-primary, #165dff) !important;
}

.tree-item :deep(.arco-checkbox-icon) {
  transition: all 0.2s ease;
}

.tree-item :deep(.arco-checkbox-checked:hover .arco-checkbox-icon) {
  background-color: var(--color-primary-light-4, #4080ff) !important;
  border-color: var(--color-primary-light-4, #4080ff) !important;
}
</style>
