<template>
  <a-drawer
    :width="300"
    :visible="appStore.settingsVisible"
    unmount-on-close
    @cancel="appStore.toggleSettings(false)"
    @ok="appStore.toggleSettings(false)"
    :footer="false"
  >
    <template #title>页面配置</template>
    


    <div class="settings-block">
      <h5 class="block-title">主题色</h5>
      <div class="color-picker-list">
         <div 
            v-for="color in themeColors"
            :key="color.value"
            class="color-item" 
            :class="{ active: appStore.themeColor === color.value }"
            :style="{ background: color.value }"
            @click="handleColorChange(color.value)"
            :title="color.name"
         ></div>
      </div>
    </div>

    <div class="settings-block">
      <h5 class="block-title">顶栏样式</h5>
      <div class="color-picker-list">
         <div 
            class="color-item" 
            :class="{ active: appStore.headerTheme === 'transparent' }"
            style="background: #f2f3f5; border: 1px dashed #ccc;"
            @click="appStore.updateSettings({ headerTheme: 'transparent' })"
            title="透明"
         ></div>
         <div 
            class="color-item" 
            :class="{ active: appStore.headerTheme === 'light' }"
            style="background: #fff; border: 1px solid #eee;"
            @click="appStore.updateSettings({ headerTheme: 'light' })"
            title="白色"
         ></div>
         <div 
            class="color-item" 
            :class="{ active: appStore.headerTheme === 'dark' }"
            style="background: #232324;"
            @click="appStore.updateSettings({ headerTheme: 'dark' })"
            title="暗黑"
         ></div>
         <div 
            class="color-item" 
            :class="{ active: appStore.headerTheme === 'primary' }"
            style="background: linear-gradient(135deg, var(--arcoblue-6) 0%, var(--arcoblue-5) 100%);"
            @click="appStore.updateSettings({ headerTheme: 'primary' })"
            title="主色"
         ></div>
      </div>
    </div>

    <div class="settings-block">
      <h5 class="block-title">侧边栏样式</h5>
      <div class="color-picker-list">
         <div 
            class="color-item" 
            :class="{ active: appStore.sidebarTheme === 'white' }"
            style="background: #fff; border: 1px solid #eee;"
            @click="appStore.updateSettings({ sidebarTheme: 'white' })"
            title="白色"
         ></div>
         <div 
            class="color-item" 
            :class="{ active: appStore.sidebarTheme === 'dark' }"
            style="background: #232324;"
            @click="appStore.updateSettings({ sidebarTheme: 'dark' })"
            title="暗黑"
         ></div>
         <div 
            class="color-item" 
            :class="{ active: appStore.sidebarTheme === 'primary' }"
            style="background: linear-gradient(135deg, var(--arcoblue-6) 0%, var(--arcoblue-5) 100%);"
            @click="appStore.updateSettings({ sidebarTheme: 'primary' })"
            title="主色"
         ></div>
      </div>
    </div>
    <div class="settings-block">
      <h5 class="block-title">界面显示</h5>
      <div class="setting-item">
        <span>色弱模式</span>
        <a-switch v-model="appStore.colorWeek" @change="handleColorWeekChange" />
      </div>
    </div>
    
  </a-drawer>
</template>

<script lang="ts" setup>
import { useAppStore } from '../store/app';

const appStore = useAppStore();

const themeColors = [
    { name: '拂晓蓝 (默认)', value: '#165DFF', key: 'arcoblue' },
    { name: '薄暮红', value: '#F53F3F', key: 'red' },
    { name: '火山橘', value: '#FF7D00', key: 'orange' },
    { name: '金盏黄', value: '#FADC19', key: 'gold' },
    { name: '仙野绿', value: '#00B42A', key: 'green' },
    { name: '明青', value: '#14C9C9', key: 'cyan' },
    { name: '酱紫', value: '#722ED1', key: 'purple' },
    { name: '粉紫', value: '#D91AD9', key: 'magenta' },
    { name: '极客黑', value: '#1d2129', key: 'gray' },
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
        // Fallback for custom or gray
        document.body.style.setProperty('--arcoblue-6', color);
    }
}

const handleColorWeekChange = (val: any) => {
    appStore.toggleColorWeek(val);
}
</script>

<style scoped>

.settings-block {
    margin-bottom: 24px;
}
.block-title {
    margin: 0 0 12px 0;
    font-size: 14px;
    color: var(--color-text-1);
}

.color-picker-list {
    display: flex;
    gap: 12px;
}
.color-item {
    width: 32px;
    height: 32px;
    border-radius: 4px;
    cursor: pointer;
    position: relative;
    transition: all 0.2s;
}
.color-item.active::after {
    content: '';
    position: absolute;
    top: 50%; left: 50%;
    transform: translate(-50%, -50%);
    width: 8px; height: 8px;
    border-radius: 50%;
    background: #165dff;
    box-shadow: 0 0 0 2px #fff;
}
.color-item.active {
    transform: scale(1.1);
    box-shadow: 0 4px 10px rgba(0,0,0,0.1);
}

.setting-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: var(--color-text-2);
    font-size: 14px;
}
</style>
