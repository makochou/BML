<template>
  <a-drawer
    class="bml-hyper-drawer settings-drawer"
    :width="380"
    :visible="appStore.settingsVisible"
    unmount-on-close
    :footer="false"
    :header="false"
    @cancel="appStore.toggleSettings(false)"
  >
    <div class="h-container">
      
      <!-- 超视觉幻彩头部 -->
      <div class="h-header h-settings-header">
        <div class="h-glow-orb orb-1"></div>
        <div class="h-glow-orb orb-2"></div>
        
        <div class="h-header-inner">
           <div class="h-brand">
              <div class="h-icon-wrapper settings-spin">
                 <icon-settings />
              </div>
              <div class="h-title-group">
                 <h2 class="h-title">偏好设置</h2>
                 <p class="h-sub">APPEARANCE</p>
              </div>
           </div>
           
           <div class="h-close" @click="appStore.toggleSettings(false)">
              <icon-close />
           </div>
        </div>
      </div>

      <div class="h-scroll">
        
        <!-- 核心色彩 / 苹果风 Color Picker -->
        <div class="h-section">
          <div class="h-sec-header">
             <span class="h-sec-title">核心色彩</span>
             <span class="h-sec-badge">{{ themeColors.find(c => c.value === appStore.themeColor)?.name || 'Custom' }}</span>
          </div>
          
          <div class="h-color-grid">
            <div 
              v-for="color in themeColors" :key="color.value"
              class="h-color-item"
              :class="{ 'is-active': appStore.themeColor === color.value }"
              @click="handleColorChange(color.value)"
            >
              <div class="h-color-inner" :style="{ background: color.value }">
                 <icon-check class="h-check" v-if="appStore.themeColor === color.value" />
              </div>
            </div>
          </div>
        </div>

        <div class="h-divider"></div>

        <!-- 顶栏预设 / Vercel 风高光骨架屏 -->
        <div class="h-section">
          <div class="h-sec-header">
             <span class="h-sec-title">顶栏预设</span>
             <icon-layout class="h-sec-icon" />
          </div>
          
          <div class="h-layout-grid">
            <div class="h-layout-card" :class="{ 'is-active': appStore.headerTheme === 'transparent' }" @click="appStore.updateSettings({ headerTheme: 'transparent' })">
              <div class="h-card-visual">
                 <div class="h-skeleton-top glass"></div>
                 <div class="h-skeleton-body glass-bg"></div>
              </div>
              <span class="h-card-text">超感玻璃</span>
            </div>
            
            <div class="h-layout-card" :class="{ 'is-active': appStore.headerTheme === 'light' }" @click="appStore.updateSettings({ headerTheme: 'light' })">
              <div class="h-card-visual">
                 <div class="h-skeleton-top light"></div>
                 <div class="h-skeleton-body light-bg"></div>
              </div>
              <span class="h-card-text">纯净明亮</span>
            </div>

            <div class="h-layout-card" :class="{ 'is-active': appStore.headerTheme === 'dark' }" @click="appStore.updateSettings({ headerTheme: 'dark' })">
              <div class="h-card-visual">
                 <div class="h-skeleton-top dark"></div>
                 <div class="h-skeleton-body dark-bg"></div>
              </div>
              <span class="h-card-text">暗夜黑魅</span>
            </div>

            <div class="h-layout-card" :class="{ 'is-active': appStore.headerTheme === 'primary' }" @click="appStore.updateSettings({ headerTheme: 'primary' })">
              <div class="h-card-visual">
                 <div class="h-skeleton-top primary"></div>
                 <div class="h-skeleton-body light-bg"></div>
              </div>
              <span class="h-card-text">品牌焦点</span>
            </div>
          </div>
        </div>

        <div class="h-divider"></div>

        <!-- 侧边栏预设 -->
        <div class="h-section">
          <div class="h-sec-header">
             <span class="h-sec-title">侧边导航</span>
             <icon-menu-fold class="h-sec-icon" />
          </div>
          
          <div class="h-layout-grid grid-3">
            <div class="h-layout-card" :class="{ 'is-active': appStore.sidebarTheme === 'white' }" @click="appStore.updateSettings({ sidebarTheme: 'white' })">
              <div class="h-card-visual flex-row">
                 <div class="h-skeleton-side light"></div>
                 <div class="h-skeleton-body light-bg"></div>
              </div>
              <span class="h-card-text">亮色</span>
            </div>

            <div class="h-layout-card" :class="{ 'is-active': appStore.sidebarTheme === 'dark' }" @click="appStore.updateSettings({ sidebarTheme: 'dark' })">
              <div class="h-card-visual flex-row">
                 <div class="h-skeleton-side dark"></div>
                 <div class="h-skeleton-body light-bg"></div>
              </div>
              <span class="h-card-text">暗夜</span>
            </div>

            <div class="h-layout-card" :class="{ 'is-active': appStore.sidebarTheme === 'primary' }" @click="appStore.updateSettings({ sidebarTheme: 'primary' })">
              <div class="h-card-visual flex-row">
                 <div class="h-skeleton-side primary"></div>
                 <div class="h-skeleton-body light-bg"></div>
              </div>
              <span class="h-card-text">品牌</span>
            </div>
          </div>
        </div>
        
        <div class="h-divider"></div>

        <!-- 高级功能组 -->
        <div class="h-section">
          <div class="h-sec-header">
             <span class="h-sec-title">实验室功能</span>
             <icon-experiment class="h-sec-icon" />
          </div>
          
          <div class="h-list-group">
            <div class="h-list-item">
              <div class="h-item-icon color-weak-icon">
                 <icon-eye />
              </div>
              <div class="h-item-content">
                <span class="h-item-title">色弱滤镜</span>
                <span class="h-item-desc">全局高对比度色彩映射</span>
              </div>
              <a-switch v-model="appStore.colorWeek" @change="handleColorWeekChange" type="round" class="h-switch" />
            </div>
          </div>
        </div>

      </div>
    </div>
  </a-drawer>
</template>

<script lang="ts" setup>
import { useAppStore } from '../store/app';
import { 
    IconSettings, IconLayout, 
    IconMenuFold, IconClose, IconCheck,
    IconExperiment, IconEye
} from '@arco-design/web-vue/es/icon';

const appStore = useAppStore();

const themeColors = [
    { name: '拂晓蓝', value: '#165DFF', key: 'arcoblue' },
    { name: '极光绿', value: '#00B42A', key: 'green' },
    { name: '晚霞橘', value: '#FF7D00', key: 'orange' },
    { name: '火山红', value: '#F53F3F', key: 'red' },
    { name: '暗黑紫', value: '#722ED1', key: 'purple' },
    { name: '科技青', value: '#14C9C9', key: 'cyan' },
    { name: '流沙金', value: '#FADC19', key: 'gold' },
    { name: '星空黑', value: '#1d2129', key: 'gray' },
];

const handleColorChange = (color: string) => {
    appStore.updateSettings({ themeColor: color });
    const colorItem = themeColors.find(c => c.value === color);
    const colorKey = colorItem ? colorItem.key : '';

    if (colorKey && colorKey !== 'gray') {
        for (let i = 1; i <= 10; i++) {
            document.body.style.setProperty(`--arcoblue-${i}`, `var(--${colorKey}-${i})`);
        }
    } else {
        document.body.style.setProperty('--arcoblue-6', color);
    }
}

const handleColorWeekChange = (val: boolean | string | number) => {
    appStore.toggleColorWeek(!!val);
}
</script>

<style scoped>
/* ================= 全局 Drawer 魔改 ================= */
:global(.bml-hyper-drawer .arco-drawer) {
    border-top-left-radius: 28px !important;
    border-bottom-left-radius: 28px !important;
    box-shadow: -30px 0 80px rgba(0, 0, 0, 0.08) !important;
    overflow: hidden !important;
    background: #fdfdfd !important;
    border-left: 1px solid rgba(255,255,255,0.8);
}
:global(body[arco-theme='dark']) :global(.bml-hyper-drawer .arco-drawer) {
    background: #18181A !important;
    border-left: 1px solid rgba(255,255,255,0.03);
    box-shadow: -30px 0 80px rgba(0, 0, 0, 0.4) !important;
}

.h-container { height: 100%; display: flex; flex-direction: column; }

/* ================= 超视觉头部 ================= */
.h-header {
    position: relative; overflow: hidden; padding: 40px 32px 28px;
    border-bottom: 1px solid rgba(0,0,0,0.03);
    background: #fdfdfd;
}
.h-settings-header .orb-1 {
    position: absolute; top: -40px; right: -40px; width: 140px; height: 140px;
    background: var(--color-primary-light-2, rgba(22, 93, 255, 0.15));
    border-radius: 50%; filter: blur(40px); z-index: 0;
}
.h-settings-header .orb-2 {
    position: absolute; bottom: -20px; left: -20px; width: 100px; height: 100px;
    background: rgba(114, 46, 209, 0.1);
    border-radius: 50%; filter: blur(30px); z-index: 0;
}

.h-header-inner { position: relative; z-index: 1; display: flex; justify-content: space-between; align-items: flex-start; }
.h-brand { display: flex; align-items: center; gap: 16px; }
.h-icon-wrapper {
    width: 44px; height: 44px; border-radius: 14px;
    background: linear-gradient(135deg, #ffffff 0%, #f4f5f7 100%);
    box-shadow: 0 10px 20px rgba(0,0,0,0.04), inset 0 2px 4px rgba(255,255,255,0.8), inset 0 -2px 4px rgba(0,0,0,0.02);
    display: flex; align-items: center; justify-content: center;
    font-size: 22px; color: #165dff;
}
.settings-spin { animation: hyper-spin 12s linear infinite; }
@keyframes hyper-spin { 100% { transform: rotate(360deg); } }

.h-title { margin: 0; font-size: 24px; font-weight: 800; color: #111; letter-spacing: 0.5px; line-height: 1.2; font-family: 'Inter', sans-serif;}
.h-sub { display: inline-block; margin: 4px 0 0 0; font-size: 11px; font-weight: 800; color: #86909c; letter-spacing: 1.5px; background: rgba(0,0,0,0.04); padding: 2px 8px; border-radius: 20px;}

.h-close {
    width: 36px; height: 36px; border-radius: 50%;
    background: rgba(0,0,0,0.03); display: flex; align-items: center; justify-content: center;
    color: #4e5969; font-size: 16px; transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); cursor: pointer;
}
.h-close:hover { background: #165dff; color: #fff; transform: rotate(90deg) scale(1.1); box-shadow: 0 8px 16px rgba(22,93,255,0.25); }


/* ================= 滚动内容 ================= */
.h-scroll { flex: 1; overflow-y: auto; padding: 32px; display: flex; flex-direction: column; gap: 32px; }
.h-divider { height: 1px; background: rgba(0,0,0,0.04); width: 100%; border-radius: 1px; }

.h-section { display: flex; flex-direction: column; gap: 20px; }
.h-sec-header { display: flex; justify-content: space-between; align-items: center; }
.h-sec-title { font-size: 16px; font-weight: 700; color: #111; letter-spacing: -0.2px; }
.h-sec-icon { font-size: 18px; color: #86909c; }
.h-sec-badge { font-size: 12px; font-weight: 600; color: #165dff; background: rgba(22,93,255,0.08); padding: 4px 10px; border-radius: 12px; }

/* ================= 色彩苹果核 ================= */
.h-color-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.h-color-item {
    aspect-ratio: 1; border-radius: 50%; display: flex; align-items: center; justify-content: center;
    cursor: pointer; position: relative;
}
.h-color-inner {
    width: 32px; height: 32px; border-radius: 50%;
    box-shadow: inset 0 2px 4px rgba(255,255,255,0.3), 0 4px 12px rgba(0,0,0,0.08);
    display: flex; align-items: center; justify-content: center;
    transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.h-color-item:hover .h-color-inner { transform: scale(1.15) translateY(-2px); box-shadow: inset 0 2px 4px rgba(255,255,255,0.4), 0 8px 16px rgba(0,0,0,0.12); }
.h-color-item.is-active .h-color-inner {
    transform: scale(1.3);
}
.h-color-item.is-active::after {
    content: ''; position: absolute; top: -6px; left: -6px; right: -6px; bottom: -6px;
    border: 2px solid var(--color-primary-light-4, #165dff); border-radius: 50%; opacity: 0.3;
    animation: pulse-ring 2s infinite cubic-bezier(0.215, 0.61, 0.355, 1);
}
@keyframes pulse-ring { 0% { transform: scale(0.8); opacity: 0.5; } 100% { transform: scale(1.3); opacity: 0; } }
.h-check { color: #fff; font-size: 16px; filter: drop-shadow(0 2px 2px rgba(0,0,0,0.3)); font-weight: 900;}


/* ================= 骨架预设卡片 ================= */
.h-layout-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.grid-3 { grid-template-columns: repeat(3, 1fr); }

.h-layout-card {
    display: flex; flex-direction: column; align-items: center; gap: 12px; cursor: pointer;
}
.h-card-text { font-size: 13px; font-weight: 600; color: #86909c; transition: all 0.3s; }
.h-layout-card:hover .h-card-text { color: #1d2129; }
.h-layout-card.is-active .h-card-text { color: #165dff; font-weight: 700; }

.h-card-visual {
    width: 100%; height: 76px; border-radius: 16px;
    background: #fff; border: 2px solid transparent;
    box-shadow: 0 6px 16px rgba(0,0,0,0.03), inset 0 0 0 1px rgba(0,0,0,0.02);
    position: relative; overflow: hidden;
    transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.flex-row { display: flex; }

.h-layout-card:hover .h-card-visual { transform: translateY(-4px); box-shadow: 0 12px 24px rgba(0,0,0,0.08); }
.h-layout-card.is-active .h-card-visual {
    border-color: #165dff;
    box-shadow: 0 8px 24px rgba(22, 93, 255, 0.25);
}

/* 骨架元素 */
.h-skeleton-top { position: absolute; top:0; left:0; width:100%; height: 18px; z-index:2; }
.h-skeleton-side { width: 32%; height: 100%; z-index:2; }
.h-skeleton-body { flex: 1; height: 100%; z-index:1; }
/* Vercel 的顶栏绝对定位导致body需下移 */
.h-layout-grid:not(.grid-3) .h-skeleton-body { position: absolute; bottom: 0; right: 0; width: 100%; height: calc(100% - 18px); }

/* 质感填充 */
.glass { background: linear-gradient(180deg, rgba(255,255,255,0.9), rgba(240,240,240,0.5)); backdrop-filter: blur(4px); border-bottom: 1px solid #fff; }
.glass-bg { background: linear-gradient(135deg, #f0f2f5, #e0e3e8); }
.light { background: #fff; box-shadow: 0 2px 6px rgba(0,0,0,0.06); border-right: 1px solid #f2f3f5;}
.light-bg { background: #f4f5f7; }
.dark { background: #1c1c1e; }
.dark-bg { background: #2c2c2e; }
.primary { background: linear-gradient(135deg, #165dff, #722ed1); }


/* ================= 列表型功能组 ================= */
.h-list-group {
    background: #fff; border-radius: 20px;
    box-shadow: 0 4px 20px rgba(0,0,0,0.02), inset 0 0 0 1px rgba(0,0,0,0.03);
    padding: 8px;
}
.h-list-item {
    display: flex; align-items: center; padding: 12px 16px;
    border-radius: 14px; transition: background 0.3s;
}
.h-list-item:hover { background: #f9f9fa; }
.h-item-icon {
    width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center;
    font-size: 18px; margin-right: 16px;
}
.color-weak-icon { background: rgba(0, 180, 42, 0.1); color: #00b42a; }
.h-item-content { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.h-item-title { font-size: 15px; font-weight: 700; color: #111; }
.h-item-desc { font-size: 12px; color: #86909c; }
.h-switch { transform: scale(1.1); }

/* ================= 极致暗黑模式 ================= */
:global(body[arco-theme='dark']) .h-header { background: #18181A; border-bottom-color: rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .h-icon-wrapper { background: #2A2A2C; box-shadow: 0 10px 20px rgba(0,0,0,0.4), inset 0 2px 4px rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .h-title, :global(body[arco-theme='dark']) .h-sec-title, :global(body[arco-theme='dark']) .h-item-title { color: #E5E6EB; }
:global(body[arco-theme='dark']) .h-sub { background: rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .h-close { color: #C9CDD4; background: rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .h-close:hover { background: #165dff; color: #fff; }
:global(body[arco-theme='dark']) .h-divider { background: rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .h-card-visual { background: #2A2A2C; border-color: rgba(255,255,255,0.05); box-shadow: 0 6px 16px rgba(0,0,0,0.4); }
:global(body[arco-theme='dark']) .h-layout-card:hover .h-card-visual { box-shadow: 0 12px 24px rgba(0,0,0,0.6); }
:global(body[arco-theme='dark']) .h-card-text { color: #86909c; }
:global(body[arco-theme='dark']) .light { background: #333; border-color: #444; }
:global(body[arco-theme='dark']) .light-bg, :global(body[arco-theme='dark']) .glass-bg { background: #1C1C1E; }
:global(body[arco-theme='dark']) .h-list-group { background: #2A2A2C; box-shadow: none; border: 1px solid rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .h-list-item:hover { background: rgba(255,255,255,0.05); }
</style>
