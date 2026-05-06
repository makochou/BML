/**
 * 中台管理员空闲超时检测 Composable
 * <p>
 * 用于中台管理平台的空闲超时控制，与业务系统的 {@link useIdleTimeout} 独立。
 * 超时时长从后端配置文件 {@code application.yml} 中的 {@code bml.admin.session-timeout-minutes} 读取。
 * </p>
 *
 * <h3>持久化机制：</h3>
 * 每次用户活动时，将最后活动时间戳写入 localStorage（admin 作用域）。
 * 启动检测时，先比较 localStorage 中的上次活动时间与当前时间差：
 * 若已超过配置的超时时长（如关闭浏览器 / 停止服务后再启动），
 * 则<b>立即触发登出</b>，而非重新倒计时。
 * 这确保了超时策略在页面刷新、浏览器重启、服务重启等场景下依然有效。
 *
 * <h3>工作原理：</h3>
 * <ol>
 *   <li>组件挂载时调用 {@code start()}，从后端获取超时配置</li>
 *   <li>检查 localStorage 中持久化的上次活动时间，若已超时则立即登出</li>
 *   <li>监听用户活动事件（鼠标移动、键盘输入、点击、滚动、触摸）</li>
 *   <li>每次活动重置定时器并更新 localStorage 中的时间戳</li>
 *   <li>超时后触发 {@code onIdle} 回调，执行登出操作</li>
 *   <li>组件卸载时调用 {@code stop()}，清理定时器和事件监听器</li>
 * </ol>
 *
 * <h3>使用方式：</h3>
 * <pre>
 * const { start, stop } = useAdminIdleTimeout({
 *   onIdle: () => {
 *     Message.warning('会话已超时，请重新登录');
 *     handleLogout();
 *   }
 * });
 *
 * onMounted(() => {
 *   start(); // 启动空闲检测
 * });
 *
 * onUnmounted(() => {
 *   stop(); // 清理资源
 * });
 * </pre>
 *
 * <h3>与业务系统的区别：</h3>
 * <ul>
 *   <li>中台管理员：超时配置在 {@code application.yml}，通过后端接口获取</li>
 *   <li>业务系统用户：超时配置在数据库 {@code sys_config} 表，通过 {@code /auth/login/config} 获取</li>
 * </ul>
 *
 * @author BML Team
 */

import { ref, onUnmounted } from 'vue';
import request from '../utils/request';
import { setLastActivityTime, isSessionIdleExpired } from '../utils/auth';

/**
 * Composable 配置参数
 */
interface AdminIdleTimeoutOptions {
  /**
   * 空闲超时回调函数
   * <p>
   * 当用户空闲时间超过配置的超时时长时触发。
   * 通常在此回调中执行登出操作。
   * </p>
   */
  onIdle: () => void;
}

/**
 * 需要监听的用户活动事件列表
 * <p>
 * 涵盖鼠标、键盘、触摸、滚动等常见交互行为。
 * 任何一个事件触发都会重置空闲计时器。
 * </p>
 */
const ACTIVITY_EVENTS: (keyof WindowEventMap)[] = [
  'mousedown',   // 鼠标按下
  'mousemove',   // 鼠标移动
  'keydown',     // 键盘按下
  'scroll',      // 页面滚动
  'touchstart',  // 触摸开始
  'click'        // 点击事件
];

/**
 * 中台管理员空闲超时检测 Composable
 *
 * @param options 配置选项
 * @returns 包含 start 和 stop 方法的对象
 */
export function useAdminIdleTimeout(options: AdminIdleTimeoutOptions) {
  /** 定时器 ID */
  let timer: ReturnType<typeof setTimeout> | null = null;

  /** 超时时长（分钟），从后端配置获取 */
  const timeoutMinutes = ref<number>(30);

  /** 是否已启动检测 */
  const isStarted = ref(false);

  /**
   * 重置空闲计时器
   * <p>
   * 每次用户活动时调用，清除旧定时器并启动新定时器。
   * </p>
   */
  const resetTimer = () => {
    // 清除旧定时器
    if (timer) {
      clearTimeout(timer);
      timer = null;
    }

    // 如果超时时长 <= 0，表示不限制，不启动定时器
    if (timeoutMinutes.value <= 0) {
      return;
    }

    // 启动新定时器
    const timeoutMs = timeoutMinutes.value * 60 * 1000;
    timer = setTimeout(() => {
      console.info('[AdminIdleTimeout] 中台管理员会话超时，触发登出');
      stop(); // 停止检测
      options.onIdle(); // 触发回调
    }, timeoutMs);
  };

  /**
   * 活动事件处理器
   * <p>
   * 使用节流优化，避免频繁重置定时器和 localStorage 写入影响性能。
   * 节流间隔内同时更新 localStorage 中的最后活动时间。
   * </p>
   */
  let throttleTimer: ReturnType<typeof setTimeout> | null = null;
  const handleActivity = () => {
    if (throttleTimer) return;

    // 持久化最后活动时间到 localStorage
    setLastActivityTime();

    throttleTimer = setTimeout(() => {
      resetTimer();
      throttleTimer = null;
    }, 1000); // 1秒内最多重置一次
  };

  /**
   * 从后端加载中台管理员超时配置
   * <p>
   * 调用后端接口获取 {@code bml.admin.session-timeout-minutes} 配置值。
   * 如果获取失败或未配置，使用默认值 30 分钟。
   * </p>
   */
  const loadTimeoutConfig = async () => {
    try {
      // 调用后端接口获取管理员配置
      const res = await request.get('/auth/admin/config') as any;
      const config = res.data || {};
      const minutes = parseInt(config['sessionTimeoutMinutes'] || '30', 10);
      timeoutMinutes.value = isNaN(minutes) ? 30 : minutes;
      console.info('[AdminIdleTimeout] 中台管理员会话超时时长:', timeoutMinutes.value, '分钟');
    } catch (error) {
      console.warn('[AdminIdleTimeout] 获取超时配置失败，使用默认值 30 分钟', error);
      timeoutMinutes.value = 30;
    }
  };

  /**
   * 启动空闲检测
   * <p>
   * 1. 从后端加载超时配置
   * 2. 检查 localStorage 中持久化的上次活动时间，若已超时则立即登出
   * 3. 注册活动事件监听器
   * 4. 启动初始定时器
   * </p>
   */
  const start = async () => {
    if (isStarted.value) {
      console.warn('[AdminIdleTimeout] 空闲检测已启动，无需重复启动');
      return;
    }

    // 加载超时配置
    await loadTimeoutConfig();

    // 如果超时时长 <= 0，表示不限制，不启动检测
    if (timeoutMinutes.value <= 0) {
      console.info('[AdminIdleTimeout] 超时时长为 0，不启动空闲检测');
      return;
    }

    // ── 关键修复：检查持久化的最后活动时间 ──
    // 场景：管理员关闭浏览器/停止服务后再打开，localStorage 中的时间戳已过期
    if (isSessionIdleExpired(timeoutMinutes.value)) {
      console.info('[AdminIdleTimeout] 上次活动距今已超过', timeoutMinutes.value, '分钟，立即触发登出');
      options.onIdle();
      return;
    }

    // 记录启动时间作为初始活动时间
    setLastActivityTime();

    // 注册活动事件监听器
    ACTIVITY_EVENTS.forEach(event => {
      window.addEventListener(event, handleActivity, { passive: true });
    });

    // 启动初始定时器
    resetTimer();
    isStarted.value = true;
    console.info('[AdminIdleTimeout] 空闲检测已启动');
  };

  /**
   * 停止空闲检测
   * <p>
   * 1. 清除定时器
   * 2. 移除活动事件监听器
   * 3. 清理资源
   * </p>
   */
  const stop = () => {
    if (!isStarted.value) {
      return;
    }

    // 清除定时器
    if (timer) {
      clearTimeout(timer);
      timer = null;
    }

    if (throttleTimer) {
      clearTimeout(throttleTimer);
      throttleTimer = null;
    }

    // 移除活动事件监听器
    ACTIVITY_EVENTS.forEach(event => {
      window.removeEventListener(event, handleActivity);
    });

    isStarted.value = false;
    console.info('[AdminIdleTimeout] 空闲检测已停止');
  };

  /**
   * 组件卸载时自动清理
   */
  onUnmounted(() => {
    stop();
  });

  return {
    start,
    stop
  };
}
