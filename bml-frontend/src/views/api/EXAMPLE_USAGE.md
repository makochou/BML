# 授权治理查询面板 - 使用示例

## 📖 目录

1. [基础使用](#基础使用)
2. [自定义主题](#自定义主题)
3. [响应式配置](#响应式配置)
4. [动画控制](#动画控制)
5. [性能优化](#性能优化)
6. [常见场景](#常见场景)

---

## 🎯 基础使用

### 示例 1：标准集成

最简单的集成方式，直接导入样式文件：

```vue
<!-- ApiAccountManage.vue -->
<template>
  <div ref="apiAccountPageRef" class="page api-account-page">
    <GovernanceCompactQueryPanel 
      class="query-panel" 
      :max-width="accountWorkspaceMaxWidth" 
      density="ultra"
      theme="aurora"
    >
      <!-- 查询表单内容 -->
      <template #footerActions>
        <div class="query-panel__mode-actions">
          <a-button type="primary" class="query-panel__mode-btn">
            模糊查找
          </a-button>
          <a-button type="primary" class="query-panel__mode-btn">
            精确查找
          </a-button>
        </div>
        <a-button @click="handleResetSearch">重置条件</a-button>
      </template>
      
      <a-form :model="queryForm" layout="vertical" class="query-form">
        <!-- 表单字段 -->
      </a-form>
    </GovernanceCompactQueryPanel>
  </div>
</template>

<script lang="ts" setup>
// 组件逻辑
</script>

<style scoped lang="scss">
/* 导入增强样式 */
@import './ApiAccountManageEnhanced.scss';

/* 页面基础样式 */
.api-account-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px;
  background: #f5f7fa;
}
</style>
```

---

## 🎨 自定义主题

### 示例 2：自定义品牌色

修改主色调以匹配你的品牌色：

```vue
<style scoped lang="scss">
@import './ApiAccountManageEnhanced.scss';

/* 自定义品牌色 */
.api-account-page .query-panel {
  /* 使用公司品牌色 */
  --query-panel-accent-bar-background: 
    linear-gradient(90deg, 
      #ff6b6b 0%,   /* 品牌红 */
      #4ecdc4 50%,  /* 品牌青 */
      #45b7d1 100%  /* 品牌蓝 */
    ) !important;
  
  /* 主按钮使用品牌色 */
  --query-panel-primary-button-background:
    linear-gradient(135deg, 
      #ff6b6b 0%, 
      #ee5a6f 50%,
      #e74c3c 100%
    ) !important;
  
  /* 输入框聚焦色 */
  --query-panel-input-focus-border-color: rgba(255, 107, 107, 0.6) !important;
  --query-panel-input-focus-shadow: 
    0 0 0 4px rgba(255, 107, 107, 0.12),
    0 0 0 1px rgba(255, 107, 107, 0.3),
    0 8px 16px -4px rgba(255, 107, 107, 0.15) !important;
}
</style>
```

### 示例 3：深色主题

创建深色模式的查询面板：

```vue
<style scoped lang="scss">
@import './ApiAccountManageEnhanced.scss';

/* 深色主题 */
.api-account-page.dark-mode .query-panel {
  /* 外壳背景 */
  --query-panel-shell-background:
    radial-gradient(ellipse at top left, rgba(99, 102, 241, 0.15), transparent 50%),
    radial-gradient(ellipse at bottom right, rgba(59, 130, 246, 0.12), transparent 50%),
    linear-gradient(135deg, 
      rgba(30, 41, 59, 0.95) 0%, 
      rgba(15, 23, 42, 0.92) 50%,
      rgba(2, 6, 23, 0.90) 100%
    ) !important;
  
  /* 边框颜色 */
  --query-panel-shell-border-color: rgba(71, 85, 105, 0.4) !important;
  
  /* 内容区背景 */
  --query-panel-body-background:
    linear-gradient(135deg,
      rgba(30, 41, 59, 0.9) 0%,
      rgba(15, 23, 42, 0.85) 50%,
      rgba(2, 6, 23, 0.8) 100%
    ) !important;
  
  /* 输入框样式 */
  --query-panel-input-background: rgba(30, 41, 59, 0.8) !important;
  --query-panel-input-border-color: rgba(71, 85, 105, 0.6) !important;
  --query-panel-label-color: #e2e8f0 !important;
  
  /* 文字颜色 */
  --query-panel-muted-button-color: #cbd5e1 !important;
}

/* 深色模式下的输入框 */
.api-account-page.dark-mode .query-panel :deep(.arco-input),
.api-account-page.dark-mode .query-panel :deep(.arco-select-view-value) {
  color: #f1f5f9 !important;
}

.api-account-page.dark-mode .query-panel :deep(.arco-input::placeholder) {
  color: #64748b !important;
}
</style>
```

### 示例 4：极简主题

创建极简风格的查询面板：

```vue
<style scoped lang="scss">
@import './ApiAccountManageEnhanced.scss';

/* 极简主题 */
.api-account-page.minimal-theme .query-panel {
  /* 移除装饰元素 */
  --query-panel-accent-bar-background: transparent !important;
  --query-panel-orb-opacity: 0 !important;
  
  /* 简化背景 */
  --query-panel-shell-background:
    linear-gradient(180deg, 
      rgba(255, 255, 255, 0.98) 0%, 
      rgba(249, 250, 251, 0.96) 100%
    ) !important;
  
  /* 简化阴影 */
  --query-panel-shell-shadow:
    0 2px 8px rgba(0, 0, 0, 0.08) !important;
  
  /* 简化输入框 */
  --query-panel-input-border-color: rgba(226, 232, 240, 0.8) !important;
  --query-panel-input-background: #ffffff !important;
  
  /* 简化按钮 */
  --query-panel-primary-button-background: #3b82f6 !important;
  --query-panel-primary-button-shadow:
    0 2px 4px rgba(59, 130, 246, 0.2) !important;
}
</style>
```

---

## 📱 响应式配置

### 示例 5：自定义断点

根据项目需求自定义响应式断点：

```vue
<style scoped lang="scss">
@import './ApiAccountManageEnhanced.scss';

/* 大屏幕（1920px+） */
@media (min-width: 1920px) {
  .api-account-page .query-panel {
    --query-panel-padding: 16px !important;
    --query-panel-body-padding: 24px 28px 16px !important;
    --query-panel-input-min-height: 44px !important;
    --query-panel-button-height: 44px !important;
  }
}

/* 笔记本（1366px - 1920px） */
@media (min-width: 1366px) and (max-width: 1919px) {
  .api-account-page .query-panel {
    --query-panel-padding: 12px !important;
    --query-panel-body-padding: 18px 20px 12px !important;
  }
}

/* 小屏笔记本（1024px - 1365px） */
@media (min-width: 1024px) and (max-width: 1365px) {
  .api-account-page .query-panel {
    --query-panel-padding: 10px !important;
    --query-panel-body-padding: 14px 16px 10px !important;
    --query-panel-field-gap-x: 10px !important;
  }
}

/* 平板横屏（768px - 1023px） */
@media (min-width: 768px) and (max-width: 1023px) {
  .api-account-page .query-panel {
    --query-panel-padding: 8px !important;
    --query-panel-body-padding: 12px 14px 8px !important;
    --query-panel-field-gap-x: 8px !important;
  }
  
  /* 按钮组改为纵向 */
  .api-account-page .query-panel__mode-actions {
    flex-direction: column;
    width: 100%;
  }
}

/* 手机（< 768px） */
@media (max-width: 767px) {
  .api-account-page .query-panel {
    --query-panel-padding: 6px !important;
    --query-panel-radius: 16px !important;
    --query-panel-body-padding: 10px 12px 6px !important;
    --query-panel-body-radius: 12px !important;
  }
  
  /* 所有按钮全宽 */
  .api-account-page .query-panel :deep(.arco-btn) {
    width: 100% !important;
    min-width: 0 !important;
  }
}
</style>
```

---

## 🎬 动画控制

### 示例 6：禁用动画

为低性能设备或用户偏好禁用动画：

```vue
<template>
  <div class="api-account-page" :class="{ 'no-animations': disableAnimations }">
    <!-- 内容 -->
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';

// 检测用户偏好
const disableAnimations = ref(false);

onMounted(() => {
  // 检测系统设置
  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  disableAnimations.value = prefersReducedMotion.matches;
  
  // 监听设置变化
  prefersReducedMotion.addEventListener('change', (e) => {
    disableAnimations.value = e.matches;
  });
});
</script>

<style scoped lang="scss">
@import './ApiAccountManageEnhanced.scss';

/* 禁用动画 */
.api-account-page.no-animations .query-panel {
  --query-panel-orb-animation: none !important;
}

.api-account-page.no-animations * {
  animation: none !important;
  transition: none !important;
}
</style>
```

### 示例 7：自定义动画速度

调整动画速度以匹配整体应用风格：

```vue
<style scoped lang="scss">
@import './ApiAccountManageEnhanced.scss';

/* 快速动画（适合高性能设备） */
.api-account-page.fast-animations {
  .query-panel__mode-btn,
  .query-panel__toggle-btn,
  :deep(.arco-input-wrapper),
  :deep(.arco-select-view) {
    transition-duration: 0.15s !important;
  }
  
  .query-panel {
    --query-panel-orb-animation: query-orb-float 4s ease-in-out infinite !important;
  }
}

/* 慢速动画（适合演示或教学） */
.api-account-page.slow-animations {
  .query-panel__mode-btn,
  .query-panel__toggle-btn,
  :deep(.arco-input-wrapper),
  :deep(.arco-select-view) {
    transition-duration: 0.6s !important;
  }
  
  .query-panel {
    --query-panel-orb-animation: query-orb-float 16s ease-in-out infinite !important;
  }
}
</style>
```

---

## ⚡ 性能优化

### 示例 8：懒加载动画

仅在查询面板可见时播放动画：

```vue
<template>
  <div ref="queryPanelRef" class="api-account-page" :class="{ 'is-visible': isVisible }">
    <GovernanceCompactQueryPanel class="query-panel">
      <!-- 内容 -->
    </GovernanceCompactQueryPanel>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted } from 'vue';

const queryPanelRef = ref<HTMLElement>();
const isVisible = ref(false);
let observer: IntersectionObserver | null = null;

onMounted(() => {
  // 创建 Intersection Observer
  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        isVisible.value = entry.isIntersecting;
      });
    },
    {
      threshold: 0.1, // 10% 可见时触发
    }
  );
  
  // 观察查询面板
  if (queryPanelRef.value) {
    observer.observe(queryPanelRef.value);
  }
});

onUnmounted(() => {
  // 清理 observer
  if (observer) {
    observer.disconnect();
  }
});
</script>

<style scoped lang="scss">
@import './ApiAccountManageEnhanced.scss';

/* 默认暂停动画 */
.api-account-page .query-panel {
  --query-panel-orb-animation: none !important;
}

/* 可见时播放动画 */
.api-account-page.is-visible .query-panel {
  --query-panel-orb-animation: query-orb-float 8s ease-in-out infinite !important;
}
</style>
```

### 示例 9：GPU 加速优化

强制使用 GPU 加速提升性能：

```vue
<style scoped lang="scss">
@import './ApiAccountManageEnhanced.scss';

/* GPU 加速优化 */
.api-account-page .query-panel {
  /* 提示浏览器使用 GPU */
  will-change: transform, opacity;
  transform: translateZ(0);
  backface-visibility: hidden;
  perspective: 1000px;
}

.api-account-page .query-panel__mode-btn,
.api-account-page .query-panel__toggle-btn {
  will-change: transform, box-shadow;
  transform: translateZ(0);
}

.api-account-page .query-panel :deep(.arco-input-wrapper),
.api-account-page .query-panel :deep(.arco-select-view) {
  will-change: border-color, box-shadow;
  transform: translateZ(0);
}
</style>
```

---

## 🎯 常见场景

### 示例 10：多语言支持

根据语言调整字体和间距：

```vue
<template>
  <div class="api-account-page" :class="`lang-${currentLang}`">
    <!-- 内容 -->
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';

const currentLang = ref('zh-CN'); // 'zh-CN' | 'en-US' | 'ja-JP'
</script>

<style scoped lang="scss">
@import './ApiAccountManageEnhanced.scss';

/* 中文 */
.api-account-page.lang-zh-CN .query-panel {
  --query-panel-label-size: 13px !important;
  --query-panel-input-font-size: 14px !important;
}

/* 英文 */
.api-account-page.lang-en-US .query-panel {
  --query-panel-label-size: 12px !important;
  --query-panel-input-font-size: 13px !important;
  --query-panel-button-padding-inline: 20px !important;
}

.api-account-page.lang-en-US .query-panel :deep(.arco-form-item-label-col > label) {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif !important;
  letter-spacing: 0.02em !important;
}

/* 日文 */
.api-account-page.lang-ja-JP .query-panel {
  --query-panel-label-size: 13px !important;
  --query-panel-input-font-size: 14px !important;
  --query-panel-form-item-gap: 12px !important;
}

.api-account-page.lang-ja-JP .query-panel :deep(.arco-form-item-label-col > label) {
  font-family: 'Noto Sans JP', sans-serif !important;
}
</style>
```

### 示例 11：打印样式

优化打印输出：

```vue
<style scoped lang="scss">
@import './ApiAccountManageEnhanced.scss';

/* 打印样式 */
@media print {
  .api-account-page .query-panel {
    /* 移除装饰 */
    --query-panel-accent-bar-background: transparent !important;
    --query-panel-orb-opacity: 0 !important;
    
    /* 简化背景 */
    background: #ffffff !important;
    box-shadow: none !important;
    border: 1px solid #000000 !important;
    
    /* 移除动画 */
    animation: none !important;
  }
  
  /* 隐藏操作按钮 */
  .api-account-page .query-panel__mode-actions,
  .api-account-page .query-panel :deep(.governance-compact-query-panel__footer-actions) {
    display: none !important;
  }
  
  /* 优化输入框显示 */
  .api-account-page .query-panel :deep(.arco-input-wrapper),
  .api-account-page .query-panel :deep(.arco-select-view) {
    border: 1px solid #000000 !important;
    background: #ffffff !important;
    box-shadow: none !important;
  }
}
</style>
```

### 示例 12：高对比度模式

为视力障碍用户提供高对比度版本：

```vue
<template>
  <div class="api-account-page" :class="{ 'high-contrast': highContrastMode }">
    <button @click="toggleHighContrast" class="contrast-toggle">
      {{ highContrastMode ? '标准模式' : '高对比度' }}
    </button>
    <!-- 内容 -->
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';

const highContrastMode = ref(false);

const toggleHighContrast = () => {
  highContrastMode.value = !highContrastMode.value;
};
</script>

<style scoped lang="scss">
@import './ApiAccountManageEnhanced.scss';

/* 高对比度模式 */
.api-account-page.high-contrast .query-panel {
  /* 强化边框 */
  --query-panel-shell-border-color: rgba(0, 0, 0, 0.6) !important;
  --query-panel-input-border-color: rgba(0, 0, 0, 0.8) !important;
  
  /* 强化文字 */
  --query-panel-label-color: #000000 !important;
  --query-panel-muted-button-color: #000000 !important;
  
  /* 简化背景 */
  --query-panel-shell-background: #ffffff !important;
  --query-panel-body-background: #f5f5f5 !important;
  
  /* 移除装饰 */
  --query-panel-accent-bar-opacity: 0 !important;
  --query-panel-orb-opacity: 0 !important;
}

.api-account-page.high-contrast .query-panel :deep(.arco-input),
.api-account-page.high-contrast .query-panel :deep(.arco-select-view-value) {
  color: #000000 !important;
  font-weight: 600 !important;
}

.api-account-page.high-contrast .query-panel__mode-btn {
  background: #000000 !important;
  color: #ffffff !important;
  border: 2px solid #000000 !important;
}

/* 对比度切换按钮 */
.contrast-toggle {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
  padding: 8px 16px;
  border: 2px solid #000000;
  background: #ffffff;
  color: #000000;
  font-weight: 600;
  cursor: pointer;
  border-radius: 8px;
}

.contrast-toggle:hover {
  background: #000000;
  color: #ffffff;
}
</style>
```

---

## 🔧 高级配置

### 示例 13：动态主题切换

根据时间或用户偏好动态切换主题：

```vue
<template>
  <div class="api-account-page" :class="`theme-${currentTheme}`">
    <div class="theme-switcher">
      <button @click="setTheme('light')">浅色</button>
      <button @click="setTheme('dark')">深色</button>
      <button @click="setTheme('auto')">自动</button>
    </div>
    <!-- 内容 -->
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, watch } from 'vue';

type Theme = 'light' | 'dark' | 'auto';

const currentTheme = ref<Theme>('auto');
const systemTheme = ref<'light' | 'dark'>('light');

// 检测系统主题
const detectSystemTheme = () => {
  const isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  systemTheme.value = isDark ? 'dark' : 'light';
};

// 设置主题
const setTheme = (theme: Theme) => {
  currentTheme.value = theme;
  localStorage.setItem('theme', theme);
};

// 获取实际主题
const getActualTheme = () => {
  if (currentTheme.value === 'auto') {
    return systemTheme.value;
  }
  return currentTheme.value;
};

onMounted(() => {
  // 读取保存的主题
  const savedTheme = localStorage.getItem('theme') as Theme;
  if (savedTheme) {
    currentTheme.value = savedTheme;
  }
  
  // 检测系统主题
  detectSystemTheme();
  
  // 监听系统主题变化
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
  mediaQuery.addEventListener('change', detectSystemTheme);
});

// 监听主题变化
watch([currentTheme, systemTheme], () => {
  const actualTheme = getActualTheme();
  document.documentElement.setAttribute('data-theme', actualTheme);
});
</script>

<style scoped lang="scss">
@import './ApiAccountManageEnhanced.scss';

/* 浅色主题（默认） */
.api-account-page.theme-light .query-panel,
.api-account-page.theme-auto .query-panel {
  /* 使用默认样式 */
}

/* 深色主题 */
.api-account-page.theme-dark .query-panel {
  --query-panel-shell-background:
    radial-gradient(ellipse at top left, rgba(99, 102, 241, 0.15), transparent 50%),
    radial-gradient(ellipse at bottom right, rgba(59, 130, 246, 0.12), transparent 50%),
    linear-gradient(135deg, 
      rgba(30, 41, 59, 0.95) 0%, 
      rgba(15, 23, 42, 0.92) 50%,
      rgba(2, 6, 23, 0.90) 100%
    ) !important;
  
  --query-panel-shell-border-color: rgba(71, 85, 105, 0.4) !important;
  --query-panel-body-background:
    linear-gradient(135deg,
      rgba(30, 41, 59, 0.9) 0%,
      rgba(15, 23, 42, 0.85) 50%,
      rgba(2, 6, 23, 0.8) 100%
    ) !important;
  
  --query-panel-input-background: rgba(30, 41, 59, 0.8) !important;
  --query-panel-input-border-color: rgba(71, 85, 105, 0.6) !important;
  --query-panel-label-color: #e2e8f0 !important;
  --query-panel-muted-button-color: #cbd5e1 !important;
}

/* 主题切换器 */
.theme-switcher {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
  display: flex;
  gap: 8px;
  padding: 4px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.theme-switcher button {
  padding: 8px 16px;
  border: none;
  background: transparent;
  color: #475569;
  font-weight: 600;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s;
}

.theme-switcher button:hover {
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
}
</style>
```

---

## 📝 总结

以上示例涵盖了授权治理查询面板的各种使用场景，包括：

1. ✅ 基础集成
2. 🎨 主题自定义
3. 📱 响应式配置
4. 🎬 动画控制
5. ⚡ 性能优化
6. 🎯 特殊场景

你可以根据实际需求选择合适的示例进行参考和修改。

---

**提示**：所有示例都可以组合使用，根据项目需求灵活调整。

**最后更新**：2026-04-30  
**文档版本**：v1.0.0  
**作者**：BML 前端团队
