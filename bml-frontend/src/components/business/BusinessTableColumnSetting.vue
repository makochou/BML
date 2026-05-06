<template>
  <!--
    业务系统通用列设置面板组件
    ─────────────────────────────
    功能：
    1. 列显示/隐藏开关
    2. 列上移/下移排序
    3. 列左侧固定切换
    4. 拖拽排序
    5. 恢复默认配置

    使用方式：
    <BusinessTableColumnSetting
      :items="columnSettingItems"
      :drag-state="dragState"
      @toggle-visible="toggleColumnVisible"
      @move="moveColumn"
      @toggle-fixed="toggleColumnFixed"
      @drag-start="handleDragStart"
      @drag-over="handleDragOver"
      @drop="handleDrop"
      @drag-end="handleDragEnd"
      @reset="resetColumns"
    />
  -->
  <div class="biz-col-setting">
    <!-- 面板头部 -->
    <div class="biz-col-setting__head">
      <strong class="biz-col-setting__title">字段显示与顺序</strong>
      <a-link class="biz-col-setting__reset" @click="$emit('reset')">恢复默认</a-link>
    </div>

    <!-- 列列表 -->
    <div class="biz-col-setting__list">
      <div
        v-for="item in items"
        :key="item.key"
        class="biz-col-setting__item"
        :class="{
          'is-draggable': !item.locked,
          'is-drag-source': dragState.draggingKey === item.key,
          'is-drag-over-before': dragState.overKey === item.key && dragState.dropPosition === 'before',
          'is-drag-over-after': dragState.overKey === item.key && dragState.dropPosition === 'after',
        }"
        @dragover="$emit('drag-over', item.key, $event)"
        @drop="$emit('drop', item.key, $event)"
      >
        <!-- 左侧：拖拽手柄 + 列名 + 锁定标记 -->
        <div class="biz-col-setting__label">
          <span
            v-if="!item.locked"
            class="biz-col-setting__drag-handle"
            draggable="true"
            title="拖动调整字段顺序"
            @dragstart="$emit('drag-start', item.key, $event)"
            @dragend="$emit('drag-end')"
          >
            <icon-drag-arrow />
          </span>
          <span class="biz-col-setting__col-name">{{ item.title }}</span>
          <small v-if="item.locked" class="biz-col-setting__locked-badge">固定</small>
        </div>

        <!-- 右侧：上移/下移/固定/显示开关 -->
        <div class="biz-col-setting__actions">
          <a-button
            size="mini"
            class="biz-col-setting__order-btn"
            :disabled="item.moveUpDisabled"
            @click="$emit('move', item.key, -1)"
          >
            <template #icon><icon-up /></template>
          </a-button>
          <a-button
            size="mini"
            class="biz-col-setting__order-btn"
            :disabled="item.moveDownDisabled"
            @click="$emit('move', item.key, 1)"
          >
            <template #icon><icon-down /></template>
          </a-button>
          <a-tooltip :content="item.fixed === 'left' ? '取消左侧固定' : '固定在左侧'">
            <a-button
              size="mini"
              class="biz-col-setting__fixed-btn"
              :class="{ 'is-active': item.fixed === 'left' }"
              :disabled="item.locked"
              @click="$emit('toggle-fixed', item.key)"
            >
              <template #icon><icon-pushpin /></template>
            </a-button>
          </a-tooltip>
          <a-switch
            size="small"
            :model-value="item.visible"
            :disabled="item.locked"
            @change="$emit('toggle-visible', item.key, Boolean($event))"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
/**
 * 业务系统通用列设置面板。
 * 与中台授权治理的列设置面板保持一致的交互体验。
 */
import { IconDragArrow, IconUp, IconDown, IconPushpin } from '@arco-design/web-vue/es/icon';
import type { ColumnSettingItem } from '../../composables/useBusinessTableColumns';

interface DragState {
  draggingKey: string | null;
  overKey: string | null;
  dropPosition: 'before' | 'after' | null;
}

defineProps<{
  /** 列设置项列表 */
  items: ColumnSettingItem[];
  /** 拖拽状态 */
  dragState: DragState;
}>();

defineEmits<{
  /** 切换列显示/隐藏 */
  (e: 'toggle-visible', key: string, visible: boolean): void;
  /** 移动列（-1 上移，1 下移） */
  (e: 'move', key: string, direction: -1 | 1): void;
  /** 切换列固定 */
  (e: 'toggle-fixed', key: string): void;
  /** 拖拽开始 */
  (e: 'drag-start', key: string, event: DragEvent): void;
  /** 拖拽经过 */
  (e: 'drag-over', key: string, event: DragEvent): void;
  /** 拖拽放置 */
  (e: 'drop', key: string, event: DragEvent): void;
  /** 拖拽结束 */
  (e: 'drag-end'): void;
  /** 恢复默认 */
  (e: 'reset'): void;
}>();
</script>

<style scoped>
/*
 * ════════════════════════════════════════════════════════════════
 * 列设置面板样式（与授权治理 table-column-setting-panel 完全一致）
 * ────────────────────────────────────────────────────────────────
 * 设计要素：
 * 1. 280px 紧凑宽度，渐变背景玻璃质感
 * 2. 卡片式列项（带边框和圆角），舒适的点击区域
 * 3. 精致拖拽手柄、固定按钮渐变高亮
 * 4. 美化滚动条（Webkit + Firefox）
 * 5. 开关使用主题色，清晰可见
 * ════════════════════════════════════════════════════════════════
 */

/* ── 面板容器：渐变背景 + 玻璃质感 ── */
.biz-col-setting {
  width: 280px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(209, 220, 235, 0.9);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 250, 255, 0.96));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.96),
    0 12px 24px rgba(16, 43, 82, 0.12);
}

/* ── 面板头部 ── */
.biz-col-setting__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.biz-col-setting__title {
  color: #1e293b;
  font-size: 13px;
  font-weight: 700;
}

.biz-col-setting__reset {
  font-size: 12px;
  color: #1769ff;
}

/* ── 列列表容器 ── */
.biz-col-setting__list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 320px;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px;
  /* Firefox 滚动条 */
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 rgba(241, 245, 249, 0.6);
}

/* Webkit 浏览器滚动条 */
.biz-col-setting__list::-webkit-scrollbar {
  width: 6px;
}

.biz-col-setting__list::-webkit-scrollbar-track {
  background: rgba(241, 245, 249, 0.6);
  border-radius: 10px;
  margin: 4px 0;
}

.biz-col-setting__list::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #cbd5e1 0%, #94a3b8 100%);
  border-radius: 10px;
  transition: background 0.3s ease;
}

.biz-col-setting__list::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #94a3b8 0%, #64748b 100%);
}

.biz-col-setting__list::-webkit-scrollbar-thumb:active {
  background: linear-gradient(180deg, #64748b 0%, #475569 100%);
}

/* ── 列项：卡片式边框，舒适点击区域 ── */
.biz-col-setting__item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  padding: 6px 8px;
  min-height: 36px;
  border-radius: 6px;
  border: 1px solid rgba(220, 230, 243, 0.92);
  background: rgba(255, 255, 255, 0.92);
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.biz-col-setting__item.is-draggable {
  cursor: default;
}

/* 拖拽源（正在被拖拽的列） */
.biz-col-setting__item.is-drag-source {
  opacity: 0.65;
  border-color: rgba(96, 146, 205, 0.5);
}

/* 拖拽目标：插入指示线（蓝绿渐变） */
.biz-col-setting__item.is-drag-over-before::before,
.biz-col-setting__item.is-drag-over-after::after {
  content: '';
  position: absolute;
  left: 10px;
  right: 10px;
  height: 2px;
  border-radius: 2px;
  background: linear-gradient(90deg, #2f80ed, #22b5a5);
}

.biz-col-setting__item.is-drag-over-before::before {
  top: -1px;
}

.biz-col-setting__item.is-drag-over-after::after {
  bottom: -1px;
}

/* ── 左侧标签区 ── */
.biz-col-setting__label {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  flex: 1;
}

/* 拖拽手柄：方块图标，悬浮高亮 */
.biz-col-setting__drag-handle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 4px;
  color: #94a3b8;
  cursor: grab;
  user-select: none;
  flex: 0 0 auto;
  transition: color 0.2s ease, background-color 0.2s ease;
  font-size: 11px;
}

.biz-col-setting__drag-handle:hover {
  color: #2563eb;
  background: rgba(37, 99, 235, 0.12);
}

.biz-col-setting__drag-handle:active {
  cursor: grabbing;
}

/* 列名 */
.biz-col-setting__col-name {
  color: #334155;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 锁定标记 */
.biz-col-setting__locked-badge {
  padding: 1px 4px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.9);
  color: #64748b;
  font-size: 9px;
  font-weight: 700;
  flex-shrink: 0;
}

/* ── 右侧操作区 ── */
.biz-col-setting__actions {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  flex-shrink: 0;
}

/* 开关：使用主题色 */
.biz-col-setting__actions :deep(.arco-switch) {
  flex-shrink: 0;
}

.biz-col-setting__actions :deep(.arco-switch-checked) {
  background-color: var(--color-primary, #165dff) !important;
}

.biz-col-setting__actions :deep(.arco-switch-checked:hover) {
  background-color: var(--color-primary-light-4, #4080ff) !important;
}

.biz-col-setting__actions :deep(.arco-switch:not(.arco-switch-checked)) {
  background-color: #e5e6eb !important;
}

.biz-col-setting__actions :deep(.arco-switch:not(.arco-switch-checked):hover) {
  background-color: #c9cdd4 !important;
}

/* 上移/下移 + 固定按钮通用尺寸 */
.biz-col-setting__order-btn.arco-btn,
.biz-col-setting__fixed-btn.arco-btn {
  width: 20px !important;
  min-width: 20px !important;
  height: 20px !important;
  padding: 0 !important;
  border-radius: 4px !important;
  font-size: 11px !important;
}

/* 上移/下移按钮 */
.biz-col-setting__order-btn.arco-btn {
  color: #64748b;
}

/* 固定按钮：渐变背景 */
.biz-col-setting__fixed-btn.arco-btn {
  color: #64748b;
  border-color: rgba(203, 213, 225, 0.88);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 249, 255, 0.96));
}

.biz-col-setting__fixed-btn.arco-btn:not(:disabled):hover {
  color: #1d4ed8;
  border-color: rgba(59, 130, 246, 0.52);
}

/* 固定按钮激活态：蓝色渐变高亮 */
.biz-col-setting__fixed-btn.arco-btn.is-active {
  color: #1d4ed8;
  border-color: rgba(59, 130, 246, 0.62);
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.95), rgba(224, 242, 254, 0.95));
  box-shadow: inset 0 0 0 1px rgba(147, 197, 253, 0.48);
}

/* 固定按钮禁用态 */
.biz-col-setting__fixed-btn.arco-btn:disabled,
.biz-col-setting__fixed-btn.arco-btn.is-active:disabled {
  color: #94a3b8;
  border-color: rgba(226, 232, 240, 0.9);
  background: rgba(248, 250, 252, 0.96);
  box-shadow: none;
}
</style>
