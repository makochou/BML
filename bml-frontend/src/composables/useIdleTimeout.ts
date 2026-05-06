/**
 * 空闲超时自动登出 Composable（业务系统专用）
 * <p>
 * 监听用户交互事件（鼠标移动、点击、键盘输入、滚动、触摸），
 * 在指定的空闲时长（分钟）后自动执行回调（通常是退出登录）。
 *
 * <h3>持久化机制：</h3>
 * 每次用户活动时，将最后活动时间戳写入 localStorage。
 * 启动检测时，先比较 localStorage 中的上次活动时间与当前时间差：
 * 若已超过配置的超时时长（如关闭浏览器 / 停止服务后再启动），
 * 则<b>立即触发登出</b>，而非重新倒计时。
 * 这确保了超时策略在页面刷新、浏览器重启、服务重启等场景下依然有效。
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
 *   - 使用节流（1 秒）控制 localStorage 写入频率，避免性能开销
 *   - 提供 start / stop 方法，方便在组件生命周期中调用
 * </p>
 *
 * @author BML Team
 */

import { ref, onUnmounted } from 'vue';
import { setLastActivityTime, isSessionIdleExpired } from '../utils/auth';

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

  /** 节流定时器（控制 localStorage 写入频率） */
  let throttleTimer: ReturnType<typeof setTimeout> | null = null;

  /** 是否已启动 */
  const isRunning = ref(false);

  /** 将分钟转换为毫秒 */
  const getTimeoutMs = () => options.timeoutMinutes * 60 * 1000;

  /**
   * 重置倒计时（每次用户操作时调用）。
   * 同时将当前时间持久化到 localStorage，供下次启动时判断是否已超时。
   */
  const resetTimer = () => {
    if (!isRunning.value) return;

    // 节流写入 localStorage（1 秒内最多写一次，减少 IO 开销）
    if (!throttleTimer) {
      setLastActivityTime();
      throttleTimer = setTimeout(() => {
        throttleTimer = null;
      }, 1000);
    }

    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
      if (isRunning.value) {
        options.onIdle();
      }
    }, getTimeoutMs());
  };

  /**
   * 启动空闲检测
   * <p>
   * 1. 先检查 localStorage 中持久化的上次活动时间，若已超时则立即触发登出
   * 2. 未超时则注册事件监听并启动倒计时
   * </p>
   */
  const start = () => {
    if (options.timeoutMinutes <= 0) return;
    if (isRunning.value) return;

    // ── 关键修复：检查持久化的最后活动时间 ──
    // 场景：用户关闭浏览器/停止服务后再打开，localStorage 中的时间戳已过期
    if (isSessionIdleExpired(options.timeoutMinutes)) {
      console.info('[IdleTimeout] 上次活动距今已超过', options.timeoutMinutes, '分钟，立即触发登出');
      options.onIdle();
      return;
    }

    isRunning.value = true;

    // 记录启动时间作为初始活动时间
    setLastActivityTime();

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
    if (throttleTimer) {
      clearTimeout(throttleTimer);
      throttleTimer = null;
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
