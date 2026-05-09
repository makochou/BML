/**
 * BML 主题色彩引擎
 * ──────────────────────────────────────────────────────
 * 所有色彩预设和 CSS 变量应用逻辑集中在此，
 * 供 Pinia Store 和 ThemeSettings 组件共用。
 *
 * 功能特性：
 *   1. 支持 8 种预设主题色彩
 *   2. 基于 HSL 色彩空间的真实 10 级色阶生成算法
 *   3. 兼容 Arco Design CSS 变量体系（--arcoblue-*、--color-primary-*）
 *   4. 自动派生语义化变量（选中背景、按钮渐变、阴影等）
 *   5. 持久化到 localStorage
 *   6. 平滑过渡动画
 *
 * 设计原则：
 *   - 所有组件只依赖 CSS 变量 `var(--bml-*)` 或 Arco 官方变量
 *   - 切换主题色时，只需调用 `applyThemeColor(hex)` 即可全局生效
 *   - 暗色模式由 Arco `body[arco-theme='dark']` 控制，此模块自动适配
 */

/* ═══════════════════════════════════════════════════════
   类型定义
   ═══════════════════════════════════════════════════════ */

/** 单个预设主题色配置 */
export interface ThemeColorItem {
    /** 中文名称（用于 UI 显示） */
    name: string;
    /** 主色 HEX 值（#RRGGBB） */
    value: string;
    /** 预设标识 key */
    key: string;
}

/* ═══════════════════════════════════════════════════════
   预设主题色
   ═══════════════════════════════════════════════════════ */

export const themeColors: ThemeColorItem[] = [
    { name: '拂晓蓝', value: '#165DFF', key: 'arcoblue' },
    { name: '极光绿', value: '#00B42A', key: 'green' },
    { name: '晚霞橘', value: '#FF7D00', key: 'orange' },
    { name: '火山红', value: '#F53F3F', key: 'red' },
    { name: '暗黑紫', value: '#722ED1', key: 'purple' },
    { name: '科技青', value: '#14C9C9', key: 'cyan' },
    { name: '薄荷绿', value: '#0FC6C2', key: 'mint' },
    { name: '品牌靛', value: '#3491FA', key: 'indigo' },
];

/* ═══════════════════════════════════════════════════════
   LocalStorage Key
   ═══════════════════════════════════════════════════════ */

const STORAGE_KEY = 'bml-theme-color';

/* ═══════════════════════════════════════════════════════
   色彩工具函数（纯函数，零外部依赖）
   ═══════════════════════════════════════════════════════ */

/**
 * 将 HEX 颜色解析为 RGB 三元组
 * @param hex - #RGB 或 #RRGGBB 格式
 * @returns [r, g, b]（0-255）
 */
function hexToRgb(hex: string): [number, number, number] {
    let h = hex.replace('#', '');
    if (h.length === 3) {
        h = h[0] + h[0] + h[1] + h[1] + h[2] + h[2];
    }
    const num = parseInt(h, 16);
    return [(num >> 16) & 255, (num >> 8) & 255, num & 255];
}

/**
 * 将 RGB 三元组转为 HEX 字符串
 * @returns #RRGGBB
 */
function rgbToHex(r: number, g: number, b: number): string {
    const clamp = (v: number) => Math.max(0, Math.min(255, Math.round(v)));
    return '#' + [clamp(r), clamp(g), clamp(b)]
        .map(v => v.toString(16).padStart(2, '0'))
        .join('');
}

/**
 * 将 RGB 三元组转为 HSL 三元组
 * @returns [h(0-360), s(0-1), l(0-1)]
 */
function rgbToHsl(r: number, g: number, b: number): [number, number, number] {
    r /= 255; g /= 255; b /= 255;
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    const l = (max + min) / 2;
    if (max === min) return [0, 0, l];
    const d = max - min;
    const s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    let h = 0;
    if (max === r) h = ((g - b) / d + (g < b ? 6 : 0)) / 6;
    else if (max === g) h = ((b - r) / d + 2) / 6;
    else h = ((r - g) / d + 4) / 6;
    return [h * 360, s, l];
}

/**
 * 将 HSL 三元组转为 RGB 三元组
 * @param h - 色相 0-360
 * @param s - 饱和度 0-1
 * @param l - 亮度 0-1
 * @returns [r, g, b]（0-255）
 */
function hslToRgb(h: number, s: number, l: number): [number, number, number] {
    h /= 360;
    if (s === 0) {
        const v = Math.round(l * 255);
        return [v, v, v];
    }
    const hue2rgb = (p: number, q: number, t: number) => {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1 / 6) return p + (q - p) * 6 * t;
        if (t < 1 / 2) return q;
        if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
        return p;
    };
    const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
    const p = 2 * l - q;
    return [
        Math.round(hue2rgb(p, q, h + 1 / 3) * 255),
        Math.round(hue2rgb(p, q, h) * 255),
        Math.round(hue2rgb(p, q, h - 1 / 3) * 255),
    ];
}

/**
 * 基于 HSL 色彩空间生成 10 级色阶
 * ──────────────────────────────
 * 算法逻辑：
 *   级别 1-5：从白色向主色过渡，亮度由高到低、饱和度由低到高
 *   级别 6  ：主色本身
 *   级别 7-10：从主色向深色过渡，亮度逐步降低、饱和度微增
 *
 * @param hex - 主色 HEX 值
 * @returns 长度为 10 的 HEX 数组（[0]=色阶1 最浅 … [9]=色阶10 最深）
 */
function generateColorPalette(hex: string): string[] {
    const [r, g, b] = hexToRgb(hex);
    const [h, s, l] = rgbToHsl(r, g, b);

    /**
     * 浅色色阶（1-5）：与白色混合，保持色相不变
     * ratios 数组对应色阶 1→5 中主色占比，越小越浅
     */
    const lightRatios = [0.06, 0.14, 0.24, 0.38, 0.58];

    /**
     * 深色色阶（7-10）：降低亮度、微增饱和度
     * darkSteps 对应色阶 7→10 的亮度衰减系数
     */
    const darkSteps = [0.88, 0.76, 0.62, 0.48];

    const palette: string[] = [];

    /* ── 色阶 1-5（浅色） ── */
    for (let i = 0; i < 5; i++) {
        const ratio = lightRatios[i];
        const mixR = Math.round(255 + (r - 255) * ratio);
        const mixG = Math.round(255 + (g - 255) * ratio);
        const mixB = Math.round(255 + (b - 255) * ratio);
        palette.push(rgbToHex(mixR, mixG, mixB));
    }

    /* ── 色阶 6（主色） ── */
    palette.push(hex.toUpperCase());

    /* ── 色阶 7-10（深色） ── */
    for (let i = 0; i < 4; i++) {
        const factor = darkSteps[i];
        const newL = l * factor;
        const newS = Math.min(1, s * (1 + (1 - factor) * 0.3));
        const [dr, dg, db] = hslToRgb(h, newS, newL);
        palette.push(rgbToHex(dr, dg, db));
    }

    return palette;
}

/* ═══════════════════════════════════════════════════════
   主题应用
   ═══════════════════════════════════════════════════════ */

/**
 * 应用主题色到全局 CSS 变量，并持久化到 localStorage
 *
 * 设置的变量清单：
 * ────────────────────────
 * BML 自定义变量：
 *   --bml-primary           主色
 *   --bml-primary-rgb       主色 RGB（用于 rgba()）
 *   --bml-primary-light     浅色（色阶 4）
 *   --bml-primary-lighter   极浅色（色阶 1）
 *   --bml-primary-dark      深色（色阶 7）
 *   --bml-primary-1 ~ 10   完整 10 级色阶
 *   --bml-gradient           主色渐变
 *   --bml-gradient-alt       备选渐变（主色→深色）
 *   --bml-shadow             主色投影
 *   --bml-selected-bg        选中行背景
 *   --bml-hover-bg           悬停背景
 *
 * Arco Design 覆盖：
 *   --arcoblue-1 ~ 10       替换 Arco 默认主色色阶
 *   --color-primary          Arco 主色
 *   --color-primary-light-*  Arco 浅色阶
 *   --color-primary-dark-*   Arco 深色阶
 */
export function applyThemeColor(color: string) {
    try {
        const el = document.body;
        const palette = generateColorPalette(color);
        const [r, g, b] = hexToRgb(color);
        const rgb = `${r},${g},${b}`;

        /* ── 过渡动画 ── */
        el.classList.add('theme-transitioning');

        /* ── BML 核心变量 ── */
        el.style.setProperty('--bml-primary', color);
        el.style.setProperty('--bml-primary-rgb', rgb);
        el.style.setProperty('--bml-primary-light', palette[3]);
        el.style.setProperty('--bml-primary-lighter', palette[0]);
        el.style.setProperty('--bml-primary-dark', palette[6]);

        /* ── BML 10 级色阶 ── */
        palette.forEach((hex, i) => {
            el.style.setProperty(`--bml-primary-${i + 1}`, hex);
        });

        /* ── BML 语义化变量 ── */
        el.style.setProperty('--bml-gradient', `linear-gradient(135deg, ${color} 0%, ${palette[4]} 100%)`);
        el.style.setProperty('--bml-gradient-alt', `linear-gradient(135deg, ${color} 0%, ${palette[7]} 100%)`);
        el.style.setProperty('--bml-shadow', `rgba(${rgb}, 0.3)`);
        el.style.setProperty('--bml-selected-bg', `rgba(${rgb}, 0.06)`);
        el.style.setProperty('--bml-hover-bg', `rgba(${rgb}, 0.04)`);
        el.style.setProperty('--bml-active-bg', `rgba(${rgb}, 0.10)`);
        el.style.setProperty('--bml-border-active', `rgba(${rgb}, 0.4)`);

        /* ── Arco Design 主色覆盖（确保所有 Arco 组件跟随主题） ── */
        palette.forEach((hex, i) => {
            el.style.setProperty(`--arcoblue-${i + 1}`, hex);
        });
        el.style.setProperty('--color-primary-light-1', palette[0]);
        el.style.setProperty('--color-primary-light-2', palette[1]);
        el.style.setProperty('--color-primary-light-3', palette[2]);
        el.style.setProperty('--color-primary-light-4', palette[3]);
        el.style.setProperty('--color-primary', color);
        el.style.setProperty('--color-primary-dark-1', palette[6]);
        el.style.setProperty('--color-primary-dark-2', palette[7]);

        /* ── 持久化 ── */
        localStorage.setItem(STORAGE_KEY, color);

        /* ── 移除过渡类 ── */
        setTimeout(() => {
            el.classList.remove('theme-transitioning');
        }, 300);
    } catch (error) {
        console.error('[BML Theme] 应用主题色失败:', error);
    }
}

/* ═══════════════════════════════════════════════════════
   读取持久化主题色
   ═══════════════════════════════════════════════════════ */

/**
 * 从 localStorage 读取保存的主题色
 * @returns HEX 色值，默认拂晓蓝 #165DFF
 */
export function getSavedThemeColor(): string {
    try {
        return localStorage.getItem(STORAGE_KEY) || '#165DFF';
    } catch {
        return '#165DFF';
    }
}

/* ═══════════════════════════════════════════════════════
   导出工具函数（供外部组件使用）
   ═══════════════════════════════════════════════════════ */

export { generateColorPalette, hexToRgb };
