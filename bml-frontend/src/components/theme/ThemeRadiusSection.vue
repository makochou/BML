<template>
    <!--
        ══════════════════════════════════════════════════════
        BML 圆角风格 Section（ThemeRadiusSection）
        ──────────────────────────────────────────────────────
        - 在 SHARP / SMALL / MEDIUM / LARGE 四档之间切换；
        - 每个档位附带一个迷你方块预览，让用户在确认前感知圆角差异。
        ══════════════════════════════════════════════════════
    -->
    <section class="theme-radius-section" :aria-label="'圆角风格 - ' + scope">
        <header class="theme-radius-section__header">
            <h3 class="theme-radius-section__title">圆角风格</h3>
            <p class="theme-radius-section__hint">
                影响全局 `--bml-radius-sm/md/lg` 三个 Token 的取值。
            </p>
        </header>

        <a-radio-group
            type="button"
            size="medium"
            :model-value="profile.radius"
            class="theme-radius-section__group"
            @change="handleChange"
        >
            <a-radio
                v-for="opt in radiusOptions"
                :key="opt.value"
                :value="opt.value"
            >
                <span class="theme-radius-section__option">
                    <span
                        class="theme-radius-section__preview"
                        :style="{ borderRadius: opt.previewRadius }"
                    ></span>
                    {{ opt.label }}
                </span>
            </a-radio>
        </a-radio-group>
    </section>
</template>

<script setup lang="ts">
/**
 * ThemeRadiusSection.vue —— 圆角风格选择区块。
 *
 * 关联任务：tasks.md 16.3。
 * 关联需求：requirements.md R4.3（圆角档位） / R8.2 / R11.5。
 */
import { computed } from 'vue';
import type { RadiusStyle, ThemeProfile, ThemeScope } from '@/types/theme';
import { useThemeStore } from '@/store/theme';

/** 圆角选项元信息。 */
interface RadiusOption {
    /** 枚举值，与后端 `RadiusStyle` 对齐。 */
    value: RadiusStyle;
    /** 中文标签。 */
    label: string;
    /** 缩略预览方块的圆角值（仅供本 Section 自身可视化使用，不写入 Token）。 */
    previewRadius: string;
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

/** 四档圆角，用于本地预览方块的圆角参考值。 */
const radiusOptions: ReadonlyArray<RadiusOption> = [
    { value: 'SHARP', label: '直角', previewRadius: '0px' },
    { value: 'SMALL', label: '小圆角', previewRadius: '2px' },
    { value: 'MEDIUM', label: '中圆角', previewRadius: '6px' },
    { value: 'LARGE', label: '大圆角', previewRadius: '12px' },
];

/** 写回 store。 */
function handleChange(value: string | number | boolean): void {
    if (
        value !== 'SHARP' &&
        value !== 'SMALL' &&
        value !== 'MEDIUM' &&
        value !== 'LARGE'
    ) {
        return;
    }
    themeStore.patch(props.scope, { radius: value });
}
</script>

<style scoped>
.theme-radius-section {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.theme-radius-section__header {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.theme-radius-section__title {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    color: var(--bml-color-text-1, #1d2129);
    letter-spacing: -0.2px;
}

.theme-radius-section__hint {
    margin: 0;
    font-size: 12px;
    line-height: 1.5;
    color: var(--bml-color-text-3, #86909c);
}

.theme-radius-section__group {
    align-self: flex-start;
}

.theme-radius-section__option {
    display: inline-flex;
    align-items: center;
    gap: 6px;
}

.theme-radius-section__preview {
    display: inline-block;
    width: 14px;
    height: 14px;
    background-color: var(--bml-color-primary, #165dff);
    border: 1px solid var(--bml-color-border, #e5e6eb);
}
</style>
