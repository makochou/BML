<template>
  <!--
    ══════════════════════════════════════════════════════
    BML 主题设置面板（ThemeSettingsPanel）
    ──────────────────────────────────────────────────────
    任务 16.4：保留原 `ThemeSettings.vue` 抽屉布局，组合
        7 个 Section（颜色 / 明暗 / 圆角 / 紧凑度 / 侧边栏 / 顶栏 / 字体）
        + 预设网格（ThemePresetCard）
        + 恢复默认按钮（ThemeRestoreButton）
    形成完整的双作用域主题配置入口。

    需求映射：
      - R4.8：字段非法时高亮 FieldError（顶部展示 fieldErrors 列表）。
      - R8.2：通用组件，通过 prop `scope` 控制作用域。
      - R11.5：每个维度提供实时预览（由各 Section 内部 patch + applyTokens 完成）。
      - R11.6：面板自身样式以 PRESET_BEST 作为基底，保持 “最美默认” 体验。

    可见性沿用现有 `useAppStore().settingsVisible`，与旧版 `ThemeSettings.vue`
    保持完全相同的开关入口（Layout / BusinessLayout 中的 dock 按钮）。
    ══════════════════════════════════════════════════════
  -->
  <a-drawer
    class="bml-theme-settings-drawer"
    :class="{ 'bml-theme-settings-drawer--compact': compact }"
    :width="380"
    :visible="appStore.settingsVisible"
    unmount-on-close
    :footer="false"
    :header="false"
    :closable="false"
    @cancel="appStore.toggleSettings(false)"
  >
    <div
      class="bml-theme-panel"
      :class="{ 'bml-theme-panel--compact': compact }"
      :data-bml-scope="scope"
    >
      <!-- ── 顶部 Hero ── -->
      <header class="bml-theme-panel__hero">
        <div class="bml-theme-panel__hero-bg" aria-hidden="true"></div>
        <div class="bml-theme-panel__hero-content">
          <div class="bml-theme-panel__hero-left">
            <div class="bml-theme-panel__hero-icon">
              <icon-palette class="bml-theme-panel__hero-icon-spin" />
            </div>
            <div>
              <h2 class="bml-theme-panel__hero-title">主题设置</h2>
              <p v-if="!compact" class="bml-theme-panel__hero-sub">
                THEME &amp; APPEARANCE · {{ scope }}
              </p>
            </div>
          </div>
          <button
            type="button"
            class="bml-theme-panel__close"
            aria-label="关闭主题设置"
            @click="appStore.toggleSettings(false)"
          >
            <icon-close />
          </button>
        </div>
      </header>

      <!-- ── 字段级错误提示（R4.8） ── -->
      <a-alert
        v-if="hasFieldErrors"
        class="bml-theme-panel__field-errors"
        type="error"
        :show-icon="true"
        :closable="false"
      >
        <template #title>主题配置存在 {{ fieldErrors!.length }} 个非法字段</template>
        <ul class="bml-theme-panel__field-error-list">
          <li
            v-for="fe in fieldErrors"
            :key="fe.field + ':' + fe.code"
            class="bml-theme-panel__field-error-item"
          >
            <code class="bml-theme-panel__field-error-name">{{ fe.field }}</code>
            <span class="bml-theme-panel__field-error-msg">{{ fe.message }}</span>
          </li>
        </ul>
      </a-alert>

      <!-- ── 主体：7 个 Section ── -->
      <div class="bml-theme-panel__body">
        <ThemeColorSection :scope="scope" />
        <div class="bml-theme-panel__divider" aria-hidden="true"></div>

        <ThemeModeSection :scope="scope" />
        <div class="bml-theme-panel__divider" aria-hidden="true"></div>

        <ThemeRadiusSection :scope="scope" />
        <div class="bml-theme-panel__divider" aria-hidden="true"></div>

        <ThemeDensitySection :scope="scope" />
        <div class="bml-theme-panel__divider" aria-hidden="true"></div>

        <ThemeSidebarSection :scope="scope" />
        <div class="bml-theme-panel__divider" aria-hidden="true"></div>

        <ThemeHeaderSection :scope="scope" />
        <div class="bml-theme-panel__divider" aria-hidden="true"></div>

        <ThemeFontSection :scope="scope" />
        <div class="bml-theme-panel__divider" aria-hidden="true"></div>

        <!-- ── 预设网格 ── -->
        <section class="bml-theme-panel__presets" aria-label="主题预设">
          <header class="bml-theme-panel__section-header">
            <h3 class="bml-theme-panel__section-title">主题预设</h3>
            <p v-if="!compact" class="bml-theme-panel__section-hint">
              点击卡片应用整套预设；内置预设带 “内置” 标识。
            </p>
          </header>

          <div v-if="presets.length === 0" class="bml-theme-panel__presets-empty">
            暂无可用预设
          </div>
          <div v-else class="bml-theme-panel__presets-grid">
            <ThemePresetCard
              v-for="preset in presets"
              :key="preset.id"
              :preset="preset"
              :scope="scope"
              :active="isPresetActive(preset.id)"
              @apply="handleApplyPreset"
            />
          </div>
        </section>
      </div>

      <!-- ── 底部：恢复默认 ── -->
      <footer class="bml-theme-panel__footer">
        <ThemeRestoreButton :scope="scope" />
        <span v-if="!compact" class="bml-theme-panel__footer-tag">
          BML Design System
        </span>
      </footer>
    </div>
  </a-drawer>
</template>

<script setup lang="ts">
/**
 * ThemeSettingsPanel.vue —— 双作用域主题设置面板。
 *
 * 关联任务：tasks.md 16.4。
 * 关联需求：requirements.md R4.8 / R8.2 / R11.5 / R11.6。
 *
 * 设计要点：
 *   - 仅作为 “容器 + 装饰 + 错误展示”，全部维度的实际读写交由 7 个子
 *     Section 通过 `useThemeStore().patch(scope, partial)` 完成；
 *   - 预设应用与恢复默认走 `useTheme(scope)` 的 `applyPreset` /
 *     `restoreDefault`，以触发 “本地 → 后端持久化 → 服务端结果回填” 全流程；
 *   - 字段级错误高亮通过 `useTheme().error.fieldErrors` 在顶部统一展示，
 *     保留与后端 `THEME_INVALID_PROFILE` 的字段对照；
 *   - 抽屉可见性沿用 `useAppStore().settingsVisible`，与旧版 `ThemeSettings.vue`
 *     的入口（Layout / BusinessLayout 的 dock 按钮）完全对齐；
 *   - `compact` 模式下隐藏次要文案（hero 副标题、提示文本、底部签名），
 *     便于在更紧凑的入口（如下拉菜单弹层）中复用同一组件。
 */
import { computed } from 'vue';
import { IconClose, IconPalette } from '@arco-design/web-vue/es/icon';

import ThemeColorSection from './ThemeColorSection.vue';
import ThemeModeSection from './ThemeModeSection.vue';
import ThemeRadiusSection from './ThemeRadiusSection.vue';
import ThemeDensitySection from './ThemeDensitySection.vue';
import ThemeSidebarSection from './ThemeSidebarSection.vue';
import ThemeHeaderSection from './ThemeHeaderSection.vue';
import ThemeFontSection from './ThemeFontSection.vue';
import ThemePresetCard from './ThemePresetCard.vue';
import ThemeRestoreButton from './ThemeRestoreButton.vue';

import { useAppStore } from '@/store/app';
import { useTheme } from '@/composables/useTheme';
import type { ThemeFieldError, ThemeScope } from '@/types/theme';

/**
 * 组件入参。
 *
 * - `scope`：必填，决定面板操作的主题作用域（ADMIN / BUSINESS）。
 *   抽屉本身全局唯一（按 `appStore.settingsVisible` 控制可见性），
 *   父布局根据当前所在作用域显式传入。
 * - `compact`：可选，紧凑模式下隐藏次要文案，缩减内边距。
 */
const props = withDefaults(
    defineProps<{
        /** 当前主题作用域。 */
        scope: ThemeScope;
        /** 是否启用紧凑布局。 */
        compact?: boolean;
    }>(),
    {
        compact: false,
    },
);

/** 应用级 store：提供抽屉可见性状态与切换动作。 */
const appStore = useAppStore();

/**
 * 主题 Composable。
 *
 * 显式传入 `props.scope`，避免 `useTheme` 在路由不在 ADMIN / BUSINESS
 * 任一被识别布局时抛出 `THEME_SCOPE_UNRESOLVED`（R8.AC8）。
 */
const theme = useTheme(props.scope);

/** 当前作用域生效的 Profile（响应式只读）。 */
const profile = theme.profile;
/** 共享预设列表。 */
const presets = theme.presets;
/** 当前作用域最近一次错误对象（含字段级明细）。 */
const error = theme.error;

/** 字段级错误列表，仅在 `THEME_INVALID_PROFILE` 时存在。 */
const fieldErrors = computed<ThemeFieldError[] | undefined>(
    () => error.value?.fieldErrors,
);

/** 是否需要展示顶部字段错误提示。 */
const hasFieldErrors = computed<boolean>(
    () => Array.isArray(fieldErrors.value) && fieldErrors.value.length > 0,
);

/**
 * 判断某预设是否为当前作用域生效预设。
 *
 * 与 `ThemePresetCard.active` 联动，提供选中态视觉反馈。
 */
function isPresetActive(presetId: string): boolean {
    return profile.value.presetRef === presetId;
}

/**
 * 处理 `ThemePresetCard` 的 `apply` 事件。
 *
 * 直接转发至 `useTheme().applyPreset`，由 Composable 完成本地写入、
 * 服务端持久化与失败兜底；任何异常均在 Composable 内部消化，不会
 * 抛回到组件层。
 */
async function handleApplyPreset(presetId: string): Promise<void> {
    await theme.applyPreset(presetId);
}
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   一、Drawer 外壳（沿用旧版 ThemeSettings 的圆角 / 阴影质感）
   ═══════════════════════════════════════════════════════ */
:global(.bml-theme-settings-drawer .arco-drawer) {
    border-radius: 24px 0 0 24px !important;
    box-shadow:
        -40px 0 100px rgba(0, 0, 0, 0.06),
        -8px 0 30px rgba(0, 0, 0, 0.04) !important;
    overflow: hidden !important;
    /* 面板自身以 PRESET_BEST 为基底（R11.6）：使用 --bml-color-bg-2 兜底 #f7f8fa */
    background: var(--bml-color-bg-2, #f7f8fa) !important;
    border: none !important;
}

:global(body[arco-theme='dark'] .bml-theme-settings-drawer .arco-drawer) {
    background: var(--bml-color-bg-2, #141416) !important;
    box-shadow: -40px 0 100px rgba(0, 0, 0, 0.5) !important;
}

:global(.bml-theme-settings-drawer .arco-drawer-mask) {
    background: rgba(0, 0, 0, 0.15) !important;
    backdrop-filter: blur(2px) !important;
}

/* ═══════════════════════════════════════════════════════
   二、面板容器
   ═══════════════════════════════════════════════════════ */
.bml-theme-panel {
    position: relative;
    height: 100%;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    color: var(--bml-color-text-1, #1d2129);
    font-size: var(--bml-font-size-base, 14px);
    line-height: var(--bml-line-height-base, 1.5715);
}

/* ═══════════════════════════════════════════════════════
   三、Hero 顶部横幅
   ═══════════════════════════════════════════════════════ */
.bml-theme-panel__hero {
    position: relative;
    flex-shrink: 0;
    padding: 24px 22px 20px;
    overflow: hidden;
}

.bml-theme-panel--compact .bml-theme-panel__hero {
    padding: 16px 18px 14px;
}

.bml-theme-panel__hero-bg {
    position: absolute;
    inset: 0;
    z-index: 0;
    background: linear-gradient(
        135deg,
        var(--bml-color-primary, #165dff) 0%,
        var(--bml-color-secondary, #4080ff) 100%
    );
    opacity: 0.08;
    pointer-events: none;
}

.bml-theme-panel__hero-content {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.bml-theme-panel__hero-left {
    display: flex;
    align-items: center;
    gap: 14px;
    min-width: 0;
}

.bml-theme-panel__hero-icon {
    width: 42px;
    height: 42px;
    border-radius: var(--bml-radius-lg, 12px);
    background: linear-gradient(
        135deg,
        var(--bml-color-primary, #165dff) 0%,
        var(--bml-color-secondary, #4080ff) 100%
    );
    display: flex;
    align-items: center;
    justify-content: center;
    color: #ffffff;
    font-size: 20px;
    box-shadow: 0 8px 20px rgba(22, 93, 255, 0.3);
    flex-shrink: 0;
}

.bml-theme-panel__hero-icon-spin {
    animation: bml-theme-panel__icon-rotate 12s linear infinite;
}

@keyframes bml-theme-panel__icon-rotate {
    to {
        transform: rotate(360deg);
    }
}

.bml-theme-panel__hero-title {
    margin: 0;
    font-size: 18px;
    font-weight: 800;
    color: var(--bml-color-text-1, #1d2129);
    letter-spacing: -0.3px;
    line-height: 1.2;
}

.bml-theme-panel__hero-sub {
    margin: 4px 0 0;
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 1.6px;
    color: var(--bml-color-text-3, #86909c);
}

.bml-theme-panel__close {
    flex-shrink: 0;
    width: 32px;
    height: 32px;
    padding: 0;
    border: none;
    border-radius: var(--bml-radius-md, 10px);
    background: rgba(0, 0, 0, 0.05);
    color: var(--bml-color-text-3, #86909c);
    font-size: 14px;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
    appearance: none;
}

.bml-theme-panel__close:hover {
    background: var(--bml-color-error, #f53f3f);
    color: #ffffff;
    transform: rotate(90deg) scale(1.05);
    box-shadow: 0 6px 16px rgba(245, 63, 63, 0.3);
}

.bml-theme-panel__close:focus-visible {
    outline: 2px solid var(--bml-color-primary, #165dff);
    outline-offset: 2px;
}

/* ═══════════════════════════════════════════════════════
   四、字段错误提示
   ═══════════════════════════════════════════════════════ */
.bml-theme-panel__field-errors {
    margin: 0 22px 12px;
    border-radius: var(--bml-radius-md, 8px);
}

.bml-theme-panel--compact .bml-theme-panel__field-errors {
    margin: 0 18px 10px;
}

.bml-theme-panel__field-error-list {
    margin: 6px 0 0;
    padding: 0 0 0 4px;
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 12px;
    line-height: 1.5;
}

.bml-theme-panel__field-error-item {
    display: flex;
    gap: 8px;
    align-items: baseline;
}

.bml-theme-panel__field-error-name {
    flex-shrink: 0;
    padding: 1px 6px;
    border-radius: var(--bml-radius-sm, 4px);
    background-color: rgba(245, 63, 63, 0.12);
    color: var(--bml-color-error, #f53f3f);
    font-family: 'JetBrains Mono', 'Source Code Pro', Consolas, monospace;
    font-size: 11px;
    font-weight: 600;
}

.bml-theme-panel__field-error-msg {
    color: var(--bml-color-text-2, #4e5969);
    word-break: break-word;
}

/* ═══════════════════════════════════════════════════════
   五、主体区
   ═══════════════════════════════════════════════════════ */
.bml-theme-panel__body {
    flex: 1;
    overflow-y: auto;
    padding: 4px 22px 24px;
    display: flex;
    flex-direction: column;
    gap: 18px;
    scrollbar-width: thin;
    scrollbar-color: var(--bml-color-border, #e5e6eb) transparent;
}

.bml-theme-panel--compact .bml-theme-panel__body {
    padding: 4px 18px 18px;
    gap: 14px;
}

.bml-theme-panel__body::-webkit-scrollbar {
    width: 6px;
}

.bml-theme-panel__body::-webkit-scrollbar-thumb {
    background-color: var(--bml-color-border, #e5e6eb);
    border-radius: 999px;
}

.bml-theme-panel__divider {
    height: 1px;
    background: linear-gradient(
        to right,
        transparent,
        var(--bml-color-border, #e5e6eb),
        transparent
    );
    border: 0;
    margin: 0;
}

/* ═══════════════════════════════════════════════════════
   六、预设网格
   ═══════════════════════════════════════════════════════ */
.bml-theme-panel__presets {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.bml-theme-panel__section-header {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.bml-theme-panel__section-title {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    color: var(--bml-color-text-1, #1d2129);
    letter-spacing: -0.2px;
}

.bml-theme-panel__section-hint {
    margin: 0;
    font-size: 12px;
    line-height: 1.5;
    color: var(--bml-color-text-3, #86909c);
}

.bml-theme-panel__presets-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
}

.bml-theme-panel__presets-empty {
    padding: 16px;
    border: 1px dashed var(--bml-color-border, #e5e6eb);
    border-radius: var(--bml-radius-md, 8px);
    color: var(--bml-color-text-3, #86909c);
    font-size: 12px;
    text-align: center;
}

/* ═══════════════════════════════════════════════════════
   七、底部 Footer
   ═══════════════════════════════════════════════════════ */
.bml-theme-panel__footer {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 14px 22px 18px;
    border-top: 1px solid var(--bml-color-border, #e5e6eb);
    background-color: var(--bml-color-bg-1, #ffffff);
}

.bml-theme-panel--compact .bml-theme-panel__footer {
    padding: 10px 18px 14px;
}

.bml-theme-panel__footer-tag {
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 1.2px;
    color: var(--bml-color-text-3, #86909c);
    text-transform: uppercase;
}
</style>
