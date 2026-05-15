<template>
    <!--
        ══════════════════════════════════════════════════════
        BML 侧边栏风格 Section（ThemeSidebarSection）
        ──────────────────────────────────────────────────────
        - 包含两组单选：
            1. 展开态 sidebarStyle（LIGHT / DARK / TRANSPARENT / PRIMARY）；
            2. 折叠态 sidebarCollapsedStyle（仅 LIGHT / DARK）。
        - 任意修改即时写入 store，通过 applyTokens 更新侧边栏 Token。
        ══════════════════════════════════════════════════════
    -->
    <section class="theme-sidebar-section" :aria-label="'侧边栏风格 - ' + scope">
        <header class="theme-sidebar-section__header">
            <h3 class="theme-sidebar-section__title">侧边栏风格</h3>
            <p class="theme-sidebar-section__hint">
                展开态与折叠态可分别配置，便于实现“主色展开 + 深色折叠”等组合。
            </p>
        </header>

        <div class="theme-sidebar-section__field">
            <span class="theme-sidebar-section__label">展开态</span>
            <a-radio-group
                type="button"
                size="medium"
                :model-value="profile.sidebarStyle"
                @change="handleSidebarChange"
            >
                <a-radio
                    v-for="opt in sidebarOptions"
                    :key="opt.value"
                    :value="opt.value"
                >
                    {{ opt.label }}
                </a-radio>
            </a-radio-group>
        </div>

        <div class="theme-sidebar-section__field">
            <span class="theme-sidebar-section__label">折叠态</span>
            <a-radio-group
                type="button"
                size="medium"
                :model-value="profile.sidebarCollapsedStyle"
                @change="handleCollapsedChange"
            >
                <a-radio
                    v-for="opt in collapsedOptions"
                    :key="opt.value"
                    :value="opt.value"
                >
                    {{ opt.label }}
                </a-radio>
            </a-radio-group>
        </div>
    </section>
</template>

<script setup lang="ts">
/**
 * ThemeSidebarSection.vue —— 侧边栏风格选择区块。
 *
 * 关联任务：tasks.md 16.3。
 * 关联需求：requirements.md R4.5（侧边栏风格 Token 联动） /
 *           R8.2（通用子组件） / R11.5（实时预览）。
 *
 * 双 RadioGroup 实现：
 *   - `sidebarStyle`：展开态，4 选 1；
 *   - `sidebarCollapsedStyle`：折叠态，仅允许 LIGHT / DARK 两值，与
 *     `ThemeProfile.sidebarCollapsedStyle` 类型严格对齐。
 */
import { computed } from 'vue';
import type {
    SidebarCollapsedStyle,
    SidebarStyle,
    ThemeProfile,
    ThemeScope,
} from '@/types/theme';
import { useThemeStore } from '@/store/theme';

/** 展开态选项元信息。 */
interface SidebarStyleOption {
    /** 枚举值，与后端 `SidebarStyle` 对齐。 */
    value: SidebarStyle;
    /** 中文标签。 */
    label: string;
}

/** 折叠态选项元信息。 */
interface CollapsedStyleOption {
    value: SidebarCollapsedStyle;
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

const sidebarOptions: ReadonlyArray<SidebarStyleOption> = [
    { value: 'LIGHT', label: '浅色' },
    { value: 'DARK', label: '深色' },
    { value: 'TRANSPARENT', label: '透明' },
    { value: 'PRIMARY', label: '主色' },
];

const collapsedOptions: ReadonlyArray<CollapsedStyleOption> = [
    { value: 'LIGHT', label: '浅色' },
    { value: 'DARK', label: '深色' },
];

/** 展开态变更。 */
function handleSidebarChange(value: string | number | boolean): void {
    if (
        value !== 'LIGHT' &&
        value !== 'DARK' &&
        value !== 'TRANSPARENT' &&
        value !== 'PRIMARY'
    ) {
        return;
    }
    themeStore.patch(props.scope, { sidebarStyle: value });
}

/** 折叠态变更。 */
function handleCollapsedChange(value: string | number | boolean): void {
    if (value !== 'LIGHT' && value !== 'DARK') {
        return;
    }
    themeStore.patch(props.scope, { sidebarCollapsedStyle: value });
}
</script>

<style scoped>
.theme-sidebar-section {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.theme-sidebar-section__header {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.theme-sidebar-section__title {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    color: var(--bml-color-text-1, #1d2129);
    letter-spacing: -0.2px;
}

.theme-sidebar-section__hint {
    margin: 0;
    font-size: 12px;
    line-height: 1.5;
    color: var(--bml-color-text-3, #86909c);
}

.theme-sidebar-section__field {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
}

.theme-sidebar-section__label {
    flex-shrink: 0;
    font-size: 12px;
    font-weight: 600;
    color: var(--bml-color-text-2, #4e5969);
    letter-spacing: 0.2px;
    min-width: 48px;
}
</style>
