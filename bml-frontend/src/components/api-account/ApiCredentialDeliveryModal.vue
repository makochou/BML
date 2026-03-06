<template>
  <a-modal
    v-model:visible="visibleModel"
    width="1000px"
    :footer="false"
    class="credential-delivery-modal-v2"
    unmount-on-close
    :mask-closable="false"
  >
    <div v-if="payload" class="credential-cert">
      <!-- 顶部装饰与成功状态 -->
      <header class="cert-header">
        <div class="cert-header__decoration">
          <div class="orb orb--1"></div>
          <div class="orb orb--2"></div>
        </div>
        
        <div class="cert-header__main">
          <div class="cert-status">
            <div class="cert-status__icon">
              <icon-check-circle-fill />
            </div>
            <div class="cert-status__text">
              <p>ACCOUNT SECURITY PROVISIONING</p>
              <h2>API账号凭证交付成功</h2>
            </div>
          </div>
          <div class="cert-badge">
            <icon-safe />
            <span>SecretKey 仅展示一次</span>
          </div>
        </div>
      </header>

      <main class="cert-content">
        <!-- 核心凭证区 -->
        <section class="cert-section cert-section--keys">
          <div class="section-title">
            <icon-lock />
            <h3>核心调用凭证</h3>
            <small>SECURITY CREDENTIALS</small>
          </div>
          
          <div class="key-cards">
            <article 
              v-for="item in keyCards" 
              :key="item.label"
              class="key-card"
              :class="`is-${item.tone}`"
            >
              <div class="key-card__info">
                <strong>{{ item.label }}</strong>
                <p>{{ item.hint }}</p>
              </div>
              <div class="key-card__value">
                <code>{{ item.value }}</code>
                <a-tooltip :content="`复制 ${item.label}`">
                  <a-button 
                    shape="circle" 
                    size="small" 
                    class="key-copy-btn"
                    @click="copyCredentialValue(item.label, String(item.value))"
                  >
                    <template #icon><icon-copy /></template>
                  </a-button>
                </a-tooltip>
              </div>
            </article>
          </div>
        </section>

        <!-- 详细画像与环境区 -->
        <div class="cert-grid">
          <section class="cert-section">
            <div class="section-title">
              <icon-idcard />
              <h3>账号身份画像</h3>
            </div>
            <div class="profile-grid">
              <div v-for="item in profileCards" :key="item.label" class="profile-item">
                <span class="profile-item__label">{{ item.label }}</span>
                <div class="profile-item__value">
                  <EllipsisTooltipText :text="item.value || '-'" />
                  <icon-copy 
                    v-if="item.copyable" 
                    class="copy-mini"
                    @click="copyCredentialValue(item.label, String(item.value))"
                  />
                </div>
              </div>
            </div>
          </section>

          <section class="cert-section">
            <div class="section-title">
              <icon-settings />
              <h3>运行时配置</h3>
            </div>
            <div class="runtime-grid">
              <div class="runtime-item">
                <span class="runtime-item__label">当前接入环境</span>
                <a-tag :color="getEnvironmentTagColor(payload.accessEnvironment)" bordered>
                  {{ getAccessEnvironmentLabel(payload.accessEnvironment) }}
                </a-tag>
              </div>
              <div class="runtime-item">
                <span class="runtime-item__label">限流阈值 (QPS)</span>
                <strong>{{ payload.rateLimit || 1000 }}</strong>
              </div>
              <div class="runtime-item">
                <span class="runtime-item__label">签名版本</span>
                <a-tag size="small" bordered>{{ payload.signVersion || 'v1' }}</a-tag>
              </div>
              <div class="runtime-item">
                <span class="runtime-item__label">授权API数量</span>
                <strong>{{ payload.authorizedApiCount || 0 }}</strong>
              </div>
            </div>
            
            <div class="whitelist-summary">
              <p>来源 IP 白名单 ({{ getAccessEnvironmentLabel(payload.accessEnvironment) }})</p>
              <div class="ip-list">
                <template v-if="currentEnvWhitelist.length">
                  <a-tag v-for="ip in currentEnvWhitelist" :key="ip" size="small" color="gray">{{ ip }}</a-tag>
                </template>
                <span v-else class="empty-text">未配置（允许所有 IP）</span>
              </div>
            </div>
          </section>
        </div>

        <!-- 安全警示 -->
        <footer class="cert-footer">
          <div class="safety-notices">
            <div v-for="item in safetyGuide" :key="item.title" class="safety-notice">
              <icon-info-circle />
              <div>
                <strong>{{ item.title }}</strong>
                <p>{{ item.description }}</p>
              </div>
            </div>
          </div>
          
          <div class="cert-actions">
            <a-button size="large" class="btn-bundle" @click="copyCredentialBundle">
              <template #icon><icon-copy /></template>
              复制整套凭证
            </a-button>
            <a-button type="primary" size="large" class="btn-done" @click="closeModal">
              我已安全保存
            </a-button>
          </div>
        </footer>
      </main>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import { Message } from '@arco-design/web-vue';
import { 
  IconCheckCircleFill, 
  IconSafe, 
  IconLock, 
  IconCopy, 
  IconIdcard, 
  IconSettings, 
  IconInfoCircle 
} from '@arco-design/web-vue/es/icon';
import EllipsisTooltipText from '../common/EllipsisTooltipText.vue';
import type { AccessEnvironment, ApiCredentialPayload } from '../../types/apiAccount';
import type { FactCard } from '../../types/governance';
import {
  getAccessEnvironmentLabel,
  getClientTypeLabels,
  getEnvironmentTagColor,
  getWhitelistByEnvironment
} from '../../utils/api-account-governance';

type CredentialKeyCard = {
  label: string;
  value: string;
  hint: string;
  tone: 'blue' | 'violet';
};

type CredentialGuideItem = {
  title: string;
  description: string;
};

const props = defineProps<{
  visible: boolean;
  payload: ApiCredentialPayload | null;
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
}>();

const visibleModel = computed({
  get: () => props.visible,
  set: value => emit('update:visible', value)
});

const safetyGuide: CredentialGuideItem[] = [
  { title: '先复制，再关闭', description: 'SecretKey 仅在本次交付窗口中可见。' },
  { title: '仅在安全域内分发', description: '推荐录入企业密钥管理平台（如 Apollo / Vault）。' },
  { title: '核对接入环境', description: '确保白名单已包含当前调用方的来源 IP。' }
];

const keyCards = computed<CredentialKeyCard[]>(() => {
  const payload = props.payload;
  if (!payload) return [];
  return [
    { label: 'AccessKey', value: payload.accessKey, hint: '公开身份标识', tone: 'blue' },
    { label: 'SecretKey', value: payload.secretKey, hint: '机密私钥，请严格保密', tone: 'violet' }
  ];
});

const profileCards = computed<FactCard[]>(() => {
  const payload = props.payload;
  if (!payload) return [];
  return [
    { label: '账号名称', value: payload.accountName, hint: '' },
    { label: '业务系统', value: payload.systemName, hint: '' },
    { label: '负责人', value: payload.ownerName, hint: '' },
    { label: '系统ID', value: `#${payload.id}`, hint: '', copyable: true },
    { label: '系统编码', value: payload.systemCode, hint: '', copyable: true },
    { label: '联系方式', value: payload.ownerContact, hint: '', copyable: true }
  ];
});

const currentEnvWhitelist = computed(() => {
  if (!props.payload) return [];
  return getWhitelistByEnvironment(props.payload, props.payload.accessEnvironment as AccessEnvironment);
});

const credentialBundleText = computed(() => {
  const payload = props.payload;
  if (!payload) return '';
  return [
    `【BML API账号凭证交付】`,
    `账号名称: ${payload.accountName}`,
    `系统账号ID: #${payload.id}`,
    `业务系统: ${payload.systemName} (${payload.systemCode})`,
    `负责人: ${payload.ownerName} (${payload.ownerContact})`,
    `接入环境: ${getAccessEnvironmentLabel(payload.accessEnvironment)}`,
    `调用方范围: ${getClientTypeLabels(payload.clientTypes).join('、') || '未限制'}`,
    `签名算法版本: ${payload.signVersion || 'v1'}`,
    `限流配置 (QPS): ${payload.rateLimit || 1000}`,
    `AccessKey: ${payload.accessKey}`,
    `SecretKey: ${payload.secretKey}`,
    `--------------------------`,
    `注意：SecretKey 仅展示一次，请务必妥善保存。`
  ].join('\n');
});

function closeModal() {
  visibleModel.value = false;
}

async function writeClipboardText(value: string) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(value);
    return;
  }
  const textarea = document.createElement('textarea');
  textarea.value = value;
  textarea.setAttribute('readonly', 'readonly');
  textarea.style.position = 'fixed';
  textarea.style.top = '-9999px';
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand('copy');
  document.body.removeChild(textarea);
}

async function copyCredentialValue(label: string, value?: string | null) {
  if (!value) return;
  await writeClipboardText(value);
  Message.success(`${label}已复制`);
}

async function copyCredentialBundle() {
  if (!credentialBundleText.value) return;
  await writeClipboardText(credentialBundleText.value);
  Message.success('完整凭证已复制');
}
</script>

<style scoped>
/* 弹窗基础重置 */
:deep(.credential-delivery-modal-v2 .arco-modal) {
  border-radius: 24px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 40px 100px rgba(15, 23, 42, 0.25);
}

:deep(.credential-delivery-modal-v2 .arco-modal-body) {
  padding: 0;
  background: #fff;
}

.credential-cert {
  display: flex;
  flex-direction: column;
  background: #f8fafc;
}

/* 顶部 Header：冰川蓝渐变 + 装饰性光球 */
.cert-header {
  position: relative;
  padding: 16px 48px;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  color: #fff;
  overflow: hidden;
}

.cert-header__decoration .orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  z-index: 0;
}

.orb--1 {
  width: 200px;
  height: 200px;
  top: -50px;
  right: -50px;
  background: rgba(59, 130, 246, 0.3);
}

.orb--2 {
  width: 150px;
  height: 150px;
  bottom: -30px;
  left: 10%;
  background: rgba(139, 92, 246, 0.2);
}

.cert-header__main {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.cert-status {
  display: flex;
  align-items: center;
  gap: 20px;
}

.cert-status__icon {
  font-size: 36px;
  color: #10b981;
  filter: drop-shadow(0 0 15px rgba(16, 185, 129, 0.4));
}

.cert-status__text p {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.15em;
  color: rgba(255, 255, 255, 0.6);
  text-transform: uppercase;
}

.cert-status__text h2 {
  margin: 4px 0 0;
  font-size: 24px;
  font-weight: 800;
}

.cert-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 999px;
  backdrop-filter: blur(10px);
  font-size: 13px;
  font-weight: 600;
  color: #fbbf24;
}

/* 主体内容 */
.cert-content {
  padding: 16px 48px;
}

.cert-section {
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.section-title .arco-icon {
  font-size: 20px;
  color: #3b82f6;
}

.section-title h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.section-title small {
  margin-left: 8px;
  font-size: 11px;
  color: #94a3b8;
  letter-spacing: 0.05em;
}

/* 凭证卡片：强调安全性与现代感 */
.key-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.key-card {
  padding: 12px 20px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid #e2e8f0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.key-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
}

.key-card.is-blue {
  background: linear-gradient(135deg, #f0f9ff 0%, #ffffff 100%);
  border-left: 6px solid #3b82f6;
}

.key-card.is-violet {
  background: linear-gradient(135deg, #f5f3ff 0%, #ffffff 100%);
  border-left: 6px solid #8b5cf6;
}

.key-card__info strong {
  display: block;
  font-size: 15px;
  color: #1e293b;
}

.key-card__info p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #64748b;
}

.key-card__value {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.key-card__value code {
  flex: 1;
  padding: 6px 10px;
  background: #0f172a;
  color: #f8fafc;
  border-radius: 12px;
  font-size: 14px;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  word-break: break-all;
}

.key-copy-btn {
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #e2e8f0;
  color: #64748b;
}

.key-copy-btn:hover {
  background: #f8fafc;
  color: #3b82f6;
  border-color: #3b82f6;
}

/* 详情网格 */
.cert-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 40px;
}

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 12px;
  background: #fff;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
}

.profile-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.profile-item__label {
  font-size: 12px;
  color: #94a3b8;
}

.profile-item__value {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.copy-mini {
  font-size: 14px;
  color: #cbd5e1;
  cursor: pointer;
  transition: color 0.2s;
}

.copy-mini:hover {
  color: #3b82f6;
}

.runtime-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 12px;
}

.runtime-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}

.runtime-item__label {
  font-size: 12px;
  color: #94a3b8;
}

.runtime-item strong {
  font-size: 16px;
  color: #0f172a;
}

.whitelist-summary {
  padding: 10px;
  background: rgba(241, 245, 249, 0.5);
  border-radius: 16px;
  border: 1px dashed #cbd5e1;
}

.whitelist-summary p {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
}

.ip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.empty-text {
  font-size: 12px;
  color: #94a3b8;
  font-style: italic;
}

/* 底部功能区 */
.cert-footer {
  margin-top: 4px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.safety-notices {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 12px;
}

.safety-notice {
  display: flex;
  gap: 12px;
}

.safety-notice .arco-icon {
  margin-top: 2px;
  font-size: 18px;
  color: #64748b;
}

.safety-notice strong {
  display: block;
  font-size: 14px;
  color: #1e293b;
}

.safety-notice p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
}

.cert-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.btn-bundle {
  min-width: 180px;
  height: 38px;
  border-radius: 12px;
  font-weight: 700;
  border: 1px solid #e2e8f0;
}

.btn-done {
  min-width: 220px;
  height: 38px;
  border-radius: 12px;
  font-weight: 700;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  box-shadow: 0 10px 25px rgba(37, 99, 235, 0.25);
  border: none;
}

.btn-done:hover {
  transform: translateY(-2px);
  box-shadow: 0 15px 30px rgba(37, 99, 235, 0.35);
}

/* 响应式适配 */
@media (max-width: 1024px) {
  .cert-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .cert-header {
    padding: 32px 24px;
  }
  .cert-content {
    padding: 24px;
  }
  .key-cards {
    grid-template-columns: 1fr;
  }
  .safety-notices {
    grid-template-columns: 1fr;
  }
  .cert-actions {
    flex-direction: column;
  }
}
</style>
