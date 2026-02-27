<template>
  <a-drawer
    class="bml-settings-drawer"
    :width="360"
    :visible="appStore.settingsVisible"
    unmount-on-close
    :footer="false"
    :header="false"
    :closable="false"
    @cancel="appStore.toggleSettings(false)"
  >
    <div class="s-panel">
      
      <!-- 顶部渐变横幅 + 关闭按钮 -->
      <div class="s-hero">
        <div class="s-hero-bg"></div>
        <div class="s-hero-content">
          <div class="s-hero-left">
            <div class="s-hero-icon">
              <icon-settings class="s-gear-spin" />
            </div>
            <div>
              <h2 class="s-hero-title">偏好设置</h2>
              <p class="s-hero-sub">APPEARANCE</p>
            </div>
          </div>
          <div class="s-close-btn" @click="appStore.toggleSettings(false)">✕</div>
        </div>
      </div>

      <!-- 主体内容 -->
      <div class="s-body">

        <!-- 色彩区域 -->
        <section class="s-section">
          <div class="s-sec-head">
            <div class="s-sec-label">
              <icon-star-fill class="s-sec-icon" />
              <span>核心色彩</span>
            </div>
            <span class="s-active-name">{{ themeColors.find(c => c.value === appStore.themeColor)?.name || 'Custom' }}</span>
          </div>

          <div class="s-color-picker">
            <div
              v-for="color in themeColors" :key="color.value"
              class="s-color-dot"
              :class="{ active: appStore.themeColor === color.value }"
              @click="handleColorChange(color.value)"
            >
              <div class="s-dot-inner" :style="{ background: color.value }">
                <icon-check v-if="appStore.themeColor === color.value" class="s-dot-check" />
              </div>
              <span class="s-dot-label">{{ color.name }}</span>
            </div>
          </div>
        </section>

        <div class="s-sep"></div>

        <!-- 实验室功能 -->
        <section class="s-section">
          <div class="s-sec-head">
            <div class="s-sec-label">
              <icon-experiment class="s-sec-icon" />
              <span>实验室</span>
            </div>
          </div>

          <div class="s-feature-card">
            <div class="s-feat-left">
              <div class="s-feat-icon">
                <icon-eye />
              </div>
              <div class="s-feat-info">
                <span class="s-feat-title">色弱滤镜</span>
                <span class="s-feat-desc">全局高对比度色彩映射</span>
              </div>
            </div>
            <a-switch v-model="appStore.colorWeek" @change="handleColorWeekChange" type="round" size="small" />
          </div>
        </section>

      </div>

      <!-- 底部装饰 -->
      <div class="s-footer">
        <span>BML Design System</span>
      </div>
    </div>
  </a-drawer>
</template>

<script lang="ts" setup>
import { useAppStore } from '../store/app';
import { themeColors, applyThemeColor } from '../utils/theme';
import { 
    IconSettings, IconCheck,
    IconExperiment, IconEye, IconStarFill
} from '@arco-design/web-vue/es/icon';

const appStore = useAppStore();

const handleColorChange = (color: string) => {
    appStore.updateSettings({ themeColor: color });
    applyThemeColor(color);
}

const handleColorWeekChange = (val: boolean | string | number) => {
    appStore.toggleColorWeek(!!val);
}
</script>

<style scoped>
/* ===================== Drawer 外壳 ===================== */
:global(.bml-settings-drawer .arco-drawer) {
    border-radius: 24px 0 0 24px !important;
    box-shadow:
        -40px 0 100px rgba(0, 0, 0, 0.06),
        -8px 0 30px rgba(0, 0, 0, 0.04) !important;
    overflow: hidden !important;
    background: #f8f9fb !important;
    border: none !important;
}
/* Arco close button is already prevented via :header="false" + :closable="false" */
:global(body[arco-theme='dark'] .bml-settings-drawer .arco-drawer) {
    background: #141416 !important;
    box-shadow: -40px 0 100px rgba(0, 0, 0, 0.5) !important;
}
/* 隐藏默认遮罩的不透明度 */
:global(.bml-settings-drawer .arco-drawer-mask) {
    background: rgba(0, 0, 0, 0.15) !important;
    backdrop-filter: blur(2px) !important;
}

/* ===================== 面板容器 ===================== */
.s-panel {
    height: 100%; display: flex; flex-direction: column;
    position: relative; overflow: hidden;
}

/* ===================== 顶部横幅（Hero） ===================== */
.s-hero {
    position: relative; padding: 32px 28px 24px; overflow: hidden; flex-shrink: 0;
}
.s-hero-bg {
    position: absolute; inset: 0; z-index: 0;
    background: var(--bml-gradient, linear-gradient(135deg, #165DFF 0%, #3c7eff 100%));
    opacity: 0.06;
}
.s-hero-content {
    position: relative; z-index: 1;
    display: flex; justify-content: space-between; align-items: center;
}
.s-hero-left {
    display: flex; align-items: center; gap: 14px;
}
.s-hero-icon {
    width: 42px; height: 42px; border-radius: 12px;
    background: var(--bml-primary, #165dff);
    display: flex; align-items: center; justify-content: center;
    font-size: 20px; color: #fff;
    box-shadow: 0 8px 20px var(--bml-shadow, rgba(22,93,255,0.3));
}
.s-gear-spin { animation: gear-rotate 10s linear infinite; }
@keyframes gear-rotate { to { transform: rotate(360deg); } }

.s-hero-title {
    margin: 0; font-size: 20px; font-weight: 800; color: #1d2129;
    letter-spacing: -0.3px; line-height: 1.2;
}
.s-hero-sub {
    margin: 2px 0 0; font-size: 10px; font-weight: 700; letter-spacing: 2px; color: #86909c;
}
.s-close-btn {
    width: 32px; height: 32px; border-radius: 10px; border: none; outline: none;
    background: rgba(0,0,0,0.05); color: #86909c; font-size: 14px;
    display: flex; align-items: center; justify-content: center;
    cursor: pointer; transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
    -webkit-appearance: none; appearance: none; padding: 0;
}
.s-close-btn:hover {
    background: #f53f3f; color: #fff;
    transform: rotate(90deg) scale(1.1);
    box-shadow: 0 6px 16px rgba(245,63,63,0.3);
}

/* ===================== 主体区域 ===================== */
.s-body {
    flex: 1; overflow-y: auto; padding: 24px 28px;
    display: flex; flex-direction: column; gap: 24px;
}

/* ===================== Section 通用 ===================== */
.s-section { display: flex; flex-direction: column; gap: 16px; }
.s-sec-head {
    display: flex; justify-content: space-between; align-items: center;
}
.s-sec-label {
    display: flex; align-items: center; gap: 8px;
    font-size: 14px; font-weight: 700; color: #1d2129; letter-spacing: -0.2px;
}
.s-sec-icon { font-size: 16px; color: var(--bml-primary, #165dff); }
.s-active-name {
    font-size: 11px; font-weight: 700; letter-spacing: 0.5px;
    color: var(--bml-primary, #165dff);
    background: var(--bml-primary-lighter, rgba(22,93,255,0.08));
    padding: 3px 10px; border-radius: 20px;
}

.s-sep { height: 1px; background: rgba(0,0,0,0.05); margin: 0 -4px; }

/* ===================== 色彩选择器 ===================== */
.s-color-picker {
    display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px 0;
}
.s-color-dot {
    display: flex; flex-direction: column; align-items: center; gap: 6px;
    cursor: pointer; padding: 8px 0;
    border-radius: 12px; transition: background 0.2s;
    position: relative;
}
.s-color-dot:hover { background: rgba(0,0,0,0.02); }

.s-dot-inner {
    width: 36px; height: 36px; border-radius: 50%;
    display: flex; align-items: center; justify-content: center;
    box-shadow: inset 0 2px 4px rgba(255,255,255,0.35), 0 3px 8px rgba(0,0,0,0.1);
    transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
    position: relative;
}
.s-color-dot:hover .s-dot-inner {
    transform: scale(1.12) translateY(-2px);
    box-shadow: inset 0 2px 4px rgba(255,255,255,0.4), 0 8px 20px rgba(0,0,0,0.15);
}
.s-color-dot.active .s-dot-inner {
    transform: scale(1.18);
    box-shadow: inset 0 2px 4px rgba(255,255,255,0.4), 0 6px 20px rgba(0,0,0,0.2);
}
/* 活跃态外圈 */
.s-color-dot.active::before {
    content: ''; position: absolute; top: 2px;
    width: 46px; height: 46px; border-radius: 50%;
    border: 2.5px solid var(--bml-primary, #165dff);
    opacity: 0.5;
    animation: ring-breathe 2.5s ease-in-out infinite;
}
@keyframes ring-breathe {
    0%, 100% { transform: scale(1); opacity: 0.5; }
    50% { transform: scale(1.08); opacity: 0.2; }
}

.s-dot-check { color: #fff; font-size: 16px; filter: drop-shadow(0 1px 2px rgba(0,0,0,0.3)); }

.s-dot-label {
    font-size: 10px; font-weight: 600; color: #86909c;
    transition: color 0.2s;
}
.s-color-dot:hover .s-dot-label { color: #4e5969; }
.s-color-dot.active .s-dot-label { color: var(--bml-primary, #165dff); font-weight: 700; }

/* ===================== 功能卡片 ===================== */
.s-feature-card {
    display: flex; align-items: center; justify-content: space-between;
    padding: 14px 16px; border-radius: 16px;
    background: #fff;
    border: 1px solid rgba(0,0,0,0.04);
    box-shadow: 0 2px 12px rgba(0,0,0,0.02);
    transition: all 0.3s;
}
.s-feature-card:hover {
    box-shadow: 0 6px 20px rgba(0,0,0,0.04);
    transform: translateY(-1px);
}
.s-feat-left { display: flex; align-items: center; gap: 14px; }
.s-feat-icon {
    width: 36px; height: 36px; border-radius: 10px;
    background: rgba(0, 180, 42, 0.1); color: #00b42a;
    display: flex; align-items: center; justify-content: center; font-size: 18px;
}
.s-feat-info { display: flex; flex-direction: column; gap: 2px; }
.s-feat-title { font-size: 14px; font-weight: 700; color: #1d2129; }
.s-feat-desc { font-size: 11px; color: #86909c; }

/* ===================== 底栏 ===================== */
.s-footer {
    flex-shrink: 0; padding: 16px 28px;
    text-align: center; font-size: 10px; font-weight: 600;
    color: #c9cdd4; letter-spacing: 1px;
}

/* ===================== 暗色模式 ===================== */
:global(body[arco-theme='dark']) .s-hero-bg { opacity: 0.12; }
:global(body[arco-theme='dark']) .s-hero-title { color: #f2f3f5; }
:global(body[arco-theme='dark']) .s-close-btn { background: rgba(255,255,255,0.06); color: #86909c; }
:global(body[arco-theme='dark']) .s-close-btn:hover { background: #f53f3f; color: #fff; }
:global(body[arco-theme='dark']) .s-sec-label { color: #e5e6eb; }
:global(body[arco-theme='dark']) .s-color-dot:hover { background: rgba(255,255,255,0.04); }
:global(body[arco-theme='dark']) .s-dot-label { color: #6b7785; }
:global(body[arco-theme='dark']) .s-color-dot:hover .s-dot-label { color: #86909c; }
:global(body[arco-theme='dark']) .s-sep { background: rgba(255,255,255,0.06); }
:global(body[arco-theme='dark']) .s-feature-card {
    background: #1e1e20; border-color: rgba(255,255,255,0.04);
    box-shadow: 0 2px 12px rgba(0,0,0,0.3);
}
:global(body[arco-theme='dark']) .s-feature-card:hover { box-shadow: 0 8px 24px rgba(0,0,0,0.4); }
:global(body[arco-theme='dark']) .s-feat-title { color: #e5e6eb; }
:global(body[arco-theme='dark']) .s-footer { color: #3a3a3c; }
</style>
