<template>
    <!--
        ══════════════════════════════════════════════════════
        BML 主题颜色 Section（ThemeColorSection）
        ──────────────────────────────────────────────────────
        - 渲染 10 个核心语义色字段（主色 / 辅色 / 状态色 / 文字 / 背景 / 边框）；
        - 使用 Arco `a-color-picker`，change 即触发 themeStore.patch 实时预览；
        - 自身样式仅消费 `var(--bml-*)` Token，禁止硬编码主题色。
        ══════════════════════════════════════════════════════
    -->
    <section class="theme-color-section" :aria-label="'颜色设置 - ' + scope">
        <header class="theme-color-section__header">
            <h3 class="theme-color-section__title">颜色设置</h3>
            <p class="theme-color-section__hint">
                调整 10 个核心语义色，所有改动即时同步至全部组件。
            </p>
        </header>

        <ul class="theme-color-section__grid">
            <li
                v-for="item in colorFields"
                :key="item.field"
                class="theme-color-section__item"
            >
                <div class="theme-color-section__row">
                    <span
                        class="theme-color-section__swatch"
                        :style="{ backgroundColor: profile[item.field] }"
                        aria-hidden="true"
                    ></span>
                    <div class="theme-color-section__meta">
                        <span class="theme-color-section__label">{{ item.label }}</span>
                        <span class="theme-color-section__value">
                            {{ profile[item.field] }}
                        </span>
                    </div>
                    <a-color-picker
                        :model-value="profile[item.field]"
                        size="small"
                        :show-history="false"
                        :show-preset="false"
                        format="hex"
                        @change="(value) => handleChange(item.field, value)"
                    />
                </div>
            </li>
        </ul>
    </section>
</template>

<script setup lang="ts">
/**
 * ThemeColorSection.vue —— 主题颜色配置区块。
 *
 * 关联任务：tasks.md 16.3。
 * 关联需求：requirements.md R4.1（10 个语义色字段） / R8.2（通用子组件） /
 *           R11.5（实时预览）。
 *
 * 渲染 10 个核心颜色字段：
 *   primaryColor / secondaryColor / accentColor /
 *   successColor / warningColor / errorColor / infoColor /
 *   textColor / backgroundColor / borderColor。
 *
 * 用户调整任一字段，组件立即调用 `themeStore.patch(scope, partial)`，
 * `applyTokens` 写入 DOM 自定义属性，实现“无刷新”预览。
 * 服务端持久化由 `useTheme().updateProfile`（任务 14.2）在更上层完成。
 */
import { computed } from 'vue';
import type { ThemeProfile, ThemeScope } from '@/types/theme';
import { useThemeStore } from '@/store/theme';

/**
 * 颜色字段元信息：字段名 + 中文标签。
 * 顺序与设计文档 ThemeProfile 字段表保持一致。
 */
type ColorField =
    | 'primaryColor'
    | 'secondaryColor'
    | 'accentColor'
    | 'successColor'
    | 'warningColor'
    | 'errorColor'
    | 'infoColor'
    | 'textColor'
    | 'backgroundColor'
    | 'borderColor';

interface ColorFieldMeta {
    /** ThemeProfile 字段名。 */
    field: ColorField;
    /** 中文标签。 */
    label: string;
}

/**
 * 组件入参。
 *
 * - `scope`：必填，决定读写 ADMIN 还是 BUSINESS 作用域的 ThemeProfile。
 */
const props = defineProps<{
    /** 当前 Section 所属的主题作用域。 */
    scope: ThemeScope;
}>();

/** 主题 store。提供 `patch(scope, partial)` 与作用域 Profile。 */
const themeStore = useThemeStore();

/**
 * 当前作用域生效的 ThemeProfile（响应式只读视图）。
 *
 * 避免直接在模板里写 `themeStore[scope.toLowerCase()]`，统一通过 computed
 * 拿到合法 Profile 引用，保证模板始终类型安全。
 */
const profile = computed<ThemeProfile>(() =>
    props.scope === 'ADMIN' ? themeStore.admin : themeStore.business,
);

/**
 * 颜色字段顺序与中文标签定义。
 *
 * 顺序与 design.md “Data Models / 共享 ThemeProfile 字段” 表对齐，
 * 便于后续维护时直接对照需求。
 */
const colorFields: ReadonlyArray<ColorFieldMeta> = [
    { field: 'primaryColor', label: '主色' },
    { field: 'secondaryColor', label: '辅助色' },
    { field: 'accentColor', label: '强调色' },
    { field: 'successColor', label: '成功色' },
    { field: 'warningColor', label: '警告色' },
    { field: 'errorColor', label: '错误色' },
    { field: 'infoColor', label: '信息色' },
    { field: 'textColor', label: '文字色' },
    { field: 'backgroundColor', label: '背景色' },
    { field: 'borderColor', label: '边框色' },
];

/**
 * 颜色变更处理。
 *
 * Arco `a-color-picker` 的 `change` 事件会传出 hex 字符串（不含透明度）。
 * 这里直接写回 store；非法 hex 由后端 `THEME_INVALID_PROFILE` 在持久化阶段拦截，
 * 本组件内不再做格式校验，避免重复实现。
 *
 * @param field 字段名。
 * @param value 颜色选择器吐出的值，正常情形下为 `#RRGGBB`。
 */
function handleChange(field: ColorField, value: string | number | undefined): void {
    if (typeof value !== 'string' || !value) {
        return;
    }
    themeStore.patch(props.scope, { [field]: value } as Partial<ThemeProfile>);
}
</script>

<style scoped>
.theme-color-section {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.theme-color-section__header {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.theme-color-section__title {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    color: var(--bml-color-text-1, #1d2129);
    letter-spacing: -0.2px;
}

.theme-color-section__hint {
    margin: 0;
    font-size: 12px;
    line-height: 1.5;
    color: var(--bml-color-text-3, #86909c);
}

.theme-color-section__grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
    margin: 0;
    padding: 0;
    list-style: none;
}

.theme-color-section__item {
    display: flex;
    flex-direction: column;
    padding: 8px 10px;
    border: 1px solid var(--bml-color-border, #e5e6eb);
    border-radius: var(--bml-radius-md, 6px);
    background-color: var(--bml-color-bg-2, #ffffff);
    transition:
        border-color 200ms ease,
        box-shadow 200ms ease;
}

.theme-color-section__item:hover {
    border-color: var(--bml-color-primary, #165dff);
    box-shadow: var(--bml-shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.04));
}

.theme-color-section__row {
    display: flex;
    align-items: center;
    gap: 10px;
}

.theme-color-section__swatch {
    flex-shrink: 0;
    width: 24px;
    height: 24px;
    border-radius: var(--bml-radius-sm, 4px);
    border: 1px solid var(--bml-color-border, #e5e6eb);
    box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.4);
}

.theme-color-section__meta {
    display: flex;
    flex-direction: column;
    gap: 2px;
    flex: 1;
    min-width: 0;
}

.theme-color-section__label {
    font-size: 12px;
    font-weight: 600;
    color: var(--bml-color-text-1, #1d2129);
    line-height: 1.3;
}

.theme-color-section__value {
    font-size: 11px;
    font-family: 'JetBrains Mono', 'Source Code Pro', Consolas, monospace;
    color: var(--bml-color-text-3, #86909c);
    text-transform: uppercase;
}
</style>
