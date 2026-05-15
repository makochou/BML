<template>
    <!--
        ══════════════════════════════════════════════════════
        BML 主题明暗模式 Section（ThemeModeSection）
        ──────────────────────────────────────────────────────
        - 在 LIGHT / DARK / AUTO 三个 ThemeMode 之间切换；
        - 选中状态直接绑定 themeStore 中当前作用域的 mode 字段，
          change 事件触发 patch，applyTokens 立即生效。
        ══════════════════════════════════════════════════════
    -->
    <section class="theme-mode-section" :aria-label="'明暗模式 - ' + scope">
        <header class="theme-mode-section__header">
            <h3 class="theme-mode-section__title">明暗模式</h3>
            <p class="theme-mode-section__hint">
                AUTO 模式将自动跟随系统的深色 / 浅色偏好。
            </p>
        </header>

        <a-radio-group
            type="button"
            size="medium"
            :model-value="profile.mode"
            class="theme-mode-section__group"
            @change="handleChange"
        >
            <a-radio
                v-for="opt in modeOptions"
                :key="opt.value"
                :value="opt.value"
            >
                <span class="theme-mode-section__option">
                    <component :is="opt.icon" class="theme-mode-section__icon" />
                    {{ opt.label }}
                </span>
            </a-radio>
        </a-radio-group>
    </section>
</template>

<script setup lang="ts">
/**
 * ThemeModeSection.vue —— 明暗模式选择区块。
 *
 * 关联任务：tasks.md 16.3。
 * 关联需求：requirements.md R4.2（AUTO 跟随系统） / R8.2 / R11.5。
 *
 * 通过 Arco `a-radio-group` 在 LIGHT / DARK / AUTO 之间切换；
 * 选中变化 → `themeStore.patch(scope, { mode })` → `applyTokens` 写入
 * `body[arco-theme]`，实现毫秒级预览。
 *
 * 注意：AUTO 模式下的实际 light/dark 解析已由 `themeEngine.applyTokens`
 * 通过 `prefers-color-scheme` 完成，本组件无需重复处理。
 */
import { computed, h, type Component } from 'vue';
import {
    IconSun,
    IconMoon,
    IconDesktop,
} from '@arco-design/web-vue/es/icon';
import type { ThemeMode, ThemeProfile, ThemeScope } from '@/types/theme';
import { useThemeStore } from '@/store/theme';

/** 单个 mode 选项元信息。 */
interface ModeOption {
    /** 枚举值，与后端 `ThemeMode` 对齐。 */
    value: ThemeMode;
    /** 中文标签。 */
    label: string;
    /** 图标组件，用于按钮内可视化提示。 */
    icon: Component;
}

/** 组件入参。 */
const props = defineProps<{
    /** 主题作用域。 */
    scope: ThemeScope;
}>();

/** 主题 store。 */
const themeStore = useThemeStore();

/** 当前作用域生效 Profile。 */
const profile = computed<ThemeProfile>(() =>
    props.scope === 'ADMIN' ? themeStore.admin : themeStore.business,
);

/**
 * 三种模式的展示顺序：浅色 → 深色 → 跟随系统。
 *
 * 用 `h(IconXxx)` 风格直接传图标组件而不是 string，避免在模板里再绕一次
 * `<component :is="...">` 的额外查表。
 */
const modeOptions: ReadonlyArray<ModeOption> = [
    { value: 'LIGHT', label: '浅色', icon: IconSun },
    { value: 'DARK', label: '深色', icon: IconMoon },
    { value: 'AUTO', label: '跟随系统', icon: IconDesktop },
];

/**
 * 处理 radio change：写回 store。
 *
 * Arco `a-radio-group` 的事件签名为 `(value: string | number | boolean) => void`，
 * 这里收窄为 `ThemeMode` 后调用 patch。非法值（如 string 类型字面量错误）
 * 仅会被 TypeScript 拦截，运行期保留 noop 兜底。
 *
 * 注：`h` 仅用于触发 vue 的 import 检查（在某些 setup 配置下移除未使用 import 会报错），
 * 此处保留给未来直接渲染 icon 时使用。
 */
function handleChange(value: string | number | boolean): void {
    void h;
    if (
        value !== 'LIGHT' &&
        value !== 'DARK' &&
        value !== 'AUTO'
    ) {
        return;
    }
    themeStore.patch(props.scope, { mode: value });
}
</script>

<style scoped>
.theme-mode-section {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.theme-mode-section__header {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.theme-mode-section__title {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    color: var(--bml-color-text-1, #1d2129);
    letter-spacing: -0.2px;
}

.theme-mode-section__hint {
    margin: 0;
    font-size: 12px;
    line-height: 1.5;
    color: var(--bml-color-text-3, #86909c);
}

.theme-mode-section__group {
    align-self: flex-start;
}

.theme-mode-section__option {
    display: inline-flex;
    align-items: center;
    gap: 6px;
}

.theme-mode-section__icon {
    font-size: 14px;
}
</style>
