/**
 * 空闲超时自动登出 Composable
 * <p>
 * 监听用户交互事件（鼠标移动、点击、键盘输入、滚动、触摸），
 * 在指定的空闲时长（分钟）后自动执行回调（通常是退出登录）。
 *
 * 使用方式：
 * ```ts
 * const { start, stop } = useIdleTimeout({
 *   timeoutMinutes: 30,
 *   onIdle: () => { /* 执行登出逻辑 *\/ }
 * });
 * onMounted(() => start());
 * onUnmounted(() => stop());
 * ```
 *
 * 设计说明：
 *   - timeoutMinutes <= 0 时不启动检测（即不限制）
 *   - 监听的交互事件覆盖主流用户操作场景
 *   - 使用 setTimeout 重置机制，避免 setInterval 的累积误差
 *   - 提供 start / stop 方法，方便在组件生命周期中调用
 * </p>
 *
 * @author BML Team
 */

import { ref, onUnmounted } from 'vue';

/** Composable 配置参数 */
interface IdleTimeoutOptions {
  /** 空闲超时时长（分钟），<= 0 表示不限制 */
  timeoutMinutes: number;
  /** 空闲超时后的回调函数 */
  onIdle: () => void;
}

/** 需要监听的用户交互事件列表 */
const ACTIVITY_EVENTS: (keyof WindowEventMap)[] = [
  'mousemove',
  'mousedown',
  'keydown',
  'scroll',
  'touchstart',
  'pointerdown'
];

export function useIdleTimeout(options: IdleTimeoutOptions) {
  /** 定时器 ID */
  let timer: ReturnType<typeof setTimeout> | null = null;

  /** 是否已启动 */
  const isRunning = ref(false);

  /** 将分钟转换为毫秒 */
  const getTimeoutMs = () => options.timeoutMinutes * 60 * 1000;

  /** 重置倒计时（每次用户操作时调用） */
  const resetTimer = () => {
    if (!isRunning.value) return;
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
      if (isRunning.value) {
        options.onIdle();
      }
    }, getTimeoutMs());
  };

  /**
   * 启动空闲检测
   * 仅在 timeoutMinutes > 0 时生效
   */
  const start = () => {
    if (options.timeoutMinutes <= 0) return;
    if (isRunning.value) return;

    isRunning.value = true;

    // 注册用户交互事件监听（使用 passive 提升性能）
    ACTIVITY_EVENTS.forEach(event => {
      window.addEventListener(event, resetTimer, { passive: true });
    });

    // 启动首次倒计时
    resetTimer();
  };

  /** 停止空闲检测，清除定时器和事件监听 */
  const stop = () => {
    isRunning.value = false;
    if (timer) {
      clearTimeout(timer);
      timer = null;
    }
    ACTIVITY_EVENTS.forEach(event => {
      window.removeEventListener(event, resetTimer);
    });
  };

  // 组件卸载时自动清理
  onUnmounted(() => {
    stop();
  });

  return { start, stop, isRunning };
}
