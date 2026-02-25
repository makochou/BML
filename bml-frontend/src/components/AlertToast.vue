<template>
  <!--
    右下角告警弹窗组件 (AlertToast)

    功能说明：
    - 监听 notificationStore.newAlerts 队列，有新告警时自动弹出
    - 每条弹窗停留 5 秒后自动消失（带滑出动画）
    - 队列模式：上一条消失后才弹出下一条，不叠加
    - 支持手动关闭和点击跳转
    - 显示告警级别图标（warning/error/critical 不同颜色）
  -->
  <Teleport to="body">
    <Transition name="toast-slide">
      <div
        v-if="currentAlert"
        :class="['alert-toast', `toast-${currentAlert.alertLevel}`]"
        @click="handleClick"
      >
        <!-- 关闭按钮 -->
        <div class="toast-close" @click.stop="dismissToast">
          <icon-close />
        </div>

        <!-- 级别顶部色带 -->
        <div :class="['toast-stripe', `stripe-${currentAlert.alertLevel}`]"></div>

        <!-- 主体内容 -->
        <div class="toast-body">
          <!-- 左侧图标 -->
          <div :class="['toast-icon', `icon-${currentAlert.alertLevel}`]">
            <icon-exclamation-circle v-if="currentAlert.alertLevel === 'critical'" />
            <icon-close-circle v-else-if="currentAlert.alertLevel === 'error'" />
            <icon-exclamation v-else-if="currentAlert.alertLevel === 'warning'" />
            <icon-info-circle v-else />
          </div>

          <!-- 右侧文字 -->
          <div class="toast-text">
            <div class="toast-label">系统告警</div>
            <div class="toast-title">{{ currentAlert.alertTitle }}</div>
            <div class="toast-desc">{{ currentAlert.alertContent }}</div>
            <div class="toast-time">{{ currentAlert.createTime }}</div>
          </div>
        </div>

        <!-- 自动消失进度条 -->
        <div class="toast-progress">
          <div
            class="toast-progress-bar"
            :class="`bar-${currentAlert.alertLevel}`"
            :style="{ animationDuration: `${TOAST_DURATION}ms` }"
          ></div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script lang="ts" setup>
/**
 * AlertToast — 右下角告警弹窗通知组件
 *
 * 工作机制：
 * 1. 通过 watch 监听 notificationStore.newAlerts 队列长度变化
 * 2. 队列有数据时，consumeNewAlert() 取出一条显示
 * 3. 启动 5 秒倒计时定时器
 * 4. 倒计时结束或手动关闭后，检查队列是否还有数据，有则继续弹出
 *
 * 挂载位置：Layout.vue 模板底部
 */
import { ref, watch, onUnmounted } from 'vue';
import {
  IconClose,
  IconExclamationCircle,
  IconCloseCircle,
  IconExclamation,
  IconInfoCircle,
} from '@arco-design/web-vue/es/icon';
import { useNotificationStore, type AlertItem } from '../store/notification';

/** Toast 停留时长：5 秒 */
const TOAST_DURATION = 5000;

const notificationStore = useNotificationStore();

/** 当前正在展示的告警 */
const currentAlert = ref<AlertItem | null>(null);

/** 自动消失定时器 */
let dismissTimer: number | null = null;

/**
 * 展示下一条告警
 * 从 newAlerts 队列中取出一条，启动倒计时
 */
const showNext = () => {
  // 清除可能残留的定时器
  if (dismissTimer) {
    clearTimeout(dismissTimer);
    dismissTimer = null;
  }

  const alert = notificationStore.consumeNewAlert();
  if (alert) {
    currentAlert.value = alert;
    // 启动 5 秒自动消失
    dismissTimer = window.setTimeout(() => {
      dismissToast();
    }, TOAST_DURATION);
  }
};

/**
 * 关闭当前 Toast，并检查队列是否还有下一条
 */
const dismissToast = () => {
  currentAlert.value = null;
  if (dismissTimer) {
    clearTimeout(dismissTimer);
    dismissTimer = null;
  }
  // 延迟 300ms（等动画完成）后展示下一条
  setTimeout(() => {
    if (notificationStore.newAlerts.length > 0) {
      showNext();
    }
  }, 350);
};

/**
 * 点击 Toast → 标记已读 + 关闭
 */
const handleClick = () => {
  if (currentAlert.value && currentAlert.value.readStatus === 0) {
    notificationStore.markAsRead(currentAlert.value.id);
  }
  dismissToast();
};

/**
 * 监听 newAlerts 队列变化
 * 当队列有新数据且当前没有正在展示的 Toast 时，触发展示
 */
watch(
  () => notificationStore.newAlerts.length,
  (newLen) => {
    if (newLen > 0 && !currentAlert.value) {
      showNext();
    }
  }
);

onUnmounted(() => {
  if (dismissTimer) {
    clearTimeout(dismissTimer);
  }
});
</script>

<style scoped>
/* ==================== Toast 容器 ==================== */
.alert-toast {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 380px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 20px 60px -12px rgba(0, 0, 0, 0.2),
              0 0 0 1px rgba(255, 255, 255, 0.6);
  overflow: hidden;
  cursor: pointer;
  z-index: 9999;
  transition: all 0.3s ease;
}

.alert-toast:hover {
  transform: translateY(-2px);
  box-shadow: 0 24px 72px -12px rgba(0, 0, 0, 0.25),
              0 0 0 1px rgba(255, 255, 255, 0.8);
}

/* ==================== 顶部色带 ==================== */
.toast-stripe {
  height: 3px;
  width: 100%;
}
.toast-stripe.stripe-info { background: linear-gradient(90deg, #165dff, #4080ff); }
.toast-stripe.stripe-warning { background: linear-gradient(90deg, #ff7d00, #ffb65d); }
.toast-stripe.stripe-error { background: linear-gradient(90deg, #f53f3f, #ff7875); }
.toast-stripe.stripe-critical { background: linear-gradient(90deg, #cb2634, #f53f3f); }

/* ==================== 主体 ==================== */
.toast-body {
  display: flex;
  padding: 16px 44px 14px 16px;
  gap: 14px;
}

/* 图标 */
.toast-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}
.toast-icon.icon-info { background: rgba(22, 93, 255, 0.1); color: #165dff; }
.toast-icon.icon-warning { background: rgba(255, 125, 0, 0.1); color: #ff7d00; }
.toast-icon.icon-error { background: rgba(245, 63, 63, 0.1); color: #f53f3f; }
.toast-icon.icon-critical { background: rgba(203, 38, 52, 0.15); color: #cb2634; }

/* 文字 */
.toast-text {
  flex: 1;
  min-width: 0;
}
.toast-label {
  font-size: 11px;
  color: #86909c;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 2px;
}
.toast-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.toast-desc {
  font-size: 13px;
  color: #4e5969;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.toast-time {
  font-size: 11px;
  color: #c9cdd4;
  margin-top: 6px;
  font-family: 'Courier New', monospace;
}

/* ==================== 关闭按钮 ==================== */
.toast-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #86909c;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 1;
}
.toast-close:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #1d2129;
}

/* ==================== 底部进度条（自动消失倒计时） ==================== */
.toast-progress {
  height: 3px;
  background: rgba(0, 0, 0, 0.04);
}
.toast-progress-bar {
  height: 100%;
  width: 100%;
  animation: progress-shrink linear forwards;
  transform-origin: left;
}
.toast-progress-bar.bar-info { background: #165dff; }
.toast-progress-bar.bar-warning { background: #ff7d00; }
.toast-progress-bar.bar-error { background: #f53f3f; }
.toast-progress-bar.bar-critical { background: #cb2634; }

@keyframes progress-shrink {
  from { transform: scaleX(1); }
  to { transform: scaleX(0); }
}

/* ==================== 进入/离开动画 ==================== */
.toast-slide-enter-active {
  animation: toast-in 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.toast-slide-leave-active {
  animation: toast-out 0.3s ease-in forwards;
}

@keyframes toast-in {
  from {
    opacity: 0;
    transform: translateX(100px) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}
@keyframes toast-out {
  from {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateX(100px) scale(0.9);
  }
}

/* ==================== 深色模式 ==================== */
:global(body[arco-theme='dark']) .alert-toast {
  background: rgba(35, 35, 36, 0.95);
  border-color: rgba(255, 255, 255, 0.08);
}
:global(body[arco-theme='dark']) .toast-title { color: #f2f3f5; }
:global(body[arco-theme='dark']) .toast-desc { color: #86909c; }
:global(body[arco-theme='dark']) .toast-close:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #f2f3f5;
}
</style>
