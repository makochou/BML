<template>
  <!--
    ══════════════════════════════════════════════════════
    BML 主题恢复默认按钮（ThemeRestoreButton）
    ──────────────────────────────────────────────────────
    需求映射：
      - R3.1：在 Admin / Business 主题设置面板中提供 “恢复默认” 按钮组件。
      - R3.5：恢复默认调用失败时，前端先以本地缓存的 PRESET_BEST 完成兜底，
              并通过 Message.warning 提示 “主题云端同步失败，已使用本地配置”。
      - R8.4：作为通用组件封装确认弹窗、API 调用、错误提示与状态指示。
    ══════════════════════════════════════════════════════
  -->
  <a-popconfirm
    content="确认恢复到系统默认主题（PRESET_BEST）？此操作将重置当前作用域的全部主题维度。"
    type="warning"
    ok-text="恢复默认"
    cancel-text="取消"
    @ok="handleRestore"
  >
    <a-button
      :loading="loading"
      type="outline"
      status="warning"
      class="bml-theme-restore-btn"
    >
      <template #icon>
        <icon-refresh />
      </template>
      恢复默认
    </a-button>
  </a-popconfirm>
</template>

<script setup lang="ts">
/**
 * ThemeRestoreButton.vue —— 主题 “恢复默认” 通用按钮组件。
 *
 * 概述
 * ────
 * 在中台管理（ADMIN）与业务系统（BUSINESS）两个作用域的主题设置面板中复用，
 * 内嵌 Arco Design `a-popconfirm` 二次确认弹窗，避免误触；
 * 用户确认后调用主题 store 的 {@link restore}（受 task 13.1 提供）将当前
 * 作用域的 ThemeProfile 重置为 `PRESET_BEST` 对应变体。
 *
 * 错误处理（R3.5 / R13.1）
 * ────────────────────────
 * - 恢复成功：调用 `Message.success` 提示 “已恢复至系统默认主题”。
 * - 恢复失败：捕获 store 抛出的异常，调用 `Message.warning` 提示
 *   “主题云端同步失败，已使用本地配置”；store 内部已负责本地兜底，
 *   组件层不再重复处理。
 *
 * 设计决策
 * ────────
 * - 直接依赖 {@link useThemeStore}（来自 `@/store/theme`，task 13.1 中实现）
 *   而非 `useTheme` Composable，以避免引入额外的作用域推断逻辑：
 *   外层调用方已经明确传入 `scope`，组件按实参原样转发。
 * - `loading` 仅由本组件内部维护，不依赖 store 的 `loading` 字段，
 *   以便在同一面板中存在多个并发主题写操作时仍能精准反映 “恢复默认”
 *   按钮自身的进行中状态。
 *
 * @see Requirements 3.1 / 3.5 / 8.4
 */
import { ref } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconRefresh } from '@arco-design/web-vue/es/icon';

import type { ThemeScope } from '@/types/theme';
import { useThemeStore } from '@/store/theme';

/**
 * 组件入参。
 *
 * - `scope`：必填的主题作用域（`ADMIN` / `BUSINESS`），调用方负责显式传入。
 *   组件不会基于路由自动推断作用域，避免在 Admin 嵌套 Business 等复杂布局
 *   中产生歧义。
 */
const props = defineProps<{
    /** 当前按钮所属的主题作用域。 */
    scope: ThemeScope;
}>();

/** 主题 Pinia store。提供 `restore(scope)` 等动作（task 13.1 中实现）。 */
const themeStore = useThemeStore();

/**
 * 恢复操作进行中状态。
 *
 * 用于驱动 `a-button` 的 `loading` 态，在 store 调用期间禁用重复点击；
 * 与 store 自身的 loading 字段相互独立，仅反映本按钮的执行状态。
 */
const loading = ref(false);

/**
 * 处理 “恢复默认” 确认回调。
 *
 * 流程：
 * 1. 置 `loading=true` 以阻止重复触发；
 * 2. 调用 `themeStore.restore(scope)` 将当前作用域 ThemeProfile 重置为
 *    `PRESET_BEST` 对应变体；
 * 3. 成功路径：`Message.success` 提示用户；
 * 4. 失败路径：`Message.warning` 提示 “主题云端同步失败，已使用本地配置”
 *    （R3.5）；store 内部负责本地兜底，因此此处无需再次写本地缓存；
 * 5. 无论成败，`finally` 复位 `loading=false`。
 *
 * 不会抛出异常给上层：所有错误都在组件内部消化，避免破坏 popconfirm
 * 的关闭流程。
 */
async function handleRestore(): Promise<void> {
    loading.value = true;
    try {
        await themeStore.restore(props.scope);
        Message.success('已恢复至系统默认主题');
    } catch (error) {
        // 仅记录到控制台供开发态排查；用户侧仅展示统一的中文提示。
        // eslint-disable-next-line no-console
        console.warn('[BML ThemeRestoreButton] restore failed:', error);
        Message.warning('主题云端同步失败，已使用本地配置');
    } finally {
        loading.value = false;
    }
}
</script>

<style scoped>
.bml-theme-restore-btn {
    /* 与主题面板风格保持一致：使用 PRESET_BEST 派生的 Token 控制圆角与间距。 */
    border-radius: var(--bml-radius-md, 6px);
    font-weight: 500;
}
</style>
