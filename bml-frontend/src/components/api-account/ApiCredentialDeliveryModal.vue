<template>
  <a-modal
    v-model:visible="visibleModel"
    width="1180px"
    :footer="false"
    class="credential-delivery-modal"
    unmount-on-close
  >
    <template #title>
      <div class="credential-delivery-modal__title">
        <div class="credential-delivery-modal__title-main">
          <p>Security Credential Delivery Center</p>
          <strong>请立即保存新凭证</strong>
        </div>
        <span class="credential-delivery-modal__badge">SecretKey 仅展示一次</span>
      </div>
    </template>

    <GovernanceWorkbenchShell
      v-if="payload"
      class="credential-delivery-workbench"
      eyebrow="Secure Credential Bundle"
      title="凭证只会在本次交付窗口中返回"
      description="建议先复制整套凭证，再同步保存到企业密钥平台、部署变量或安全保险箱。关闭弹窗、刷新页面或再次重置后，系统不会返回旧 SecretKey。"
      :tags="heroTags"
      :stats="summaryCards"
      theme="violet"
    >
      <template #aside>
        <GovernancePanel class="credential-delivery-panel credential-delivery-panel--attention" title="保存动作">
          <div class="credential-delivery-step-list">
            <article
              v-for="item in safetyGuide"
              :key="item.title"
              class="credential-delivery-step"
            >
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
            </article>
          </div>
        </GovernancePanel>

        <GovernancePanel class="credential-delivery-panel" title="签名请求头">
          <div class="credential-delivery-header-list">
            <article
              v-for="item in requestHeaders"
              :key="item.name"
              class="credential-delivery-header-item"
            >
              <div class="credential-delivery-header-item__top">
                <strong>{{ item.name }}</strong>
                <span>{{ item.value }}</span>
              </div>
              <small>{{ item.description }}</small>
            </article>
          </div>
        </GovernancePanel>
      </template>

      <div class="credential-delivery-main">
        <GovernancePanel class="credential-delivery-panel credential-delivery-panel--keys">
          <template #header>
            <div class="section-heading">
              <div>
                <h3>密钥交付区</h3>
                <p>以下内容为本次生成的真实凭证，建议按字段复制后立即完成安全留存。</p>
              </div>
            </div>
          </template>
          <div class="credential-delivery-key-grid">
            <article
              v-for="item in keyCards"
              :key="item.label"
              class="credential-delivery-key-card"
              :class="`tone-${item.tone}`"
            >
              <div class="credential-delivery-key-card__head">
                <div>
                  <span>{{ item.label }}</span>
                  <small>{{ item.hint }}</small>
                </div>
                <a-button size="mini" @click="copyCredentialValue(item.label, item.value)">复制</a-button>
              </div>
              <code>{{ item.value }}</code>
            </article>
          </div>
        </GovernancePanel>

        <GovernancePanel class="credential-delivery-panel">
          <template #header>
            <div class="section-heading">
              <div>
                <h3>账号画像</h3>
                <p>账号身份、归属系统与责任人信息已与本次凭证绑定，便于后续授权排查和日志追踪。</p>
              </div>
            </div>
          </template>
          <GovernanceFactGrid
            class="credential-delivery-profile-grid"
            card-class="credential-delivery-profile-card"
            copy-class="credential-delivery-profile-card__copy"
            :items="profileCards"
          />
          <div class="credential-delivery-aux-grid">
            <article class="credential-delivery-aux-card">
              <span>调用客户端</span>
              <div v-if="clientLabels.length" class="tags">
                <a-tag
                  v-for="item in clientLabels"
                  :key="`credential-client-${item}`"
                  color="arcoblue"
                >
                  {{ item }}
                </a-tag>
              </div>
              <strong v-else>未维护</strong>
              <small>用于识别当前账号允许接入的终端形态。</small>
            </article>
            <article class="credential-delivery-aux-card">
              <span>业务回调地址</span>
              <a-typography-text
                v-if="payload.callbackUrl"
                copyable
                class="credential-delivery-profile-card__copy"
              >
                {{ payload.callbackUrl }}
              </a-typography-text>
              <strong v-else>{{ callbackValue }}</strong>
              <small>用于异步结果通知和回调联调，未配置时可按需补充。</small>
            </article>
          </div>
        </GovernancePanel>

        <GovernancePanel class="credential-delivery-panel">
          <template #header>
            <div class="section-heading section-heading--row">
              <div>
                <h3>环境白名单</h3>
                <p>测试、预发、生产环境独立维护来源 IP，系统会按当前接入环境自动选择生效清单。</p>
              </div>
              <a-tag :color="getEnvironmentTagColor(payload.accessEnvironment)">
                当前生效：{{ getAccessEnvironmentLabel(payload.accessEnvironment) }}
              </a-tag>
            </div>
          </template>
          <ApiEnvironmentWhitelistCards :payload="payload" />
        </GovernancePanel>
      </div>

      <template #footer>
        <div class="credential-delivery-footer">
          <div class="credential-delivery-footer__tip">
            <strong>交付建议</strong>
            <span>复制完成后，建议立即录入企业密钥管理系统，并避免通过聊天工具、明文邮件或工单评论传播 SecretKey。</span>
          </div>
          <div class="credential-delivery-footer__actions">
            <a-button @click="copyCredentialBundle">复制整套凭证</a-button>
            <a-button type="primary" @click="closeModal">我已保存</a-button>
          </div>
        </div>
      </template>
    </GovernanceWorkbenchShell>
  </a-modal>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import { Message } from '@arco-design/web-vue';
import ApiEnvironmentWhitelistCards from './ApiEnvironmentWhitelistCards.vue';
import GovernanceFactGrid from '../governance/GovernanceFactGrid.vue';
import GovernancePanel from '../governance/GovernancePanel.vue';
import GovernanceWorkbenchShell from '../governance/GovernanceWorkbenchShell.vue';
import type { AccessEnvironment, ApiCredentialPayload } from '../../types/apiAccount';
import type { FactCard, WorkbenchStatCard } from '../../types/governance';
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

type CredentialHeaderItem = {
  name: string;
  value: string;
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

// 凭证交付组件内部统一维护提示文案、请求头说明和摘要卡映射，避免多个页面重复拼装同一份安全交付逻辑。
const heroTags = ['SecretKey 仅回传一次', '支持整套复制', '建议立即入库', '环境白名单独立治理'];
const safetyGuide: CredentialGuideItem[] = [
  { title: '先复制，再关闭', description: '建议优先点击“复制整套凭证”，确保 AccessKey 与 SecretKey 一并被安全留存。' },
  { title: '仅在安全域内分发', description: 'SecretKey 不应通过聊天工具、明文邮件或工单评论传播，推荐录入企业密钥平台。' },
  { title: '按环境完成联调', description: '核对当前接入环境、白名单和回调地址后，再向业务方交付正式调用能力。' }
];
const requestHeaders: CredentialHeaderItem[] = [
  { name: 'X-Bml-App-Key', value: 'AccessKey', description: '调用方身份标识，所有签名请求都必须携带。' },
  { name: 'X-Bml-Sign-Version', value: '签名算法版本', description: '需与当前账号配置的签名版本保持一致。' },
  { name: 'X-Bml-Timestamp', value: '当前毫秒时间戳', description: '用于防重放校验，建议与服务端时间保持同步。' },
  { name: 'X-Bml-Nonce', value: '每次请求唯一随机串', description: '同一请求不可复用，避免签名被重复消费。' },
  { name: 'X-Bml-Sign', value: '签名结果', description: '按请求方法、路径、Query 与 Body 计算出的最终签名值。' }
];

const summaryCards = computed<WorkbenchStatCard[]>(() => {
  const payload = props.payload;
  if (!payload) return [];
  return [
    { label: '系统账号ID', value: `#${payload.id}`, hint: '用于日志检索、工单排查与授权追踪', tone: 'blue' },
    {
      label: '当前环境',
      value: getAccessEnvironmentLabel(payload.accessEnvironment),
      hint: `${getWhitelistByEnvironment(payload, payload.accessEnvironment as AccessEnvironment).length} 条白名单命中此环境`,
      tone: 'green'
    },
    {
      label: '客户端范围',
      value: payload.clientTypes?.length ? `${payload.clientTypes.length} 类终端` : '未维护',
      hint: payload.clientTypes?.length ? getClientTypeLabels(payload.clientTypes).join('、') : '当前账号未维护客户端范围',
      tone: 'gold'
    },
    { label: '签名版本', value: payload.signVersion || 'v1', hint: '调用方需按此版本生成请求签名', tone: 'violet' }
  ];
});

const keyCards = computed<CredentialKeyCard[]>(() => {
  const payload = props.payload;
  if (!payload) return [];
  return [
    { label: 'AccessKey', value: payload.accessKey, hint: '作为请求身份标识，可在调用链路中公开传输。', tone: 'blue' },
    { label: 'SecretKey', value: payload.secretKey, hint: '仅本次返回，必须安全保管，系统不会再次回显旧值。', tone: 'violet' }
  ];
});

const profileCards = computed<FactCard[]>(() => {
  const payload = props.payload;
  if (!payload) return [];
  return [
    { label: '账号名称', value: payload.accountName, hint: '当前凭证绑定的业务接入账号。' },
    { label: '业务系统名称', value: payload.systemName, hint: '用于识别所属业务系统与接入主体。' },
    { label: '业务系统编码', value: payload.systemCode, hint: '建议在调用方配置中心中保持同一编码。', copyable: true },
    { label: '接入方负责人', value: payload.ownerName, hint: '用于联调、异常处理和责任人追踪。' },
    { label: '联系方式', value: payload.ownerContact, hint: '推荐填写手机号、邮箱或企业微信。', copyable: true },
    { label: '系统账号ID', value: `#${payload.id}`, hint: '可直接用于后台检索账号详情与授权记录。', copyable: true }
  ];
});

const clientLabels = computed(() => getClientTypeLabels(props.payload?.clientTypes));
const callbackValue = computed(() => props.payload?.callbackUrl || '未配置');
const credentialBundleText = computed(() => {
  const payload = props.payload;
  if (!payload) return '';
  return [
    `账号名称: ${payload.accountName}`,
    `系统账号ID: #${payload.id}`,
    `业务系统名称: ${payload.systemName}`,
    `业务系统编码: ${payload.systemCode}`,
    `接入方负责人: ${payload.ownerName}`,
    `联系方式: ${payload.ownerContact}`,
    `接入环境: ${getAccessEnvironmentLabel(payload.accessEnvironment)}`,
    `签名算法版本: ${payload.signVersion || 'v1'}`,
    `测试环境白名单: ${getWhitelistByEnvironment(payload, 'test').join('、') || '未限制'}`,
    `预发环境白名单: ${getWhitelistByEnvironment(payload, 'staging').join('、') || '未限制'}`,
    `生产环境白名单: ${getWhitelistByEnvironment(payload, 'production').join('、') || '未限制'}`,
    `业务回调地址: ${payload.callbackUrl || '未配置'}`,
    `AccessKey: ${payload.accessKey}`,
    `调用客户端: ${getClientTypeLabels(payload.clientTypes).join('、') || '未维护'}`,
    `SecretKey: ${payload.secretKey}`
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
  Message.success(`${label}已复制到剪贴板`);
}

async function copyCredentialBundle() {
  if (!credentialBundleText.value) return;
  await writeClipboardText(credentialBundleText.value);
  Message.success('凭证已复制到剪贴板');
}
</script>

<style scoped>
:deep(.credential-delivery-modal .arco-modal) {
  overflow: hidden;
  border-radius: 32px;
  box-shadow: 0 36px 96px rgba(15, 23, 42, 0.2);
}

:deep(.credential-delivery-modal .arco-modal-header) {
  padding: 22px 28px 0;
  border-bottom: 0;
}

:deep(.credential-delivery-modal .arco-modal-body) {
  padding: 20px 28px 28px;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.07), transparent 28%),
    linear-gradient(180deg, #f7fafc, #f3f6fb);
}

.credential-delivery-modal__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.credential-delivery-modal__title-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.credential-delivery-modal__title-main p {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.credential-delivery-modal__title-main strong {
  font-size: 20px;
  color: #0f172a;
}

.credential-delivery-modal__badge {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.1), rgba(16, 185, 129, 0.1));
  border: 1px solid rgba(37, 99, 235, 0.12);
  font-size: 12px;
  font-weight: 600;
  color: #0f766e;
}

.credential-delivery-workbench,
.credential-delivery-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.credential-delivery-panel {
  padding: 22px;
  border-radius: 28px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.06);
}

.credential-delivery-panel--attention {
  background:
    radial-gradient(circle at top right, rgba(56, 189, 248, 0.08), transparent 34%),
    linear-gradient(180deg, #f8fbff, #ffffff);
}

.credential-delivery-panel--keys {
  background:
    radial-gradient(circle at top right, rgba(99, 102, 241, 0.07), transparent 30%),
    linear-gradient(180deg, #fbfdff, #ffffff);
}

.credential-delivery-step-list,
.credential-delivery-header-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.credential-delivery-step,
.credential-delivery-header-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  background: linear-gradient(180deg, #fbfdff, #f8fbff);
}

.credential-delivery-step strong,
.credential-delivery-header-item strong {
  color: #0f172a;
  font-size: 15px;
}

.credential-delivery-step p,
.credential-delivery-header-item small {
  margin: 0;
  color: #64748b;
  line-height: 1.75;
}

.credential-delivery-header-item__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.credential-delivery-header-item__top span {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: #eef6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
}

.credential-delivery-key-grid,
.credential-delivery-aux-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.credential-delivery-key-card,
.credential-delivery-profile-card,
.credential-delivery-aux-card {
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: linear-gradient(180deg, #ffffff, #fbfdff);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.credential-delivery-key-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 182px;
  padding: 20px;
  border-radius: 24px;
}

.credential-delivery-key-card.tone-blue {
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.95), rgba(255, 255, 255, 0.98));
  border-color: rgba(59, 130, 246, 0.2);
}

.credential-delivery-key-card.tone-violet {
  background: linear-gradient(180deg, rgba(245, 243, 255, 0.95), rgba(255, 255, 255, 0.98));
  border-color: rgba(139, 92, 246, 0.2);
}

.credential-delivery-key-card__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
}

.credential-delivery-key-card__head span {
  display: block;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.credential-delivery-key-card__head small {
  display: block;
  margin-top: 6px;
  color: #64748b;
  line-height: 1.65;
}

.credential-delivery-key-card code {
  display: block;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.92);
  color: #f8fafc;
  font-size: 20px;
  line-height: 1.65;
  word-break: break-all;
  white-space: pre-wrap;
}

.credential-delivery-profile-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.credential-delivery-profile-card,
.credential-delivery-aux-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px 20px;
  border-radius: 22px;
}

.credential-delivery-profile-card span,
.credential-delivery-aux-card span,
.credential-delivery-footer__tip span {
  color: #64748b;
  font-size: 12px;
  line-height: 1.7;
}

.credential-delivery-profile-card strong,
.credential-delivery-aux-card strong,
.credential-delivery-footer__tip strong {
  color: #0f172a;
  font-size: 18px;
  line-height: 1.5;
  word-break: break-word;
}

.credential-delivery-profile-card__copy {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.5;
  color: #0f172a;
  word-break: break-word;
}

.credential-delivery-aux-card {
  min-height: 148px;
  justify-content: space-between;
}

.credential-delivery-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 18px 22px;
  border-radius: 24px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  background: linear-gradient(180deg, #ffffff, #fbfdff);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.05);
}

.credential-delivery-footer__tip {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-width: 720px;
}

.credential-delivery-footer__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.credential-delivery-footer__actions :deep(.arco-btn),
.credential-delivery-key-card__head :deep(.arco-btn) {
  border-radius: 999px;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.section-heading--row {
  align-items: center;
}

.section-heading h3 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.section-heading p {
  margin: 6px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 1280px) {
  .credential-delivery-modal__title,
  .credential-delivery-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .credential-delivery-key-grid,
  .credential-delivery-aux-grid,
  .credential-delivery-profile-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  :deep(.credential-delivery-modal .arco-modal-header) {
    padding: 18px 18px 0;
  }

  :deep(.credential-delivery-modal .arco-modal-body) {
    padding: 16px 18px 18px;
  }

  .credential-delivery-panel,
  .credential-delivery-footer {
    padding: 18px;
  }

  .credential-delivery-key-card code {
    font-size: 16px;
  }
}
</style>
