<template>
    <!--
        ══════════════════════════════════════════════════════
        BML 字体大小 Section（ThemeFontSection）
        ──────────────────────────────────────────────────────
        - 在 SMALL / DEFAULT / LARGE / XLARGE 之间切换；
        - 影响 `--bml-font-size-base`，所有字号通过相对单位派生，
          确保选择即时生效。
        ══════════════════════════════════════════════════════
    -->
    <section class="theme-font-section" :aria-label="'字体大小 - ' + scope">
        <header class="theme-font-section__header">
            <h3 class="theme-font-section__title">字体大小</h3>
            <p class="theme-font-section__hint">
                通过 `--bml-font-size-base` 控制全局基础字号。
            </p>
        </header>

        <a-radio-group
            type="button"
            size="medium"
            :model-value="profile.fontScale"
            class="theme-font-section__group"
            @change="handleChange"
        >
            <a-radio
                v-for="opt in fontOptions"
                :key="opt.value"
                :value="opt.value"
            >
                <span
                    class="theme-font-section__option"
                    :style="{ fontSize: opt.previewFontSize }"
                >
                    {{ opt.label }}
                </span>
            </a-radio>
        </a-radio-group>
    </section>
</template>

<script setup lang="ts">
/**
 * ThemeFontSection.vue —— 字体大小档位选择区块。
 *
 * 关联任务：tasks.md 16.3。
 * 关联需求：requirements.md R4.7（字体档位 Token） / R8.2 / R11.5。
 *
 * 选项内文字使用对应档位的字号作为缩略预览，便于用户在切换前直观对比。
 */
import { computed } from 'vue';
import type { FontScale, ThemeProfile, ThemeScope } from '@/types/theme';
import { useThemeStore } from '@/store/theme';

/** 字体档位选项元信息。 */
interface FontOption {
    /** 枚举值，与后端 `FontScale` 对齐。 */
    value: FontScale;
    /** 中文标签。 */
    label: string;
    /** 选项标签的预览字号。 */
    previewFontSize: string;
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

const fontOptions: ReadonlyArray<FontOption> = [
    { value: 'SMALL', label: '小', previewFontSize: '12px' },
    { value: 'DEFAULT', label: '默认', previewFontSize: '14px' },
    { value: 'LARGE', label: '大', previewFontSize: '15px' },
    { value: 'XLARGE', label: '超大', previewFontSize: '16px' },
];

/** 写回 store。 */
function handleChange(value: string | number | boolean): void {
    if (
        value !== 'SMALL' &&
        value !== 'DEFAULT' &&
        value !== 'LARGE' &&
        value !== 'XLARGE'
    ) {
        return;
    }
    themeStore.patch(props.scope, { fontScale: value });
}
</script>

<style scoped>
.theme-font-section {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.theme-font-section__header {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.theme-font-section__title {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    color: var(--bml-color-text-1, #1d2129);
    letter-spacing: -0.2px;
}

.theme-font-section__hint {
    margin: 0;
    font-size: 12px;
    line-height: 1.5;
    color: var(--bml-color-text-3, #86909c);
}

.theme-font-section__group {
    align-self: flex-start;
}

.theme-font-section__option {
    display: inline-flex;
    align-items: center;
    line-height: 1;
}
</style>
