import { ref, computed } from 'vue';

type ResizeDirection = 'n' | 's' | 'e' | 'w' | 'ne' | 'nw' | 'se' | 'sw' | '';

export interface UseResizableModalOptions {
  initialWidth?: number;
  initialHeight?: number;
  minWidth?: number;
  minHeight?: number;
  gap?: number;
  /**
   * 缩放时与视口边缘保留的安全内边距（px）。
   * 仅用于 east/south/west 缩放边界，不影响弹窗居中计算。
   */
  resizeViewportInset?: number;
}

type RecenterOptions = {
  width?: number;
  height?: number;
  /**
   * 是否强制重算居中坐标。
   * - true：无论当前是否已存在 left/top 都重新计算并覆盖；
   * - false：仅在未初始化坐标时才计算。
   */
  force?: boolean;
};

export function useResizableModal(options: UseResizableModalOptions = {}) {
  const {
    initialWidth = 800,
    initialHeight = 600,
    minWidth = 400,
    minHeight = 300,
    gap = 24,
    resizeViewportInset = gap
  } = options;

  const width = ref(initialWidth);
  const height = ref(initialHeight);
  const left = ref(-1);
  const top = ref(-1);
  const fullscreen = ref(false);

  const isDragging = ref(false);
  const isResizing = ref(false);
  const resizeDirection = ref<ResizeDirection>('');

  let startX = 0;
  let startY = 0;
  let startWidth = 0;
  let startHeight = 0;
  let startLeft = 0;
  let startTop = 0;

  function getViewportSize() {
    const vw = Math.max(document.documentElement.clientWidth || 0, window.innerWidth || 0);
    const vh = Math.max(document.documentElement.clientHeight || 0, window.innerHeight || 0);
    return { vw, vh };
  }

  function getCenteredCoordinate(viewportLength: number, modalLength: number) {
    const centered = (viewportLength - modalLength) / 2;
    /**
     * 当弹窗尺寸已接近视口时，允许坐标贴近 0，避免被 gap 反向挤压后看起来不居中。
     * 当视口空间充足时，保留安全边距 gap。
     */
    if (viewportLength - modalLength >= gap * 2) {
      return Math.max(gap, centered);
    }
    return Math.max(0, centered);
  }

  /**
   * 将弹窗放置到当前浏览器视口中心位置。
   * 该方法不修改 fullscreen 状态，只负责 left/top 重算。
   */
  const centerInViewport = () => {
    if (typeof window === 'undefined') return;
    const { vw, vh } = getViewportSize();
    left.value = getCenteredCoordinate(vw, width.value);
    top.value = getCenteredCoordinate(vh, height.value);
  };

  /**
   * 初始化定位。
   * 默认仅在未初始化坐标时执行；可通过 force=true 强制重算居中。
   */
  const initPosition = (force = false) => {
    if (!force && left.value !== -1 && top.value !== -1) return;
    centerInViewport();
  };

  const constrainPosition = (l: number, t: number, w: number) => {
    let newLeft = l;
    let newTop = t;
    if (typeof window !== 'undefined') {
      const { vw, vh } = getViewportSize();
      
      // Ensure it doesn't go completely off-screen, keep at least a grab area
      const minVisible = 40;
      newLeft = Math.max(-w + minVisible, Math.min(newLeft, vw - minVisible));
      // Top should never go above 0 to prevent losing the header
      newTop = Math.max(0, Math.min(newTop, vh - minVisible));
    }
    return { left: newLeft, top: newTop };
  };

  const startDrag = (e: MouseEvent) => {
    if (fullscreen.value) return;
    initPosition(); // Ensure initial position is set before dragging
    
    isDragging.value = true;
    startX = e.clientX;
    startY = e.clientY;
    startLeft = left.value;
    startTop = top.value;

    document.addEventListener('mousemove', onDrag);
    document.addEventListener('mouseup', stopDrag);
    
    // Disable text selection during drag
    document.body.style.userSelect = 'none';
  };

  const onDrag = (e: MouseEvent) => {
    if (!isDragging.value) return;
    
    const dx = e.clientX - startX;
    const dy = e.clientY - startY;
    
    const targetLeft = startLeft + dx;
    const targetTop = startTop + dy;
    
    const constrained = constrainPosition(targetLeft, targetTop, width.value);
    left.value = constrained.left;
    top.value = constrained.top;
  };

  const stopDrag = () => {
    isDragging.value = false;
    document.removeEventListener('mousemove', onDrag);
    document.removeEventListener('mouseup', stopDrag);
    document.body.style.userSelect = '';
  };

  const startResize = (dir: ResizeDirection, e: MouseEvent) => {
    if (fullscreen.value) return;
    initPosition();

    isResizing.value = true;
    resizeDirection.value = dir;
    
    startX = e.clientX;
    startY = e.clientY;
    startWidth = width.value;
    startHeight = height.value;
    startLeft = left.value;
    startTop = top.value;

    document.addEventListener('mousemove', onResize);
    document.addEventListener('mouseup', stopResize);
    
    document.body.style.userSelect = 'none';
  };

  const onResize = (e: MouseEvent) => {
    if (!isResizing.value) return;
    
    const dx = e.clientX - startX;
    const dy = e.clientY - startY;
    const dir = resizeDirection.value;

    let targetWidth = startWidth;
    let targetHeight = startHeight;
    let targetLeft = startLeft;
    let targetTop = startTop;

    // Calculate new dimensions based on drag direction
    if (dir.includes('e')) {
      targetWidth = Math.max(minWidth, startWidth + dx);
    }
    if (dir.includes('s')) {
      targetHeight = Math.max(minHeight, startHeight + dy);
    }
    if (dir.includes('w')) {
      const allowedDx = Math.min(dx, startWidth - minWidth);
      targetWidth = startWidth - allowedDx;
      targetLeft = startLeft + allowedDx;
    }
    if (dir.includes('n')) {
      // Prevent resizing above the top edge of the viewport
      const maxDyup = startTop; 
      // maxDyup is the max distance we can go up (negative dy)
      const actualDy = Math.max(dy, -maxDyup);
      
      const allowedDy = Math.min(actualDy, startHeight - minHeight);
      targetHeight = startHeight - allowedDy;
      targetTop = startTop + allowedDy;
    }

    // Apply viewport constraints for the new size (don't grow beyond viewport)
    if (typeof window !== 'undefined') {
       const { vw, vh } = getViewportSize();
       
       if (dir.includes('e') && targetLeft + targetWidth > vw - resizeViewportInset) {
          targetWidth = vw - resizeViewportInset - targetLeft;
       }
       if (dir.includes('s') && targetTop + targetHeight > vh - resizeViewportInset) {
          targetHeight = vh - resizeViewportInset - targetTop;
       }
       if (dir.includes('w') && targetLeft < resizeViewportInset) {
          targetLeft = resizeViewportInset;
          targetWidth = startLeft + startWidth - resizeViewportInset;
       }
    }

    width.value = Math.max(minWidth, targetWidth);
    height.value = Math.max(minHeight, targetHeight);
    left.value = targetLeft;
    top.value = targetTop;
  };

  const stopResize = () => {
    isResizing.value = false;
    resizeDirection.value = '';
    document.removeEventListener('mousemove', onResize);
    document.removeEventListener('mouseup', stopResize);
    document.body.style.userSelect = '';
  };

  const toggleFullscreen = () => {
    fullscreen.value = !fullscreen.value;
  };

  /**
   * 按需重置尺寸并居中。
   * 用于“每次打开弹窗都回到浏览器中心”的业务场景，保持控制逻辑通用可复用。
   */
  const recenter = (options: RecenterOptions = {}) => {
    const { width: targetWidth, height: targetHeight, force = true } = options;
    if (typeof targetWidth === 'number' && Number.isFinite(targetWidth)) {
      width.value = Math.max(minWidth, Math.round(targetWidth));
    }
    if (typeof targetHeight === 'number' && Number.isFinite(targetHeight)) {
      height.value = Math.max(minHeight, Math.round(targetHeight));
    }
    initPosition(force);
  };

  const modalStyle = computed(() => {
    initPosition(); // Ensure it's centered if nothing has been dragged yet
    
    if (fullscreen.value) {
      return {
        width: '100vw',
        height: '100vh',
        left: '0px',
        top: '0px',
        margin: '0',
        borderRadius: '0',
        transform: 'none',
        transition: isDragging.value || isResizing.value ? 'none' : 'all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)'
      };
    }

    return {
      width: `${width.value}px`,
      height: `${height.value}px`,
      left: `${left.value}px`,
      top: `${top.value}px`,
      position: 'fixed' as const,
      margin: '0',
      transform: 'none',
      transition: isDragging.value || isResizing.value ? 'none' : 'box-shadow 0.3s ease, border-radius 0.3s ease'
    };
  });

  return {
    width,
    height,
    left,
    top,
    fullscreen,
    isDragging,
    isResizing,
    resizeDirection,
    startDrag,
    startResize,
    toggleFullscreen,
    modalStyle,
    initPosition,
    centerInViewport,
    recenter
  };
}
