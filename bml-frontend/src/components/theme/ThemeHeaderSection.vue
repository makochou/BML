<template>
    <!--
        ══════════════════════════════════════════════════════
        BML 顶部栏风格 Section（ThemeHeaderSection）
        ──────────────────────────────────────────────────────
        - 在 LIGHT / DARK / PRIMARY / TRANSPARENT 之间切换；
        - 控制顶部标题栏背景、文字、图标、用户菜单的 Token。
        ══════════════════════════════════════════════════════
    -->
    <section class="theme-header-section" :aria-label="'顶部栏风格 - ' + scope">
        <header class="theme-header-section__header">
            <h3 class="theme-header-section__title">顶部栏风格</h3>
            <p class="theme-header-section__hint">
                同步影响标题栏背景、文字、图标、用户菜单等元素。
            </p>
        </header>

        <a-radio-group
            type="button"
            size="medium"
            :model-value="profile.headerStyle"
            class="theme-header-section__group"
            @change="handleChange"
        >
            <a-radio
                v-for="opt in headerOptions"
                :key="opt.value"
                :value="opt.value"
            >
                {{ opt.label }}
            </a-radio>
        </a-radio-group>
    </section>
</template>

<script setup lang="ts">
/**
 * ThemeHeaderSection.vue —— 顶部栏风格选择区块。
 *
 * 关联任务：tasks.md 16.3。
 * 关联需求：requirements.md R4.6（顶部栏 Token 联动） / R8.2 / R11.5。
 */
import { computed } from 'vue';
import type { HeaderStyle, ThemeProfile, ThemeScope } from '@/types/theme';
import { useThemeStore } from '@/store/theme';

/** 顶部栏风格选项元信息。 */
interface HeaderOption {
    /** 枚举值，与后端 `HeaderStyle` 对齐。 */
    value: HeaderStyle;
    /** 中文标签。 */
    label: string;
}

/** 组件入参。 */
const props = defineProps<{
    /** 主题作用域。 */
    scope: ThemeScope;
}>();

const themeStore = useThemeStore();

const profile = computed<ThemeProfile>(() =>
    props.scope === 'ADMIN' ? themeStore.admin : themeStore.business,
);

const headerOptions: ReadonlyArray<HeaderOption> = [
    { value: 'LIGHT', label: '浅色' },
    { value: 'DARK', label: '深色' },
    { value: 'PRIMARY', label: '主色' },
    { value: 'TRANSPARENT', label: '透明' },
];

/** 写回 store。 */
function handleChange(value: string | number | boolean): void {
    if (
        value !== 'LIGHT' &&
        value !== 'DARK' &&
        value !== 'PRIMARY' &&
        value !== 'TRANSPARENT'
    ) {
        return;
    }
    themeStore.patch(props.scope, { headerStyle: value });
}
</script>

<style scoped>
.theme-header-section {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.theme-header-section__header {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.theme-header-section__title {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    color: var(--bml-color-text-1, #1d2129);
    letter-spacing: -0.2px;
}

.theme-header-section__hint {
    margin: 0;
    font-size: 12px;
    line-height: 1.5;
    color: var(--bml-color-text-3, #86909c);
}

.theme-header-section__group {
    align-self: flex-start;
}
</style>
