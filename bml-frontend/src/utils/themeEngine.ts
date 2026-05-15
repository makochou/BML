/**
 * 主题引擎核心模块（`themeEngine.ts`）
 * ──────────────────────────────────────────────────────
 * 本模块是 “中台管理 / 业务系统” 双作用域主题系统的运行期核心，
 * 所有由 {@link ThemeProfile} 驱动的可视化变化最终都需要经过这里：
 *
 *   1. 颜色相关：基于 HSL 色彩空间生成 {@link generatePalette} 10 级色阶，
 *      并将语义色（primary / secondary / accent / 状态色 / 文字 / 背景 / 边框）
 *      与 Arco Design 的 `--arcoblue-{1..10}` 同步写入；
 *   2. 维度相关：将 {@link RadiusStyle} / {@link Density} / {@link FontScale}
 *      映射为对应的 `--bml-radius-*` / `--bml-spacing-*` / `--bml-font-size-base` Token；
 *   3. 行为相关：根据 {@link ThemeMode} 解析得到最终的亮 / 暗模式，
 *      写入 `<body>` 上的 `arco-theme`、`data-bml-density`、`data-bml-radius`、
 *      `data-bml-sidebar`、`data-bml-header`、`data-bml-scope` 等属性；
 *   4. 过渡相关：在 Token 写入瞬间为 `<body>` 加上 `theme-transitioning` 类
 *      （～300ms 后移除），让全平台共享一套主题切换的 transition 动画；
 *   5. 自动模式相关：通过 {@link subscribeAutoMode} 订阅浏览器
 *      `prefers-color-scheme` 媒体查询，让 `mode=AUTO` 的用户能跟随系统切换；
 *   6. 开发态校验：在 `import.meta.env.DEV` 下对 Profile 的颜色字段做合法性
 *      检查，发现非法 / 未知 Token 时通过 `console.warn` 提示。
 *
 * 本文件迁移自旧版 `utils/theme.ts` 的 {@link generatePalette} / {@link hexToRgb}
 * 算法，并扩展为支持完整 {@link ThemeProfile} 的 Token 写入器；旧文件保留过渡，
 * 待后续任务统一切换调用方后再行删除。
 *
 * 关联需求：Requirements 1.1 / 1.2 / 1.6 / 4.2 / 4.3 / 4.4 / 4.5 / 4.6 / 4.7 / 8.5 / 11.4。
 */

import type {
    Density,
    FontScale,
    HeaderStyle,
    RadiusStyle,
    SidebarStyle,
    ThemeMode,
    ThemeProfile,
    ThemeScope,
} from '@/types/theme';

/* ═══════════════════════════════════════════════════════
   一、颜色工具函数（纯函数，零外部依赖）
   ═══════════════════════════════════════════════════════ */

/**
 * 将 HEX 颜色解析为 RGB 三元组。
 *
 * 兼容三位简写（`#RGB`）与六位标准形式（`#RRGGBB`），不区分大小写；
 * 对于格式非法的输入，回退到黑色 `(0, 0, 0)` 并在开发模式输出警告，
 * 避免运行期抛错导致 {@link applyTokens} 中断。
 *
 * @param hex 形如 `#165DFF` / `#fff` 的颜色字符串。
 * @returns `{ r, g, b }`，每个分量取值范围 `0-255`。
 */
export function hexToRgb(hex: string): { r: number; g: number; b: number } {
    if (typeof hex !== 'string') {
        warnDev(`[BML ThemeEngine] hexToRgb 接收到非字符串输入: ${String(hex)}`);
        return { r: 0, g: 0, b: 0 };
    }
    let h = hex.trim().replace(/^#/, '');
    if (h.length === 3) {
        h = h[0] + h[0] + h[1] + h[1] + h[2] + h[2];
    }
    if (!/^[0-9a-fA-F]{6}$/.test(h)) {
        warnDev(`[BML ThemeEngine] hexToRgb 接收到非法 HEX 颜色: ${hex}`);
        return { r: 0, g: 0, b: 0 };
    }
    const num = Number.parseInt(h, 16);
    return {
        r: (num >> 16) & 0xff,
        g: (num >> 8) & 0xff,
        b: num & 0xff,
    };
}

/**
 * 将 RGB 分量 `[0-255]` 转换为 `#RRGGBB` 字符串。
 *
 * 自动对越界值做 clamp，并保证两位 16 进制（不足补零）；输出统一大写以
 * 与设计文档中的颜色字面量风格保持一致。
 */
function rgbToHex(r: number, g: number, b: number): string {
    const clamp = (v: number) => Math.max(0, Math.min(255, Math.round(v)));
    return (
        '#' +
        [clamp(r), clamp(g), clamp(b)]
            .map((v) => v.toString(16).padStart(2, '0'))
            .join('')
            .toUpperCase()
    );
}

/**
 * 将 RGB 三元组转换为 HSL 三元组。
 *
 * 用于色阶生成中的浅色 / 深色档位推导：浅色档保持色相不变、与白色按比例混合；
 * 深色档则按因子降低亮度并微增饱和度，从而获得视觉上协调的 10 级色阶。
 *
 * @returns `[h, s, l]`，`h` 取值 `0-360`，`s`、`l` 取值 `0-1`。
 */
function rgbToHsl(r: number, g: number, b: number): [number, number, number] {
    const rn = r / 255;
    const gn = g / 255;
    const bn = b / 255;
    const max = Math.max(rn, gn, bn);
    const min = Math.min(rn, gn, bn);
    const l = (max + min) / 2;
    if (max === min) {
        return [0, 0, l];
    }
    const d = max - min;
    const s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    let h = 0;
    if (max === rn) h = ((gn - bn) / d + (gn < bn ? 6 : 0)) / 6;
    else if (max === gn) h = ((bn - rn) / d + 2) / 6;
    else h = ((rn - gn) / d + 4) / 6;
    return [h * 360, s, l];
}

/**
 * 将 HSL 三元组转换为 RGB 三元组。
 *
 * @param h 色相，取值 `0-360`。
 * @param s 饱和度，取值 `0-1`。
 * @param l 亮度，取值 `0-1`。
 * @returns `[r, g, b]`，每个分量取值 `0-255`。
 */
function hslToRgb(h: number, s: number, l: number): [number, number, number] {
    const hn = h / 360;
    if (s === 0) {
        const v = Math.round(l * 255);
        return [v, v, v];
    }
    const hue2rgb = (p: number, q: number, t: number) => {
        let tn = t;
        if (tn < 0) tn += 1;
        if (tn > 1) tn -= 1;
        if (tn < 1 / 6) return p + (q - p) * 6 * tn;
        if (tn < 1 / 2) return q;
        if (tn < 2 / 3) return p + (q - p) * (2 / 3 - tn) * 6;
        return p;
    };
    const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
    const p = 2 * l - q;
    return [
        Math.round(hue2rgb(p, q, hn + 1 / 3) * 255),
        Math.round(hue2rgb(p, q, hn) * 255),
        Math.round(hue2rgb(p, q, hn - 1 / 3) * 255),
    ];
}

/**
 * 基于主色 HEX 值生成 10 级色阶。
 *
 * 算法概要：
 *   - 索引 `0..4`（色阶 1-5）：与白色按递减占比混合，色相不变；
 *   - 索引 `5`（色阶 6）：主色本身；
 *   - 索引 `6..9`（色阶 7-10）：在 HSL 空间下逐步降低亮度、微增饱和度。
 *
 * @param hex 主色 HEX 值，如 `#165DFF`。
 * @returns 长度恒为 10 的 HEX 数组，从最浅到最深。
 */
export function generatePalette(hex: string): string[] {
    const { r, g, b } = hexToRgb(hex);
    const [h, s, l] = rgbToHsl(r, g, b);

    /** 浅色 1-5：主色与白色的混合比例（值越小越浅）。 */
    const lightRatios = [0.06, 0.14, 0.24, 0.38, 0.58];
    /** 深色 7-10：HSL 空间下的亮度衰减系数。 */
    const darkSteps = [0.88, 0.76, 0.62, 0.48];

    const palette: string[] = [];

    /* 浅色 1-5：与纯白色按 ratio 混合 */
    for (let i = 0; i < 5; i++) {
        const ratio = lightRatios[i];
        palette.push(
            rgbToHex(
                Math.round(255 + (r - 255) * ratio),
                Math.round(255 + (g - 255) * ratio),
                Math.round(255 + (b - 255) * ratio),
            ),
        );
    }

    /* 主色 */
    palette.push(rgbToHex(r, g, b));

    /* 深色 7-10：降低亮度并轻微增加饱和度 */
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
   二、维度档位 -> Token 值映射
   ═══════════════════════════════════════════════════════ */

/**
 * 圆角档位映射表。
 *
 * 与 `tokens.preset-best.scss` 中 `MEDIUM` 档位的基线（4 / 8 / 12px）保持一致，
 * 其它档位以等差比例上下浮动；`SHARP` 档位强制全部为 `0px`，用于直角风格。
 */
const RADIUS_TOKEN_MAP: Record<RadiusStyle, { sm: string; md: string; lg: string }> = {
    SHARP: { sm: '0px', md: '0px', lg: '0px' },
    SMALL: { sm: '2px', md: '4px', lg: '6px' },
    MEDIUM: { sm: '4px', md: '8px', lg: '12px' },
    LARGE: { sm: '8px', md: '12px', lg: '16px' },
};

/**
 * 紧凑度档位映射表。
 *
 * 影响 `--bml-spacing-{xs,sm,md,lg,xl}`，从而让表格行高、表单控件高度、按钮
 * 高度等组件等比例缩放。基线 `DEFAULT` 与 `tokens.preset-best.scss` 完全一致。
 */
const DENSITY_TOKEN_MAP: Record<
    Density,
    { xs: string; sm: string; md: string; lg: string; xl: string }
> = {
    COMPACT: { xs: '2px', sm: '4px', md: '8px', lg: '16px', xl: '24px' },
    DEFAULT: { xs: '4px', sm: '8px', md: '16px', lg: '24px', xl: '32px' },
    LOOSE: { xs: '6px', sm: '12px', md: '24px', lg: '32px', xl: '40px' },
};

/**
 * 字体档位映射表。
 *
 * 取值为 `--bml-font-size-base` 的具体像素值，平台其它字号通过相对单位
 * （`rem` 或基于该 Token 的派生变量）派生。
 */
const FONT_SCALE_TOKEN_MAP: Record<FontScale, string> = {
    SMALL: '12px',
    DEFAULT: '14px',
    LARGE: '16px',
    XLARGE: '18px',
};

/**
 * 主题作用域 -> `data-bml-scope` 属性值映射。
 *
 * 与 `tokens.scss` 中 `body[data-bml-scope='business']` 选择器的取值约定保持
 * 一致；`<body>` 上始终显式标注 scope，方便 CSS 与开发者排查。
 */
const SCOPE_ATTR_MAP: Record<ThemeScope, 'admin' | 'business'> = {
    ADMIN: 'admin',
    BUSINESS: 'business',
};

/**
 * 侧边栏 / 顶栏风格 -> 属性值映射（统一转小写以符合 HTML 属性惯例）。
 *
 * 仅用于在 `<body>` 上输出 `data-bml-sidebar` / `data-bml-header` 属性，
 * 实际颜色仍由 CSS 变量 `--bml-color-*` 直接驱动。
 */
function styleAttrValue(style: SidebarStyle | HeaderStyle): string {
    return String(style).toLowerCase();
}

/* ═══════════════════════════════════════════════════════
   三、过渡 class 与暗色模式解析
   ═══════════════════════════════════════════════════════ */

/** 过渡 class 名称，与全局样式中 `body.theme-transitioning *` 的过渡规则配套。 */
const TRANSITION_CLASS = 'theme-transitioning';
/** 过渡持续时间（毫秒），与 `requirements.md` R6.AC4 / R11.AC4 的 ≤300ms 限制对齐。 */
const TRANSITION_DURATION_MS = 300;

/**
 * 当前过渡 class 的 `setTimeout` 句柄。
 *
 * 模块级单例：连续多次 {@link applyTokens} 时只保留一个 timeout，
 * 避免后续调用提前清除前一次仍在生效的过渡。
 */
let transitionTimer: ReturnType<typeof setTimeout> | null = null;

/**
 * 检测 “暗色模式媒体查询” 是否可用，并返回 `MediaQueryList`（不可用时返回 `null`）。
 *
 * 在 SSR / 旧浏览器 / 部分测试 jsdom 环境下，`window.matchMedia` 可能缺失，
 * 这里统一封装以便降级处理。
 */
function getDarkMediaQueryList(): MediaQueryList | null {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
        return null;
    }
    try {
        return window.matchMedia('(prefers-color-scheme: dark)');
    } catch {
        return null;
    }
}

/**
 * 将 {@link ThemeMode} 解析为最终生效的亮 / 暗模式。
 *
 * - `LIGHT` / `DARK`：直接返回；
 * - `AUTO`：根据 `prefers-color-scheme` 媒体查询；不可用时回退到 `LIGHT`。
 *
 * @returns `'LIGHT'` 或 `'DARK'`。
 */
export function resolveMode(mode: ThemeMode): 'LIGHT' | 'DARK' {
    if (mode === 'LIGHT' || mode === 'DARK') {
        return mode;
    }
    const mql = getDarkMediaQueryList();
    return mql && mql.matches ? 'DARK' : 'LIGHT';
}

/* ═══════════════════════════════════════════════════════
   四、核心 API：applyTokens
   ═══════════════════════════════════════════════════════ */

/**
 * 将单个 {@link ThemeProfile} 应用到当前 DOM。
 *
 * 调用该函数后将完成（按顺序）：
 *   1. 主色 10 级色阶生成并写入 `--arcoblue-{1..10}` 与 `--bml-color-primary-{1..10}`，
 *      同时设置 `--bml-color-primary-rgb`（供 `rgba()` 使用）；
 *   2. 写入语义色：`--bml-color-{primary,secondary,accent,success,warning,error,info,
 *      text-1,bg-1,border}` 等；
 *   3. 写入维度 Token：`--bml-radius-{sm,md,lg}`、`--bml-spacing-*`、`--bml-font-size-base`；
 *   4. 在 `<body>` 上设置主题相关属性：`arco-theme`、`data-bml-density`、
 *      `data-bml-radius`、`data-bml-sidebar`、`data-bml-sidebar-collapsed`、
 *      `data-bml-header`、`data-bml-scope`、`data-bml-mode`；
 *   5. 给 `<body>` 加上 `theme-transitioning` 类，约 300ms 后自动移除；
 *   6. 在开发模式下校验 Profile，发现非法 / 缺失字段时通过 `console.warn` 提示。
 *
 * 该函数不会抛出异常 —— 任何运行期错误（如 SSR 环境无 `document`、Profile
 * 字段缺失）都将以警告形式上报，并在可能的情况下保留剩余字段的写入，
 * 以避免主题系统在异常路径下让整页崩溃。
 *
 * @param profile 要应用的完整主题配置对象。
 * @param scope   该 Profile 所属作用域，用于设置 `data-bml-scope` 与触发不同
 *                的 CSS 微调（参见 `tokens.scss` 中 `body[data-bml-scope='business']`）。
 * @param root    可选的根元素；默认使用 `document.documentElement`，便于
 *                单元测试 / 多根容器场景（如未来引入 Shadow DOM 时）注入隔离根。
 */
export function applyTokens(
    profile: ThemeProfile,
    scope: ThemeScope,
    root?: HTMLElement,
): void {
    if (!profile || typeof profile !== 'object') {
        warnDev('[BML ThemeEngine] applyTokens 收到非法 Profile，已跳过。');
        return;
    }
    if (typeof document === 'undefined') {
        // SSR 或无 DOM 环境下不抛错，主题在客户端 hydration 时再生效。
        return;
    }

    const targetRoot: HTMLElement = root ?? document.documentElement;
    const body: HTMLElement | null = document.body ?? null;

    /* —— 开发态字段校验 —— */
    validateProfileInDev(profile);

    /* —— 一、生成主色 10 级色阶并写入 --arcoblue-* / --bml-color-primary-* —— */
    const palette = generatePalette(profile.primaryColor);
    palette.forEach((hex, idx) => {
        const n = idx + 1;
        targetRoot.style.setProperty(`--arcoblue-${n}`, hex);
        targetRoot.style.setProperty(`--bml-color-primary-${n}`, hex);
    });
    const primaryRgb = hexToRgb(profile.primaryColor);
    targetRoot.style.setProperty('--bml-color-primary', profile.primaryColor);
    targetRoot.style.setProperty(
        '--bml-color-primary-rgb',
        `${primaryRgb.r},${primaryRgb.g},${primaryRgb.b}`,
    );

    /* —— 二、写入其余语义色 —— */
    targetRoot.style.setProperty('--bml-color-secondary', profile.secondaryColor);
    targetRoot.style.setProperty('--bml-color-accent', profile.accentColor);
    targetRoot.style.setProperty('--bml-color-success', profile.successColor);
    targetRoot.style.setProperty('--bml-color-warning', profile.warningColor);
    targetRoot.style.setProperty('--bml-color-error', profile.errorColor);
    targetRoot.style.setProperty('--bml-color-info', profile.infoColor);
    targetRoot.style.setProperty('--bml-color-text-1', profile.textColor);
    targetRoot.style.setProperty('--bml-color-bg-1', profile.backgroundColor);
    targetRoot.style.setProperty('--bml-color-border', profile.borderColor);

    /* —— 三、写入维度 Token —— */
    const radius = RADIUS_TOKEN_MAP[profile.radius] ?? RADIUS_TOKEN_MAP.MEDIUM;
    targetRoot.style.setProperty('--bml-radius-sm', radius.sm);
    targetRoot.style.setProperty('--bml-radius-md', radius.md);
    targetRoot.style.setProperty('--bml-radius-lg', radius.lg);

    const spacing = DENSITY_TOKEN_MAP[profile.density] ?? DENSITY_TOKEN_MAP.DEFAULT;
    targetRoot.style.setProperty('--bml-spacing-xs', spacing.xs);
    targetRoot.style.setProperty('--bml-spacing-sm', spacing.sm);
    targetRoot.style.setProperty('--bml-spacing-md', spacing.md);
    targetRoot.style.setProperty('--bml-spacing-lg', spacing.lg);
    targetRoot.style.setProperty('--bml-spacing-xl', spacing.xl);

    targetRoot.style.setProperty(
        '--bml-font-size-base',
        FONT_SCALE_TOKEN_MAP[profile.fontScale] ?? FONT_SCALE_TOKEN_MAP.DEFAULT,
    );

    /* —— 四、写入 <body> 主题属性 —— */
    if (body) {
        const resolved = resolveMode(profile.mode);
        body.setAttribute('arco-theme', resolved === 'DARK' ? 'dark' : 'light');
        body.setAttribute('data-bml-mode', String(profile.mode).toLowerCase());
        body.setAttribute('data-bml-density', String(profile.density).toLowerCase());
        body.setAttribute('data-bml-radius', String(profile.radius).toLowerCase());
        body.setAttribute('data-bml-sidebar', styleAttrValue(profile.sidebarStyle));
        body.setAttribute(
            'data-bml-sidebar-collapsed',
            styleAttrValue(profile.sidebarCollapsedStyle),
        );
        body.setAttribute('data-bml-header', styleAttrValue(profile.headerStyle));
        body.setAttribute('data-bml-scope', SCOPE_ATTR_MAP[scope] ?? 'admin');

        /* —— 五、过渡 class 加移 —— */
        body.classList.add(TRANSITION_CLASS);
        if (transitionTimer !== null) {
            clearTimeout(transitionTimer);
        }
        transitionTimer = setTimeout(() => {
            body.classList.remove(TRANSITION_CLASS);
            transitionTimer = null;
        }, TRANSITION_DURATION_MS);
    }
}

/* ═══════════════════════════════════════════════════════
   五、AUTO 模式订阅
   ═══════════════════════════════════════════════════════ */

/**
 * AUTO 模式回调签名。
 *
 * @param resolvedMode `prefers-color-scheme` 切换后解析得到的最终模式。
 */
export type AutoModeCallback = (resolvedMode: 'LIGHT' | 'DARK') => void;

/**
 * 订阅浏览器 `prefers-color-scheme` 媒体查询变化。
 *
 * 仅用于 `mode = AUTO` 场景：当系统在 “浅色 ⇆ 深色” 之间切换时，
 * 回调会接收到对应的 `'LIGHT'` / `'DARK'`，调用方据此重新调用
 * {@link applyTokens} 或更新 `<body arco-theme>`，从而满足
 * Property 8（AUTO 模式跟随）的正确性约束。
 *
 * 兼容性：
 *   - 优先使用 `MediaQueryList.addEventListener('change')`；
 *   - 在仅支持已弃用 `addListener` 的浏览器上自动回退。
 *   - 在不支持 `matchMedia` 的环境（SSR / 老浏览器）下返回 No-op
 *     反订阅函数，且回调不会被触发。
 *
 * @param callback 系统模式变化时执行的回调。
 * @returns 反订阅函数，幂等可多次调用。
 */
export function subscribeAutoMode(callback: AutoModeCallback): () => void {
    if (typeof callback !== 'function') {
        throw new TypeError('[BML ThemeEngine] subscribeAutoMode 仅接受函数类型的回调。');
    }
    const mql = getDarkMediaQueryList();
    if (!mql) {
        return () => {
            /* 无 matchMedia 支持，直接 No-op */
        };
    }

    const handler = (event: MediaQueryListEvent | MediaQueryList) => {
        const matches = 'matches' in event ? event.matches : false;
        try {
            callback(matches ? 'DARK' : 'LIGHT');
        } catch (err) {
            warnDev('[BML ThemeEngine] subscribeAutoMode 回调执行失败:', err);
        }
    };

    // 现代浏览器：addEventListener('change')
    if (typeof mql.addEventListener === 'function') {
        mql.addEventListener('change', handler);
        return () => {
            try {
                mql.removeEventListener('change', handler);
            } catch {
                /* 反订阅失败时静默：通常意味着 mql 已失效 */
            }
        };
    }

    // 旧浏览器（Safari < 14 等）：addListener
    type LegacyMQL = MediaQueryList & {
        addListener?: (cb: (e: MediaQueryListEvent) => void) => void;
        removeListener?: (cb: (e: MediaQueryListEvent) => void) => void;
    };
    const legacy = mql as LegacyMQL;
    if (typeof legacy.addListener === 'function') {
        legacy.addListener(handler);
        return () => {
            try {
                legacy.removeListener?.(handler);
            } catch {
                /* 反订阅失败时静默 */
            }
        };
    }

    return () => {
        /* 无可用订阅 API,直接 No-op */
    };
}

/* ═══════════════════════════════════════════════════════
   六、开发态告警与字段校验
   ═══════════════════════════════════════════════════════ */

/** 必须存在的颜色字段，开发态用于格式校验。 */
const REQUIRED_HEX_FIELDS: ReadonlyArray<keyof ThemeProfile> = [
    'primaryColor',
    'secondaryColor',
    'accentColor',
    'successColor',
    'warningColor',
    'errorColor',
    'infoColor',
    'textColor',
    'backgroundColor',
    'borderColor',
];

/** 形如 `#RRGGBB` 的严格 6 位 HEX 颜色正则（与后端 `@HexColor` 注解保持一致）。 */
const HEX_COLOR_REGEX = /^#[0-9a-fA-F]{6}$/;

/**
 * 仅在开发模式下输出 `console.warn`，生产环境完全静默以避免污染线上日志。
 *
 * 通过 `import.meta.env?.DEV` 进行守卫，使本模块同样可在没有 Vite 环境
 * 变量注入的纯单元测试环境（例如 vitest 在 node 环境）中安全运行。
 */
function warnDev(...args: unknown[]): void {
    try {
        if (import.meta.env?.DEV) {
            // eslint-disable-next-line no-console
            console.warn(...args);
        }
    } catch {
        /* 静默：访问 import.meta.env 失败时不阻塞主流程 */
    }
}

/**
 * 在开发模式下校验 Profile 字段，对格式异常的颜色值与缺失字段输出警告。
 *
 * 该函数仅在 `import.meta.env.DEV` 时执行实际逻辑，生产构建会被
 * Tree-shaking / 死代码消除掉绝大部分内容；运行期不会抛出异常，
 * 即使 Profile 字段非法也只会警告而不阻塞 Token 写入。
 */
function validateProfileInDev(profile: ThemeProfile): void {
    let isDev = false;
    try {
        isDev = !!import.meta.env?.DEV;
    } catch {
        return;
    }
    if (!isDev) return;

    for (const field of REQUIRED_HEX_FIELDS) {
        const value = profile[field];
        if (typeof value !== 'string' || !HEX_COLOR_REGEX.test(value)) {
            warnDev(
                `[BML ThemeEngine] Profile 字段 \`${String(field)}\` 不是合法的 #RRGGBB 颜色: ${String(value)}`,
            );
        }
    }
    if (!(profile.mode in { LIGHT: 1, DARK: 1, AUTO: 1 })) {
        warnDev(`[BML ThemeEngine] Profile 字段 \`mode\` 不是合法枚举: ${String(profile.mode)}`);
    }
    if (!(profile.radius in RADIUS_TOKEN_MAP)) {
        warnDev(
            `[BML ThemeEngine] Profile 字段 \`radius\` 不是已知档位: ${String(profile.radius)}，将回退至 MEDIUM`,
        );
    }
    if (!(profile.density in DENSITY_TOKEN_MAP)) {
        warnDev(
            `[BML ThemeEngine] Profile 字段 \`density\` 不是已知档位: ${String(profile.density)}，将回退至 DEFAULT`,
        );
    }
    if (!(profile.fontScale in FONT_SCALE_TOKEN_MAP)) {
        warnDev(
            `[BML ThemeEngine] Profile 字段 \`fontScale\` 不是已知档位: ${String(profile.fontScale)}，将回退至 DEFAULT`,
        );
    }
}
