<template>
  <a-tooltip
    class="ellipsis-tooltip-trigger"
    :content="resolvedTooltipText"
    :disabled="!tooltipEnabled"
    position="top"
  >
    <span
      ref="textRef"
      class="ellipsis-tooltip-text"
      :class="`ellipsis-tooltip-text--${variant}`"
    >
      {{ resolvedDisplayText }}
    </span>
  </a-tooltip>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

type EllipsisTooltipVariant = 'text' | 'primary' | 'mono';

const props = withDefaults(defineProps<{
  text?: string | number | null;
  tooltipText?: string;
  fallback?: string;
  variant?: EllipsisTooltipVariant;
  alwaysShowTooltip?: boolean;
}>(), {
  fallback: '-',
  variant: 'text',
  alwaysShowTooltip: false
});

const textRef = ref<HTMLElement | null>(null);
const overflowed = ref(false);
let textResizeObserver: ResizeObserver | null = null;
let listeningWindowResize = false;

const resolvedDisplayText = computed(() => {
  const raw = props.text;
  if (raw == null) {
    return props.fallback;
  }
  const normalized = String(raw);
  return normalized === '' ? props.fallback : normalized;
});

const resolvedTooltipText = computed(() => {
  if (props.tooltipText && props.tooltipText.trim()) {
    return props.tooltipText.trim();
  }
  return resolvedDisplayText.value;
});

const tooltipEnabled = computed(() => {
  if (!resolvedTooltipText.value) {
    return false;
  }
  return props.alwaysShowTooltip || overflowed.value;
});

/**
 * 同步文本是否发生视觉截断：
 * 当文本真实滚动宽度超过可视宽度时，开启悬浮提示展示完整值。
 */
const syncOverflowState = () => {
  const element = textRef.value;
  if (!element) {
    overflowed.value = false;
    return;
  }
  overflowed.value = element.scrollWidth - element.clientWidth > 1;
};

const scheduleOverflowStateSync = () => {
  void nextTick(syncOverflowState);
};

onMounted(() => {
  scheduleOverflowStateSync();

  if (typeof ResizeObserver === 'function') {
    textResizeObserver = new ResizeObserver(() => {
      syncOverflowState();
    });
    if (textRef.value) {
      textResizeObserver.observe(textRef.value);
    }
    return;
  }

  if (typeof window !== 'undefined') {
    listeningWindowResize = true;
    window.addEventListener('resize', syncOverflowState, { passive: true });
  }
});

watch([resolvedDisplayText, resolvedTooltipText], () => {
  scheduleOverflowStateSync();
});

onBeforeUnmount(() => {
  if (textResizeObserver) {
    textResizeObserver.disconnect();
    textResizeObserver = null;
  }
  if (listeningWindowResize && typeof window !== 'undefined') {
    window.removeEventListener('resize', syncOverflowState);
    listeningWindowResize = false;
  }
});
</script>

<style scoped>
.ellipsis-tooltip-trigger {
  display: block;
  width: 100%;
  min-width: 0;
}

.ellipsis-tooltip-text {
  display: block;
  width: 100%;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.25;
}

.ellipsis-tooltip-text--text {
  color: #1e293b;
  font-size: 14px;
  font-weight: 500;
}

.ellipsis-tooltip-text--primary {
  color: #1e293b;
  font-size: 14px;
  font-weight: 700;
}

.ellipsis-tooltip-text--mono {
  color: #334155;
  font-family: 'SFMono-Regular', 'Consolas', 'Menlo', 'Monaco', monospace;
  font-size: 13px;
  font-weight: 500;
}
</style>
