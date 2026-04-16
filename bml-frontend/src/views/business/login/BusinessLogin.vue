<template>
  <div class="business-login-wrapper">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
    </div>

    <div class="login-container">
      <!-- 左侧品牌区域 -->
      <div class="brand-panel">
        <div class="brand-content">
          <div class="brand-icon">
            <icon-apps :size="36" />
          </div>
          <h1 class="brand-title">BML</h1>
          <p class="brand-subtitle">业务管理系统</p>
          <div class="brand-divider"></div>
          <p class="brand-desc">
            统一身份认证 · 角色权限管理<br/>
            组织架构维护 · 菜单资源配置
          </p>
        </div>
        <div class="brand-footer">
          <span>Business Management Platform</span>
        </div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="form-panel">
        <div class="form-inner">
          <h2 class="form-title">用户登录</h2>
          <p class="form-hint">请使用系统账号登录</p>

          <a-form
            :model="form"
            @submit="handleSubmit"
            layout="vertical"
            class="login-form"
          >
            <a-form-item field="username" hide-label>
              <a-input
                v-model="form.username"
                placeholder="请输入用户名"
                allow-clear
                size="large"
              >
                <template #prefix><icon-user /></template>
              </a-input>
            </a-form-item>

            <a-form-item field="password" hide-label>
              <a-input-password
                v-model="form.password"
                placeholder="请输入密码"
                allow-clear
                size="large"
              >
                <template #prefix><icon-lock /></template>
              </a-input-password>
            </a-form-item>

            <a-form-item>
              <a-button
                type="primary"
                html-type="submit"
                long
                size="large"
                :loading="loading"
                class="submit-btn"
              >
                登 录
              </a-button>
            </a-form-item>
          </a-form>

          <div class="form-footer">
            <a-link @click="goAdmin" :hoverable="false">
              <icon-settings :size="14" style="margin-right: 4px;" />
              中台管理入口
            </a-link>
          </div>
        </div>
      </div>
    </div>

    <div class="copyright">
      COPYRIGHT &copy; 2026 BML TEAM. ALL RIGHTS RESERVED.
    </div>
  </div>
</template>

<script lang="ts" setup>
/**
 * 前台业务系统登录页
 * <p>
 * 使用数据库用户（sys_user 表）进行认证，登录成功后跳转至业务系统首页。
 * 与中台管理平台共用后端 /auth/login 接口，但登录后跳转路径不同。
 * </p>
 */
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Message } from '@arco-design/web-vue';
import { IconUser, IconLock, IconApps, IconSettings } from '@arco-design/web-vue/es/icon';
import request from '../../../utils/request';
import { setAuthTokens } from '../../../utils/auth';

const router = useRouter();
const loading = ref(false);

const form = reactive({
  username: '',
  password: ''
});

/** 处理登录提交 */
const handleSubmit = async ({ errors }: { errors?: unknown }) => {
  if (errors) return;

  loading.value = true;
  try {
    const { data } = await request.post('/auth/login', {
      username: form.username,
      password: form.password
    });
    setAuthTokens({
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      userIdentity: form.username
    });
    Message.success('登录成功');
    router.push('/dashboard');
  } finally {
    loading.value = false;
  }
};

/** 跳转中台管理入口 */
const goAdmin = () => {
  router.push('/admin/login');
};
</script>

<style scoped>
.business-login-wrapper {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;
}

/* 背景装饰 */
.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.bg-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
}
.bg-circle-1 {
  width: 600px;
  height: 600px;
  background: #fff;
  top: -200px;
  right: -100px;
}
.bg-circle-2 {
  width: 400px;
  height: 400px;
  background: #fff;
  bottom: -150px;
  left: -100px;
}
.bg-circle-3 {
  width: 200px;
  height: 200px;
  background: #fff;
  top: 40%;
  left: 20%;
}

/* 登录容器 */
.login-container {
  display: flex;
  width: 840px;
  min-height: 480px;
  border-radius: 24px;
  overflow: hidden;
  background: #fff;
  box-shadow:
    0 32px 80px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.1);
  z-index: 1;
}

/* 左侧品牌 */
.brand-panel {
  flex: 0 0 340px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 36px 32px;
  background: linear-gradient(160deg, #4f46e5 0%, #7c3aed 50%, #a855f7 100%);
  color: #fff;
}
.brand-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.brand-icon {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}
.brand-title {
  font-size: 40px;
  font-weight: 900;
  letter-spacing: 4px;
  margin: 0;
  line-height: 1;
}
.brand-subtitle {
  font-size: 16px;
  font-weight: 500;
  margin: 8px 0 0;
  opacity: 0.9;
  letter-spacing: 2px;
}
.brand-divider {
  width: 40px;
  height: 3px;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 2px;
  margin: 24px 0;
}
.brand-desc {
  font-size: 14px;
  line-height: 2;
  opacity: 0.75;
  margin: 0;
}
.brand-footer {
  font-size: 11px;
  opacity: 0.4;
  letter-spacing: 1px;
  text-transform: uppercase;
}

/* 右侧表单 */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
}
.form-inner {
  width: 100%;
  max-width: 320px;
}
.form-title {
  font-size: 24px;
  font-weight: 700;
  color: #1d2129;
  margin: 0 0 4px;
}
.form-hint {
  font-size: 14px;
  color: #86909c;
  margin: 0 0 32px;
}

/* 表单样式 */
.login-form :deep(.arco-input-wrapper) {
  border-radius: 12px;
  height: 48px;
  background: #f7f8fa;
  border: 1px solid #e5e6eb;
  transition: all 0.3s;
}
.login-form :deep(.arco-input-wrapper:hover),
.login-form :deep(.arco-input-wrapper.arco-input-focus) {
  background: #fff;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}
.submit-btn {
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  border: none;
  margin-top: 8px;
}
.submit-btn:hover {
  background: linear-gradient(135deg, #4338ca, #6d28d9);
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.35);
  transform: translateY(-1px);
}

/* 底部链接 */
.form-footer {
  text-align: center;
  margin-top: 24px;
}
.form-footer :deep(.arco-link) {
  color: #86909c;
  font-size: 13px;
}
.form-footer :deep(.arco-link:hover) {
  color: #4f46e5;
}

/* 版权 */
.copyright {
  position: absolute;
  bottom: 20px;
  width: 100%;
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 1px;
  z-index: 1;
}

/* 响应式 */
@media (max-width: 900px) {
  .login-container {
    flex-direction: column;
    width: 90%;
    max-width: 420px;
    min-height: auto;
  }
  .brand-panel {
    flex: 0 0 auto;
    padding: 32px 24px 24px;
  }
  .brand-icon { display: none; }
  .brand-title { font-size: 28px; }
  .brand-desc { display: none; }
  .form-panel { padding: 32px 24px; }
}
</style>
