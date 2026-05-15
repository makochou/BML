<template>
  <!-- ══════════════════════════════════════════════════════
       BML 偏好设置抽屉
       ──────────────────────────────────────────────────────
       功能模块：
         1. 核心色彩  — 8 种预设主题色一键切换
         2. 外观模式  — 亮色 / 暗色模式切换
         3. 侧边栏主题 — 白色 / 暗色 / 主色 三种外观
         4. 头部栏主题 — 透明 / 白色 / 暗色 / 主色 四种外观
         5. 实验室    — 色弱滤镜等辅助功能

       说明：本组件是任务 13.2 迁移后的“过渡版本”，
       主题维度（主色、明暗、侧边栏、顶栏）已改为 `useThemeStore()`，
       色弱滤镜与抽屉可见性仍由 `useAppStore()` 维护。
       该组件将在任务 16.5 中被完整 `ThemeSettingsPanel.vue` 替换。
       ══════════════════════════════════════════════════════ -->
  <a-drawer
    class="bml-settings-drawer"
    :width="380"
    :visible="appStore.settingsVisible"
    unmount-on-close
    :footer="false"
    :header="false"
    :closable="false"
    @cancel="appStore.toggleSettings(false)"
  >
    <div class="s-panel">

      <!-- ── 顶部横幅 ── -->
      <div class="s-hero">
        <div class="s-hero-bg"></div>
        <div class="s-hero-content">
          <div class="s-hero-left">
            <div class="s-hero-icon">
              <icon-palette class="s-gear-spin" />
            </div>
            <div>
              <h2 class="s-hero-title">偏好设置</h2>
              <p class="s-hero-sub">THEME &amp; APPEARANCE</p>
            </div>
          </div>
          <div class="s-close-btn" @click="appStore.toggleSettings(false)">
            <icon-close />
          </div>
        </div>
      </div>

      <!-- ── 主体内容 ── -->
      <div class="s-body">

        <!-- ———— 1. 核心色彩 ———— -->
        <section class="s-section">
          <div class="s-sec-head">
            <div class="s-sec-label">
              <icon-star-fill class="s-sec-icon" />
              <span>核心色彩</span>
            </div>
            <span class="s-active-badge">
              {{ currentColorName }}
            </span>
          </div>
          <div class="s-color-grid">
            <div
              v-for="color in themeColors" :key="color.value"
              class="s-color-item"
              :class="{ active: currentProfile.primaryColor.toUpperCase() === color.value.toUpperCase() }"
              @click="handleColorChange(color.value)"
            >
              <div class="s-color-swatch" :style="{ background: color.value }">
                <transition name="s-check-fade">
                  <icon-check v-if="currentProfile.primaryColor.toUpperCase() === color.value.toUpperCase()" class="s-check-icon" />
                </transition>
              </div>
              <span class="s-color-name">{{ color.name }}</span>
            </div>
          </div>
        </section>

        <div class="s-divider"></div>

        <!-- ———— 2. 外观模式 ———— -->
        <section class="s-section">
          <div class="s-sec-head">
            <div class="s-sec-label">
              <icon-sun class="s-sec-icon" />
              <span>外观模式</span>
            </div>
          </div>
          <div class="s-mode-row">
            <div
              class="s-mode-card"
              :class="{ active: currentProfile.mode === 'LIGHT' }"
              @click="handleModeChange('LIGHT')"
            >
              <div class="s-mode-preview s-mode-light">
                <div class="s-mp-sidebar"></div>
                <div class="s-mp-main">
                  <div class="s-mp-header"></div>
                  <div class="s-mp-content"></div>
                </div>
              </div>
              <span class="s-mode-label">浅色</span>
            </div>
            <div
              class="s-mode-card"
              :class="{ active: currentProfile.mode === 'DARK' }"
              @click="handleModeChange('DARK')"
            >
              <div class="s-mode-preview s-mode-dark">
                <div class="s-mp-sidebar"></div>
                <div class="s-mp-main">
                  <div class="s-mp-header"></div>
                  <div class="s-mp-content"></div>
                </div>
              </div>
              <span class="s-mode-label">深色</span>
            </div>
          </div>
        </section>

        <div class="s-divider"></div>

        <!-- ———— 3. 侧边栏主题 ———— -->
        <section class="s-section">
          <div class="s-sec-head">
            <div class="s-sec-label">
              <icon-layout class="s-sec-icon" />
              <span>侧边栏主题</span>
            </div>
          </div>
          <div class="s-mode-row s-mode-row-3">
            <div
              v-for="opt in sidebarOptions" :key="opt.value"
              class="s-mode-card"
              :class="{ active: currentProfile.sidebarStyle === opt.value }"
              @click="handleSidebarChange(opt.value)"
            >
              <div class="s-mode-preview" :class="'s-sidebar-' + opt.classKey">
                <div class="s-mp-sidebar"></div>
                <div class="s-mp-main">
                  <div class="s-mp-header"></div>
                  <div class="s-mp-content"></div>
                </div>
              </div>
              <span class="s-mode-label">{{ opt.label }}</span>
            </div>
          </div>
        </section>

        <div class="s-divider"></div>

        <!-- ———— 4. 头部栏主题 ———— -->
        <section class="s-section">
          <div class="s-sec-head">
            <div class="s-sec-label">
              <icon-nav class="s-sec-icon" />
              <span>头部栏主题</span>
            </div>
          </div>
          <div class="s-mode-row s-mode-row-4">
            <div
              v-for="opt in headerOptions" :key="opt.value"
              class="s-mode-card"
              :class="{ active: currentProfile.headerStyle === opt.value }"
              @click="handleHeaderChange(opt.value)"
            >
              <div class="s-mode-preview" :class="'s-header-' + opt.classKey">
                <div class="s-mp-sidebar"></div>
                <div class="s-mp-main">
                  <div class="s-mp-header"></div>
                  <div class="s-mp-content"></div>
                </div>
              </div>
              <span class="s-mode-label">{{ opt.label }}</span>
            </div>
          </div>
        </section>

        <div class="s-divider"></div>

        <!-- ———— 5. 实验室 ———— -->
        <section class="s-section">
          <div class="s-sec-head">
            <div class="s-sec-label">
              <icon-experiment class="s-sec-icon" />
              <span>实验室</span>
            </div>
          </div>
          <div class="s-feature-card">
            <div class="s-feat-left">
              <div class="s-feat-icon s-feat-icon--green">
                <icon-eye />
              </div>
              <div class="s-feat-info">
                <span class="s-feat-title">色弱滤镜</span>
                <span class="s-feat-desc">全局高对比度色彩映射</span>
              </div>
            </div>
            <a-switch
              v-model="appStore.colorWeek"
              @change="handleColorWeekChange"
              type="round"
              size="small"
            />
          </div>
        </section>
      </div>

      <!-- ── 底部装饰 ── -->
      <div class="s-footer">
        <span>BML Design System v2.0</span>
      </div>
    </div>
  </a-drawer>
</template>

<script lang="ts" setup>
/**
 * BML 偏好设置抽屉组件（过渡版本）
 * ──────────────────────────────────────
 * 提供主题色、明暗模式、侧边栏 / 顶栏外观、色弱滤镜配置。
 *
 * 任务 13.2 迁移说明：
 *   - 主题维度（`primaryColor` / `mode` / `sidebarStyle` / `headerStyle`）
 *     现在通过 `useThemeStore()` 读取并通过 `themeStore.patch(scope, partial)`
 *     更新；写入会自动完成 store + DOM CSS 变量 + localStorage + 跨标签广播
 *     的三方一致性；
 *   - 抽屉可见性 (`settingsVisible`) 与色弱滤镜 (`colorWeek`) 仍由
 *     `useAppStore()` 维护；
 *   - 当前作用域通过路由路径推断（`/admin*` → ADMIN，否则 BUSINESS），
 *     与 `themeBootstrap.ts` 引导脚本中的判定保持一致；
 *   - 任务 16.5 将以新版 `ThemeSettingsPanel.vue` 完整替换本组件。
 */
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useAppStore } from '../store/app';
import { useThemeStore } from '../store/theme';
import { themeColors } from '../utils/theme';
import type {
    HeaderStyle,
    SidebarStyle,
    ThemeMode,
    ThemeProfile,
    ThemeScope,
} from '../types/theme';
import {
    IconClose, IconCheck, IconSun,
    IconExperiment, IconEye, IconStarFill,
    IconPalette, IconLayout, IconNav,
} from '@arco-design/web-vue/es/icon';

const appStore = useAppStore();
const themeStore = useThemeStore();
const route = useRoute();

/**
 * 根据当前路由推断主题作用域。
 *
 * - 路径以 `/admin` 开头时取 `ADMIN`；
 * - 其它（含 `/`、`/dashboard`、`/system/*` 等业务系统路由）取 `BUSINESS`；
 * - 与 `themeBootstrap.ts` 引导脚本及 `useTheme` Composable 的推断逻辑一致。
 */
const currentScope = computed<ThemeScope>(() => {
    const path = route.path || '';
    return path.startsWith('/admin') ? 'ADMIN' : 'BUSINESS';
});

/** 当前作用域生效的 ThemeProfile（响应式只读视图）。 */
const currentProfile = computed<ThemeProfile>(() =>
    currentScope.value === 'ADMIN' ? themeStore.admin : themeStore.business,
);

/* ── 当前选中的主题色名称 ── */
const currentColorName = computed(() =>
    themeColors.find(c => c.value.toUpperCase() === currentProfile.value.primaryColor.toUpperCase())
        ?.name || '自定义',
);

/* ── 侧边栏外观选项 ──
   `value` 为 SidebarStyle 枚举值（写入 themeStore），
   `classKey` 为缩略图样式类后缀（沿用旧版 white / dark / primary）。 */
const sidebarOptions: { value: SidebarStyle; classKey: string; label: string }[] = [
    { value: 'LIGHT', classKey: 'white', label: '白色' },
    { value: 'DARK', classKey: 'dark', label: '暗色' },
    { value: 'PRIMARY', classKey: 'primary', label: '主色' },
];

/* ── 头部栏外观选项 ── */
const headerOptions: { value: HeaderStyle; classKey: string; label: string }[] = [
    { value: 'TRANSPARENT', classKey: 'transparent', label: '透明' },
    { value: 'LIGHT', classKey: 'light', label: '白色' },
    { value: 'DARK', classKey: 'dark', label: '暗色' },
    { value: 'PRIMARY', classKey: 'primary', label: '主色' },
];

/* ── 事件处理 ── */

/** 切换主题色：写入 themeStore.primaryColor。 */
const handleColorChange = (color: string) => {
    /* themeColors 中的 value 形如 '#165DFF'，与后端 @HexColor 校验格式一致，
       直接 patch 即可触发 applyTokens 重新生成 10 级色阶。 */
    themeStore.patch(currentScope.value, { primaryColor: color });
};

/** 切换明暗模式（仅在抽屉中提供 LIGHT / DARK，AUTO 走系统订阅）。 */
const handleModeChange = (value: ThemeMode) => {
    themeStore.patch(currentScope.value, { mode: value });
};

/** 切换侧边栏风格。 */
const handleSidebarChange = (value: SidebarStyle) => {
    themeStore.patch(currentScope.value, { sidebarStyle: value });
};

/** 切换头部栏风格。 */
const handleHeaderChange = (value: HeaderStyle) => {
    themeStore.patch(currentScope.value, { headerStyle: value });
};

/** 色弱滤镜（无障碍特性，仍由 appStore 管理）。 */
const handleColorWeekChange = (val: boolean | string | number) => {
    appStore.toggleColorWeek(!!val);
};
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   Drawer 外壳
   ═══════════════════════════════════════════════════════ */
:global(.bml-settings-drawer .arco-drawer) {
    border-radius: 24px 0 0 24px !important;
    box-shadow:
        -40px 0 100px rgba(0, 0, 0, 0.06),
        -8px 0 30px rgba(0, 0, 0, 0.04) !important;
    overflow: hidden !important;
    background: #f8f9fb !important;
    border: none !important;
}
:global(body[arco-theme='dark'] .bml-settings-drawer .arco-drawer) {
    background: #141416 !important;
    box-shadow: -40px 0 100px rgba(0, 0, 0, 0.5) !important;
}
:global(.bml-settings-drawer .arco-drawer-mask) {
    background: rgba(0, 0, 0, 0.15) !important;
    backdrop-filter: blur(2px) !important;
}

/* ═══════════════════════════════════════════════════════
   面板容器
   ═══════════════════════════════════════════════════════ */
.s-panel {
    height: 100%;
    display: flex;
    flex-direction: column;
    position: relative;
    overflow: hidden;
}

/* ═══════════════════════════════════════════════════════
   顶部横幅（Hero）
   ═══════════════════════════════════════════════════════ */
.s-hero {
    position: relative;
    padding: 28px 24px 22px;
    overflow: hidden;
    flex-shrink: 0;
}
.s-hero-bg {
    position: absolute;
    inset: 0;
    z-index: 0;
    background: var(--bml-gradient, linear-gradient(135deg, #165DFF 0%, #3c7eff 100%));
    opacity: 0.06;
}
.s-hero-content {
    position: relative;
    z-index: 1;
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.s-hero-left {
    display: flex;
    align-items: center;
    gap: 14px;
}
.s-hero-icon {
    width: 42px;
    height: 42px;
    border-radius: 12px;
    background: var(--bml-gradient, linear-gradient(135deg, #165dff, #3c7eff));
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    color: #fff;
    box-shadow: 0 8px 20px var(--bml-shadow, rgba(22,93,255,0.3));
}
.s-gear-spin {
    animation: gear-rotate 10s linear infinite;
}
@keyframes gear-rotate {
    to { transform: rotate(360deg); }
}
.s-hero-title {
    margin: 0;
    font-size: 19px;
    font-weight: 800;
    color: #1d2129;
    letter-spacing: -0.3px;
    line-height: 1.2;
}
.s-hero-sub {
    margin: 2px 0 0;
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 2px;
    color: #86909c;
}
.s-close-btn {
    width: 32px;
    height: 32px;
    border-radius: 10px;
    border: none;
    outline: none;
    background: rgba(0,0,0,0.05);
    color: #86909c;
    font-size: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
    -webkit-appearance: none;
    appearance: none;
    padding: 0;
}
.s-close-btn:hover {
    background: #f53f3f;
    color: #fff;
    transform: rotate(90deg) scale(1.1);
    box-shadow: 0 6px 16px rgba(245,63,63,0.3);
}

/* ═══════════════════════════════════════════════════════
   主体区域
   ═══════════════════════════════════════════════════════ */
.s-body {
    flex: 1;
    overflow-y: auto;
    padding: 20px 24px 24px;
    display: flex;
    flex-direction: column;
    gap: 20px;
}
.s-body::-webkit-scrollbar {
    width: 4px;
}
.s-body::-webkit-scrollbar-thumb {
    background: rgba(0,0,0,0.08);
    border-radius: 4px;
}

/* ═══════════════════════════════════════════════════════
   Section 通用
   ═══════════════════════════════════════════════════════ */
.s-section {
    display: flex;
    flex-direction: column;
    gap: 12px;
}
.s-sec-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.s-sec-label {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    font-weight: 700;
    color: #1d2129;
    letter-spacing: -0.2px;
}
.s-sec-icon {
    font-size: 15px;
    color: var(--bml-primary, #165dff);
}
.s-active-badge {
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 0.5px;
    color: var(--bml-primary, #165dff);
    background: var(--bml-selected-bg, rgba(22,93,255,0.06));
    padding: 3px 10px;
    border-radius: 20px;
}
.s-divider {
    height: 1px;
    background: rgba(0,0,0,0.04);
    margin: 0 -4px;
}

/* ═══════════════════════════════════════════════════════
   色彩选择器
   ═══════════════════════════════════════════════════════ */
.s-color-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 6px 0;
}
.s-color-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 5px;
    cursor: pointer;
    padding: 8px 0;
    border-radius: 12px;
    transition: background 0.2s;
    position: relative;
}
.s-color-item:hover {
    background: rgba(0,0,0,0.02);
}
.s-color-swatch {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: inset 0 2px 4px rgba(255,255,255,0.35), 0 3px 8px rgba(0,0,0,0.1);
    transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
    position: relative;
}
.s-color-item:hover .s-color-swatch {
    transform: scale(1.12) translateY(-2px);
    box-shadow: inset 0 2px 4px rgba(255,255,255,0.4), 0 8px 20px rgba(0,0,0,0.15);
}
.s-color-item.active .s-color-swatch {
    transform: scale(1.15);
    box-shadow: inset 0 2px 4px rgba(255,255,255,0.4), 0 6px 20px rgba(0,0,0,0.2);
}
/* 选中态脉冲圈 */
.s-color-item.active::before {
    content: '';
    position: absolute;
    top: 2px;
    width: 44px;
    height: 44px;
    border-radius: 50%;
    border: 2.5px solid var(--bml-primary, #165dff);
    opacity: 0.5;
    animation: ring-breathe 2.5s ease-in-out infinite;
}
@keyframes ring-breathe {
    0%, 100% { transform: scale(1); opacity: 0.5; }
    50% { transform: scale(1.06); opacity: 0.2; }
}
.s-check-icon {
    color: #fff;
    font-size: 16px;
    filter: drop-shadow(0 1px 2px rgba(0,0,0,0.3));
}
/* 勾选动画 */
.s-check-fade-enter-active { transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
.s-check-fade-leave-active { transition: all 0.15s ease; }
.s-check-fade-enter-from { opacity: 0; transform: scale(0.3); }
.s-check-fade-leave-to { opacity: 0; transform: scale(0.3); }
.s-color-name {
    font-size: 10px;
    font-weight: 600;
    color: #86909c;
    transition: color 0.2s;
}
.s-color-item:hover .s-color-name { color: #4e5969; }
.s-color-item.active .s-color-name { color: var(--bml-primary, #165dff); font-weight: 700; }

/* ═══════════════════════════════════════════════════════
   外观模式 / 侧边栏 / 头部栏 — 缩略图卡片
   ═══════════════════════════════════════════════════════ */
.s-mode-row {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
}
.s-mode-row-3 {
    grid-template-columns: repeat(3, 1fr);
}
.s-mode-row-4 {
    grid-template-columns: repeat(4, 1fr);
}
.s-mode-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 10px 6px 8px;
    border-radius: 12px;
    border: 2px solid transparent;
    background: rgba(0,0,0,0.015);
    transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.s-mode-card:hover {
    background: rgba(0,0,0,0.03);
    transform: translateY(-1px);
}
.s-mode-card.active {
    border-color: var(--bml-primary, #165dff);
    background: var(--bml-selected-bg, rgba(22,93,255,0.06));
    box-shadow: 0 4px 14px rgba(var(--bml-primary-rgb, 22,93,255), 0.12);
}
.s-mode-label {
    font-size: 11px;
    font-weight: 600;
    color: #86909c;
    transition: color 0.2s;
}
.s-mode-card.active .s-mode-label {
    color: var(--bml-primary, #165dff);
    font-weight: 700;
}

/* ── 缩略图预览 ── */
.s-mode-preview {
    width: 100%;
    aspect-ratio: 16 / 10;
    border-radius: 8px;
    display: flex;
    overflow: hidden;
    border: 1px solid rgba(0,0,0,0.08);
    position: relative;
}
.s-mp-sidebar {
    width: 28%;
    height: 100%;
}
.s-mp-main {
    flex: 1;
    display: flex;
    flex-direction: column;
}
.s-mp-header {
    height: 22%;
}
.s-mp-content {
    flex: 1;
}

/* ── 外观模式缩略图配色 ── */
.s-mode-light .s-mp-sidebar { background: #fff; border-right: 1px solid rgba(0,0,0,0.06); }
.s-mode-light .s-mp-header { background: rgba(0,0,0,0.02); border-bottom: 1px solid rgba(0,0,0,0.04); }
.s-mode-light .s-mp-content { background: #f7f8fa; }

.s-mode-dark .s-mp-sidebar { background: #1e1e20; border-right: 1px solid rgba(255,255,255,0.06); }
.s-mode-dark .s-mp-header { background: #2a2a2d; border-bottom: 1px solid rgba(255,255,255,0.04); }
.s-mode-dark .s-mp-content { background: #17171a; }

/* ── 侧边栏主题缩略图 ── */
.s-sidebar-white .s-mp-sidebar { background: #fff; border-right: 1px solid rgba(0,0,0,0.06); }
.s-sidebar-white .s-mp-header { background: rgba(0,0,0,0.02); }
.s-sidebar-white .s-mp-content { background: #f7f8fa; }

.s-sidebar-dark .s-mp-sidebar { background: #1e1e20; }
.s-sidebar-dark .s-mp-header { background: rgba(0,0,0,0.02); }
.s-sidebar-dark .s-mp-content { background: #f7f8fa; }

.s-sidebar-primary .s-mp-sidebar { background: var(--bml-primary, #165dff); }
.s-sidebar-primary .s-mp-header { background: rgba(0,0,0,0.02); }
.s-sidebar-primary .s-mp-content { background: #f7f8fa; }

/* ── 头部栏主题缩略图 ── */
.s-header-transparent .s-mp-sidebar { background: #fff; border-right: 1px solid rgba(0,0,0,0.06); }
.s-header-transparent .s-mp-header { background: transparent; }
.s-header-transparent .s-mp-content { background: #f7f8fa; }

.s-header-light .s-mp-sidebar { background: #fff; border-right: 1px solid rgba(0,0,0,0.06); }
.s-header-light .s-mp-header { background: #fff; border-bottom: 1px solid rgba(0,0,0,0.06); }
.s-header-light .s-mp-content { background: #f7f8fa; }

.s-header-dark .s-mp-sidebar { background: #fff; border-right: 1px solid rgba(0,0,0,0.06); }
.s-header-dark .s-mp-header { background: #1e1e20; }
.s-header-dark .s-mp-content { background: #f7f8fa; }

.s-header-primary .s-mp-sidebar { background: #fff; border-right: 1px solid rgba(0,0,0,0.06); }
.s-header-primary .s-mp-header { background: var(--bml-primary, #165dff); }
.s-header-primary .s-mp-content { background: #f7f8fa; }

/* ═══════════════════════════════════════════════════════
   功能卡片
   ═══════════════════════════════════════════════════════ */
.s-feature-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
    border-radius: 14px;
    background: #fff;
    border: 1px solid rgba(0,0,0,0.04);
    box-shadow: 0 2px 10px rgba(0,0,0,0.02);
    transition: all 0.3s;
}
.s-feature-card:hover {
    box-shadow: 0 6px 20px rgba(0,0,0,0.04);
    transform: translateY(-1px);
}
.s-feat-left {
    display: flex;
    align-items: center;
    gap: 12px;
}
.s-feat-icon {
    width: 34px;
    height: 34px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 17px;
}
.s-feat-icon--green {
    background: rgba(0, 180, 42, 0.1);
    color: #00b42a;
}
.s-feat-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
}
.s-feat-title {
    font-size: 13px;
    font-weight: 700;
    color: #1d2129;
}
.s-feat-desc {
    font-size: 11px;
    color: #86909c;
}

/* ═══════════════════════════════════════════════════════
   底栏
   ═══════════════════════════════════════════════════════ */
.s-footer {
    flex-shrink: 0;
    padding: 14px 24px;
    text-align: center;
    font-size: 10px;
    font-weight: 600;
    color: #c9cdd4;
    letter-spacing: 1px;
}

/* ═══════════════════════════════════════════════════════
   暗色模式适配
   ═══════════════════════════════════════════════════════ */
:global(body[arco-theme='dark']) .s-hero-bg { opacity: 0.12; }
:global(body[arco-theme='dark']) .s-hero-title { color: #f2f3f5; }
:global(body[arco-theme='dark']) .s-close-btn { background: rgba(255,255,255,0.06); color: #86909c; }
:global(body[arco-theme='dark']) .s-close-btn:hover { background: #f53f3f; color: #fff; }
:global(body[arco-theme='dark']) .s-sec-label { color: #e5e6eb; }
:global(body[arco-theme='dark']) .s-divider { background: rgba(255,255,255,0.06); }
:global(body[arco-theme='dark']) .s-color-item:hover { background: rgba(255,255,255,0.04); }
:global(body[arco-theme='dark']) .s-color-name { color: #6b7785; }
:global(body[arco-theme='dark']) .s-color-item:hover .s-color-name { color: #86909c; }
:global(body[arco-theme='dark']) .s-mode-card { background: rgba(255,255,255,0.02); }
:global(body[arco-theme='dark']) .s-mode-card:hover { background: rgba(255,255,255,0.04); }
:global(body[arco-theme='dark']) .s-mode-card.active { background: rgba(var(--bml-primary-rgb, 22,93,255), 0.08); }
:global(body[arco-theme='dark']) .s-mode-preview { border-color: rgba(255,255,255,0.08); }
:global(body[arco-theme='dark']) .s-mode-label { color: #6b7785; }
:global(body[arco-theme='dark']) .s-feature-card {
    background: #1e1e20;
    border-color: rgba(255,255,255,0.04);
    box-shadow: 0 2px 12px rgba(0,0,0,0.3);
}
:global(body[arco-theme='dark']) .s-feature-card:hover { box-shadow: 0 8px 24px rgba(0,0,0,0.4); }
:global(body[arco-theme='dark']) .s-feat-title { color: #e5e6eb; }
:global(body[arco-theme='dark']) .s-footer { color: #3a3a3c; }
</style>
