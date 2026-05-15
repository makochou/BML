<template>
  <!--
    主题预设卡片：
    - 顶部展示一段精简的“迷你工作台”缩略图，按 sidebar + header + content
      四个区块铺设主色 / 状态色，让用户在点击之前即可直观感知该预设的视觉风格；
    - 底部展示预设名称与可选描述；
    - 点击整张卡片对外抛出 `apply` 事件，由父级（通常是
      `ThemeSettingsPanel` 或直接调用 `useThemeStore().applyPreset`）执行切换；
    - `active` 控制选中态高亮（边框 + 阴影 + 主色描边）。
  -->
  <button
    type="button"
    class="theme-preset-card"
    :class="{ 'theme-preset-card--active': active }"
    :aria-pressed="active ? 'true' : 'false'"
    :title="preset.description || preset.name"
    @click="handleClick"
  >
    <!-- ── 缩略图：侧栏 + 顶栏 + 内容区四色块 ── -->
    <div
      class="theme-preset-card__thumbnail"
      :style="{ backgroundColor: profile.backgroundColor }"
      aria-hidden="true"
    >
      <!-- 左侧侧栏色块（取主色或主色相关风格） -->
      <div
        class="theme-preset-card__sidebar"
        :style="{ background: sidebarBackground }"
      >
        <span class="theme-preset-card__sidebar-item theme-preset-card__sidebar-item--active"></span>
        <span class="theme-preset-card__sidebar-item"></span>
        <span class="theme-preset-card__sidebar-item"></span>
      </div>

      <div class="theme-preset-card__main">
        <!-- 顶部 header 色块 -->
        <div
          class="theme-preset-card__header"
          :style="{ background: headerBackground }"
        >
          <span
            class="theme-preset-card__header-dot"
            :style="{ backgroundColor: profile.primaryColor }"
          ></span>
          <span
            class="theme-preset-card__header-bar"
            :style="{ backgroundColor: profile.borderColor }"
          ></span>
        </div>

        <!-- 内容区 4 个色块（主色 / 辅色 / 状态色 / 边框） -->
        <div class="theme-preset-card__content">
          <span
            class="theme-preset-card__swatch"
            :style="{ backgroundColor: profile.primaryColor }"
          ></span>
          <span
            class="theme-preset-card__swatch"
            :style="{ backgroundColor: profile.secondaryColor }"
          ></span>
          <span
            class="theme-preset-card__swatch"
            :style="{ backgroundColor: profile.successColor }"
          ></span>
          <span
            class="theme-preset-card__swatch"
            :style="{ backgroundColor: profile.warningColor }"
          ></span>
        </div>
      </div>

      <!-- 内置标识徽标（仅内置预设展示） -->
      <span v-if="preset.isBuiltIn" class="theme-preset-card__badge">内置</span>

      <!-- 选中态对勾 -->
      <span v-if="active" class="theme-preset-card__check" aria-hidden="true">
        <icon-check />
      </span>
    </div>

    <!-- ── 文案区：名称 + 描述 ── -->
    <div class="theme-preset-card__meta">
      <div class="theme-preset-card__name">
        {{ preset.name }}
        <span v-if="preset.isDefault" class="theme-preset-card__default-tag">默认</span>
      </div>
      <div v-if="preset.description" class="theme-preset-card__desc">
        {{ preset.description }}
      </div>
    </div>
  </button>
</template>

<script setup lang="ts">
/**
 * 通用主题预设卡片组件 `ThemePresetCard`。
 *
 * 关联任务：tasks.md 16.1
 * 关联需求：requirements.md R8.AC3（前端 SHALL 提供通用组件 `ThemePresetCard`
 * 用于展示单个 `Theme_Preset` 的预览缩略图与名称）。
 *
 * 设计说明：
 * - 组件本身是“无状态展示 + 事件外抛”，不直接依赖 `useThemeStore`，
 *   以便在主题设置面板、预设管理后台、登录前预览等不同上下文中复用；
 * - 作用域 `scope` 决定从 `preset.profileAdmin` 还是 `preset.profileBusiness`
 *   读取颜色，用于在 ADMIN / BUSINESS 两套布局中分别给出最贴近实际的预览；
 * - 视觉以 ThemeSettings.vue 旧版“mode-card”预览结构为参考，使用项目
 *   全局 Token (`--bml-radius-md` / `--bml-color-border` 等) 让卡片自身
 *   也跟随当前主题切换，保持设置面板视觉一致。
 */
import { computed } from 'vue';
import { IconCheck } from '@arco-design/web-vue/es/icon';
import type { ThemePreset, ThemeProfile, ThemeScope } from '@/types/theme';

/**
 * 组件属性。
 *
 * - `preset`：必填，待渲染的预设；
 * - `active`：可选，是否处于选中态（通常由父级根据用户当前 `presetRef` 计算得出）；
 * - `scope`：可选，决定缩略图采用 ADMIN 还是 BUSINESS 变体，默认 `ADMIN`，
 *   主要影响 `backgroundColor` / `bg-*` 等背景三级色的差异展示。
 */
const props = withDefaults(
    defineProps<{
        preset: ThemePreset;
        active?: boolean;
        scope?: ThemeScope;
    }>(),
    {
        active: false,
        scope: 'ADMIN',
    },
);

/**
 * 事件定义。
 *
 * - `apply`：用户点击卡片希望应用该预设。携带 `preset.id`，由调用方决定
 *   通过 `useTheme().applyPreset` / `useThemeStore().applyPreset` 等通道执行实际切换。
 */
const emit = defineEmits<{
    (e: 'apply', presetId: string): void;
}>();

/**
 * 当前作用域下使用的 ThemeProfile（缩略图所有色块均从该对象取色）。
 */
const profile = computed<ThemeProfile>(() =>
    props.scope === 'BUSINESS' ? props.preset.profileBusiness : props.preset.profileAdmin,
);

/**
 * 侧边栏色块背景：根据 `sidebarStyle` 在主色 / 暗色 / 浅色 / 透明之间切换，
 * 让缩略图能区分不同预设的侧边栏风格倾向。
 */
const sidebarBackground = computed<string>(() => {
    const p = profile.value;
    switch (p.sidebarStyle) {
        case 'PRIMARY':
            return p.primaryColor;
        case 'DARK':
            return '#1d2129';
        case 'TRANSPARENT':
            return 'rgba(0, 0, 0, 0.04)';
        case 'LIGHT':
        default:
            return '#ffffff';
    }
});

/**
 * 顶部栏色块背景：根据 `headerStyle` 派生。
 */
const headerBackground = computed<string>(() => {
    const p = profile.value;
    switch (p.headerStyle) {
        case 'PRIMARY':
            return p.primaryColor;
        case 'DARK':
            return '#1d2129';
        case 'TRANSPARENT':
            return 'transparent';
        case 'LIGHT':
        default:
            return '#ffffff';
    }
});

/**
 * 点击处理：抛出 `apply` 事件给上层。
 */
function handleClick(): void {
    emit('apply', props.preset.id);
}
</script>

<style scoped>
.theme-preset-card {
    /* 卡片整体：使用全局 Token 让自身跟随当前主题样式 */
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 10px;
    width: 100%;
    padding: 10px;
    border: 1px solid var(--bml-color-border, #e5e6eb);
    border-radius: var(--bml-radius-md, 8px);
    background-color: var(--bml-color-bg-2, #ffffff);
    box-shadow: var(--bml-shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.04));
    color: var(--bml-color-text-1, #1d2129);
    cursor: pointer;
    text-align: left;
    transition:
        border-color 200ms ease,
        box-shadow 200ms ease,
        transform 200ms ease;
    appearance: none;
    font: inherit;
}

.theme-preset-card:hover {
    border-color: var(--bml-color-primary, #165dff);
    box-shadow: var(--bml-shadow-md, 0 4px 12px rgba(0, 0, 0, 0.08));
    transform: translateY(-1px);
}

.theme-preset-card:focus-visible {
    outline: 2px solid var(--bml-color-primary, #165dff);
    outline-offset: 2px;
}

.theme-preset-card--active {
    border-color: var(--bml-color-primary, #165dff);
    box-shadow:
        0 0 0 2px var(--bml-color-primary, #165dff) inset,
        var(--bml-shadow-md, 0 4px 12px rgba(0, 0, 0, 0.08));
}

/* ── 缩略图主体 ── */
.theme-preset-card__thumbnail {
    position: relative;
    display: flex;
    height: 96px;
    overflow: hidden;
    border-radius: var(--bml-radius-sm, 4px);
    border: 1px solid var(--bml-color-border, #e5e6eb);
}

.theme-preset-card__sidebar {
    display: flex;
    flex-direction: column;
    gap: 6px;
    width: 28%;
    padding: 8px 6px;
}

.theme-preset-card__sidebar-item {
    display: block;
    height: 6px;
    border-radius: 3px;
    background-color: rgba(255, 255, 255, 0.35);
}

.theme-preset-card__sidebar-item--active {
    background-color: rgba(255, 255, 255, 0.85);
}

.theme-preset-card__main {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-width: 0;
}

.theme-preset-card__header {
    display: flex;
    align-items: center;
    gap: 6px;
    height: 22px;
    padding: 0 8px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.theme-preset-card__header-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
}

.theme-preset-card__header-bar {
    flex: 1;
    height: 4px;
    border-radius: 2px;
    opacity: 0.7;
}

.theme-preset-card__content {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 6px;
    flex: 1;
    padding: 8px;
}

.theme-preset-card__swatch {
    display: block;
    border-radius: var(--bml-radius-sm, 4px);
    box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.04);
}

/* ── 角标 ── */
.theme-preset-card__badge {
    position: absolute;
    top: 6px;
    right: 6px;
    padding: 1px 6px;
    border-radius: 999px;
    background-color: rgba(0, 0, 0, 0.55);
    color: #ffffff;
    font-size: 10px;
    line-height: 1.4;
    letter-spacing: 0.04em;
    pointer-events: none;
}

.theme-preset-card__check {
    position: absolute;
    bottom: 6px;
    right: 6px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background-color: var(--bml-color-primary, #165dff);
    color: #ffffff;
    font-size: 12px;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
    pointer-events: none;
}

/* ── 文案 ── */
.theme-preset-card__meta {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
}

.theme-preset-card__name {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 600;
    line-height: 1.4;
    color: var(--bml-color-text-1, #1d2129);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.theme-preset-card__default-tag {
    flex-shrink: 0;
    padding: 0 6px;
    border-radius: 999px;
    background-color: var(--bml-color-primary, #165dff);
    color: #ffffff;
    font-size: 10px;
    font-weight: 500;
    line-height: 1.6;
}

.theme-preset-card__desc {
    font-size: 12px;
    line-height: 1.5;
    color: var(--bml-color-text-3, #86909c);
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}
</style>
