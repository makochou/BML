<template>
  <!--
    BML 通用弹窗组件
    ─────────────────
    功能：拖拽移动 / 八方向缩放 / 全屏切换 / 自适应布局
    用法：替换 <a-modal>，通过 v-model:visible 控制显隐，
         通过 slot 填充 header-extra / default(body) / footer 区域。

    设计理念：
    1. 顶部标题栏固定不滚动，可拖拽移动弹窗；
    2. 中间内容区自动出现滚动条（仅垂直方向）；
    3. 底部操作栏（footer slot）固定不滚动，始终可见；
    4. 全屏时自动铺满视口，退出全屏恢复原始尺寸和位置。
  -->
  <teleport to="body">
    <div v-if="visible" class="bml-modal-overlay" @mousedown.self="handleMaskClick">
      <!-- 遮罩层 -->
      <div class="bml-modal-overlay__mask"></div>

      <!-- 弹窗主体 -->
      <div
        ref="modalRef"
        class="bml-modal"
        :class="{
          'bml-modal--fullscreen': controller.fullscreen.value,
          'bml-modal--dragging': controller.isDragging.value,
          'bml-modal--resizing': controller.isResizing.value
        }"
        :style="controller.modalStyle.value"
      >
        <!-- ── 缩放热区（八个方向） ── -->
        <div class="bml-modal-resize bml-modal-resize--n" @mousedown.stop.prevent="e => controller.startResize('n', e)"></div>
        <div class="bml-modal-resize bml-modal-resize--s" @mousedown.stop.prevent="e => controller.startResize('s', e)"></div>
        <div class="bml-modal-resize bml-modal-resize--e" @mousedown.stop.prevent="e => controller.startResize('e', e)"></div>
        <div class="bml-modal-resize bml-modal-resize--w" @mousedown.stop.prevent="e => controller.startResize('w', e)"></div>
        <div class="bml-modal-resize bml-modal-resize--ne" @mousedown.stop.prevent="e => controller.startResize('ne', e)"></div>
        <div class="bml-modal-resize bml-modal-resize--nw" @mousedown.stop.prevent="e => controller.startResize('nw', e)"></div>
        <div class="bml-modal-resize bml-modal-resize--se" @mousedown.stop.prevent="e => controller.startResize('se', e)"></div>
        <div class="bml-modal-resize bml-modal-resize--sw" @mousedown.stop.prevent="e => controller.startResize('sw', e)"></div>

        <!-- ── 顶部标题栏（固定不滚动，可拖拽） ── -->
        <div class="bml-modal__header" @mousedown="controller.startDrag">
          <div class="bml-modal__header-left">
            <strong class="bml-modal__title">{{ title }}</strong>
            <!-- 标题右侧额外内容插槽（如副标题、标签等） -->
            <slot name="header-extra"></slot>
          </div>
          <div class="bml-modal__header-actions">
            <!-- 自定义操作按钮区域（表单的「取消/确定」等按钮提升到标题栏右侧） -->
            <div v-if="$slots['header-actions']" class="bml-modal__header-form-actions">
              <slot name="header-actions"></slot>
            </div>
            <a-tooltip :content="controller.fullscreen.value ? '退出全屏' : '全屏展示'">
              <button class="bml-modal__action-btn" type="button" @click.stop="controller.toggleFullscreen()">
                <component :is="controller.fullscreen.value ? IconFullscreenExit : IconFullscreen" />
              </button>
            </a-tooltip>
            <a-tooltip content="关闭窗口">
              <button class="bml-modal__action-btn bml-modal__action-btn--close" type="button" @click.stop="handleClose">
                <icon-close />
              </button>
            </a-tooltip>
          </div>
        </div>

        <!-- ── 中间内容区（自动垂直滚动） ── -->
        <div class="bml-modal__body" @wheel.stop>
          <slot></slot>
        </div>

        <!-- ── 底部操作栏（固定不滚动） ── -->
        <div v-if="$slots.footer" class="bml-modal__footer">
          <slot name="footer"></slot>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script lang="ts" setup>
/**
 * BML 通用弹窗组件。
 *
 * 提供与授权治理新建 API 账号弹窗一致的交互体验：
 * - 自由拖拽移动位置
 * - 八方向缩放调整大小
 * - 全屏 / 退出全屏切换
 * - 标题栏 / 内容区 / 底部栏三段式布局
 *
 * Props:
 *  - visible: 是否显示弹窗
 *  - title: 弹窗标题
 *  - width: 初始宽度 (px)
 *  - height: 初始高度 (px)，默认按内容自适应
 *  - minWidth: 最小宽度 (px)
 *  - minHeight: 最小高度 (px)
 *  - maskClosable: 点击遮罩是否关闭
 *
 * Events:
 *  - update:visible: 关闭弹窗时触发
 *  - close: 关闭弹窗时触发
 *
 * Slots:
 *  - default: 弹窗主体内容
 *  - header-extra: 标题右侧额外内容
 *  - header-actions: 标题栏操作按钮（位于全屏/关闭按钮左侧）
 *  - footer: 底部操作栏
 */
import { watch, ref, nextTick } from 'vue';
import { IconClose, IconFullscreen, IconFullscreenExit } from '@arco-design/web-vue/es/icon';
import { useResizableModal } from '../composables/useResizableModal';

const props = withDefaults(defineProps<{
  /** 是否显示弹窗 */
  visible: boolean;
  /** 弹窗标题 */
  title?: string;
  /** 初始宽度 (px) */
  width?: number;
  /** 初始高度 (px)，0 表示按内容自适应 */
  height?: number;
  /** 最小宽度 (px) */
  minWidth?: number;
  /** 最小高度 (px) */
  minHeight?: number;
  /** 点击遮罩是否关闭弹窗 */
  maskClosable?: boolean;
}>(), {
  title: '',
  width: 640,
  height: 520,
  minWidth: 360,
  minHeight: 240,
  maskClosable: false
});

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void;
  (e: 'close'): void;
}>();

const modalRef = ref<HTMLElement | null>(null);

/** 拖拽 + 缩放 + 全屏控制器（复用已有 composable） */
const controller = useResizableModal({
  initialWidth: props.width,
  initialHeight: props.height,
  minWidth: props.minWidth,
  minHeight: props.minHeight,
  gap: 24
});

/** 弹窗打开时居中定位 */
watch(() => props.visible, async (val) => {
  if (val) {
    controller.fullscreen.value = false;
    await nextTick();
    controller.recenter({ width: props.width, height: props.height, force: true });
  }
});

/** 关闭弹窗 */
function handleClose() {
  emit('update:visible', false);
  emit('close');
}

/** 点击遮罩区域 */
function handleMaskClick() {
  if (props.maskClosable) {
    handleClose();
  }
}
</script>

<style>
/* ════════════════════════════════════════════════════════════
 * BML 通用弹窗样式（非 scoped，以便外部可通过类名覆盖微调）
 * ════════════════════════════════════════════════════════════ */

/* ── 遮罩层 ── */
.bml-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 遮罩层：仅半透明遮罩，不模糊背景内容 */
.bml-modal-overlay__mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
}

/* ── 弹窗主体 ── */
.bml-modal {
  position: fixed;
  z-index: 1001;
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(226, 232, 240, 0.7);
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  box-shadow:
    0 32px 80px rgba(15, 23, 42, 0.18),
    0 8px 20px rgba(15, 23, 42, 0.06);
  overflow: hidden;
  /* 防止弹窗溢出视口 */
  max-width: 100vw;
  max-height: 100vh;
}

/* 拖拽 / 缩放中禁用过渡动画 */
.bml-modal--dragging,
.bml-modal--resizing {
  transition: none !important;
}

.bml-modal--dragging * {
  cursor: grabbing !important;
}

/* 全屏模式 */
.bml-modal--fullscreen {
  border-radius: 0;
  border: none;
}

/* ── 缩放热区 ── */
.bml-modal-resize {
  position: absolute;
  z-index: 10;
}

.bml-modal-resize--n  { top: -4px; left: 8px; right: 8px; height: 8px; cursor: n-resize; }
.bml-modal-resize--s  { bottom: -4px; left: 8px; right: 8px; height: 8px; cursor: s-resize; }
.bml-modal-resize--e  { top: 8px; right: -4px; bottom: 8px; width: 8px; cursor: e-resize; }
.bml-modal-resize--w  { top: 8px; left: -4px; bottom: 8px; width: 8px; cursor: w-resize; }
.bml-modal-resize--ne { top: -4px; right: -4px; width: 14px; height: 14px; cursor: ne-resize; }
.bml-modal-resize--nw { top: -4px; left: -4px; width: 14px; height: 14px; cursor: nw-resize; }
.bml-modal-resize--se { bottom: -4px; right: -4px; width: 14px; height: 14px; cursor: se-resize; }
.bml-modal-resize--sw { bottom: -4px; left: -4px; width: 14px; height: 14px; cursor: sw-resize; }

/* 全屏时隐藏缩放热区 */
.bml-modal--fullscreen .bml-modal-resize { display: none; }

/* ── 顶部标题栏 ── */
.bml-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
  height: 40px;
  padding: 0 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  background: linear-gradient(180deg, #ffffff, #f8faff);
  cursor: grab;
  user-select: none;
}

.bml-modal--dragging .bml-modal__header {
  cursor: grabbing;
}

.bml-modal__header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.bml-modal__title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bml-modal__header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

/* 标题栏操作按钮 */
.bml-modal__action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 16px;
}

.bml-modal__action-btn:hover {
  background: rgba(241, 245, 249, 0.9);
  color: #334155;
  border-color: rgba(203, 213, 225, 0.6);
}

.bml-modal__action-btn--close:hover {
  background: rgba(239, 68, 68, 0.08);
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.2);
}

/* ── 中间内容区 ── */
.bml-modal__body {
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 14px 18px;
  min-height: 0;
}

/* 精美自定义滚动条 */
.bml-modal__body::-webkit-scrollbar {
  width: 8px;
}

.bml-modal__body::-webkit-scrollbar-track {
  background: transparent;
}

.bml-modal__body::-webkit-scrollbar-thumb {
  border: 2px solid transparent;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(148, 163, 184, 0.7), rgba(100, 116, 139, 0.7));
  background-clip: padding-box;
}

.bml-modal__body::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(100, 116, 139, 0.9), rgba(71, 85, 105, 0.9));
  background-clip: padding-box;
}

/* ── 底部操作栏 ── */
.bml-modal__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-shrink: 0;
  padding: 10px 18px;
  border-top: 1px solid rgba(226, 232, 240, 0.8);
  background: linear-gradient(180deg, #f8faff, #ffffff);
}

/* 底部按钮默认美化 */
.bml-modal__footer :deep(.arco-btn) {
  min-width: 96px;
  border-radius: 10px;
  font-weight: 600;
}

.bml-modal__footer :deep(.arco-btn:not(.arco-btn-primary):not(.arco-btn-status-danger)) {
  border-color: rgba(203, 213, 225, 0.9);
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  color: #334155;
}

.bml-modal__footer :deep(.arco-btn:not(.arco-btn-primary):not(.arco-btn-status-danger):hover) {
  border-color: rgba(47, 109, 246, 0.4);
  color: #1d4ed8;
}

.bml-modal__footer :deep(.arco-btn-primary:not(.arco-btn-status-danger)) {
  border: 0;
  background: linear-gradient(135deg, #2f6df6 0%, #1f8bff 46%, #12b8a6 100%);
  box-shadow: 0 8px 20px rgba(47, 109, 246, 0.22);
}

.bml-modal__footer :deep(.arco-btn-primary:not(.arco-btn-status-danger):hover) {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(47, 109, 246, 0.28);
}

/* ── 全屏模式下标题栏高度顶线装饰 ── */
.bml-modal--fullscreen .bml-modal__header {
  position: relative;
}

.bml-modal--fullscreen .bml-modal__header::after {
  position: absolute;
  bottom: -1px;
  left: 16px;
  right: 16px;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(47, 109, 246, 0.5), rgba(21, 184, 167, 0.5));
  content: '';
}

/* ── 表单在弹窗内的通用美化 ── */
.bml-modal__body :deep(.arco-form-layout-vertical .arco-form-item-label) {
  font-weight: 600;
  color: #334155;
}

.bml-modal__body :deep(.arco-form .arco-input-wrapper),
.bml-modal__body :deep(.arco-form .arco-select-view),
.bml-modal__body :deep(.arco-form .arco-textarea-wrapper),
.bml-modal__body :deep(.arco-form .arco-picker),
.bml-modal__body :deep(.arco-form .arco-input-number) {
  border-radius: 8px;
}

/* ═══════════════════════════════════════════════════════════════
 * 标题栏表单操作按钮区域
 * ═══════════════════════════════════════════════════════════════
 * 用途：将弹窗表单的操作按钮（取消/确定等）提升到标题栏右侧，
 * 节省底部空间，操作更直观。与窗口控制按钮（全屏/关闭）之间
 * 用竖线分隔，视觉层次分明。
 * ═══════════════════════════════════════════════════════════════ */
.bml-modal__header-form-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  /* 右侧竖线分隔符：与窗口控制按钮（全屏/关闭）区分 */
  margin-right: 6px;
  padding-right: 10px;
  border-right: 1px solid rgba(226, 232, 240, 0.8);
}

/* 表单操作按钮 —— 紧凑尺寸，适配标题栏高度 */
.bml-modal__header-form-actions .arco-btn {
  min-width: 68px;
  height: 28px;
  padding: 0 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  line-height: 28px;
}

/* 非主按钮（取消/关闭）—— 低调次要样式 */
.bml-modal__header-form-actions .arco-btn:not(.arco-btn-primary):not(.arco-btn-status-danger):not(.arco-btn-status-warning) {
  border-color: rgba(203, 213, 225, 0.9);
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  color: #334155;
}

.bml-modal__header-form-actions .arco-btn:not(.arco-btn-primary):not(.arco-btn-status-danger):not(.arco-btn-status-warning):hover {
  border-color: rgba(47, 109, 246, 0.4);
  color: #1d4ed8;
}

/* 主按钮（确定/保存）—— 蓝绿渐变，与全站主色一致 */
.bml-modal__header-form-actions .arco-btn-primary:not(.arco-btn-status-danger) {
  border: 0;
  background: linear-gradient(135deg, #2f6df6 0%, #1f8bff 46%, #12b8a6 100%);
  box-shadow: 0 4px 12px rgba(47, 109, 246, 0.22);
  color: #ffffff;
}

.bml-modal__header-form-actions .arco-btn-primary:not(.arco-btn-status-danger):hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(47, 109, 246, 0.28);
}

/* 警告按钮（重置密码等） */
.bml-modal__header-form-actions .arco-btn-status-warning {
  font-size: 12px;
}

/* 危险按钮（删除等） */
.bml-modal__header-form-actions .arco-btn-status-danger {
  font-size: 12px;
}
</style>
