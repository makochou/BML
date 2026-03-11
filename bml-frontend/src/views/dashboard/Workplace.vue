<template>
  <div class="welcome-container">
    <div class="welcome-content">
      <div class="logo-circle">
        <img src="../../assets/logo.svg" alt="BML Logo" v-if="hasLogo" />
        <icon-command v-else class="fallback-icon" />
      </div>
      <h1 class="welcome-title">欢迎进入 BML 中台管理系统</h1>
      <p class="welcome-subtitle">
        高效、稳定、全域的 API 资产治理与服务器监控平台
      </p>
      
      <div class="quick-stats">
        <div class="stat-item">
          <div class="stat-value">Active</div>
          <div class="stat-label">系统状态</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-value">{{ currentTime }}</div>
          <div class="stat-label">当前时间</div>
        </div>
      </div>

      <div class="action-hint">
        请从左侧菜单开始您的治理之旅
      </div>
    </div>
    
    <!-- Decorative Elements -->
    <div class="bg-decoration-1"></div>
    <div class="bg-decoration-2"></div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { IconCommand } from '@arco-design/web-vue/es/icon';
import dayjs from 'dayjs';

defineOptions({ name: 'Dashboard' });

const hasLogo = ref(false); // Can be toggled if logo.svg is verified
const currentTime = ref(dayjs().format('HH:mm:ss'));
let timer: any = null;

onMounted(() => {
  timer = setInterval(() => {
    currentTime.value = dayjs().format('HH:mm:ss');
  }, 1000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<style scoped>
.welcome-container {
  height: calc(100vh - 100px);
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f2f3f5;
  border-radius: 12px;
  position: relative;
  overflow: hidden;
}

.welcome-content {
  text-align: center;
  z-index: 10;
  animation: fadeInDown 0.8s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.logo-circle {
  width: 100px;
  height: 100px;
  background: white;
  border-radius: 30px;
  margin: 0 auto 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 30px rgba(22, 93, 255, 0.1);
  transition: transform 0.3s;
}

.logo-circle:hover {
  transform: rotate(10deg) scale(1.05);
}

.fallback-icon {
  font-size: 40px;
  color: #165dff;
}

.welcome-title {
  font-size: 32px;
  font-weight: 800;
  color: #1d2129;
  margin-bottom: 12px;
  letter-spacing: -0.5px;
}

.welcome-subtitle {
  font-size: 16px;
  color: #86909c;
  margin-bottom: 48px;
}

.quick-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  padding: 24px 48px;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  margin-bottom: 40px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #165dff;
  font-family: 'Courier New', monospace;
}

.stat-label {
  font-size: 12px;
  color: #86909c;
  margin-top: 4px;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: #e5e6eb;
  margin: 0 40px;
}

.action-hint {
  font-size: 14px;
  color: #c9cdd4;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.action-hint::before,
.action-hint::after {
  content: '';
  width: 30px;
  height: 1px;
  background: #e5e6eb;
}

/* Decorative Backgrounds */
.bg-decoration-1 {
  position: absolute;
  top: -10%;
  right: -5%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(22, 93, 255, 0.05) 0%, transparent 70%);
  border-radius: 50%;
}

.bg-decoration-2 {
  position: absolute;
  bottom: -10%;
  left: -5%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(0, 180, 42, 0.03) 0%, transparent 70%);
  border-radius: 50%;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
