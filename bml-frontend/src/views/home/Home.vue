<script setup lang="ts">
import { useRouter } from 'vue-router';
import {
  IconApps,
  IconArrowRight,
  IconCloud,
  IconLock,
  IconRelation,
  IconStorage,
  IconUserGroup
} from '@arco-design/web-vue/es/icon';

const router = useRouter();

const businessCards = [
  {
    title: '企业档案中心',
    description: '统一承载企业资料、行业信息、联系人和状态流转，后续开放接口统一挂载到 /api/open/api/enterprise/company/**。',
    icon: IconStorage
  },
  {
    title: '企业系统账号',
    description: '企业前台中的账号查询、启停和权限联动能力，建议统一规划到 /api/open/api/enterprise/system-account/**。',
    icon: IconUserGroup
  },
  {
    title: 'BML API 授权中台',
    description: '所有对外开放业务接口都通过 BML 中台统一同步目录、分配 API 账号并配置接口授权。',
    icon: IconLock
  }
];

const architectureSteps = [
  {
    title: '企业业务前台',
    value: 'http://localhost:5173/',
    description: '承载企业管理业务页面、组织视图和系统账号使用界面。'
  },
  {
    title: '企业开放接口',
    value: '/api/open/api/enterprise/**',
    description: '未来所有给客户接入的企业业务接口都统一走这一层，自动纳入 BML API 授权目录。'
  },
  {
    title: 'BML 中台',
    value: 'http://localhost:5173/admin',
    description: '在 API账号管理中维护 AccessKey / SecretKey，并授权可访问的企业业务接口。'
  }
];

const deliveryItems = [
  '已补齐企业业务开放接口样板：企业档案分页/详情、企业系统账号分页/详情、业务概览摘要。',
  '已补齐开放接口目录启动自动同步，新增 /open/api/enterprise/** 后可自动进入 BML 中台授权目录。',
  '已明确前台入口与中台入口边界：前台走业务页面，/admin 走中台授权与运维管理。'
];

const securityNotices = [
  'BML API 账号当前基于 AccessKey + SecretKey 签名，最适合服务端对服务端，或前台经自有业务服务端/BFF 代签访问。',
  '浏览器、H5、小程序、公开 App 直接内置 SecretKey 会暴露密钥，不建议把 SecretKey 下发到终端。',
  '如果后续要支持真正的终端直连，需要再补短期令牌、设备绑定或 OAuth2 等机制。'
];

const goAdmin = () => {
  router.push('/admin');
};

const goApiAccountManage = () => {
  router.push('/admin/api/account');
};
</script>

<template>
  <div class="enterprise-home">
    <section class="hero-panel">
      <div class="hero-copy">
        <p class="eyebrow">Enterprise Management Front Portal</p>
        <h1>企业管理系统前台入口</h1>
        <p class="hero-text">
          这里是后续企业管理业务系统的前台入口，面向企业业务功能、系统账号使用界面和接入说明。
          所有需要给外部客户调用的业务接口，统一规划到
          <code>/api/open/api/enterprise/**</code>，
          并由 BML 中台中的 API 账号管理进行授权。
        </p>
        <div class="hero-actions">
          <a-button type="primary" size="large" class="hero-button" @click="goAdmin">
            <template #icon><icon-apps /></template>
            进入 BML 中台
          </a-button>
          <a-button size="large" class="hero-button hero-button-secondary" @click="goApiAccountManage">
            <template #icon><icon-relation /></template>
            打开 API账号管理
          </a-button>
        </div>
      </div>

      <div class="hero-highlight">
        <div class="highlight-card">
          <span>业务开放接口规范</span>
          <strong>/api/open/api/enterprise/**</strong>
        </div>
        <div class="highlight-card">
          <span>BML 中台入口</span>
          <strong>http://localhost:5173/admin</strong>
        </div>
        <div class="highlight-card">
          <span>前台入口</span>
          <strong>http://localhost:5173/</strong>
        </div>
      </div>
    </section>

    <section class="section-grid">
      <article v-for="item in businessCards" :key="item.title" class="feature-card">
        <div class="feature-icon">
          <component :is="item.icon" />
        </div>
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
      </article>
    </section>

    <section class="architecture-panel">
      <div class="section-heading">
        <div>
          <p class="section-eyebrow">Access Architecture</p>
          <h2>统一接入与授权路径</h2>
        </div>
        <a-tag color="arcoblue">统一走开放接口目录同步与 API 账号授权</a-tag>
      </div>
      <div class="architecture-steps">
        <div v-for="(step, index) in architectureSteps" :key="step.title" class="step-card">
          <span class="step-index">0{{ index + 1 }}</span>
          <h3>{{ step.title }}</h3>
          <strong>{{ step.value }}</strong>
          <p>{{ step.description }}</p>
          <icon-arrow-right v-if="index < architectureSteps.length - 1" class="step-arrow" />
        </div>
      </div>
    </section>

    <section class="split-layout">
      <article class="info-panel">
        <div class="section-heading">
          <div>
            <p class="section-eyebrow">Delivered Foundation</p>
            <h2>当前已交付的企业开放接口基础</h2>
          </div>
          <icon-cloud class="heading-icon" />
        </div>
        <ul class="bullet-list">
          <li v-for="item in deliveryItems" :key="item">{{ item }}</li>
        </ul>
      </article>

      <article class="warning-panel">
        <div class="section-heading">
          <div>
            <p class="section-eyebrow">Security Boundary</p>
            <h2>前台接入安全边界</h2>
          </div>
          <icon-lock class="heading-icon" />
        </div>
        <ul class="bullet-list bullet-list-warning">
          <li v-for="item in securityNotices" :key="item">{{ item }}</li>
        </ul>
      </article>
    </section>
  </div>
</template>

<style scoped>
.enterprise-home {
  min-height: 100vh;
  padding: 40px 32px 56px;
  background:
    radial-gradient(circle at top left, rgba(25, 118, 210, 0.14), transparent 28%),
    radial-gradient(circle at top right, rgba(0, 180, 150, 0.18), transparent 24%),
    linear-gradient(180deg, #eef5fb 0%, #f7fafc 52%, #edf4f8 100%);
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, 0.75fr);
  gap: 24px;
  padding: 40px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 32px;
  background:
    linear-gradient(135deg, rgba(15, 108, 255, 0.96), rgba(27, 190, 184, 0.92)),
    linear-gradient(135deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.04));
  box-shadow: 0 24px 60px rgba(24, 74, 132, 0.16);
  color: #fff;
}

.eyebrow,
.section-eyebrow {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.hero-copy h1,
.section-heading h2,
.feature-card h2,
.step-card h3 {
  margin: 0;
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.hero-copy h1 {
  font-size: clamp(34px, 4vw, 56px);
  line-height: 1.06;
}

.hero-text {
  max-width: 720px;
  margin: 22px 0 0;
  font-size: 18px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.94);
}

.hero-text code {
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 28px;
}

.hero-button {
  min-width: 184px;
  height: 48px;
  border-radius: 999px;
  font-weight: 700;
}

.hero-button-secondary {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.66);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px);
}

.hero-highlight {
  display: grid;
  gap: 16px;
}

.highlight-card {
  padding: 24px 24px 22px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(14px);
}

.highlight-card span {
  display: block;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.highlight-card strong {
  display: block;
  margin-top: 10px;
  font-size: 22px;
  line-height: 1.5;
}

.section-grid,
.split-layout {
  display: grid;
  gap: 24px;
  margin-top: 28px;
}

.section-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.feature-card,
.architecture-panel,
.info-panel,
.warning-panel {
  border: 1px solid rgba(214, 225, 238, 0.88);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 44px rgba(18, 57, 94, 0.08);
}

.feature-card {
  padding: 28px;
}

.feature-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border-radius: 18px;
  background: linear-gradient(135deg, #1067f3, #23b9b2);
  color: #fff;
  font-size: 26px;
  box-shadow: 0 14px 24px rgba(16, 103, 243, 0.24);
}

.feature-card h2 {
  margin-top: 18px;
  font-size: 24px;
  color: var(--bml-color-text-1, #1f2d3d);
}

.feature-card p,
.step-card p,
.bullet-list li {
  color: var(--bml-color-text-2, #5b6b7f);
  line-height: 1.8;
}

.architecture-panel,
.info-panel,
.warning-panel {
  padding: 30px;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.section-heading h2 {
  font-size: 28px;
  color: var(--bml-color-text-1, #1f2d3d);
}

.heading-icon {
  color: var(--bml-color-primary, #0f6cff);
  font-size: 24px;
}

.architecture-steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 24px;
}

.step-card {
  position: relative;
  min-height: 210px;
  padding: 24px;
  border: 1px solid rgba(216, 226, 239, 0.94);
  border-radius: 24px;
  background: linear-gradient(180deg, #f9fcff, #f1f6fb);
}

.step-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
  height: 32px;
  border-radius: 999px;
  background: rgba(15, 108, 255, 0.12);
  color: var(--bml-color-primary, #0f6cff);
  font-weight: 700;
}

.step-card strong {
  display: block;
  margin-top: 14px;
  font-size: 20px;
  color: var(--bml-color-text-1, #16324f);
  line-height: 1.6;
  word-break: break-all;
}

.step-arrow {
  position: absolute;
  right: -10px;
  top: calc(50% - 10px);
  color: #8cb4ff;
  font-size: 20px;
}

.split-layout {
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 0.85fr);
}

.bullet-list {
  display: grid;
  gap: 16px;
  margin: 24px 0 0;
  padding: 0;
  list-style: none;
}

.bullet-list li {
  position: relative;
  padding-left: 18px;
}

.bullet-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 12px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #0f6cff;
}

.bullet-list-warning li::before {
  background: #f59e0b;
}

@media (max-width: 1280px) {
  .hero-panel,
  .section-grid,
  .architecture-steps,
  .split-layout {
    grid-template-columns: 1fr;
  }

  .step-arrow {
    display: none;
  }
}

@media (max-width: 768px) {
  .enterprise-home {
    padding: 20px 16px 32px;
  }

  .hero-panel,
  .architecture-panel,
  .info-panel,
  .warning-panel,
  .feature-card {
    padding: 24px;
    border-radius: 24px;
  }

  .hero-actions,
  .hero-button {
    width: 100%;
  }
}
</style>
