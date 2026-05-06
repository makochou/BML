<template>
  <div class="api-list-page">
    <!-- 筛选与操作栏（包含统计信息） -->
    <div class="api-list-toolbar">
      <div class="toolbar-left">
        <!-- 搜索框 - 用于搜索接口名称、路径或描述 -->
        <a-input-search
          v-model="filterKeyword"
          placeholder="搜索接口..."
          allow-clear
          size="medium"
          style="width: 240px"
          @search="loadTree"
          @press-enter="loadTree"
        >
          <template #prefix>
            <icon-search />
          </template>
        </a-input-search>
        
        <!-- HTTP 方法筛选下拉框 -->
        <a-select
          v-model="filterMethod"
          placeholder="方法"
          allow-clear
          size="medium"
          style="width: 100px"
          :options="methodOptions"
          @change="loadTree"
        >
          <template #prefix>
            <icon-filter />
          </template>
        </a-select>
        
        <!-- 刷新数据按钮 - 重新加载接口列表 -->
        <a-button type="primary" size="medium" :loading="loading" @click="loadTree">
          <template #icon><icon-refresh /></template>
          刷新
        </a-button>
        
        <!-- 同步接口按钮 - 从 OpenAPI 注册中心同步接口 -->
        <a-button size="medium" :loading="syncingRegistry" @click="handleSyncRegistry">
          <template #icon><icon-sync /></template>
          同步
        </a-button>
      </div>
      
      <!-- 统计信息区域 - 紧凑显示在右侧 -->
      <div class="toolbar-stats">
        <!-- 业务模块统计 -->
        <div class="stat-item stat-item-modules">
          <icon-apps class="stat-icon" />
          <div class="stat-content">
            <span class="stat-value">{{ moduleCount }}</span>
            <span class="stat-label">模块</span>
          </div>
        </div>
        
        <!-- 业务分组统计 -->
        <div class="stat-item stat-item-resources">
          <icon-folder class="stat-icon" />
          <div class="stat-content">
            <span class="stat-value">{{ resourceCount }}</span>
            <span class="stat-label">分组</span>
          </div>
        </div>
        
        <!-- API 接口统计 -->
        <div class="stat-item stat-item-apis">
          <icon-code-block class="stat-icon" />
          <div class="stat-content">
            <span class="stat-value">{{ apiCount }}</span>
            <span class="stat-label">接口</span>
          </div>
        </div>
        
        <!-- 已打开标签统计 -->
        <div class="stat-item stat-item-tabs">
          <icon-check-circle class="stat-icon" />
          <div class="stat-content">
            <span class="stat-value">{{ openTabs.length }}</span>
            <span class="stat-label">标签</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 两栏：左侧接口目录树（模块 → 业务分组 → 接口）| 右侧调试面板 -->
    <div class="api-list-body">
      <a-spin :loading="loading" class="api-list-spin">
        <!-- loading 时显示骨架屏，避免空白 -->
        <div v-if="loading && !catalogTree.length" class="api-list-loading-placeholder">
          <a-skeleton :animation="true">
            <a-skeleton-line :rows="8" />
          </a-skeleton>
        </div>
        <div class="hierarchy-layout" v-else-if="catalogTree.length">
          <!-- 左栏：接口目录（三级树，模块 → 业务分组 → 接口，整合原中间栏） -->
          <div class="hierarchy-col hierarchy-col-catalog">
            <div class="col-title">
              <span class="title-text">资产全量目录</span>
              <div class="title-actions">
                <a-button type="text" size="mini" @click="toggleAll" class="action-btn">
                  <template #icon>
                    <component :is="isAllExpanded ? IconShrink : IconExpand" />
                  </template>
                  {{ isAllExpanded ? '收起全部' : '展开全部' }}
                </a-button>
              </div>
            </div>
            <div class="col-list">
              <ApiCatalogTree
                :nodes="catalogTree"
                :selected-api-id="selectedApi?.id"
                v-model:expanded-module-ids="expandedModuleIds"
                v-model:expanded-resource-ids="expandedResourceIds"
                :api-label-formatter="getApiDisplayName"
                @api-click="selectApi"
              />
            </div>
          </div>

          <!-- 右栏：API 调试面板（占满剩余宽度，便于调试），已重构为标签页模式 -->
          <div class="hierarchy-col hierarchy-col-debug">
            <!-- 已移除右上角原有的固定标题栏“调试面板”，以释放垂直空间 -->
            
            <div class="col-debug-content">
              <template v-if="openTabs.length > 0">
                <div class="api-debug-tabs-wrapper">
                  <!-- 补充左侧：向左选择标签页 独立按钮 -->
                  <div 
                    class="nav-btn-custom nav-btn-left" 
                    :class="{ 'is-disabled': openTabs.length <= 1 }"
                    @click="openTabs.length > 1 && selectPrevTab()"
                  >
                    <icon-left />
                  </div>
                  
                  <!-- 补充右侧：向右选择标签页 独立按钮 -->
                  <div 
                    class="nav-btn-custom nav-btn-right" 
                    :class="{ 'is-disabled': openTabs.length <= 1 }"
                    @click="openTabs.length > 1 && selectNextTab()"
                  >
                    <icon-right />
                  </div>

                  <a-tabs 
                    v-model:active-key="activeTabId" 
                    type="card-gutter" 
                    :editable="true" 
                    @delete="handleTabDelete"
                    class="api-debug-tabs"
                  >
                    <a-tab-pane 
                      v-for="tab in openTabs" 
                      :key="tab.id" 
                    >
                      <!-- 自定义标签页标题：包裹右键菜单逻辑 -->
                      <template #title>
                        <a-dropdown trigger="contextMenu" @select="(val: any) => handleTabContextMenu(val, tab.id)" position="bl">
                          <div class="custom-tab-title">
                            {{ tab.title }}
                          </div>
                          <template #content>
                            <a-doption value="current">
                              <template #icon><icon-close /></template>
                              关闭当前标签
                            </a-doption>
                            <a-doption value="others" :disabled="openTabs.length <= 1">
                              <template #icon><icon-minus-circle /></template>
                              关闭其他标签页
                            </a-doption>
                            <!-- 分割线 -->
                            <div class="bml-dropdown-divider"></div>
                            <a-doption value="all" class="bml-dropdown-item-danger">
                              <template #icon><icon-delete /></template>
                              关闭所有标签页
                            </a-doption>
                          </template>
                        </a-dropdown>
                      </template>
                      <!-- 将每个 Tab 内部再次封装为一个完整的调试面板实例 -->
                      <ApiDebugPanel :selected-api="tab.api" :display-name="tab.displayName" />
                    </a-tab-pane>
                  </a-tabs>
                </div>
              </template>
              <div v-else class="empty-debug-placeholder">
                <a-empty
                  description="请在左侧接口目录中点击接口打开调试标签页"
                >
                  <template #image>
                    <div class="empty-icon-wrapper">
                      <icon-send />
                    </div>
                  </template>
                </a-empty>
              </div>
            </div>
          </div>
        </div>
        <a-empty
          v-else-if="!loading"
          description="暂无资产数据，请先通过「资产全量发现」同步元数据"
          class="empty-block"
        />
      </a-spin>
    </div>
  </div>
</template>

<script lang="ts" setup>
/**
 * 资产目录页面
 *
 * 重要说明：
 *   defineOptions({ name: 'ApiList' }) 是 keep-alive 缓存的关键。
 *   组件 name 必须与路由配置中的 name 字段保持一致，
 *   否则 <keep-alive :include="cachedViews"> 无法匹配到该组件，
 *   导致切换标签页后页面内容被销毁、重新加载。
 */
defineOptions({ name: 'ApiList' });

import { ref, computed, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconRefresh, IconRight, IconExpand, IconShrink, IconSend, IconLeft, IconSync, IconApps, IconFolder, IconCodeBlock, IconCheckCircle, IconSearch, IconFilter, IconClose, IconMinusCircle, IconDelete } from '@arco-design/web-vue/es/icon';
import { fetchApiListTree } from '../../api/apiList';
import { syncOpenApiRegistry } from '../../api/apiAccount';
import type { ApiCatalogTreeNode } from '../../types/apiList';
import { methodOptions } from '../../utils/api-account-governance';
import ApiDebugPanel from '../../components/api-debug/ApiDebugPanel.vue';
import ApiCatalogTree from '../../components/api/ApiCatalogTree.vue';

/**
 * 标签页的数据结构定义
 */
interface ApiTabItem {
  /** 唯一标识，通常为 api.id */
  id: string;
  /** 标签页显示的标题（即接口的业务名称或路径） */
  title: string;
  /** 对应的后端接口节点信息 */
  api: ApiCatalogTreeNode;
  /** 对应的业务分组节点信息 */
  resource: ApiCatalogTreeNode;
  /** 生成的完整友好显示名称 */
  displayName: string;
}

/** 当前打开的标签页列表 */
const openTabs = ref<ApiTabItem[]>([]);

/** 当前激活的标签页 ID */
const activeTabId = ref<string>('');

/**
 * 接口目录显示映射信息。
 * 该结构来自静态文档文件（public/api-catalog-hierarchy.json），用于在前端统一控制：
 * - 顶级菜单名称（如「API 管理」）；
 * - 上级菜单名称（如「API 账号管理」）；
 * - 接口业务名称（如「用户新增」）。
 */
interface ApiDisplayMeta {
  /** 上上级菜单名称，例如「API 管理」 */
  topMenu: string;
  /** 上级菜单名称，例如「API 账号管理」 */
  parentMenu: string;
  /** 接口业务名称，例如「用户新增」 */
  apiName: string;
  /** 可选：完整显示名称，存在时优先生效 */
  displayName?: string;
}

/** 静态文档中维护的接口显示映射表，key 形如：GET /account/{id} */
const apiDisplayMap = ref<Record<string, ApiDisplayMeta>>({});
let apiDisplayMapLoaded = false;

/** 已展开的模块 id 集合，用于在左侧树中展示其下的业务分组 */
const expandedModuleIds = ref<Set<string>>(new Set());

/** 已展开的业务分组 id 集合，用于在左侧树中展示该分组下的接口列表（原中间栏内容） */
const expandedResourceIds = ref<Set<string>>(new Set());



/**
 * 判断是否全部展开
 */
const isAllExpanded = computed(() => {
  if (catalogTree.value.length === 0) return false;
  
  // 检查是否所有模块都已在展开集合中
  for (const mod of catalogTree.value) {
    if (!expandedModuleIds.value.has(mod.id)) return false;
    // 如果模块有子节点，检查是否所有子节点也已展开
    if (mod.children) {
      for (const res of mod.children) {
        if (!expandedResourceIds.value.has(res.id)) return false;
      }
    }
  }
  return true;
});

/**
 * 智能开关：根据当前状态点击展开全部或收起全部
 */
function toggleAll() {
  if (isAllExpanded.value) {
    expandedModuleIds.value = new Set();
    expandedResourceIds.value = new Set();
  } else {
    const modIds = new Set<string>();
    const resIds = new Set<string>();
    catalogTree.value.forEach((mod) => {
      modIds.add(mod.id);
      if (mod.children) {
        mod.children.forEach((res) => {
          resIds.add(res.id);
        });
      }
    });
    expandedModuleIds.value = modIds;
    expandedResourceIds.value = resIds;
  }
}

/**
 * 选中某一接口，处理标签页的打开与激活逻辑
 */
function selectApi(resource: ApiCatalogTreeNode, api: ApiCatalogTreeNode) {
  if (api.type !== 'API') return;
  
  const tabId = api.id;
  const existingTabIndex = openTabs.value.findIndex(tab => tab.id === tabId);
  
  if (existingTabIndex > -1) {
    // 标签页已存在，直接激活
    activeTabId.value = tabId;
  } else {
    // 创建新标签页
    const displayName = getApiDisplayName(api);
    const methodPrefix = api.httpMethod ? `[${api.httpMethod}] ` : '';
    // 标签栏宽度有限，适当截断标题
    const rawTitle = api.label || api.apiUrl || '未命名接口';
    const shortTitle = rawTitle.length > 15 ? rawTitle.substring(0, 15) + '...' : rawTitle;
    
    // 打开新标签
    openTabs.value.push({
      id: tabId,
      title: methodPrefix + shortTitle,
      api: api,
      resource: resource,
      displayName: displayName
    });
    // 切换到新标签
    activeTabId.value = tabId;
  }
}

/**
 * 处理标签页关闭事件
 */
function handleTabDelete(targetKey: string | number) {
  const deletingId = String(targetKey);
  const targetIndex = openTabs.value.findIndex(tab => tab.id === deletingId);
  if (targetIndex === -1) return;

  // 如果删除的是当前激活的标签，需要计算下一个应该激活的标签
  if (activeTabId.value === deletingId) {
    if (openTabs.value.length === 1) {
      // 这是最后一个标签，全部关闭
      activeTabId.value = '';
    } else {
      // 激活前一个标签，如果是第一个则激活后一个
      const nextTab = openTabs.value[targetIndex - 1] || openTabs.value[targetIndex + 1];
      if (nextTab) {
        activeTabId.value = nextTab.id;
      } else {
        activeTabId.value = '';
      }
    }
  }

  // 移出数组
  openTabs.value.splice(targetIndex, 1);
}

/**
 * 关闭所有标签页
 */
function closeAllTabs() {
  openTabs.value = [];
  activeTabId.value = '';
}

/**
 * 关闭当前标签页
 */
function closeCurrentTab(tabId: string) {
  handleTabDelete(tabId);
}

/**
 * 关闭其他标签页，仅保留选中的
 */
function closeOtherTabs(tabId: string) {
  const targetTab = openTabs.value.find(tab => tab.id === tabId);
  if (!targetTab) return;
  openTabs.value = [targetTab];
  activeTabId.value = tabId;
}

/**
 * 统一处理标签页右键菜单的点击动作
 */
function handleTabContextMenu(action: string | number | Record<string, any>, tabId: string) {
  if (action === 'current') {
    closeCurrentTab(tabId);
  } else if (action === 'others') {
    closeOtherTabs(tabId);
  } else if (action === 'all') {
    closeAllTabs();
  }
}

/**
 * 向左选择上一个标签页
 */
function selectPrevTab() {
  if (openTabs.value.length <= 1) return;
  const currentIndex = openTabs.value.findIndex(tab => tab.id === activeTabId.value);
  if (currentIndex <= 0) {
    // 如果是第一个，则循环到最后一个
    const lastTab = openTabs.value[openTabs.value.length - 1];
    if (lastTab) activeTabId.value = lastTab.id;
  } else {
    const prevTab = openTabs.value[currentIndex - 1];
    if (prevTab) activeTabId.value = prevTab.id;
  }
}

/**
 * 向右选择下一个标签页
 */
function selectNextTab() {
  if (openTabs.value.length <= 1) return;
  const currentIndex = openTabs.value.findIndex(tab => tab.id === activeTabId.value);
  if (currentIndex === -1 || currentIndex >= openTabs.value.length - 1) {
    // 如果是最后一个，则循环到第一个
    const firstTab = openTabs.value[0];
    if (firstTab) activeTabId.value = firstTab.id;
  } else {
    const nextTab = openTabs.value[currentIndex + 1];
    if (nextTab) activeTabId.value = nextTab.id;
  }
}

/**
 * 当前激活区域选中的接口（用于左侧菜单的高亮）
 */
const selectedApi = computed(() => {
  const activeTab = openTabs.value.find(tab => tab.id === activeTabId.value);
  return activeTab ? activeTab.api : null;
});

/**
 * 构造用于查找显示映射表的 key，例如：GET /account/{id}
 */
function buildApiDisplayKey(api: ApiCatalogTreeNode): string {
  const method = (api.httpMethod || 'GET').toUpperCase();
  const path = (api.apiUrl || '').trim();
  return `${method} ${path}`;
}

/**
 * 从静态文档加载接口显示映射表。
 * 优先级：
 * 1) 优先尝试读取 CSV 文档（Excel 可直接打开编辑）：/api-catalog-hierarchy.csv；
 * 2) 如 CSV 不存在或解析失败，再尝试 JSON 文档：/api-catalog-hierarchy.json。
 * 两者均失败时不影响页面展示，仅回退到默认文案。
 */
async function loadApiDisplayMap() {
  if (apiDisplayMapLoaded) {
    return;
  }
  apiDisplayMapLoaded = true;

  // 1) 优先尝试 CSV（便于使用 Excel 维护）
  try {
    const csvResp = await fetch('/api-catalog-hierarchy.csv', { cache: 'no-cache' });
    if (csvResp.ok) {
      const text = await csvResp.text();
      const lines = text.split(/\r?\n/).map((line) => line.trim()).filter((line) => line && !line.startsWith('#'));
      if (lines.length > 1) {
        const headerLine = lines[0] || '';
        const header = headerLine.split(',').map((h) => h.trim());
        const colIndex = (name: string) => {
          const idx = header.indexOf(name);
          return idx >= 0 ? idx : -1;
        };
        const methodIdx = colIndex('method');
        const pathIdx = colIndex('path');
        const topMenuIdx = colIndex('topMenu');
        const parentMenuIdx = colIndex('parentMenu');
        const apiNameIdx = colIndex('apiName');
        const displayNameIdx = colIndex('displayName');

        // 若缺少关键列（method/path），则直接放弃 CSV，改由 JSON 或默认策略处理
        if (pathIdx < 0) {
          throw new Error('csv header missing required column: path');
        }

        const map: Record<string, ApiDisplayMeta> = {};
        for (let i = 1; i < lines.length; i += 1) {
          const row = lines[i];
          if (!row) continue;
          const cells = row.split(',').map((cell) => cell.trim());
          const method = methodIdx >= 0 ? (cells[methodIdx] || 'GET').toUpperCase() : 'GET';
          const path = pathIdx >= 0 ? (cells[pathIdx] || '') : '';
          if (!path) continue;
          const key = `${method} ${path}`;
          map[key] = {
            topMenu: topMenuIdx >= 0 ? (cells[topMenuIdx] || '') : '',
            parentMenu: parentMenuIdx >= 0 ? (cells[parentMenuIdx] || '') : '',
            apiName: apiNameIdx >= 0 ? (cells[apiNameIdx] || '') : '',
            displayName: displayNameIdx >= 0 ? (cells[displayNameIdx] || '') : ''
          };
        }
        apiDisplayMap.value = map;
        return;
      }
    }
  } catch {
    // 忽略 CSV 解析错误，继续尝试 JSON
  }

  // 2) 回退到 JSON 文档
  try {
    const resp = await fetch('/api-catalog-hierarchy.json', { cache: 'no-cache' });
    if (!resp.ok) {
      return;
    }
    const data = (await resp.json()) as Record<string, ApiDisplayMeta>;
    if (data && typeof data === 'object') {
      apiDisplayMap.value = data;
    }
  } catch {
    // 静默降级：使用默认名称策略
  }
}

/**
 * 根据静态文档和后端树节点，生成更加通俗易懂的接口显示名称。
 * 优先级：
 * 1) 静态文档中为该接口配置的 displayName / (topMenu + parentMenu + apiName)；
 * 2) 业务分组名称 + 接口 label / description / apiUrl。
 */
function getApiDisplayName(api: ApiCatalogTreeNode): string {
  const key = buildApiDisplayKey(api);
  const meta = apiDisplayMap.value[key];
  if (meta) {
  if (meta) {
    // 优先返回显式配置的 displayName 或 apiName（CSV 中的具体接口名称）
    const specificName = (meta.apiName || meta.displayName || '').trim();
    if (specificName) {
      return specificName;
    }
  }
  }

  const apiLabel = (api.label || '').trim();
  const apiDescription = (api.description || '').trim();
  const apiPath = (api.apiUrl || '').trim();

  // 直接返回接口自身的名称或描述，不再拼接上层业务名称前缀
  return apiLabel || apiDescription || apiPath || '未命名接口';
}

const loading = ref(false);
const catalogTree = ref<ApiCatalogTreeNode[]>([]);
const filterKeyword = ref('');
const filterMethod = ref<string | undefined>(undefined);

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

const syncingRegistry = ref(false);

async function handleSyncRegistry() {
  syncingRegistry.value = true;
  try {
    const { data } = await syncOpenApiRegistry();
    Message.success(`项目接口目录同步完成，本次共纳管 ${data.totalDiscovered} 个接口`);
    await loadTree();
  } catch (e) {
    Message.error((e as Error)?.message || '同步接口目录失败');
  } finally {
    syncingRegistry.value = false;
  }
}

async function loadTree() {
  loading.value = true;
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

onMounted(() => {
  loadTree();
  loadApiDisplayMap();
});
</script>

<style scoped>
/**
 * 资产目录页面样式
 * 设计理念：简洁、现代、与系统其他页面保持统一的浅色风格
 * 采用卡片式布局，清晰的层次结构，优雅的交互效果
 * 统计信息紧凑显示在工具栏右侧，节省垂直空间
 */
.api-list-page {
  display: flex;
  flex-direction: column;
  flex: 1;
  height: 100%;
  min-height: 0;
  padding: 16px 20px;
  /* 浅色背景，与系统其他页面保持一致 */
  background: #f0f2f5;
  position: relative;
  overflow: hidden;
}

/* ==================== 工具栏区域 ==================== */
.api-list-toolbar {
  flex-shrink: 0;
  display: flex;
  flex-wrap: nowrap; /* 不换行，强制在同一行显示 */
  align-items: center;
  justify-content: space-between;
  gap: 10px; /* 减小间距，从 16px 改为 10px */
  padding: 12px 16px; /* 减小内边距，从 16px 20px 改为 12px 16px */
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e5e6eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 16px;
  position: relative;
  z-index: 1;
  overflow: hidden; /* 隐藏滚动条 */
}

/* 工具栏左侧区域 - 包含所有筛选和操作按钮 */
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px; /* 减小间距，从 12px 改为 8px */
  flex-wrap: nowrap; /* 不换行 */
  flex-shrink: 0; /* 不收缩 */
}

/* 工具栏右侧统计区域 - 紧凑显示统计信息 */
.toolbar-stats {
  display: flex;
  align-items: center;
  gap: 8px; /* 减小间距，从 12px 改为 8px */
  flex-wrap: nowrap; /* 不换行 */
  flex-shrink: 0; /* 不收缩 */
  margin-left: auto; /* 自动推到右侧 */
}

/* 统计项 - 超紧凑的横向布局，纯展示无交互 */
.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #f7f8fa;
  border-radius: 6px;
  border: 1px solid #e5e6eb;
  white-space: nowrap; /* 不换行 */
  flex-shrink: 0; /* 不收缩 */
  cursor: default; /* 默认鼠标样式，表示不可点击 */
}

/* 统计图标 - 固定样式，无动画 */
.stat-icon {
  font-size: 16px;
  color: var(--stat-color, #165dff);
  flex-shrink: 0; /* 不收缩 */
}

/* 统计内容 */
.stat-content {
  display: flex;
  align-items: baseline;
  gap: 2px;
  white-space: nowrap; /* 不换行 */
}

/* 统计数值 */
.stat-value {
  font-size: 16px;
  font-weight: 700;
  line-height: 1;
  color: #1d2129;
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
}

/* 统计标签 */
.stat-label {
  font-size: 11px;
  color: #86909c;
  font-weight: 500;
}

/* 不同统计项的主题色 */
.stat-item-modules {
  --stat-color: #165dff;
  --stat-shadow: rgba(22, 93, 255, 0.15);
}

.stat-item-resources {
  --stat-color: #722ed1;
  --stat-shadow: rgba(114, 46, 209, 0.15);
}

.stat-item-apis {
  --stat-color: #00b42a;
  --stat-shadow: rgba(0, 180, 42, 0.15);
}

.stat-item-tabs {
  --stat-color: #ff7d00;
  --stat-shadow: rgba(255, 125, 0, 0.15);
}

/* 搜索框样式优化 - 紧凑尺寸 */
.toolbar-left :deep(.arco-input-wrapper) {
  background: #f7f8fa;
  border: 1px solid #e5e6eb;
  border-radius: 6px; /* 减小圆角，从 8px 改为 6px */
  transition: all 0.3s;
  height: 32px; /* 固定高度 */
}

.toolbar-left :deep(.arco-input-wrapper:hover) {
  border-color: #c9cdd4;
  background: #fff;
}

.toolbar-left :deep(.arco-input-wrapper.arco-input-focus) {
  border-color: #165dff;
  background: #fff;
  box-shadow: 0 0 0 2px rgba(22, 93, 255, 0.1); /* 减小外发光 */
}

.toolbar-left :deep(.arco-input) {
  color: #1d2129;
  font-size: 13px; /* 减小字号，从 14px 改为 13px */
  padding: 0 8px; /* 减小内边距 */
}

.toolbar-left :deep(.arco-input::placeholder) {
  color: #86909c;
  font-size: 13px; /* 减小字号 */
}

.toolbar-left :deep(.arco-input-prefix) {
  color: #4e5969;
  font-size: 14px; /* 减小图标尺寸 */
  margin-right: 4px; /* 减小间距 */
}

/* 下拉框样式优化 - 紧凑尺寸 */
.toolbar-left :deep(.arco-select-view) {
  background: #f7f8fa;
  border: 1px solid #e5e6eb;
  border-radius: 6px; /* 减小圆角 */
  color: #1d2129;
  transition: all 0.3s;
  height: 32px; /* 固定高度 */
  font-size: 13px; /* 减小字号 */
  padding: 0 8px; /* 减小内边距 */
}

.toolbar-left :deep(.arco-select-view:hover) {
  border-color: #c9cdd4;
  background: #fff;
}

.toolbar-left :deep(.arco-select-view-focus) {
  border-color: #165dff;
  background: #fff;
  box-shadow: 0 0 0 2px rgba(22, 93, 255, 0.1); /* 减小外发光 */
}

/* 主按钮样式 - 紧凑尺寸，简洁效果 */
.toolbar-left :deep(.arco-btn-primary) {
  background: linear-gradient(135deg, #165dff 0%, #4a8dff 100%);
  border: none;
  border-radius: 6px;
  padding: 0 12px;
  height: 32px;
  font-weight: 500;
  font-size: 13px;
  color: #fff;
  box-shadow: 0 2px 8px rgba(22, 93, 255, 0.3);
  transition: all 0.2s; /* 缩短过渡时间 */
}

.toolbar-left :deep(.arco-btn-primary:hover) {
  box-shadow: 0 4px 12px rgba(22, 93, 255, 0.4); /* 仅加深阴影，不上浮 */
}

/* 按钮图标尺寸 */
.toolbar-left :deep(.arco-btn .arco-icon) {
  font-size: 14px;
  margin-right: 4px;
}

/* 次按钮样式 - 紧凑尺寸，简洁效果 */
.toolbar-left :deep(.arco-btn:not(.arco-btn-primary)) {
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  padding: 0 12px;
  height: 32px;
  font-weight: 500;
  font-size: 13px;
  color: #4e5969;
  transition: all 0.2s; /* 缩短过渡时间 */
}

.toolbar-left :deep(.arco-btn:not(.arco-btn-primary):hover) {
  border-color: #165dff;
  color: #165dff;
  background: rgba(22, 93, 255, 0.05);
  box-shadow: 0 2px 8px rgba(22, 93, 255, 0.15); /* 仅加深阴影，不上浮 */
}

/* ==================== 主体内容区域 ==================== */
/* 卡片区域：占据剩余高度，内容超出时在列内上下滚动 */
.api-list-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e5e6eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 0;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

/* 滚动条样式 - 简洁现代 */
.api-list-page :deep(::-webkit-scrollbar) {
  width: 6px;
  height: 6px;
}

.api-list-page :deep(::-webkit-scrollbar-track) {
  background: #f7f8fa;
  border-radius: 3px;
}

.api-list-page :deep(::-webkit-scrollbar-thumb) {
  background: #c9cdd4;
  border-radius: 3px;
  transition: background 0.3s;
}

.api-list-page :deep(::-webkit-scrollbar-thumb:hover) {
  background: #86909c;
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

/* 两栏布局：左侧接口目录树 | 右侧调试面板（调试栏占满剩余空间） */
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

/* 左栏：接口目录（模块 → 业务分组 → 接口 三级树），固定宽度以让右侧调试有更多空间 */
.hierarchy-col-catalog {
  width: 360px;
  flex-shrink: 0;
  background: #f7f8fa;
  border-right: 1px solid #e5e6eb;
}

/* 右栏：API 调试面板，占满剩余宽度 */
.hierarchy-col-debug {
  flex: 1;
  min-width: 520px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fff;
}

.col-debug-content {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #fdfdfe;
}

/* 标签页与外层包装样式 */
.api-debug-tabs-wrapper {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  position: relative; /* 核心：让左右独立按钮绝对定位 */
}

/* 独立的左右导航按钮 (高级设计外观) */
.nav-btn-custom {
  position: absolute;
  top: 13px; /* 与标签页头部视觉高度对齐 */
  z-index: 10;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #4e5969;
  background-color: #ffffff;
  border: 1px solid #e5e6eb;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.nav-btn-left {
  left: 12px;
}

.nav-btn-right {
  right: 12px;
}

.nav-btn-custom:not(.is-disabled):hover {
  color: #165dff;
  border-color: #165dff;
  box-shadow: 0 4px 12px rgba(22, 93, 255, 0.15);
  transform: translateY(-1px);
}

.nav-btn-custom.is-disabled {
  color: #c9cdd4;
  background-color: #f7f8fa;
  border-color: #f2f3f5;
  box-shadow: none;
  cursor: not-allowed;
  transform: none;
}

.api-debug-tabs {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  position: relative; /* 确保层级不会盖住按钮 */
}

/* 隐藏原本丑陋的 Arco 内置前后箭头按钮 */
.api-debug-tabs :deep(.arco-tabs-nav-button) {
  display: none !important;
}

.api-debug-tabs :deep(.arco-tabs-nav) {
  padding: 12px 48px 0 48px; /* 调大左右侧间距 (48px)，给两侧独立的 hover button 腾出空间 */
  background: #f7f8fa;
  border-bottom: 1px solid #e5e6eb;
}

/* 右键危险按钮样式已由全局 .bml-dropdown-item-danger 统一管理（business-system.css） */

/* 补充在标签栏右侧增加的“向左向右操作栏”样式 */
.tabs-nav-actions {
  display: flex;
  align-items: center;
  margin-left: 12px;
  bottom: 8px; /* 对齐 tab 底边 */
  position: relative;
}

.tabs-nav-actions :deep(.arco-btn) {
  padding: 0 8px;
  background-color: transparent;
  color: #86909c;
  border-color: #e5e6eb;
}

.tabs-nav-actions :deep(.arco-btn:not(.arco-btn-disabled):hover) {
  color: #1d2129;
  background-color: #f0f2f5;
  border-color: #e5e6eb;
}

.api-debug-tabs :deep(.arco-tabs-nav-tab) {
  padding-bottom: 0;
}

.api-debug-tabs :deep(.arco-tabs-tab) {
  background: transparent;
  border: 1px solid transparent;
  border-bottom: none;
  border-radius: 8px 8px 0 0;
  margin-right: 4px;
  padding: 6px 16px;
  color: #4e5969;
  transition: all 0.2s;
  font-size: 13px;
}

.api-debug-tabs :deep(.arco-tabs-tab:hover) {
  background: #f0f2f5;
  color: #1d2129;
}

.api-debug-tabs :deep(.arco-tabs-tab-active) {
  background: #fff;
  border-color: #e5e6eb;
  color: #165dff;
  font-weight: 600;
  position: relative;
}

.api-debug-tabs :deep(.arco-tabs-tab-active)::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: #fff; /* 覆盖底部边框 */
  z-index: 1;
}

.api-debug-tabs :deep(.arco-tabs-content) {
  flex: 1;
  min-height: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.api-debug-tabs :deep(.arco-tabs-pane) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* 空状态占位符 */
.empty-debug-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

.empty-icon-wrapper {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #165dff 0%, #4a8dff 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  margin: 0 auto 16px;
  box-shadow: 0 4px 16px rgba(22, 93, 255, 0.3);
}

.col-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e6eb;
  position: relative;
}

.col-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  background: linear-gradient(135deg, #165dff 0%, #4a8dff 100%);
  border-radius: 0 2px 2px 0;
}

.title-text {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  margin-left: 12px;
}

.title-actions {
  display: flex;
  align-items: center;
}

.title-actions :deep(.arco-divider-vertical) {
  margin: 0 4px;
  background-color: #e5e6eb;
}

.action-btn {
  padding: 0 8px;
  font-size: 12px;
  color: #4e5969;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background-color: #f0f2f5;
  color: #165dff;
}

/* 列内列表 */
.col-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto; /* 允许垂直滚动 */
  overflow-x: hidden; /* 隐藏水平滚动 */
  display: flex;
  flex-direction: column;
}

.empty-block {
  padding: 60px 0;
}

/* loading 骨架屏占位 */
.api-list-loading-placeholder {
  flex: 1;
  padding: 24px;
  background: #fff;
  border-radius: 12px;
}
</style>
