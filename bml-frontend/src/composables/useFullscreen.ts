import { ref, onMounted, onUnmounted, type Ref } from 'vue';

/**
 * 全屏状态管理可复用逻辑 (Composable)
 * 
 * @description
 * 提供了一套通用的全屏控制方案，支持指定目标元素，并自动处理浏览器事件监听以保持状态同步。
 * 遵循“代码尽量通用化”的原则，方便在其他页面（如报表、监控大屏等）快速复用。
 * 
 * @param options 配置选项
 */
export interface UseFullscreenOptions {
  /**
   * 触发全屏切换的目标元素。不传则默认为 document.documentElement (整页全屏)。
   */
  target?: Ref<HTMLElement | null>;
  /**
   * 全屏状态改变时的回调函数。
   */
  onFullscreenChange?: (isFullscreen: boolean) => void;
  /**
   * 进入/退出全屏异常时的回调函数。
   */
  onError?: (error: any) => void;
}

export function useFullscreen(options: UseFullscreenOptions = {}) {
  const { target, onFullscreenChange, onError } = options;
  
  // 响应式状态：当前是否处于全屏模式
  const isFullscreen = ref(false);

  /**
   * 更新全屏状态标识
   */
  const updateFullscreenState = () => {
    isFullscreen.value = !!document.fullscreenElement;
    onFullscreenChange?.(isFullscreen.value);
  };

  /**
   * 进入全屏
   */
  const enter = async () => {
    const el = target?.value || document.documentElement;
    if (!el) return;

    try {
      if (el.requestFullscreen) {
        await el.requestFullscreen();
      }
    } catch (err) {
      onError?.(err);
      console.error('[Fullscreen] 进入全屏失败:', err);
    }
  };

  /**
   * 退出全屏
   */
  const exit = async () => {
    if (!document.fullscreenElement) return;
    
    try {
      if (document.exitFullscreen) {
        await document.exitFullscreen();
      }
    } catch (err) {
      onError?.(err);
      console.error('[Fullscreen] 退出全屏失败:', err);
    }
  };

  /**
   * 切换全屏状态
   */
  const toggle = async () => {
    if (isFullscreen.value) {
      await exit();
    } else {
      await enter();
    }
  };

  // 生命周期钩子：挂载时监听全屏切换事件
  onMounted(() => {
    document.addEventListener('fullscreenchange', updateFullscreenState);
    // 兼容旧版浏览器 (可选)
    // @ts-ignore
    document.addEventListener('webkitfullscreenchange', updateFullscreenState);
    // @ts-ignore
    document.addEventListener('mozfullscreenchange', updateFullscreenState);
    // @ts-ignore
    document.addEventListener('MSFullscreenChange', updateFullscreenState);
  });

  // 生命周期钩子：卸载时移除监听，防止内存泄漏
  onUnmounted(() => {
    document.removeEventListener('fullscreenchange', updateFullscreenState);
    // @ts-ignore
    document.removeEventListener('webkitfullscreenchange', updateFullscreenState);
    // @ts-ignore
    document.removeEventListener('mozfullscreenchange', updateFullscreenState);
    // @ts-ignore
    document.removeEventListener('MSFullscreenChange', updateFullscreenState);
  });

  return {
    isFullscreen,
    enter,
    exit,
    toggle
  };
}
