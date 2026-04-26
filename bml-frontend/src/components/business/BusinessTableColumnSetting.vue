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
/* ── 面板容器 ── */
.biz-col-setting {
  width: 260px;
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
}

/* ── 面板头部 ── */
.biz-col-setting__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px 8px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  background: linear-gradient(180deg, #f8faff, #ffffff);
}

.biz-col-setting__title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.biz-col-setting__reset {
  font-size: 12px;
  color: #1769ff;
}

/* ── 列列表 ── */
.biz-col-setting__list {
  max-height: 360px;
  overflow-y: auto;
  padding: 4px 0;
}

/* ── 列项 ── */
.biz-col-setting__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 5px 10px;
  border-radius: 6px;
  margin: 1px 4px;
  transition: background 0.15s ease;
  position: relative;
}

.biz-col-setting__item:hover {
  background: rgba(23, 105, 255, 0.04);
}

/* 拖拽源（正在被拖拽的列） */
.biz-col-setting__item.is-drag-source {
  opacity: 0.5;
  background: rgba(23, 105, 255, 0.06);
}

/* 拖拽目标：上方插入指示线 */
.biz-col-setting__item.is-drag-over-before::before {
  content: '';
  position: absolute;
  top: -1px;
  left: 8px;
  right: 8px;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, #1769ff, #12b8a6);
}

/* 拖拽目标：下方插入指示线 */
.biz-col-setting__item.is-drag-over-after::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 8px;
  right: 8px;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, #1769ff, #12b8a6);
}

/* ── 左侧标签区 ── */
.biz-col-setting__label {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex: 1;
}

/* 拖拽手柄 */
.biz-col-setting__drag-handle {
  display: inline-flex;
  align-items: center;
  color: #94a3b8;
  cursor: grab;
  font-size: 14px;
  flex-shrink: 0;
  transition: color 0.15s ease;
}

.biz-col-setting__drag-handle:hover {
  color: #1769ff;
}

.biz-col-setting__drag-handle:active {
  cursor: grabbing;
}

/* 列名 */
.biz-col-setting__col-name {
  font-size: 12px;
  color: #334155;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 锁定标记 */
.biz-col-setting__locked-badge {
  font-size: 10px;
  color: #94a3b8;
  background: rgba(148, 163, 184, 0.12);
  padding: 1px 5px;
  border-radius: 4px;
  flex-shrink: 0;
}

/* ── 右侧操作区 ── */
.biz-col-setting__actions {
  display: flex;
  align-items: center;
  gap: 3px;
  flex-shrink: 0;
}

/* 上移/下移按钮 */
.biz-col-setting__order-btn.arco-btn {
  width: 22px;
  height: 22px;
  padding: 0;
  border-radius: 5px;
  border-color: rgba(203, 213, 225, 0.7);
  background: rgba(255, 255, 255, 0.9);
  color: #64748b;
  font-size: 11px;
}

.biz-col-setting__order-btn.arco-btn:not(:disabled):hover {
  border-color: rgba(23, 105, 255, 0.4);
  color: #1769ff;
}

/* 固定按钮 */
.biz-col-setting__fixed-btn.arco-btn {
  width: 22px;
  height: 22px;
  padding: 0;
  border-radius: 5px;
  border-color: rgba(203, 213, 225, 0.7);
  background: rgba(255, 255, 255, 0.9);
  color: #94a3b8;
  font-size: 11px;
}

.biz-col-setting__fixed-btn.arco-btn.is-active {
  border-color: rgba(23, 105, 255, 0.5);
  background: rgba(23, 105, 255, 0.08);
  color: #1769ff;
}

.biz-col-setting__fixed-btn.arco-btn:not(:disabled):hover {
  border-color: rgba(23, 105, 255, 0.4);
  color: #1769ff;
}
</style>
