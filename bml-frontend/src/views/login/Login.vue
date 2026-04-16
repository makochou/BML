<template>
  <div class="login-wrapper">
    <div class="login-card">
      <div class="card-left">
        <div class="brand">
            <div class="brand-logo">
                <icon-thunderbolt />
            </div>
            <h1 class="brand-name">BML<span class="highlight">Pro</span></h1>
            <p class="brand-slogan">企业级 API 中台管理解决方案</p>
        </div>
        <div class="illustration">
            <div class="circle c1"></div>
            <div class="circle c2"></div>
            <div class="circle c3"></div>
        </div>
      </div>
      
      <div class="card-right">
        <div class="form-container">
            <h2 class="form-title">欢迎登录</h2>
            <p class="form-subtitle">Login to your account</p>
            
            <a-form :model="form" @submit="handleSubmit" layout="vertical" class="login-form">
                <a-form-item field="username" hide-label>
                    <a-input v-model="form.username" placeholder="请输入账号" allow-clear size="large">
                        <template #prefix><icon-user /></template>
                    </a-input>
                </a-form-item>
                <a-form-item field="password" hide-label>
                    <a-input-password v-model="form.password" placeholder="请输入密码" allow-clear size="large">
                        <template #prefix><icon-lock /></template>
                    </a-input-password>
                </a-form-item>
                
                <a-form-item>
                    <a-button type="primary" html-type="submit" long size="large" :loading="loading" class="login-btn">
                        立即登录
                        <template #icon><icon-arrow-right /></template>
                    </a-button>
                </a-form-item>
            </a-form>
        </div>
      </div>
    </div>

    <div class="copyright">
      COPYRIGHT &copy; 2026 BML TEAM. ALL RIGHTS RESERVED.
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { Message } from '@arco-design/web-vue';
import { IconUser, IconLock, IconThunderbolt, IconArrowRight } from '@arco-design/web-vue/es/icon';
import request from '../../utils/request';
import { setAuthTokens } from '../../utils/auth';

const router = useRouter();
const loading = ref(false);
const form = reactive({
  username: '',
  password: ''
});

const handleSubmit = async ({ errors }: { errors?: unknown }) => {
  if (errors) {
    return;
  }

  loading.value = true;
  try {
    const { data } = await request.post('/auth/admin/login', {
        username: form.username,
        password: form.password
    });
    setAuthTokens({
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      userIdentity: form.username
    });
    Message.success('登录成功');
    router.push('/admin');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  document.body.style.overflow = 'hidden';
});

onUnmounted(() => {
  document.body.style.overflow = '';
});
</script>

<style scoped>
.login-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #0f172a;
  background-image: 
    radial-gradient(at 0% 0%, hsla(253,16%,7%,1) 0, transparent 50%), 
    radial-gradient(at 50% 0%, hsla(225,39%,30%,1) 0, transparent 50%), 
    radial-gradient(at 100% 0%, hsla(339,49%,30%,1) 0, transparent 50%), 
    radial-gradient(at 0% 50%, hsla(253,16%,7%,1) 0, transparent 50%), 
    radial-gradient(at 100% 50%, hsla(339,49%,30%,1) 0, transparent 50%), 
    radial-gradient(at 0% 100%, hsla(253,16%,7%,1) 0, transparent 50%), 
    radial-gradient(at 50% 100%, hsla(225,39%,30%,1) 0, transparent 50%), 
    radial-gradient(at 100% 100%, hsla(339,49%,30%,1) 0, transparent 50%);
  background-size: 200% 200%;
  animation: meshFlow 15s ease infinite;
  overflow: hidden;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

@keyframes meshFlow {
    0% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
    100% { background-position: 0% 50%; }
}

.login-card {
  width: 900px;
  height: 550px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  overflow: hidden;
  z-index: 10;
  transition: transform 0.3s ease;
}

.login-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.3);
}

.card-left {
  flex: 1;
  background: rgba(0, 0, 0, 0.3);
  padding: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: white;
  position: relative;
  overflow: hidden;
}

.brand {
    display: flex;
    flex-direction: column;
    align-items: center;
    z-index: 2;
}

.brand-logo {
    font-size: 40px;
    margin-bottom: 20px;
    color: #ffd700;
}

.brand-name {
    font-size: 36px;
    font-weight: 800;
    margin: 0 0 10px 0;
    letter-spacing: 1px;
}

.brand-name .highlight {
    color: #ffd700;
}

.brand-slogan {
    font-size: 16px;
    opacity: 0.8;
    line-height: 1.6;
    width: 100%;
    text-align: center;
    white-space: nowrap;
}

.illustration {
    position: absolute;
    bottom: -50px;
    right: -50px;
    width: 300px;
    height: 300px;
}
.circle {
    position: absolute;
    border-radius: 50%;
    border: 2px solid rgba(255, 255, 255, 0.1);
}
.c1 { width: 100%; height: 100%; animation: rotate 20s linear infinite; border-top-color: rgba(255, 255, 255, 0.5); }
.c2 { width: 70%; height: 70%; top: 15%; left: 15%; animation: rotate 15s linear infinite reverse; border-bottom-color: rgba(255, 215, 0, 0.5); }
.c3 { width: 40%; height: 40%; top: 30%; left: 30%; animation: rotate 10s linear infinite; border-left-color: rgba(255, 255, 255, 0.3); }

@keyframes rotate { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.card-right {
  flex: 1;
  background: white;
  padding: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-container {
    width: 100%;
    max-width: 320px;
}

.form-title {
    font-size: 28px;
    color: #1a202c;
    margin-bottom: 8px;
    font-weight: 700;
}

.form-subtitle {
    color: #718096;
    margin-bottom: 40px;
    font-size: 14px;
}

:deep(.arco-input-wrapper), :deep(.arco-input-password) {
    background-color: #f7fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    height: 48px;
    transition: all 0.3s ease;
}
:deep(.arco-input-wrapper:hover), :deep(.arco-input-password:hover) {
    background-color: white;
    border-color: #cbd5e0;
}
:deep(.arco-input-wrapper.arco-input-focus), :deep(.arco-input-password.arco-input-focus) {
    background-color: white;
    border-color: #667eea;
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.login-btn {
    height: 48px;
    background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
    border: none;
    border-radius: 8px;
    font-size: 16px;
    font-weight: 600;
    margin-top: 10px;
    transition: all 0.3s ease;
}
.login-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 20px rgba(118, 75, 162, 0.3);
}

.copyright {
    position: absolute;
    bottom: 20px;
    color: rgba(255, 255, 255, 0.6);
    font-size: 12px;
    letter-spacing: 1px;
    z-index: 10;
}
</style>
