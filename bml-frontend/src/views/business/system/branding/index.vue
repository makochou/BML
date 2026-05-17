<template>
  <div class="branding-page">
    <a-card class="hero-card" :bordered="false">
      <div class="hero-main">
        <div>
          <div class="eyebrow">SYSTEM BRANDING</div>
          <h2>品牌设置</h2>
          <p>统一维护业务系统登录页文案、验证码开关、空闲超时与品牌图片。</p>
        </div>
        <a-space>
          <a-button :loading="loading" @click="loadConfig"><template #icon><icon-refresh /></template>刷新</a-button>
          <a-button type="primary" :loading="saving" v-if="hasPermission('system:setting:edit')" @click="saveAll"><template #icon><icon-save /></template>保存配置</a-button>
        </a-space>
      </div>
    </a-card>

    <a-row :gutter="16" class="setting-row">
      <a-col :span="12">
        <a-card :bordered="false" class="setting-card">
          <template #title>登录策略</template>
          <a-form layout="vertical">
            <a-form-item label="图形验证码">
              <a-switch v-model="captchaEnabled" type="round"><template #checked>开启</template><template #unchecked>关闭</template></a-switch>
            </a-form-item>
            <a-form-item label="空闲自动登出（分钟，0 表示不限制）">
              <a-input-number v-model="idleTimeout" :min="0" :max="1440" style="width: 220px" />
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card :bordered="false" class="setting-card">
          <template #title>品牌文案</template>
          <a-form layout="vertical">
            <a-form-item label="品牌标题"><a-input v-model="brandTitle" allow-clear placeholder="BML 企业管理系统" /></a-form-item>
            <a-form-item label="品牌副标题"><a-input v-model="brandSlogan" allow-clear placeholder="智慧企业管理平台" /></a-form-item>
            <a-form-item label="品牌描述"><a-input v-model="brandDesc" allow-clear placeholder="统一身份认证 · 权限精细管控" /></a-form-item>
          </a-form>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16">
      <a-col :span="8" v-for="item in imageCards" :key="item.type">
        <a-card :bordered="false" class="image-card">
          <template #title>{{ item.title }}</template>
          <template #extra><a-tag>{{ item.tips }}</a-tag></template>
          <div class="preview-box" :class="{ empty: !item.url }">
            <img v-if="item.url" :src="item.url" :alt="item.title" />
            <div v-else class="empty-text"><icon-image />暂未设置</div>
          </div>
          <a-space class="image-actions">
            <a-upload :auto-upload="false" :show-file-list="false" accept="image/*,.ico,.svg" @change="handleImageChange(item.type, $event)">
              <template #upload-button><a-button type="primary" v-if="hasPermission('system:setting:upload')"><template #icon><icon-upload /></template>上传/替换</a-button></template>
            </a-upload>
            <a-button status="danger" :disabled="!item.url || !hasPermission('system:setting:remove')" @click="deleteImage(item.type)"><template #icon><icon-delete /></template>恢复默认</a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { useButtonPermission } from '../../../../composables/useButtonPermission';
import { batchUpdateConfig, deleteBrandingImage, fetchLoginConfig, uploadBrandingImage } from '../../../../api/config';

const { hasPermission } = useButtonPermission();
const loading = ref(false);
const saving = ref(false);
const captchaEnabled = ref(false);
const idleTimeout = ref(30);
const brandTitle = ref('');
const brandSlogan = ref('');
const brandDesc = ref('');
const loginBgUrl = ref('');
const sidebarLogoUrl = ref('');
const faviconUrl = ref('');

const imageCards = computed(() => [
  { type: 'loginBg', title: '登录页背景图', tips: '1920×1080', url: loginBgUrl.value },
  { type: 'sidebarLogo', title: '侧边栏 Logo', tips: '200×60', url: sidebarLogoUrl.value },
  { type: 'favicon', title: '浏览器图标', tips: '64×64', url: faviconUrl.value },
]);

async function loadConfig() {
  loading.value = true;
  try {
    const res = await fetchLoginConfig() as any;
    const config = res.data || {};
    captchaEnabled.value = config['sys.login.captchaEnabled'] === 'true';
    idleTimeout.value = Number(config['sys.login.idleTimeout'] || 30);
    brandTitle.value = config['sys.login.brandTitle'] || '';
    brandSlogan.value = config['sys.login.brandSlogan'] || '';
    brandDesc.value = config['sys.login.brandDesc'] || '';
    loginBgUrl.value = config['sys.login.bgImage'] || '';
    sidebarLogoUrl.value = config['sys.sidebar.logo'] || '';
    faviconUrl.value = config['sys.login.favicon'] || '';
  } finally {
    loading.value = false;
  }
}

async function saveAll() {
  saving.value = true;
  try {
    await batchUpdateConfig({
      'sys.login.captchaEnabled': String(captchaEnabled.value),
      'sys.login.idleTimeout': String(idleTimeout.value || 0),
      'sys.login.brandTitle': brandTitle.value,
      'sys.login.brandSlogan': brandSlogan.value,
      'sys.login.brandDesc': brandDesc.value,
    });
    Message.success('品牌设置已保存');
  } finally {
    saving.value = false;
  }
}

async function uploadImage(type: string, fileItem: any) {
  const file = fileItem?.file;
  if (!file) return;
  const res = await uploadBrandingImage(type, file) as any;
  const url = res.data?.url || '';
  if (type === 'loginBg') loginBgUrl.value = url;
  if (type === 'sidebarLogo') sidebarLogoUrl.value = url;
  if (type === 'favicon') faviconUrl.value = url;
  Message.success('上传成功');
}

function handleImageChange(type: string, event: unknown) {
  const fileItem = Array.isArray(event) ? event[1] : event;
  uploadImage(type, fileItem);
}

function deleteImage(type: string) {
  Modal.warning({
    title: '确认恢复默认',
    content: '确定删除当前品牌图片并恢复默认显示吗？',
    hideCancel: false,
    async onOk() {
      await deleteBrandingImage(type);
      if (type === 'loginBg') loginBgUrl.value = '';
      if (type === 'sidebarLogo') sidebarLogoUrl.value = '';
      if (type === 'favicon') faviconUrl.value = '';
      Message.success('已恢复默认');
    },
  });
}

onMounted(loadConfig);
</script>

<style scoped>
.branding-page { min-height: 100%; padding: 18px; background: linear-gradient(180deg, #f8fbff 0%, #f3f7fb 100%); }
.hero-card, .setting-card, .image-card { border-radius: 18px; }
.hero-card, .setting-row { margin-bottom: 16px; }
.hero-main { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.eyebrow { color: #2563eb; font-size: 12px; font-weight: 800; letter-spacing: 0.12em; }
h2 { margin: 6px 0; font-size: 24px; color: #1f2d3d; }
p { margin: 0; color: #697386; }
.preview-box { height: 180px; border-radius: 14px; overflow: hidden; background: #f4f7fb; display: flex; align-items: center; justify-content: center; }
.preview-box.empty { border: 1px dashed #b7c4d6; }
.preview-box img { width: 100%; height: 100%; object-fit: cover; }
.empty-text { color: #8792a2; display: flex; align-items: center; gap: 8px; }
.image-actions { margin-top: 14px; }
</style>
