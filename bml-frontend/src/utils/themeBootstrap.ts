/**
 * 主题首屏防 FOUC 引导脚本（themeBootstrap.ts）
 * ──────────────────────────────────────────────────────
 * 在 `index.html` 的 `<head>` 内联同步执行的引导脚本，目标是在 Vue 应用尚未挂载、
 * `themeEngine.applyTokens` 尚未运行之前，立即将关键 CSS 自定义属性写入
 * `<html>` 元素，并完成 `<body>` 主题属性 (`arco-theme` / `data-bml-scope`) 设置，
 * 以彻底消除主题切换 / 刷新场景下的“先白后彩”闪烁（FOUC）。
 *
 * 工作机制：
 *   1. 由 Vite 插件 `plugins/vite-plugin-theme-bootstrap.ts` 在构建期 / 开发期
 *      将本文件导出的 {@link PRESET_BEST_TOKENS} 序列化为
 *      `window.__BML_PRESET_BEST__`，并紧随其后内联 {@link BOOTSTRAP_SCRIPT_SOURCE}
 *      的 IIFE 同步执行；
 *   2. IIFE 自身根据 `location.pathname` 推断 `Theme_Scope`（`/business` 前缀 →
 *      BUSINESS，否则 ADMIN），尝试从 `localStorage[bml-theme-{scope}]` 读取
 *      已持久化的 `ThemeProfile`；
 *   3. 若 `localStorage` 命中且 JSON 合法：将 `ThemeProfile` 的颜色字段映射为
 *      `--bml-*` 写入 `<html>`；模式按 `mode` 字段解析（AUTO 时回退到
 *      `prefers-color-scheme`）。
 *   4. 若 `localStorage` 缺失或解析失败：从 `__BML_PRESET_BEST__` 中按
 *      `best-{light|dark}-{admin|business}` 选择默认变体直接写入。
 *   5. 整个流程在 `try/catch` 中静默失败，不阻断后续 Vue 应用启动。
 *
 * 设计依据：
 *   - design.md / Components and Interfaces / 同步引导脚本（防 FOUC）
 *   - design.md / Token 命名规范
 *   - requirements.md R6.AC3 / R6.AC5 / R13.AC2
 *
 * 与 `tokens.preset-best.scss` 的关系：
 *   - 本文件内 {@link PRESET_BEST_TOKENS} 是 `tokens.preset-best.scss` 中
 *     `$preset-best-{light|dark}-{admin|business}` 4 个 SCSS Map 的“运行期镜像”，
 *     字段值 SHALL 与 SCSS 源文件保持一一对应；任何对 SCSS Map 的修改
 *     都必须同步反映到此处，反之亦然。
 *   - 后续若引入构建期解析 SCSS 的能力，可将此常量替换为自动生成；当前
 *     选择手工镜像以保持插件实现简单（参见 task 12.2 备注）。
 *
 * 模块约束：
 *   - 本文件**不得**引用任何 DOM 类型 / 浏览器全局对象，以便被 Vite 插件
 *     （Node 上下文）安全 `import`；运行期 IIFE 内部使用的 `document` /
 *     `window` 等仅出现在 {@link BOOTSTRAP_SCRIPT_SOURCE} 字符串字面量中，
 *     由浏览器在执行时解析。
 *
 * 关联需求：Requirements 6.3 / 6.5 / 13.2 / 10.2。
 */

// =============================================================================
// 类型定义
// =============================================================================

/**
 * 引导脚本可识别的最小 ThemeProfile 子集。
 *
 * 与 `src/types/theme.ts` 中的 `ThemeProfile` 字段一致；为避免在 Node 构建
 * 上下文中拉入 DOM 相关依赖，此处单独声明且全部字段为可选——
 * 引导脚本会逐字段做合法性校验（颜色匹配 `^#[0-9A-Fa-f]{6}$`、`mode` 为合法
 * 枚举值），任意非法字段会被静默忽略并回退到 PRESET_BEST。
 */
export interface BootstrapProfile {
    /** 主色，如 `#165DFF`。 */
    primaryColor?: string;
    /** 辅助色。 */
    secondaryColor?: string;
    /** 强调色。 */
    accentColor?: string;
    /** 成功状态色。 */
    successColor?: string;
    /** 警告状态色。 */
    warningColor?: string;
    /** 错误状态色。 */
    errorColor?: string;
    /** 信息状态色。 */
    infoColor?: string;
    /** 正文文字色。 */
    textColor?: string;
    /** 背景色。 */
    backgroundColor?: string;
    /** 边框色。 */
    borderColor?: string;
    /** 明暗模式。 */
    mode?: 'LIGHT' | 'DARK' | 'AUTO';
}

/**
 * PRESET_BEST 4 个变体的合法标识。
 *
 * 命名约定 `best-{light|dark}-{admin|business}` 与 `tokens.preset-best.scss`
 * 中 `:root[data-bml-preset='...']` 选择器一致。
 */
export type PresetBestVariantKey =
    | 'best-light-admin'
    | 'best-light-business'
    | 'best-dark-admin'
    | 'best-dark-business';

/**
 * `__BML_PRESET_BEST__` 全局对象的形状。
 *
 * 每个变体下的值是 “CSS 变量名 → CSS 值” 的扁平字典，
 * 引导脚本通过 `for ... in` 直接将其逐项写入 `:root.style`。
 */
export type PresetBestTokens = Record<PresetBestVariantKey, Record<string, string>>;

// =============================================================================
// 常量
// =============================================================================

/**
 * 本地存储键前缀；完整键为 `${前缀}${scope.toLowerCase()}`，
 * 例如 ADMIN → `bml-theme-admin`、BUSINESS → `bml-theme-business`。
 */
export const THEME_LOCALSTORAGE_KEY_PREFIX = 'bml-theme-';

/**
 * 注入到 `window` 的全局变量名，承载 PRESET_BEST 4 变体 Token。
 */
export const THEME_PRESET_GLOBAL_KEY = '__BML_PRESET_BEST__';

/**
 * 业务系统路由 path 前缀；用于从 URL 推断作用域。
 */
export const THEME_BUSINESS_PATH_PREFIX = '/business';

/**
 * `ThemeProfile` 颜色字段 → CSS 自定义属性名的映射。
 *
 * 引导脚本仅写入“关键 Token”以保证 IIFE 体积尽量小；
 * 维度 Token（圆角、间距、字号、阴影）由运行期 `themeEngine.applyTokens` 接管。
 * 运行期写入仍能稳定覆盖：CSS 自定义属性是 “最后写入优先”。
 */
export const PROFILE_FIELD_TO_CSS_VAR: Readonly<Record<keyof BootstrapProfile, string>> = Object.freeze({
    primaryColor: '--bml-color-primary',
    secondaryColor: '--bml-color-secondary',
    accentColor: '--bml-color-accent',
    successColor: '--bml-color-success',
    warningColor: '--bml-color-warning',
    errorColor: '--bml-color-error',
    infoColor: '--bml-color-info',
    textColor: '--bml-color-text-1',
    backgroundColor: '--bml-color-bg-1',
    borderColor: '--bml-color-border',
    mode: '__mode__', // 占位：mode 不写入 CSS 变量，由 IIFE 单独处理
});

// =============================================================================
// PRESET_BEST 4-variant Token 数据（与 tokens.preset-best.scss 同步）
// =============================================================================

/**
 * Arco Design 蓝色色阶（与 `$preset-best-arcoblue` 保持完全一致）。
 *
 * 每个变体均使用同一份色阶，仅“当前主色”由变体的 `--bml-color-primary` 字段决定。
 */
const ARCOBLUE_SHADES: ReadonlyArray<string> = Object.freeze([
    '#e8f3ff', // 1
    '#bedaff', // 2
    '#94bfff', // 3
    '#6aa1ff', // 4
    '#4080ff', // 5
    '#165dff', // 6
    '#0e42d2', // 7
    '#072ca6', // 8
    '#001d7a', // 9
    '#00114f', // 10
]);

/**
 * 构造一份 `--arcoblue-{1..10}` 与 `--bml-color-primary-{1..10}` 色阶变量集合。
 *
 * @returns CSS 变量名到色阶值的字典
 */
function buildArcoBlueShadeVars(): Record<string, string> {
    const out: Record<string, string> = {};
    ARCOBLUE_SHADES.forEach((hex, idx) => {
        const i = idx + 1;
        out[`--arcoblue-${i}`] = hex;
        out[`--bml-color-primary-${i}`] = hex;
    });
    return out;
}

/**
 * `best-light-admin` 变体 —— 与 `$preset-best-light-admin` 一一对应。
 */
const LIGHT_ADMIN: Record<string, string> = {
    // 关键色
    '--bml-color-primary': '#165dff',
    '--bml-color-secondary': '#4080ff',
    '--bml-color-accent': '#722ed1',
    // 状态色
    '--bml-color-success': '#00b42a',
    '--bml-color-warning': '#ff7d00',
    '--bml-color-error': '#f53f3f',
    '--bml-color-info': '#86909c',
    // 文字 3 级
    '--bml-color-text-1': '#1d2129',
    '--bml-color-text-2': '#4e5969',
    '--bml-color-text-3': '#86909c',
    // 背景 3 级（ADMIN）
    '--bml-color-bg-1': '#ffffff',
    '--bml-color-bg-2': '#f7f8fa',
    '--bml-color-bg-3': '#f2f3f5',
    // 边框
    '--bml-color-border': '#e5e6eb',
    // 圆角
    '--bml-radius-sm': '4px',
    '--bml-radius-md': '8px',
    '--bml-radius-lg': '12px',
    // 间距
    '--bml-spacing-xs': '4px',
    '--bml-spacing-sm': '8px',
    '--bml-spacing-md': '16px',
    '--bml-spacing-lg': '24px',
    '--bml-spacing-xl': '32px',
    // 字体
    '--bml-font-size-base': '14px',
    '--bml-line-height-base': '1.5715',
    // 阴影（亮色）
    '--bml-shadow-sm': '0 1px 2px rgba(0, 0, 0, 0.04)',
    '--bml-shadow-md': '0 4px 12px rgba(0, 0, 0, 0.08)',
    '--bml-shadow-lg': '0 12px 32px rgba(0, 0, 0, 0.12)',
};

/**
 * `best-light-business` 变体 —— 仅在 `bg-1/2/3` 三级背景上与 ADMIN 不同。
 */
const LIGHT_BUSINESS: Record<string, string> = {
    ...LIGHT_ADMIN,
    '--bml-color-bg-1': '#f7f8fa',
    '--bml-color-bg-2': '#ffffff',
    '--bml-color-bg-3': '#f2f3f5',
};

/**
 * `best-dark-admin` 变体 —— 与 `$preset-best-dark-admin` 一一对应。
 */
const DARK_ADMIN: Record<string, string> = {
    ...LIGHT_ADMIN,
    // 主色：暗色下使用 arcoblue-5 提升点击区域可见度
    '--bml-color-primary': '#4080ff',
    '--bml-color-secondary': '#6aa1ff',
    '--bml-color-accent': '#b37feb',
    // 状态色按暗色规范微调亮度
    '--bml-color-success': '#23c343',
    '--bml-color-warning': '#ffaa00',
    '--bml-color-error': '#f76560',
    '--bml-color-info': '#a9aeb8',
    // 文字（暗色）
    '--bml-color-text-1': '#f2f3f5',
    '--bml-color-text-2': '#c9cdd4',
    '--bml-color-text-3': '#86909c',
    // 背景（暗色 / ADMIN）
    '--bml-color-bg-1': '#17171a',
    '--bml-color-bg-2': '#232324',
    '--bml-color-bg-3': '#2a2a2b',
    // 边框
    '--bml-color-border': '#3a3a3b',
    // 阴影（暗色）
    '--bml-shadow-sm': '0 1px 2px rgba(0, 0, 0, 0.30)',
    '--bml-shadow-md': '0 4px 12px rgba(0, 0, 0, 0.40)',
    '--bml-shadow-lg': '0 12px 32px rgba(0, 0, 0, 0.55)',
};

/**
 * `best-dark-business` 变体 —— 仅在 `bg-1/2/3` 上与 ADMIN 暗色不同。
 */
const DARK_BUSINESS: Record<string, string> = {
    ...DARK_ADMIN,
    '--bml-color-bg-1': '#232324',
    '--bml-color-bg-2': '#17171a',
    '--bml-color-bg-3': '#2a2a2b',
};

/**
 * PRESET_BEST 4 变体 Token 集合。
 *
 * 由 Vite 插件序列化为 `window.__BML_PRESET_BEST__` 注入到 `<head>`。
 *
 * **维护约束**：本对象与 `src/styles/tokens.preset-best.scss` 中
 * `$preset-best-{light|dark}-{admin|business}` 4 个 SCSS Map 必须一一对应；
 * 任意一侧调整都需要同步另一侧。
 */
export const PRESET_BEST_TOKENS: PresetBestTokens = Object.freeze({
    'best-light-admin': Object.freeze({ ...LIGHT_ADMIN, ...buildArcoBlueShadeVars() }),
    'best-light-business': Object.freeze({ ...LIGHT_BUSINESS, ...buildArcoBlueShadeVars() }),
    'best-dark-admin': Object.freeze({ ...DARK_ADMIN, ...buildArcoBlueShadeVars() }),
    'best-dark-business': Object.freeze({ ...DARK_BUSINESS, ...buildArcoBlueShadeVars() }),
}) as PresetBestTokens;

// =============================================================================
// IIFE 引导脚本源码
// =============================================================================

/**
 * 同步引导脚本源代码（自包含 IIFE）。
 *
 * **执行约束**：
 *   - 假设 `window.__BML_PRESET_BEST__` 已被外层声明语句提前赋值；
 *   - 仅依赖最基础的浏览器 API（`localStorage` / `matchMedia` / `document`），
 *     全部访问均在 `try/catch` 内，任何失败均静默回退；
 *   - `<body>` 在 `<head>` 内联脚本执行时通常尚未存在，因此对 `<body>` 的属性
 *     设置使用 `DOMContentLoaded` 兜底方案，确保最终一定会写入。
 *
 * **大小约束**：保持简短易读以便构建产物 gzip 后体积尽可能小；不强制极限压缩。
 */
export const BOOTSTRAP_SCRIPT_SOURCE: string = `(function(){try{var W=window;var D=document;var pathname=(W.location&&W.location.pathname)||"";var scope=pathname.indexOf("/business")===0?"business":"admin";var presets=W.__BML_PRESET_BEST__||{};var raw=null;try{raw=W.localStorage&&W.localStorage.getItem("bml-theme-"+scope);}catch(_){}var profile=null;if(raw){try{profile=JSON.parse(raw);}catch(_){profile=null;}}var prefersDark=false;try{prefersDark=!!(W.matchMedia&&W.matchMedia("(prefers-color-scheme: dark)").matches);}catch(_){}var resolvedMode;if(profile&&(profile.mode==="LIGHT"||profile.mode==="DARK")){resolvedMode=profile.mode==="DARK"?"dark":"light";}else if(profile&&profile.mode==="AUTO"){resolvedMode=prefersDark?"dark":"light";}else{resolvedMode=prefersDark?"dark":"light";}var variantKey="best-"+resolvedMode+"-"+scope;var variantTokens=presets[variantKey]||presets["best-light-admin"]||{};var tokens={};var k;for(k in variantTokens){if(Object.prototype.hasOwnProperty.call(variantTokens,k)){tokens[k]=variantTokens[k];}}if(profile){var hex=/^#[0-9A-Fa-f]{6}$/;var M={primaryColor:"--bml-color-primary",secondaryColor:"--bml-color-secondary",accentColor:"--bml-color-accent",successColor:"--bml-color-success",warningColor:"--bml-color-warning",errorColor:"--bml-color-error",infoColor:"--bml-color-info",textColor:"--bml-color-text-1",backgroundColor:"--bml-color-bg-1",borderColor:"--bml-color-border"};var fld;for(fld in M){if(Object.prototype.hasOwnProperty.call(M,fld)){var val=profile[fld];if(typeof val==="string"&&hex.test(val)){tokens[M[fld]]=val;}}}}var root=D.documentElement;if(root&&root.style){var cv;for(cv in tokens){if(Object.prototype.hasOwnProperty.call(tokens,cv)){root.style.setProperty(cv,tokens[cv]);}}}var applyBodyAttrs=function(){if(!D.body)return;D.body.setAttribute("arco-theme",resolvedMode==="dark"?"dark":"light");D.body.setAttribute("data-bml-scope",scope);};if(D.body){applyBodyAttrs();}else if(D.addEventListener){D.addEventListener("DOMContentLoaded",applyBodyAttrs,{once:true});}}catch(e){}})();`;

/**
 * 将 PRESET_BEST 数据 + IIFE 引导脚本拼接为单个内联 `<script>` 标签的内容。
 *
 * Vite 插件直接调用本函数获取需要 `injectTo: 'head'` 的 `children` 字符串，
 * 既保证了二者顺序（先赋值后执行），也避免插件代码与脚本字符串散落两处。
 *
 * @param presets PRESET_BEST 4 变体 Token；默认使用 {@link PRESET_BEST_TOKENS}。
 * @returns 形如 `window.__BML_PRESET_BEST__ = {...}; (function(){...})();` 的内联脚本内容
 */
export function buildBootstrapInlineScript(presets: PresetBestTokens = PRESET_BEST_TOKENS): string {
    // JSON.stringify 输出对所有 ASCII 字符安全；颜色值与 CSS 变量名均为 ASCII，
    // 因此可直接拼接进 `<script>` 标签内容而无需额外转义。
    const presetJson = JSON.stringify(presets);
    return `window.${THEME_PRESET_GLOBAL_KEY}=${presetJson};${BOOTSTRAP_SCRIPT_SOURCE}`;
}
