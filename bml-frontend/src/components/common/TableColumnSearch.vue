<template>
  <!--
    通用表格列头搜索组件
    ─────────────────────────────────────────────────────────────────
    布局：放大镜图标（左）+ 列标题文字（右）
    ─────────────────────────────────────────────────────────────────
  -->
  <div class="tcs-wrapper">
    <!-- 放大镜图标 + Popover 搜索框（在标题左侧） -->
    <a-popover
      v-model:popup-visible="popoverVisible"
      trigger="click"
      position="bottom"
      :arrow-style="{ display: 'none' }"
      :popup-container="popupContainer"
      @popup-visible-change="handlePopoverVisibleChange"
    >
      <!-- 触发器：放大镜图标 -->
      <span
        class="tcs-icon"
        :class="{ 'tcs-icon--active': hasValue }"
        @click.stop="handleIconClick"
        :title="hasValue ? `已筛选：${modelValue}` : '点击搜索'"
      >
        <icon-search />
        <!-- 有搜索词时显示小红点提示 -->
        <span v-if="hasValue" class="tcs-badge"></span>
      </span>

      <!-- Popover 内容：搜索输入框 -->
      <template #content>
        <div class="tcs-popover" @click.stop>
          <div class="tcs-popover__header">
            <span class="tcs-popover__label">搜索 {{ title }}</span>
            <span
              v-if="hasValue"
              class="tcs-popover__clear"
              @click="handleClear"
            >清除</span>
          </div>
          <a-input
            ref="inputRef"
            v-model="inputValue"
            :placeholder="placeholder || `输入${title}关键词`"
            allow-clear
            size="small"
            class="tcs-input"
            @press-enter="handleConfirm"
            @clear="handleClear"
          >
            <template #prefix>
              <icon-search class="tcs-input-icon" />
            </template>
          </a-input>
          <div class="tcs-popover__footer">
            <a-button size="mini" @click="handleReset">重置</a-button>
            <a-button type="primary" size="mini" @click="handleConfirm">确认</a-button>
          </div>
        </div>
      </template>
    </a-popover>

    <!-- 列标题文字（在图标右侧） -->
    <span class="tcs-title">{{ title }}</span>
  </div>
</template>

<script setup lang="ts">
/**
 * 通用表格列头搜索组件
 *
 * Props：
 *   - title: 列标题文字
 *   - modelValue: 当前搜索关键词（v-model 绑定）
 *   - placeholder: 输入框占位文字（可选）
 *   - popupContainer: Popover 挂载容器（可选，默认挂载到 body）
 *
 * Emits：
 *   - update:modelValue: 搜索关键词变化时触发
 *   - search: 用户确认搜索时触发，携带关键词
 *   - reset: 用户重置搜索时触发
 */
import { ref, computed, nextTick, watch } from 'vue';
import { IconSearch } from '@arco-design/web-vue/es/icon';

// ─────────────────────────────────────────────
// Props 定义
// ─────────────────────────────────────────────
const props = withDefaults(defineProps<{
  /** 列标题文字 */
  title: string;
  /** 当前搜索关键词（v-model 绑定） */
  modelValue: string;
  /** 输入框占位文字 */
  placeholder?: string;
  /** Popover 挂载容器，默认挂载到 body，避免被表格 overflow:hidden 裁剪 */
  popupContainer?: string | HTMLElement;
}>(), {
  modelValue: '',
  placeholder: '',
  popupContainer: 'body',
});

// ─────────────────────────────────────────────
// Emits 定义
// ─────────────────────────────────────────────
const emit = defineEmits<{
  /** 搜索关键词变化 */
  'update:modelValue': [value: string];
  /** 用户确认搜索 */
  'search': [value: string];
  /** 用户重置搜索 */
  'reset': [];
}>();

// ─────────────────────────────────────────────
// 内部状态
// ─────────────────────────────────────────────
/** Popover 显示状态 */
const popoverVisible = ref(false);

/** 输入框内部值（未确认前不同步到 modelValue） */
const inputValue = ref(props.modelValue);

/** 输入框 ref，用于自动聚焦 */
const inputRef = ref<any>(null);

/** 是否有搜索词（用于图标高亮和红点显示） */
const hasValue = computed(() => Boolean(props.modelValue));

// ─────────────────────────────────────────────
// 监听外部 modelValue 变化，同步到内部输入框
// ─────────────────────────────────────────────
watch(() => props.modelValue, (newVal) => {
  inputValue.value = newVal;
});

// ─────────────────────────────────────────────
// 事件处理
// ─────────────────────────────────────────────

/** 点击放大镜图标：打开 Popover 并自动聚焦输入框 */
const handleIconClick = () => {
  popoverVisible.value = !popoverVisible.value;
};

/** Popover 显示状态变化：打开时自动聚焦输入框 */
const handlePopoverVisibleChange = (visible: boolean) => {
  if (visible) {
    // 等待 DOM 渲染完成后聚焦
    nextTick(() => {
      inputRef.value?.focus?.();
    });
  }
};

/** 确认搜索：同步值并关闭 Popover */
const handleConfirm = () => {
  const trimmed = inputValue.value.trim();
  emit('update:modelValue', trimmed);
  emit('search', trimmed);
  popoverVisible.value = false;
};

/** 清除搜索：清空输入框和外部值 */
const handleClear = () => {
  inputValue.value = '';
  emit('update:modelValue', '');
  emit('reset');
};

/** 重置搜索：同 handleClear，关闭 Popover */
const handleReset = () => {
  handleClear();
  popoverVisible.value = false;
};
</script>

<style scoped>
/* ─────────────────────────────────────────────
   列头搜索组件容器
   ───────────────────────────────────────────── */
.tcs-wrapper {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  /* 确保不超出表头单元格 */
  max-width: 100%;
  overflow: hidden;
}

/* 列标题文字 */
.tcs-title {
  flex-shrink: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 700;
  font-size: 12px;
  color: #334155;
}

/* ─────────────────────────────────────────────
   放大镜图标
   ───────────────────────────────────────────── */
.tcs-icon {
  position: relative;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  cursor: pointer;
  color: #94a3b8;
  font-size: 12px;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.tcs-icon:hover {
  color: var(--color-primary, #165dff);
  background: rgba(22, 93, 255, 0.08);
  transform: scale(1.15);
}

/* 有搜索词时：图标高亮为品牌主色 */
.tcs-icon--active {
  color: var(--color-primary, #165dff) !important;
  background: rgba(22, 93, 255, 0.1);
}

.tcs-icon--active:hover {
  background: rgba(22, 93, 255, 0.18);
}

/* 有搜索词时的小红点提示 */
.tcs-badge {
  position: absolute;
  top: 1px;
  right: 1px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #f53f3f;
  border: 1px solid #fff;
}

/* ─────────────────────────────────────────────
   Popover 内容区
   ───────────────────────────────────────────── */
.tcs-popover {
  width: 220px;
  padding: 4px 0;
}

/* Popover 头部：标签 + 清除按钮 */
.tcs-popover__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  padding: 0 2px;
}

.tcs-popover__label {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.tcs-popover__clear {
  font-size: 11px;
  color: var(--color-primary, #165dff);
  cursor: pointer;
  transition: opacity 0.15s;
}

.tcs-popover__clear:hover {
  opacity: 0.75;
}

/* 搜索输入框 */
.tcs-input {
  width: 100%;
  margin-bottom: 10px;
}

.tcs-input-icon {
  font-size: 13px;
  color: #94a3b8;
}

/* Popover 底部：操作按钮 */
.tcs-popover__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
