<template>
  <!-- ======================================================================
       BML 业务系统登录页（左右分栏布局）
       ======================================================================
       设计特点：
         1. 左侧 60%：展示区（可替换背景图 + 品牌信息叠加 + 微光动画）
         2. 右侧 40%：纯白登录表单区（简约、干净、易读）
         3. 图形验证码（后端开关控制是否展示）
         4. 背景图、登录框顶部图、favicon 均支持中台管理动态替换
         5. 移除中台管理入口链接
         6. 响应式：移动端自动切换为单列垂直布局
       ====================================================================== -->
  <div class="bl-page">
    <!-- ════════════════════════════════════════════════════
         左侧展示区（60%）
         ════════════════════════════════════════════════════ -->
    <div class="bl-showcase" :style="showcaseStyle">
      <!-- 渐变叠加层（保证文字可读性） -->
      <div class="bl-showcase__overlay" />

      <!-- 微光粒子动画 -->
      <div class="bl-showcase__particles" aria-hidden="true">
        <span v-for="i in 15" :key="i" class="bl-particle" :style="particleStyle(i)" />
      </div>

      <!-- 品牌信息 -->
      <div class="bl-showcase__content">
        <div class="bl-showcase__badge">
          <svg viewBox="0 0 36 36" width="36" height="36" fill="none">
            <rect width="36" height="36" rx="8" fill="rgba(255,255,255,0.15)" />
            <path d="M10 10h7v7h-7zM19 10h7v7h-7zM10 19h7v7h-7zM19 19h7v7h-7z"
                  fill="#fff" opacity="0.85" />
          </svg>
        </div>
        <h1 class="bl-showcase__brand">BML</h1>
        <p class="bl-showcase__slogan">Business Management Platform</p>
        <div class="bl-showcase__divider" />
        <p class="bl-showcase__desc">
          统一身份认证 · 权限精细管控 · 安全高效协同
        </p>
      </div>

      <!-- 底部版权 -->
      <p class="bl-showcase__copyright">COPYRIGHT &copy; 2026 BML TEAM</p>
    </div>

    <!-- ════════════════════════════════════════════════════
         右侧登录区（40%）
         ════════════════════════════════════════════════════ -->
    <div class="bl-form-side">
      <div class="bl-form-container">
        <!-- 顶部装饰图（可替换，登录框背景图） -->
        <div v-if="cardBgImageUrl" class="bl-form-hero">
          <img :src="cardBgImageUrl" alt="装饰图" class="bl-form-hero__img" />
        </div>

        <!-- 欢迎标题 -->
        <div class="bl-form-header">
          <h2 class="bl-form-header__title">欢迎回来</h2>
          <p class="bl-form-header__sub">请输入您的账号信息登录系统</p>
        </div>

        <!-- 登录表单 -->
        <a-form :model="form" @submit="handleSubmit" layout="vertical" class="bl-form">
          <!-- 用户名 -->
          <a-form-item field="username" hide-label>
            <a-input
              v-model="form.username"
              placeholder="用户名"
              allow-clear
              size="large"
            >
              <template #prefix><icon-user /></template>
            </a-input>
          </a-form-item>

          <!-- 密码 -->
          <a-form-item field="password" hide-label>
            <a-input-password
              v-model="form.password"
              placeholder="密码"
              allow-clear
              size="large"
            >
              <template #prefix><icon-lock /></template>
            </a-input-password>
          </a-form-item>

          <!-- 验证码（仅在后端开启时显示） -->
          <a-form-item v-if="captchaEnabled" field="captchaCode" hide-label>
            <div class="bl-captcha-row">
              <a-input
                v-model="form.captchaCode"
                placeholder="请输入验证码"
                allow-clear
                size="large"
                class="bl-captcha-input"
                @keyup.enter="handleSubmit({ errors: undefined })"
              >
                <template #prefix><icon-safe /></template>
              </a-input>
              <div class="bl-captcha-img" @click="refreshCaptcha" title="点击刷新验证码">
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                <span v-else class="bl-captcha-placeholder">加载中</span>
              </div>
            </div>
          </a-form-item>

          <!-- 登录按钮 -->
          <a-form-item hide-label>
            <a-button
              type="primary"
              html-type="submit"
              long
              size="large"
              :loading="loading"
              class="bl-submit-btn"
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
 * 采用左右分栏布局：
 *   - 左侧 60%：展示区（品牌 Logo、标语、可替换背景图、微光粒子动画）
 *   - 右侧 40%：纯白登录表单区（简约干净，信息清晰）
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
const cardBgImageUrl = ref('');

/**
 * 左侧展示区背景样式
 * 有自定义背景图时使用图片，否则使用内置深色渐变
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
    cardBgImageUrl.value = config['sys.login.cardBgImage'] || '';

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
/* ══════════════════════════════════════════════════════
   页面容器 — 全屏左右分栏
   ══════════════════════════════════════════════════════ */
.bl-page {
  position: fixed;
  inset: 0;
  display: flex;
  font-family: var(--bml-font-family, 'Inter', -apple-system, BlinkMacSystemFont,
    'PingFang SC', 'Microsoft YaHei', sans-serif);
}

/* ══════════════════════════════════════════════════════
   左侧展示区（60%）
   ══════════════════════════════════════════════════════ */
.bl-showcase {
  position: relative;
  flex: 0 0 60%;
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

/* ── 渐变叠加层 ── */
.bl-showcase__overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(0, 0, 0, 0.1) 0%, rgba(0, 0, 0, 0.4) 100%),
    radial-gradient(ellipse at 30% 20%, rgba(99, 102, 241, 0.2) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 80%, rgba(168, 85, 247, 0.15) 0%, transparent 50%);
  pointer-events: none;
  z-index: 1;
}

/* ── 微光粒子 ── */
.bl-showcase__particles {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  overflow: hidden;
}
.bl-particle {
  position: absolute;
  bottom: -10px;
  background: #fff;
  border-radius: 50%;
  animation: bl-float linear infinite;
}
@keyframes bl-float {
  0% {
    transform: translateY(0) scale(1);
    opacity: 0.3;
  }
  100% {
    transform: translateY(-110vh) scale(0.2);
    opacity: 0;
  }
}

/* ── 品牌内容 ── */
.bl-showcase__content {
  position: relative;
  z-index: 3;
  text-align: center;
  color: #fff;
  padding: 0 48px;
  animation: bl-fade-in 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.2s both;
}
@keyframes bl-fade-in {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}
.bl-showcase__badge {
  display: inline-flex;
  margin-bottom: 20px;
}
.bl-showcase__brand {
  font-size: 52px;
  font-weight: 900;
  letter-spacing: 10px;
  margin: 0;
  line-height: 1.1;
  text-shadow: 0 4px 24px rgba(0, 0, 0, 0.3);
}
.bl-showcase__slogan {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 3px;
  text-transform: uppercase;
  margin: 8px 0 0;
}
.bl-showcase__divider {
  width: 48px;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  margin: 28px auto;
  border-radius: 1px;
}
.bl-showcase__desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 2px;
  margin: 0;
  line-height: 1.8;
}

/* ── 底部版权 ── */
.bl-showcase__copyright {
  position: absolute;
  bottom: 28px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.2);
  letter-spacing: 2px;
  z-index: 3;
  margin: 0;
}

/* ══════════════════════════════════════════════════════
   右侧登录区（40%）
   ══════════════════════════════════════════════════════ */
.bl-form-side {
  flex: 0 0 40%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  overflow-y: auto;
  padding: 40px;
}
.bl-form-container {
  width: 100%;
  max-width: 380px;
  animation: bl-slide-in 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.3s both;
}
@keyframes bl-slide-in {
  from { opacity: 0; transform: translateX(24px); }
  to   { opacity: 1; transform: translateX(0); }
}

/* ── 顶部装饰图（登录框背景图） ── */
.bl-form-hero {
  width: 100%;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 28px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}
.bl-form-hero__img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  display: block;
}

/* ── 欢迎标题 ── */
.bl-form-header {
  margin-bottom: 32px;
}
.bl-form-header__title {
  font-size: 28px;
  font-weight: 800;
  color: #1d2129;
  margin: 0 0 8px;
  line-height: 1.2;
}
.bl-form-header__sub {
  font-size: 14px;
  color: #86909c;
  margin: 0;
  line-height: 1.5;
}

/* ══════════════════════════════════════════════════════
   表单样式（亮色主题）
   ══════════════════════════════════════════════════════ */
.bl-form :deep(.arco-form-item) {
  margin-bottom: 20px;
}
.bl-form :deep(.arco-input-wrapper) {
  height: 48px;
  border-radius: 12px;
  background: #f7f8fa;
  border: 1.5px solid #e5e6eb;
  transition: all 0.25s ease;
}
.bl-form :deep(.arco-input-wrapper:hover) {
  border-color: #c9cdd4;
  background: #f2f3f5;
}
.bl-form :deep(.arco-input-wrapper.arco-input-focus) {
  background: #fff;
  border-color: #165dff;
  box-shadow: 0 0 0 3px rgba(22, 93, 255, 0.1);
}
.bl-form :deep(.arco-input) {
  color: #1d2129;
}
.bl-form :deep(.arco-input::placeholder) {
  color: #c9cdd4;
}
.bl-form :deep(.arco-input-prefix) {
  color: #86909c;
}
.bl-form :deep(.arco-input-suffix .arco-icon),
.bl-form :deep(.arco-input-clear-btn) {
  color: #c9cdd4;
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
  flex: 0 0 130px;
  height: 48px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  background: #f7f8fa;
  border: 1.5px solid #e5e6eb;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s;
}
.bl-captcha-img:hover {
  border-color: #165dff;
  background: #f2f3f5;
}
.bl-captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.bl-captcha-placeholder {
  font-size: 12px;
  color: #c9cdd4;
}

/* ── 登录按钮 ── */
.bl-submit-btn {
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 6px;
  background: linear-gradient(135deg, #165dff 0%, #3c7eff 100%);
  border: none;
  box-shadow: 0 4px 16px rgba(22, 93, 255, 0.25);
  transition: all 0.3s ease;
}
.bl-submit-btn:hover {
  background: linear-gradient(135deg, #0e42d2 0%, #306fff 100%);
  box-shadow: 0 6px 24px rgba(22, 93, 255, 0.35);
  transform: translateY(-1px);
}
.bl-submit-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 12px rgba(22, 93, 255, 0.2);
}

/* ══════════════════════════════════════════════════════
   响应式适配 — 移动端切换为垂直布局
   ══════════════════════════════════════════════════════ */
@media (max-width: 900px) {
  .bl-page {
    flex-direction: column;
  }
  .bl-showcase {
    flex: 0 0 240px;
  }
  .bl-showcase__brand {
    font-size: 36px;
    letter-spacing: 6px;
  }
  .bl-showcase__desc,
  .bl-showcase__divider,
  .bl-showcase__copyright {
    display: none;
  }
  .bl-form-side {
    flex: 1;
    padding: 32px 24px;
  }
  .bl-form-container {
    max-width: 420px;
  }
}
@media (max-width: 500px) {
  .bl-showcase {
    flex: 0 0 180px;
  }
  .bl-showcase__brand {
    font-size: 28px;
    letter-spacing: 4px;
  }
  .bl-showcase__slogan {
    display: none;
  }
  .bl-form-side {
    padding: 24px 20px;
  }
  .bl-form-header__title {
    font-size: 22px;
  }
  .bl-captcha-img {
    flex: 0 0 110px;
  }
}
</style>
