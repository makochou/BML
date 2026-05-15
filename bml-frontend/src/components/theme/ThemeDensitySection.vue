<template>
    <!--
        ══════════════════════════════════════════════════════
        BML 紧凑度 Section（ThemeDensitySection）
        ──────────────────────────────────────────────────────
        - 在 COMPACT / DEFAULT / LOOSE 三档之间切换；
        - 影响全局 `--bml-spacing-*` 与组件高度，按比例缩放表格行高、
          表单控件、按钮等。
        ══════════════════════════════════════════════════════
    -->
    <section class="theme-density-section" :aria-label="'紧凑度 - ' + scope">
        <header class="theme-density-section__header">
            <h3 class="theme-density-section__title">紧凑度</h3>
            <p class="theme-density-section__hint">
                影响全局间距与控件高度，按比例缩放。
            </p>
        </header>

        <a-radio-group
            type="button"
            size="medium"
            :model-value="profile.density"
            class="theme-density-section__group"
            @change="handleChange"
        >
            <a-radio
                v-for="opt in densityOptions"
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
 * ThemeDensitySection.vue —— 紧凑度选择区块。
 *
 * 关联任务：tasks.md 16.3。
 * 关联需求：requirements.md R4.4（紧凑度档位） / R8.2 / R11.5。
 */
import { computed } from 'vue';
import type { Density, ThemeProfile, ThemeScope } from '@/types/theme';
import { useThemeStore } from '@/store/theme';

/** 紧凑度选项元信息。 */
interface DensityOption {
    /** 枚举值，与后端 `Density` 对齐。 */
    value: Density;
    /** 中文标签。 */
    label: string;
}

/** 组件入参。 */
const props = defineProps<{
    /** 主题作用域。 */
    scope: ThemeScope;
}>();

const themeStore = useThemeStore();

/** 当前作用域 Profile。 */
const profile = computed<ThemeProfile>(() =>
    props.scope === 'ADMIN' ? themeStore.admin : themeStore.business,
);

const densityOptions: ReadonlyArray<DensityOption> = [
    { value: 'COMPACT', label: '紧凑' },
    { value: 'DEFAULT', label: '默认' },
    { value: 'LOOSE', label: '宽松' },
];

/** 写回 store。 */
function handleChange(value: string | number | boolean): void {
    if (value !== 'COMPACT' && value !== 'DEFAULT' && value !== 'LOOSE') {
        return;
    }
    themeStore.patch(props.scope, { density: value });
}
</script>

<style scoped>
.theme-density-section {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.theme-density-section__header {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.theme-density-section__title {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    color: var(--bml-color-text-1, #1d2129);
    letter-spacing: -0.2px;
}

.theme-density-section__hint {
    margin: 0;
    font-size: 12px;
    line-height: 1.5;
    color: var(--bml-color-text-3, #86909c);
}

.theme-density-section__group {
    align-self: flex-start;
}
</style>
