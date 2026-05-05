# 分页和统计功能 - 使用示例

## 📋 目录

1. [基础使用](#基础使用)
2. [统计卡片](#统计卡片)
3. [分页配置](#分页配置)
4. [自定义样式](#自定义样式)

---

## 🚀 基础使用

### 1. 导入样式

在 `ApiAccountManage.vue` 中添加：

```vue
<style scoped lang="scss">
/* 查询面板样式 */
@import './ApiAccountManageEnhanced.scss';

/* 表格列表样式 */
@import './ApiAccountTableEnhanced.scss';

/* 分页和统计样式 */
@import './ApiAccountPaginationEnhanced.scss';
</style>
```

### 2. 模板结构

```vue
<template>
  <div class="api-account-page">
    <!-- 查询面板 -->
    <GovernanceCompactQueryPanel>
      <!-- ... -->
    </GovernanceCompactQueryPanel>

    <!-- 表格列表 -->
    <GovernanceListStage class="table-shell">
      <a-table class="account-table" :data="accountList">
        <!-- ... -->
      </a-table>
      
      <!-- 分页和统计区域 -->
      <template #footer>
        <div class="table-shell__footer">
          <!-- 统计信息 -->
          <div class="pagination-stats">
            <div class="pagination-stat-card">
              <div class="pagination-stat-icon">
                <icon-file />
              </div>
              <div class="pagination-stat-content">
                <span class="pagination-stat-label">总记录数</span>
                <span class="pagination-stat-value">
                  {{ totalCount }}
                  <span class="stat-unit">条</span>
                </span>
              </div>
            </div>
            
            <div class="pagination-stat-card">
              <div class="pagination-stat-icon stat-icon--success">
                <icon-check-circle />
              </div>
              <div class="pagination-stat-content">
                <span class="pagination-stat-label">已启用</span>
                <span class="pagination-stat-value">
                  {{ enabledCount }}
                  <span class="stat-unit">条</span>
                </span>
              </div>
            </div>
            
            <div class="pagination-stat-card">
              <div class="pagination-stat-icon stat-icon--info">
                <icon-info-circle />
              </div>
              <div class="pagination-stat-content">
                <span class="pagination-stat-label">当前页</span>
                <span class="pagination-stat-value">
                  {{ currentPageCount }}
                  <span class="stat-unit">条</span>
                </span>
              </div>
            </div>
          </div>
          
          <!-- 分页器 -->
          <a-pagination
            v-model:current="pagination.current"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[10, 20, 50, 100]"
            :show-total="true"
            :show-jumper="true"
            :show-page-size="true"
            @change="handlePageChange"
            @page-size-change="handlePageSizeChange"
          />
        </div>
      </template>
    </GovernanceListStage>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue';
import { IconFile, IconCheckCircle, IconInfoCircle } from '@arco-design/web-vue/es/icon';

// 分页配置
const pagination = ref({
  current: 1,
  pageSize: 20, // 默认每页20条
  total: 0
});

// 账号列表数据
const accountList = ref([]);

// 统计数据
const totalCount = computed(() => pagination.value.total);
const enabledCount = computed(() => {
  return accountList.value.filter(item => item.status === 1).length;
});
const currentPageCount = computed(() => accountList.value.length);

// 页码变化处理
const handlePageChange = (page: number) => {
  pagination.value.current = page;
  loadData();
};

// 每页条数变化处理
const handlePageSizeChange = (pageSize: number) => {
  pagination.value.pageSize = pageSize;
  pagination.value.current = 1; // 重置到第一页
  loadData();
};

// 加载数据
const loadData = async () => {
  // 调用API加载数据
  // ...
};
</script>
```

---

## 📊 统计卡片

### 基础统计卡片

```vue
<div class="pagination-stat-card">
  <div class="pagination-stat-icon">
    <icon-file />
  </div>
  <div class="pagination-stat-content">
    <span class="pagination-stat-label">总记录数</span>
    <span class="pagination-stat-value">
      {{ totalCount }}
      <span class="stat-unit">条</span>
    </span>
  </div>
</div>
```

### 成功状态卡片（绿色）

```vue
<div class="pagination-stat-card">
  <div class="pagination-stat-icon stat-icon--success">
    <icon-check-circle />
  </div>
  <div class="pagination-stat-content">
    <span class="pagination-stat-label">已启用</span>
    <span class="pagination-stat-value">
      {{ enabledCount }}
      <span class="stat-unit">条</span>
    </span>
  </div>
</div>
```

### 信息状态卡片（蓝色）

```vue
<div class="pagination-stat-card">
  <div class="pagination-stat-icon stat-icon--info">
    <icon-info-circle />
  </div>
  <div class="pagination-stat-content">
    <span class="pagination-stat-label">当前页</span>
    <span class="pagination-stat-value">
      {{ currentPageCount }}
      <span class="stat-unit">条</span>
    </span>
  </div>
</div>
```

### 自定义统计卡片

```vue
<div class="pagination-stat-card">
  <div class="pagination-stat-icon" style="background: linear-gradient(135deg, rgba(245, 158, 11, 0.15), rgba(217, 119, 6, 0.12)); color: #f59e0b;">
    <icon-exclamation-circle />
  </div>
  <div class="pagination-stat-content">
    <span class="pagination-stat-label">待审核</span>
    <span class="pagination-stat-value">
      {{ pendingCount }}
      <span class="stat-unit">条</span>
    </span>
  </div>
</div>
```

---

## ⚙️ 分页配置

### 默认配置（推荐）

```typescript
const pagination = ref({
  current: 1,
  pageSize: 20, // 每页20条
  total: 0
});

const paginationConfig = computed(() => ({
  current: pagination.value.current,
  pageSize: pagination.value.pageSize,
  total: pagination.value.total,
  pageSize Options: [10, 20, 50, 100], // 可选的每页条数
  showTotal: true, // 显示总数
  showJumper: true, // 显示跳转
  showPageSize: true, // 显示每页条数选择器
  simple: false // 非简洁模式
}));
```

### 大数据量配置

```typescript
const pagination = ref({
  current: 1,
  pageSize: 50, // 每页50条
  total: 0
});

const paginationConfig = computed(() => ({
  current: pagination.value.current,
  pageSize: pagination.value.pageSize,
  total: pagination.value.total,
  pageSizeOptions: [20, 50, 100, 200], // 更大的选项
  showTotal: true,
  showJumper: true,
  showPageSize: true
}));
```

### 简洁模式配置

```typescript
const paginationConfig = computed(() => ({
  current: pagination.value.current,
  pageSize: pagination.value.pageSize,
  total: pagination.value.total,
  simple: true, // 简洁模式
  showTotal: false,
  showJumper: false,
  showPageSize: false
}));
```

---

## 🎨 自定义样式

### 修改统计卡片颜色

```scss
/* 自定义紫色统计卡片 */
.api-account-page .pagination-stat-icon.stat-icon--purple {
  background: linear-gradient(135deg, 
    rgba(168, 85, 247, 0.15) 0%, 
    rgba(147, 51, 234, 0.12) 100%
  );
  color: #a855f7;
  box-shadow: 0 0 0 1px rgba(168, 85, 247, 0.2) inset;
}

/* 自定义橙色统计卡片 */
.api-account-page .pagination-stat-icon.stat-icon--warning {
  background: linear-gradient(135deg, 
    rgba(245, 158, 11, 0.15) 0%, 
    rgba(217, 119, 6, 0.12) 100%
  );
  color: #f59e0b;
  box-shadow: 0 0 0 1px rgba(245, 158, 11, 0.2) inset;
}
```

### 调整分页器尺寸

```scss
/* 大尺寸分页器 */
.api-account-page.large-pagination {
  .table-shell__footer :deep(.arco-pagination-item) {
    min-width: 44px !important;
    height: 44px !important;
    font-size: 15px !important;
  }
}

/* 小尺寸分页器 */
.api-account-page.small-pagination {
  .table-shell__footer :deep(.arco-pagination-item) {
    min-width: 32px !important;
    height: 32px !important;
    font-size: 12px !important;
  }
}
```

### 修改分页器颜色

```scss
/* 自定义激活颜色 */
.api-account-page .table-shell__footer :deep(.arco-pagination-item-active) {
  background: linear-gradient(135deg, 
    rgba(16, 185, 129, 0.95) 0%, 
    rgba(5, 150, 105, 0.9) 100%
  ) !important;
  box-shadow: 
    0 0 0 1px rgba(255, 255, 255, 0.2) inset,
    0 6px 16px -2px rgba(16, 185, 129, 0.4) !important;
}
```

---

## 📱 响应式示例

### 自适应布局

```vue
<template>
  <div class="table-shell__footer" :class="{ 'is-mobile': isMobile }">
    <!-- 统计信息 -->
    <div class="pagination-stats">
      <!-- 移动端只显示关键统计 -->
      <div v-if="!isMobile || showStat('total')" class="pagination-stat-card">
        <!-- 总记录数 -->
      </div>
      
      <div v-if="!isMobile || showStat('enabled')" class="pagination-stat-card">
        <!-- 已启用 -->
      </div>
      
      <div v-if="!isMobile" class="pagination-stat-card">
        <!-- 当前页（仅桌面端显示） -->
      </div>
    </div>
    
    <!-- 分页器 -->
    <a-pagination
      v-bind="paginationConfig"
      :simple="isMobile"
    />
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';

const isMobile = ref(false);

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768;
};

onMounted(() => {
  checkMobile();
  window.addEventListener('resize', checkMobile);
});

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile);
});

const showStat = (type: string) => {
  // 移动端优先显示的统计
  const mobilePriority = ['total', 'enabled'];
  return mobilePriority.includes(type);
};
</script>
```

---

## 🔧 高级功能

### 快速跳转

```vue
<template>
  <div class="pagination-quick-jump">
    <span class="pagination-quick-jump-label">快速跳转：</span>
    <div class="pagination-quick-jump-buttons">
      <button class="pagination-quick-jump-btn" @click="jumpToFirst">
        首页
      </button>
      <button class="pagination-quick-jump-btn" @click="jumpToLast">
        末页
      </button>
    </div>
  </div>
</template>

<script lang="ts" setup>
const jumpToFirst = () => {
  pagination.value.current = 1;
  loadData();
};

const jumpToLast = () => {
  const lastPage = Math.ceil(pagination.value.total / pagination.value.pageSize);
  pagination.value.current = lastPage;
  loadData();
};
</script>
```

### 加载状态

```vue
<template>
  <div class="table-shell__footer">
    <div v-if="loading" class="pagination-loading">
      <span class="pagination-loading-spinner"></span>
      加载中...
    </div>
    
    <template v-else>
      <!-- 统计和分页器 -->
    </template>
  </div>
</template>

<script lang="ts" setup>
const loading = ref(false);

const loadData = async () => {
  loading.value = true;
  try {
    // 加载数据
  } finally {
    loading.value = false;
  }
};
</script>
```

### 空状态

```vue
<template>
  <div class="table-shell__footer">
    <div v-if="!accountList.length" class="pagination-empty">
      <div class="pagination-empty-icon">📭</div>
      <div class="pagination-empty-text">暂无数据</div>
    </div>
    
    <template v-else>
      <!-- 统计和分页器 -->
    </template>
  </div>
</template>
```

---

## 📊 完整示例

```vue
<template>
  <div class="api-account-page">
    <GovernanceListStage class="table-shell">
      <a-table class="account-table" :data="accountList" :loading="loading">
        <!-- 表格列 -->
      </a-table>
      
      <template #footer>
        <div class="table-shell__footer">
          <!-- 加载状态 -->
          <div v-if="loading" class="pagination-loading">
            <span class="pagination-loading-spinner"></span>
            加载中...
          </div>
          
          <!-- 空状态 -->
          <div v-else-if="!accountList.length" class="pagination-empty">
            <div class="pagination-empty-icon">📭</div>
            <div class="pagination-empty-text">暂无数据</div>
          </div>
          
          <!-- 正常状态 -->
          <template v-else>
            <!-- 统计信息 -->
            <div class="pagination-stats">
              <div class="pagination-stat-card">
                <div class="pagination-stat-icon">
                  <icon-file />
                </div>
                <div class="pagination-stat-content">
                  <span class="pagination-stat-label">总记录数</span>
                  <span class="pagination-stat-value">
                    {{ pagination.total }}
                    <span class="stat-unit">条</span>
                  </span>
                </div>
              </div>
              
              <div class="pagination-stat-card">
                <div class="pagination-stat-icon stat-icon--success">
                  <icon-check-circle />
                </div>
                <div class="pagination-stat-content">
                  <span class="pagination-stat-label">已启用</span>
                  <span class="pagination-stat-value">
                    {{ enabledCount }}
                    <span class="stat-unit">条</span>
                  </span>
                </div>
              </div>
              
              <div class="pagination-stat-card">
                <div class="pagination-stat-icon stat-icon--info">
                  <icon-info-circle />
                </div>
                <div class="pagination-stat-content">
                  <span class="pagination-stat-label">当前页</span>
                  <span class="pagination-stat-value">
                    {{ accountList.length }}
                    <span class="stat-unit">条</span>
                  </span>
                </div>
              </div>
            </div>
            
            <!-- 分页器 -->
            <a-pagination
              v-model:current="pagination.current"
              v-model:page-size="pagination.pageSize"
              :total="pagination.total"
              :page-size-options="[10, 20, 50, 100]"
              :show-total="true"
              :show-jumper="true"
              :show-page-size="true"
              @change="handlePageChange"
              @page-size-change="handlePageSizeChange"
            />
          </template>
        </div>
      </template>
    </GovernanceListStage>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue';
import { IconFile, IconCheckCircle, IconInfoCircle } from '@arco-design/web-vue/es/icon';

const loading = ref(false);
const accountList = ref([]);

const pagination = ref({
  current: 1,
  pageSize: 20,
  total: 0
});

const enabledCount = computed(() => {
  return accountList.value.filter(item => item.status === 1).length;
});

const handlePageChange = (page: number) => {
  pagination.value.current = page;
  loadData();
};

const handlePageSizeChange = (pageSize: number) => {
  pagination.value.pageSize = pageSize;
  pagination.value.current = 1;
  loadData();
};

const loadData = async () => {
  loading.value = true;
  try {
    // 调用API
    const response = await fetchApiAccountPage({
      current: pagination.value.current,
      pageSize: pagination.value.pageSize
    });
    
    accountList.value = response.records;
    pagination.value.total = response.total;
  } finally {
    loading.value = false;
  }
};

// 初始加载
loadData();
</script>

<style scoped lang="scss">
@import './ApiAccountManageEnhanced.scss';
@import './ApiAccountTableEnhanced.scss';
@import './ApiAccountPaginationEnhanced.scss';
</style>
```

---

**最后更新**：2026-04-30  
**文档版本**：v2.0.0  
**作者**：BML 前端团队
