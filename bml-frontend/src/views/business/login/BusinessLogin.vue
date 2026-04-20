<template>
  <!-- ======================================================================
       BML 业务系统登录页（一体化卡片布局）
       ======================================================================
       设计特点：
         1. 左右分色背景（左 70% 星空墨蓝 + 右 30% 琥珀橙）+ 极光光晕 + 微光粒子
         2. 一体化登录卡片横跨中央分界线：左品牌（毛玻璃）+ 右表单（纯白）
         3. 品牌文案支持中台动态配置（系统配置 → 品牌文案）
         4. 图形验证码（后端开关控制）
         5. 背景图、登录框装饰图、favicon 支持中台动态替换
         6. 响应式：平板 / 移动端自动调整布局
       ====================================================================== -->
  <div class="bl-page">
    <!-- 左侧深色背景（70%） -->
    <div class="bl-bg-left" :style="showcaseStyle" />
    <!-- 右侧暖橙背景（30%） -->
    <div class="bl-bg-right" />

    <!-- 背景极光光晕装饰（左侧区域） -->
    <div class="bl-aurora" aria-hidden="true">
      <div class="bl-aurora__orb bl-aurora__orb--1" />
      <div class="bl-aurora__orb bl-aurora__orb--2" />
      <div class="bl-aurora__orb bl-aurora__orb--3" />
    </div>

    <!-- 微光粒子动画层 -->
    <div class="bl-particles" aria-hidden="true">
      <span v-for="i in 15" :key="i" class="bl-particle" :style="particleStyle(i)" />
    </div>

    <!-- 底部版权信息 -->
    <p class="bl-copyright">COPYRIGHT &copy; 2026 BML TEAM</p>

    <!-- 一体化登录卡片（靠右垂直居中） -->
    <div class="bl-unified-card">
      <!-- 左侧品牌展示区（毛玻璃深色） -->
      <div class="bl-brand">
        <div class="bl-brand__inner">
          <div class="bl-brand__badge">
            <svg viewBox="0 0 40 40" width="40" height="40" fill="none">
              <rect width="40" height="40" rx="10" fill="rgba(255,255,255,0.12)" />
              <path d="M11 11h8v8h-8zM21 11h8v8h-8zM11 21h8v8h-8zM21 21h8v8h-8z"
                    fill="#fff" opacity="0.9" />
            </svg>
          </div>
          <h1 class="bl-brand__title">{{ brandTitle || 'BML' }}</h1>
          <p class="bl-brand__slogan">{{ brandSlogan || '智慧企业管理平台' }}</p>
          <div class="bl-brand__divider" />
          <p class="bl-brand__desc">
            {{ brandDesc || '统一身份认证 · 权限精细管控 · 流程高效协同 · 数据驱动决策' }}
          </p>
        </div>
      </div>

      <!-- 右侧登录表单区（纯白） -->
      <div class="bl-form-side">
        <!-- 欢迎标题 -->
        <div class="bl-form-header">
          <h2 class="bl-form-header__title">欢迎回来</h2>
          <p class="bl-form-header__sub">请输入您的账号信息登录系统</p>
        </div>
        <!-- 登录表单 -->
        <a-form :model="form" @submit="handleSubmit" layout="vertical" class="bl-form">
          <a-form-item field="username" hide-label>
            <a-input v-model="form.username" placeholder="用户名" allow-clear size="large">
              <template #prefix><icon-user /></template>
            </a-input>
          </a-form-item>
          <a-form-item field="password" hide-label>
            <a-input-password v-model="form.password" placeholder="密码" allow-clear size="large">
              <template #prefix><icon-lock /></template>
            </a-input-password>
          </a-form-item>
          <a-form-item v-if="captchaEnabled" field="captchaCode" hide-label>
            <div class="bl-captcha-row">
              <a-input
                v-model="form.captchaCode"
                placeholder="请输入验证码"
                allow-clear
                size="large"
                class="bl-captcha-input"
              >
                <template #prefix><icon-safe /></template>
              </a-input>
              <div class="bl-captcha-img" @click="refreshCaptcha" title="点击刷新验证码">
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                <span v-else class="bl-captcha-placeholder">加载中</span>
              </div>
            </div>
          </a-form-item>
          <a-form-item hide-label>
            <a-button
              type="primary" html-type="submit" long size="large"
              :loading="loading" class="bl-submit-btn"
            >
              登 录
            </a-button>
          </a-form-item>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
/**
 * BML 前台业务系统登录页
 * <p>
 * 采用左右分色 + 一体化卡片布局：
 *   - 左右分色背景（左 70% 星空墨蓝 + 右 30% 琥珀橙），极光光晕 + 微光粒子
 *   - 一体化卡片横跨中央分界线：左品牌（毛玻璃）+ 右表单（纯白）
 *   - 品牌文案支持中台动态配置
 *
 * 功能说明：
 *   1. 页面初始化时调用 /auth/login/config 获取登录配置（验证码开关、品牌图片）
 *   2. 若开启验证码，自动加载图形验证码图片
 *   3. 登录成功后存储 Token 并跳转至业务系统首页
 *   4. 背景图、登录框装饰图、favicon 均从配置动态读取，支持中台替换
 * </p>
 */
import { reactive, ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Message } from '@arco-design/web-vue';
import { IconUser, IconLock, IconSafe } from '@arco-design/web-vue/es/icon';
import request from '../../../utils/request';
import { setAuthTokens } from '../../../utils/auth';
import { fetchLoginConfig, fetchCaptcha } from '../../../api/auth';

const router = useRouter();
const loading = ref(false);

// ═══════════════════════════════════════════════════════
// 表单数据
// ═══════════════════════════════════════════════════════
const form = reactive({
  username: '',
  password: '',
  captchaCode: ''
});

// ═══════════════════════════════════════════════════════
// 配置状态（由后端 /auth/login/config 接口返回）
// ═══════════════════════════════════════════════════════
const captchaEnabled = ref(false);
const captchaImage = ref('');
const captchaKey = ref('');
const bgImageUrl = ref('');

/** 品牌文案（可在中台系统配置中编辑） */
const brandTitle = ref('');
const brandSlogan = ref('');
const brandDesc = ref('');

/**
 * 左侧背景样式
 * 有自定义背景图时使用图片覆盖左侧区域，否则使用内置深色渐变
 */
const showcaseStyle = computed(() => {
  if (bgImageUrl.value) {
    return { backgroundImage: `url(${bgImageUrl.value})` };
  }
  return {};
});

/**
 * 粒子动画样式生成
 * 每个粒子具有随机大小、位置、延迟和持续时间
 */
const particleStyle = (_i: number) => {
  const size = 2 + Math.random() * 4;
  const left = Math.random() * 100;
  const delay = Math.random() * 20;
  const duration = 18 + Math.random() * 22;
  const opacity = 0.15 + Math.random() * 0.35;
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
    opacity: `${opacity}`
  };
};

/**
 * 加载登录页配置
 * 包含验证码开关、品牌图片 URL、favicon 等
 */
const loadConfig = async () => {
  try {
    const res = await fetchLoginConfig() as any;
    const config = res.data || {};
    captchaEnabled.value = config['sys.login.captchaEnabled'] === 'true';
    bgImageUrl.value = config['sys.login.bgImage'] || '';
    brandTitle.value = config['sys.login.brandTitle'] || '';
    brandSlogan.value = config['sys.login.brandSlogan'] || '';
    brandDesc.value = config['sys.login.brandDesc'] || '';

    // 动态更新浏览器标签图标
    const faviconUrl = config['sys.login.favicon'];
    if (faviconUrl) {
      const link = document.querySelector<HTMLLinkElement>("link[rel~='icon']");
      if (link) link.href = faviconUrl;
    }

    // 如果开启了验证码，自动加载
    if (captchaEnabled.value) {
      await refreshCaptcha();
    }
  } catch {
    /* 配置加载失败不阻塞页面渲染，使用默认值即可 */
  }
};

/** 刷新图形验证码 */
const refreshCaptcha = async () => {
  try {
    const res = await fetchCaptcha() as any;
    captchaKey.value = res.data.captchaKey;
    captchaImage.value = res.data.captchaImage;
  } catch {
    captchaImage.value = '';
  }
};

/** 处理登录表单提交 */
const handleSubmit = async ({ errors }: { errors?: unknown }) => {
  if (errors) return;

  // 防止重复提交（回车键 + 按钮可能同时触发）
  if (loading.value) return;

  if (!form.username || !form.password) {
    Message.warning('请输入用户名和密码');
    return;
  }
  if (captchaEnabled.value && !form.captchaCode) {
    Message.warning('请输入验证码');
    return;
  }

  loading.value = true;
  try {
    const payload: Record<string, string> = {
      username: form.username,
      password: form.password
    };
    if (captchaEnabled.value) {
      payload.captchaKey = captchaKey.value;
      payload.captchaCode = form.captchaCode;
    }

    const { data } = await request.post('/auth/login', payload);
    setAuthTokens({
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      userIdentity: form.username
    });
    Message.success('登录成功');
    router.push('/dashboard');
  } catch {
    // 登录失败，自动刷新验证码
    if (captchaEnabled.value) {
      form.captchaCode = '';
      await refreshCaptcha();
    }
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadConfig();
});
</script>

<style scoped>
/* ═══════════════════════════════════════════════════
   页面容器 — 全屏，左右分色背景
   ═══════════════════════════════════════════════════ */
.bl-page {
  position: fixed;
  inset: 0;
  font-family: var(--bml-font-family, 'Inter', -apple-system, BlinkMacSystemFont,
    'PingFang SC', 'Microsoft YaHei', sans-serif);
  overflow: hidden;
}

/* ── 左侧深色背景（70%）── 星空墨蓝渐变 ── */
.bl-bg-left {
  position: absolute;
  top: 0;
  left: 0;
  width: 70%;
  height: 100%;
  background: linear-gradient(150deg, #0b1026 0%, #111d42 25%, #182654 50%, #1e2d5e 75%, #162050 100%);
  background-size: cover;
  background-position: center;
}

/* ── 右侧暖橙背景（30%）── 琥珀金橙渐变 ── */
.bl-bg-right {
  position: absolute;
  top: 0;
  right: 0;
  width: 30%;
  height: 100%;
  background: linear-gradient(160deg, #ff8c42 0%, #ff6b35 35%, #f0541e 70%, #e8431a 100%);
}

/* ═══════════════════════════════════════════════════
   背景极光光晕（仅左侧区域）
   ═══════════════════════════════════════════════════ */
.bl-aurora {
  position: absolute;
  top: 0;
  left: 0;
  width: 70%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
  overflow: hidden;
}
.bl-aurora__orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
}
.bl-aurora__orb--1 {
  width: 600px;
  height: 600px;
  top: -15%;
  left: -10%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.3) 0%, transparent 70%);
}
.bl-aurora__orb--2 {
  width: 500px;
  height: 500px;
  bottom: -10%;
  right: -5%;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.22) 0%, transparent 70%);
}
.bl-aurora__orb--3 {
  width: 400px;
  height: 400px;
  top: 30%;
  left: 40%;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.12) 0%, transparent 70%);
}

/* ═══════════════════════════════════════════════════
   微光粒子动画（仅左侧区域）
   ═══════════════════════════════════════════════════ */
.bl-particles {
  position: absolute;
  top: 0;
  left: 0;
  width: 70%;
  height: 100%;
  z-index: 2;
  pointer-events: none;
  overflow: hidden;
}
.bl-particle {
  position: absolute;
  bottom: -10px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 50%;
  animation: bl-float linear infinite;
}
@keyframes bl-float {
  0%   { transform: translateY(0) scale(1); opacity: 0.25; }
  100% { transform: translateY(-110vh) scale(0.2); opacity: 0; }
}

/* ═══════════════════════════════════════════════════
   底部版权信息（左侧区域底部）
   ═══════════════════════════════════════════════════ */
.bl-copyright {
  position: absolute;
  bottom: 24px;
  left: 0;
  width: 70%;
  text-align: center;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.15);
  letter-spacing: 2px;
  z-index: 5;
  margin: 0;
}

/* ═══════════════════════════════════════════════════
   一体化登录卡片 — 绝对居中横跨左右分界线
   ═══════════════════════════════════════════════════ */
.bl-unified-card {
  position: absolute;
  top: 50%;
  left: 70%;
  transform: translate(-50%, -50%);
  z-index: 10;
  display: flex;
  border-radius: 24px;
  overflow: hidden;
  box-shadow:
    0 32px 80px rgba(0, 0, 0, 0.25),
    0 12px 32px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.06);
  max-height: 90vh;
  animation: bl-card-in 0.7s cubic-bezier(0.16, 1, 0.3, 1) 0.2s both;
}
@keyframes bl-card-in {
  from { opacity: 0; transform: translate(-50%, -48%) scale(0.96); }
  to   { opacity: 1; transform: translate(-50%, -50%) scale(1); }
}

/* ── 左侧品牌区（毛玻璃深色） ── */
.bl-brand {
  width: 320px;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 36px;
}
.bl-brand__inner {
  text-align: left;
  color: #fff;
}
.bl-brand__badge {
  margin-bottom: 24px;
}
.bl-brand__title {
  font-size: 48px;
  font-weight: 900;
  letter-spacing: 10px;
  margin: 0;
  line-height: 1.1;
  background: linear-gradient(135deg, #fff 0%, rgba(196, 181, 253, 0.85) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.bl-brand__slogan {
  font-size: 12px;
  color: rgba(196, 181, 253, 0.55);
  letter-spacing: 2px;
  margin: 10px 0 0;
  font-weight: 500;
}
.bl-brand__divider {
  width: 36px;
  height: 2px;
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.5), transparent);
  margin: 28px 0;
  border-radius: 1px;
}
.bl-brand__desc {
  font-size: 13px;
  color: rgba(196, 181, 253, 0.4);
  letter-spacing: 1.5px;
  margin: 0;
  line-height: 1.8;
}

/* ── 右侧表单区（纯白） ── */
.bl-form-side {
  width: 400px;
  flex-shrink: 0;
  background: #fff;
  padding: 44px 40px 36px;
  overflow-y: auto;
}

/* ── 欢迎标题 ── */
.bl-form-header {
  margin-bottom: 28px;
}
.bl-form-header__title {
  font-size: 26px;
  font-weight: 800;
  color: #1a1a2e;
  margin: 0 0 6px;
  line-height: 1.3;
}
.bl-form-header__sub {
  font-size: 13px;
  color: #8b8fa3;
  margin: 0;
  line-height: 1.5;
}

/* ═══════════════════════════════════════════════════
   表单样式
   ═══════════════════════════════════════════════════ */
.bl-form :deep(.arco-form-item) {
  margin-bottom: 18px;
}
.bl-form :deep(.arco-input-wrapper) {
  height: 48px;
  border-radius: 12px;
  background: #f5f5fa;
  border: 1.5px solid #e8e8f0;
  transition: all 0.25s ease;
}
.bl-form :deep(.arco-input-wrapper:hover) {
  border-color: #c4b5fd;
  background: #f0eef8;
}
.bl-form :deep(.arco-input-wrapper.arco-input-focus) {
  background: #fff;
  border-color: #8b5cf6;
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
}
.bl-form :deep(.arco-input) {
  color: #1a1a2e;
}
.bl-form :deep(.arco-input::placeholder) {
  color: #b8b9c9;
}
.bl-form :deep(.arco-input-prefix) {
  color: #8b8fa3;
}
.bl-form :deep(.arco-input-suffix .arco-icon),
.bl-form :deep(.arco-input-clear-btn) {
  color: #b8b9c9;
}

/* ── 验证码行 ── */
.bl-captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}
.bl-captcha-input {
  flex: 1;
}
.bl-captcha-img {
  flex: 0 0 120px;
  height: 48px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  background: #f5f5fa;
  border: 1.5px solid #e8e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s;
}
.bl-captcha-img:hover {
  border-color: #8b5cf6;
  background: #f0eef8;
}
.bl-captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.bl-captcha-placeholder {
  font-size: 12px;
  color: #b8b9c9;
}

/* ── 登录按钮（紫罗兰渐变） ── */
.bl-submit-btn {
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 6px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #a855f7 100%);
  border: none;
  box-shadow: 0 4px 20px rgba(139, 92, 246, 0.3);
  transition: all 0.3s ease;
}
.bl-submit-btn:hover {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 50%, #9333ea 100%);
  box-shadow: 0 8px 28px rgba(139, 92, 246, 0.4);
  transform: translateY(-1px);
}
.bl-submit-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 12px rgba(139, 92, 246, 0.2);
}

/* ═══════════════════════════════════════════════════
   响应式适配
   ═══════════════════════════════════════════════════ */
@media (max-width: 1100px) {
  .bl-brand {
    width: 280px;
    padding: 40px 28px;
  }
  .bl-brand__title {
    font-size: 40px;
    letter-spacing: 8px;
  }
  .bl-form-side {
    width: 360px;
    padding: 36px 32px 28px;
  }
}
@media (max-width: 900px) {
  .bl-page {
    overflow-y: auto;
  }
  .bl-bg-left {
    width: 100%;
    position: fixed;
  }
  .bl-bg-right {
    display: none;
  }
  .bl-aurora,
  .bl-particles {
    width: 100%;
  }
  .bl-copyright {
    width: 100%;
    position: fixed;
  }
  .bl-unified-card {
    position: relative;
    top: auto;
    left: auto;
    transform: none;
    flex-direction: column;
    max-width: 420px;
    width: calc(100% - 40px);
    margin: max(8vh, 48px) auto 60px;
    animation: none;
  }
  .bl-brand {
    width: 100%;
    padding: 32px 28px;
  }
  .bl-brand__inner {
    text-align: center;
  }
  .bl-brand__divider {
    margin: 20px auto;
  }
  .bl-brand__title {
    font-size: 36px;
    letter-spacing: 6px;
  }
  .bl-brand__desc {
    display: none;
  }
  .bl-form-side {
    width: 100%;
    padding: 32px 28px 28px;
  }
}
@media (max-width: 500px) {
  .bl-brand {
    padding: 24px 20px;
  }
  .bl-brand__title {
    font-size: 28px;
    letter-spacing: 4px;
  }
  .bl-brand__slogan,
  .bl-brand__divider {
    display: none;
  }
  .bl-form-side {
    padding: 28px 24px 24px;
  }
  .bl-form-header__title {
    font-size: 22px;
  }
  .bl-captcha-img {
    flex: 0 0 100px;
  }
}
</style>
