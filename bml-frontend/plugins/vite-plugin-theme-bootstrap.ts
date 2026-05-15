/**
 * Vite 插件：主题首屏防 FOUC 引导脚本注入器
 * ──────────────────────────────────────────────────────
 * 在每次 `index.html` 进入 Vite 处理流水线时，使用 `transformIndexHtml` 钩子
 * 在 `<head>` 内联同步 `<script>` 标签，承载：
 *   1. `window.__BML_PRESET_BEST__` 4 变体 Token 字典（来自
 *      `src/utils/themeBootstrap.ts` 中与 `tokens.preset-best.scss` 对齐的常量）；
 *   2. 自包含 IIFE 引导脚本，读取 `localStorage` / `prefers-color-scheme`
 *      并立即将关键 CSS 自定义属性写入 `<html>`。
 *
 * 设计要点：
 *   - 注入位置 `injectTo: 'head-prepend'`，保证脚本最早执行，先于任何
 *     CSS link / style / module script，从根本上消除主题首屏 FOUC；
 *   - 脚本内容由 {@link buildBootstrapInlineScript} 在 Node 上下文构造，
 *     避免在 IIFE 中再做模板替换；
 *   - 插件 `enforce: 'pre'`，确保不会被其它插件替换或污染该 head 顺序；
 *   - 当 `--BML-DISABLE-THEME-BOOTSTRAP=1` 环境变量被显式设置时，跳过注入，
 *     便于排查“是否是引导脚本副作用导致的样式异常”等场景。
 *
 * 关联需求：Requirements 6.3 / 6.5 / 13.2 / 10.2。
 */

import type { Plugin } from 'vite';
import { buildBootstrapInlineScript, PRESET_BEST_TOKENS } from '../src/utils/themeBootstrap';

/**
 * 插件可选配置项。
 *
 * 仅暴露最小必要项以保持调用方简单；如需在测试中替换 PRESET_BEST 数据集，
 * 可以传入自定义 `presets` 对象（必须满足 4 变体齐全）。
 */
export interface ThemeBootstrapPluginOptions {
    /**
     * 自定义 PRESET_BEST 4 变体 Token；默认使用 `themeBootstrap.PRESET_BEST_TOKENS`。
     * 仅推荐在测试 / 灰度场景下覆盖。
     */
    presets?: typeof PRESET_BEST_TOKENS;
    /**
     * 是否禁用注入；运行时 `process.env.BML_DISABLE_THEME_BOOTSTRAP === '1'`
     * 也可以禁用。默认 `false`。
     */
    disabled?: boolean;
}

/**
 * 创建主题引导脚本注入插件。
 *
 * @param options 见 {@link ThemeBootstrapPluginOptions}。
 * @returns Vite 插件对象。
 */
export function viteThemeBootstrap(options: ThemeBootstrapPluginOptions = {}): Plugin {
    const presets = options.presets ?? PRESET_BEST_TOKENS;
    const envDisabled = typeof process !== 'undefined' && process.env?.BML_DISABLE_THEME_BOOTSTRAP === '1';
    const disabled = options.disabled === true || envDisabled;

    return {
        name: 'bml:theme-bootstrap',
        // 在用户插件中尽早执行，确保 head 顶部的注入顺序稳定
        enforce: 'pre',
        transformIndexHtml: {
            order: 'pre',
            handler() {
                if (disabled) {
                    return [];
                }
                const inline = buildBootstrapInlineScript(presets);
                return [
                    {
                        tag: 'script',
                        attrs: {
                            // 标记便于调试时通过 DevTools / 文档页搜索定位
                            'data-bml-theme-bootstrap': 'true',
                        },
                        // 写入 head 顶部确保最早执行
                        injectTo: 'head-prepend',
                        children: inline,
                    },
                ];
            },
        },
    };
}

export default viteThemeBootstrap;
