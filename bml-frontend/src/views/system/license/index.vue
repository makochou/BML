<template>
  <div class="license-page-shell">
    <div class="license-page-shell__glow license-page-shell__glow--left"></div>
    <div class="license-page-shell__glow license-page-shell__glow--right"></div>

    <!--
      说明：
      1. 当前页统一承接“许可证激活 / 开发模式 / 已激活授权总览”三种状态；
      2. 所有交互仍然沿用既有接口，不改变后端契约，只重构视觉与信息架构；
      3. 页面所有展示字段均通过计算属性驱动，方便后续继续扩充授权模块与配额项。
    -->
    <section v-if="showActivationScene" class="license-activation">
      <article class="activation-story panel-surface">
        <span class="panel-eyebrow">Authorization Launchpad</span>
        <h1 class="activation-story__title">{{ activationTitle }}</h1>
        <p class="activation-story__desc">{{ activationDescription }}</p>

        <div class="activation-story__metrics">
          <div v-for="item in activationMetrics" :key="item.label" class="story-metric">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </div>
        </div>

        <div class="activation-story__timeline">
          <div v-for="item in activationSteps" :key="item.title" class="story-step">
            <div class="story-step__index">{{ item.index }}</div>
            <div class="story-step__content">
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
            </div>
          </div>
        </div>

        <div class="activation-story__tips">
          <div v-for="item in activationTips" :key="item.title" class="story-tip">
            <icon-info-circle />
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
            </div>
          </div>
        </div>
      </article>

      <article class="activation-console panel-surface">
        <div class="activation-console__head">
          <div class="console-badge">
            <icon-safe />
            <span>{{ activationConsoleBadge }}</span>
          </div>
          <div class="console-mode-tag" :class="`tone-${activationTone}`">
            {{ activationModeTag }}
          </div>
        </div>

        <div class="activation-console__hero">
          <div class="console-icon">
            <icon-upload />
          </div>
          <div>
            <h2>{{ activationConsoleTitle }}</h2>
            <p>{{ activationConsoleDescription }}</p>
          </div>
        </div>

        <div v-if="licenseErrorText" class="console-alert" :class="`tone-${activationTone}`">
          <icon-exclamation-circle />
          <span>{{ licenseErrorText }}</span>
        </div>

        <a-upload
          :auto-upload="false"
          :limit="1"
          accept=".lic"
          draggable
          :file-list="fileList"
          @change="handleFileChange"
        >
          <template #upload-button>
            <div class="upload-dropzone" :class="{ 'is-ready': selectedFileSummary }">
              <div class="upload-dropzone__icon">
                <icon-safe />
              </div>
              <div class="upload-dropzone__content">
                <strong>{{ selectedFileSummary?.name || '拖拽许可证文件到此处，或点击选择文件' }}</strong>
                <p v-if="selectedFileSummary">
                  大小 {{ selectedFileSummary.size }} · {{ selectedFileSummary.extension }}
                </p>
                <p v-else>
                  仅支持 `.lic` 文件，上传前会先在本地完成文件校验与状态联动。
                </p>
              </div>
            </div>
          </template>
        </a-upload>

        <div class="activation-console__footer">
          <div class="console-hint">
            <icon-lock />
            <span>{{ activationFooterHint }}</span>
          </div>
          <a-button
            type="primary"
            size="large"
            long
            :loading="uploading"
            :disabled="!selectedFile"
            class="activation-submit-btn"
            @click="handleUpload"
          >
            <template #icon>
              <icon-upload />
            </template>
            {{ activationSubmitLabel }}
          </a-button>
        </div>
      </article>
    </section>

    <section v-else class="license-dashboard">
      <header class="dashboard-hero panel-surface" :class="{ 'is-expired': isExpired }">
        <div class="dashboard-hero__bar">
          <div class="hero-status-group">
            <span class="hero-status-pill" :class="`tone-${healthTone}`">
              <icon-check-circle v-if="!isExpired" />
              <icon-exclamation-circle v-else />
              {{ heroStatusLabel }}
            </span>
            <span class="hero-mode-pill">{{ heroModeLabel }}</span>
          </div>

          <div class="hero-actions">
            <a-upload
              :auto-upload="false"
              :limit="1"
              :show-file-list="false"
              :file-list="replaceFileList"
              accept=".lic"
              @change="handleReplaceFile"
            >
              <template #upload-button>
                <a-button type="primary" :loading="previewing" class="hero-action-btn hero-action-btn--primary">
                  <template #icon><icon-upload /></template>
                  更新许可证
                </a-button>
              </template>
            </a-upload>
            <a-button class="hero-action-btn" @click="loadLicenseStatus">
              <template #icon><icon-refresh /></template>
              刷新状态
            </a-button>
            <a-popconfirm
              content="确定要删除许可证并重置系统吗？系统将回到未激活状态。"
              type="warning"
              @ok="handleReset"
            >
              <a-button status="danger" :loading="resetting" class="hero-action-btn hero-action-btn--danger">
                <template #icon><icon-delete /></template>
                重置许可证
              </a-button>
            </a-popconfirm>
          </div>
        </div>

        <div class="dashboard-hero__main">
          <div class="hero-customer">
            <div class="hero-customer__avatar">{{ customerMonogram }}</div>
            <div class="hero-customer__content">
              <span class="panel-eyebrow">Enterprise License Profile</span>
              <h1>{{ licenseData?.customerName || '未命名客户' }}</h1>
              <p>{{ heroStatusDescription }}</p>
              <div class="hero-meta-tags">
                <span v-for="item in heroMetaTags" :key="item" class="hero-meta-tag">{{ item }}</span>
              </div>
            </div>
          </div>

          <div class="hero-countdown-card">
            <div class="hero-countdown-card__ring" :style="daysRingStyle">
              <div class="hero-countdown-card__ring-core">
                <strong>{{ daysDisplayValue }}</strong>
                <span>天</span>
              </div>
            </div>
            <div class="hero-countdown-card__meta">
              <span class="hero-countdown-card__label">剩余有效期</span>
              <strong :class="`tone-${healthTone}`">{{ daysLeftText }}</strong>
            </div>
          </div>
        </div>

        <div class="dashboard-hero__timeline">
          <div v-for="item in lifecycleCards" :key="item.label" class="hero-timeline-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.hint }}</p>
          </div>
        </div>
      </header>

      <div v-if="licenseData?.errorMessage" class="dashboard-warning panel-surface" :class="`tone-${healthTone}`">
        <icon-exclamation-circle-fill />
        <span>{{ licenseData.errorMessage }}</span>
      </div>

      <div class="dashboard-grid">
        <section class="dashboard-panel panel-surface dashboard-panel--identity">
          <div class="panel-header">
            <div>
              <span class="panel-eyebrow">Identity Blueprint</span>
              <h2>授权档案</h2>
            </div>
            <div class="panel-header__icon tone-blue">
              <icon-safe />
            </div>
          </div>
          <div class="fact-list">
            <div v-for="item in overviewFacts" :key="item.label" class="fact-row">
              <span class="fact-row__label">{{ item.label }}</span>
              <div class="fact-row__value">
                <strong :class="{ mono: item.mono }">{{ item.value }}</strong>
                <small>{{ item.hint }}</small>
              </div>
            </div>
          </div>
        </section>

        <section class="dashboard-panel panel-surface dashboard-panel--quota">
          <div class="panel-header">
            <div>
              <span class="panel-eyebrow">Quota Envelope</span>
              <h2>配额上限</h2>
            </div>
            <div class="panel-header__icon tone-teal">
              <icon-thunderbolt />
            </div>
          </div>
          <div class="quota-list">
            <article v-for="metric in quotaMetrics" :key="metric.label" class="quota-card">
              <div class="quota-card__head">
                <div>
                  <strong>{{ metric.label }}</strong>
                  <span>{{ metric.hint }}</span>
                </div>
                <b>{{ metric.display }}</b>
              </div>
              <div class="quota-card__track">
                <div class="quota-card__fill" :style="{ width: `${metric.percent}%` }"></div>
              </div>
            </article>
          </div>
        </section>

        <section class="dashboard-panel panel-surface dashboard-panel--features">
          <div class="panel-header">
            <div>
              <span class="panel-eyebrow">Feature Ledger</span>
              <h2>授权模块</h2>
            </div>
            <div class="panel-header__icon tone-gold">
              <icon-apps />
            </div>
          </div>

          <div v-if="featureTags.length" class="feature-cloud">
            <article v-for="item in featureTags" :key="item.key" class="feature-chip" :class="`tone-${item.tone}`">
              <icon-check-circle-fill />
              <div>
                <strong>{{ item.label }}</strong>
                <span>功能许可项</span>
              </div>
            </article>
          </div>
          <a-empty v-else description="当前许可证尚未绑定具体模块功能" class="feature-empty">
            <template #image>
              <div class="feature-empty__symbol">
                <icon-bulb />
              </div>
            </template>
          </a-empty>

          <div class="feature-empty__helper">
            <strong>后续扩展建议</strong>
            <p>当前系统已支持许可证总激活与配额上限控制。待业务模块接入后，可直接在许可证中登记模块标签并映射到前台业务入口。</p>
          </div>
        </section>

        <section class="dashboard-panel panel-surface dashboard-panel--operations">
          <div class="panel-header">
            <div>
              <span class="panel-eyebrow">Delivery Checklist</span>
              <h2>运行诊断与交付提示</h2>
            </div>
            <div class="panel-header__icon tone-slate">
              <icon-lock />
            </div>
          </div>

          <div class="diagnostic-grid">
            <article v-for="item in diagnosticItems" :key="item.label" class="diagnostic-card" :class="`tone-${item.tone}`">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <p>{{ item.hint }}</p>
            </article>
          </div>

          <div class="tips-board">
            <div v-for="item in supportTips" :key="item.title" class="tips-board__item">
              <icon-info-circle />
              <div>
                <strong>{{ item.title }}</strong>
                <p>{{ item.description }}</p>
              </div>
            </div>
          </div>
        </section>
      </div>

      <footer class="dashboard-footer">
        <div v-for="item in footerBadges" :key="item.value" class="footer-badge" :class="`tone-${item.tone}`">
          <component :is="item.icon" />
          <span>{{ item.value }}</span>
        </div>
      </footer>
    </section>

    <a-modal
      v-model:visible="compareVisible"
      :width="1100"
      :mask-closable="false"
      :footer="false"
      :header="false"
      class="license-compare-modal"
      modal-class="license-compare-modal__wrap"
    >
      <section class="compare-shell">
        <header class="compare-shell__header">
          <div class="compare-shell__title">
            <div class="compare-shell__icon">
              <icon-swap />
            </div>
            <div>
              <span class="panel-eyebrow">License Compare Center</span>
              <h3>授权更新确认</h3>
              <p>{{ compareSummary }}</p>
            </div>
          </div>
          <button class="compare-shell__close" type="button" @click="closeCompare">
            <icon-close />
          </button>
        </header>

        <div v-if="downgradeWarnings.length" class="compare-shell__warning">
          <icon-exclamation-circle-fill />
          <span>{{ downgradeWarnings.join('；') }}</span>
        </div>

        <div class="compare-grid">
          <section class="compare-card compare-card--current">
            <div class="compare-card__head">
              <span>当前许可证</span>
              <small>Current</small>
            </div>
            <div class="compare-card__body">
              <div class="compare-block">
                <span class="compare-block__title">基本信息</span>
                <div v-for="row in compareBasicRows" :key="`current-${row.key}`" class="compare-row" :class="{ 'is-different': row.changed }">
                  <span class="compare-row__label">{{ row.label }}</span>
                  <strong>{{ row.current }}</strong>
                </div>
              </div>
              <div class="compare-block">
                <span class="compare-block__title">配额限制</span>
                <div v-for="row in compareQuotaRows" :key="`current-quota-${row.key}`" class="compare-row" :class="{ 'is-different': row.changed }">
                  <span class="compare-row__label">{{ row.label }}</span>
                  <strong>{{ row.current }}</strong>
                </div>
              </div>
              <div class="compare-block">
                <span class="compare-block__title">授权模块</span>
                <div class="compare-feature-list">
                  <a-tag v-for="item in currentFeatureTags" :key="`current-feature-${item.key}`" :color="item.color" class="compare-feature-tag">
                    {{ item.label }}
                  </a-tag>
                  <span v-if="!currentFeatureTags.length" class="compare-feature-empty">暂无模块</span>
                </div>
              </div>
            </div>
          </section>

          <section class="compare-card compare-card--next">
            <div class="compare-card__head">
              <span>新许可证</span>
              <small>Preview</small>
            </div>
            <div class="compare-card__body">
              <div class="compare-block">
                <span class="compare-block__title">基本信息</span>
                <div v-for="row in compareBasicRows" :key="`next-${row.key}`" class="compare-row" :class="{ 'is-different': row.changed }">
                  <span class="compare-row__label">{{ row.label }}</span>
                  <strong class="compare-row__next">
                    <icon-swap v-if="row.changed" />
                    {{ row.next }}
                  </strong>
                </div>
              </div>
              <div class="compare-block">
                <span class="compare-block__title">配额限制</span>
                <div v-for="row in compareQuotaRows" :key="`next-quota-${row.key}`" class="compare-row" :class="{ 'is-different': row.changed }">
                  <span class="compare-row__label">{{ row.label }}</span>
                  <strong class="compare-row__next" :class="row.direction">
                    <icon-arrow-up v-if="row.direction === 'up'" />
                    <icon-arrow-down v-else-if="row.direction === 'down'" />
                    <icon-swap v-else-if="row.changed" />
                    {{ row.next }}
                  </strong>
                </div>
              </div>
              <div class="compare-block">
                <span class="compare-block__title">授权模块</span>
                <div class="compare-feature-list">
                  <a-tag v-for="item in nextFeatureTags" :key="`next-feature-${item.key}`" :color="item.color" class="compare-feature-tag">
                    {{ item.label }}
                  </a-tag>
                  <span v-if="!nextFeatureTags.length" class="compare-feature-empty">暂无模块</span>
                </div>
              </div>
            </div>
          </section>
        </div>

        <footer class="compare-shell__footer">
          <div class="compare-shell__footer-tip">
            <icon-lock />
            <span>系统将先备份旧许可证，再覆盖为新许可证并即时刷新内存状态。</span>
          </div>
          <div class="compare-shell__actions">
            <a-button @click="closeCompare">取消</a-button>
            <a-button type="primary" :loading="uploading" @click="confirmUpdate">
              <template #icon><icon-check-circle /></template>
              确认更新
            </a-button>
          </div>
        </footer>
      </section>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { Message } from '@arco-design/web-vue';
import type { FileItem } from '@arco-design/web-vue';
import {
  IconApps,
  IconArrowDown,
  IconArrowUp,
  IconBulb,
  IconCheckCircle,
  IconCheckCircleFill,
  IconClose,
  IconDelete,
  IconExclamationCircle,
  IconExclamationCircleFill,
  IconFile,
  IconFolder,
  IconInfoCircle,
  IconLock,
  IconRefresh,
  IconSafe,
  IconSwap,
  IconThunderbolt,
  IconUpload
} from '@arco-design/web-vue/es/icon';
import {
  fetchLicenseStatus,
  previewLicense,
  resetLicense,
  updateLicense,
  uploadLicense,
  type LicenseStatus
} from '../../../api/license';
import { resetLicenseCache } from '../../../router';

/**
 * 页面数据结构定义。
 * 统一在页面内声明，方便未来拆分子组件或抽取为公共授权视图模型。
 */
interface SummaryMetric {
  label: string;
  value: string;
}

interface NarrativeStep {
  index: string;
  title: string;
  description: string;
}

interface NarrativeTip {
  title: string;
  description: string;
}

interface LifecycleCard {
  label: string;
  value: string;
  hint: string;
}

interface FactRow {
  label: string;
  value: string;
  hint: string;
  mono?: boolean;
}

interface LicenseQuotaMetric {
  label: string;
  value?: number;
  display: string;
  percent: number;
  hint: string;
}

interface DiagnosticCard {
  label: string;
  value: string;
  hint: string;
  tone: 'healthy' | 'warning' | 'expired' | 'slate';
}

interface FooterBadge {
  value: string;
  tone: 'blue' | 'teal' | 'gold' | 'slate';
  icon: typeof IconFile | typeof IconFolder | typeof IconInfoCircle;
}

interface CompareRow {
  key: string;
  label: string;
  current: string;
  next: string;
  changed: boolean;
}

interface CompareQuotaRow extends CompareRow {
  direction: 'up' | 'down' | 'same';
}

interface FeatureTag {
  key: string;
  label: string;
  tone: 'blue' | 'teal' | 'gold' | 'cyan';
  color: 'arcoblue' | 'green' | 'orange' | 'cyan' | 'gray' | 'red';
}

interface SelectedFileSummary {
  name: string;
  size: string;
  extension: string;
}

const DAY_MS = 24 * 60 * 60 * 1000;
const LICENSE_FULL_LIFECYCLE_DAYS = 365;
const QUOTA_VISUAL_MIN_PERCENT = 14;

/**
 * 功能模块中文标签映射。
 * 当前后端尚未真正启用模块授权，此处保留映射口，后续直接补充即可。
 */
const FEATURE_LABELS: Record<string, string> = {};

const licenseData = ref<LicenseStatus | null>(null);
const uploading = ref(false);
const previewing = ref(false);
const resetting = ref(false);
const errorMsg = ref('');

const fileList = ref<FileItem[]>([]);
const selectedFile = ref<File | null>(null);

/**
 * 由于 Arco Upload 在 limit=1 时会受到内部文件列表影响，
 * 因此更新许可证场景额外维护一个 fileList，确保重复打开上传器时按钮始终可见。
 */
const replaceFileList = ref<FileItem[]>([]);
const compareVisible = ref(false);
const previewData = ref<LicenseStatus | null>(null);
const pendingFile = ref<File | null>(null);

/**
 * 日期解析统一按“本地日期”处理，避免浏览器将 yyyy-MM-dd 误按 UTC 解析导致跨时区偏差。
 */
const parseDateOnly = (value?: string | null) => {
  if (!value) {
    return null;
  }
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value.trim());
  if (!match) {
    return null;
  }
  const [, year, month, day] = match;
  return new Date(Number(year), Number(month) - 1, Number(day), 0, 0, 0, 0);
};

/**
 * 将未知的功能编码做最小可读化处理。
 * 例如 api_gateway -> API GATEWAY，方便在未配置中文映射时仍保持可读。
 */
const humanizeFeatureCode = (code: string) =>
  code
    .split(/[_-]+/)
    .map(segment => segment.trim())
    .filter(Boolean)
    .map(segment => segment.toUpperCase())
    .join(' · ');

const formatFeatureLabel = (code: string) => FEATURE_LABELS[code] || humanizeFeatureCode(code) || code;

const formatQuota = (value?: number | null) => {
  if (value === undefined || value === null) {
    return '-';
  }
  return value === 0 ? '不限' : String(value);
};

const formatFileSize = (size: number) => {
  if (size < 1024) {
    return `${size} B`;
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }
  return `${(size / (1024 * 1024)).toFixed(2)} MB`;
};

const extractFirstFile = (items: FileItem[]) => {
  const first = items[0];
  if (!first) {
    return null;
  }
  return first.file instanceof File ? first.file : null;
};

const showActivationScene = computed(() => !licenseData.value?.activated);
const isExpired = computed(() => Boolean(licenseData.value?.expired));
const isLicenseCheckDisabled = computed(() => licenseData.value?.enabled === false);

const activationTone = computed<'healthy' | 'warning' | 'expired'>(() => {
  if (isLicenseCheckDisabled.value) {
    return 'warning';
  }
  if (errorMsg.value || licenseData.value?.errorMessage) {
    return 'expired';
  }
  return 'healthy';
});

const healthTone = computed<'healthy' | 'warning' | 'expired' | 'slate'>(() => {
  if (isLicenseCheckDisabled.value) {
    return 'warning';
  }
  if (isExpired.value) {
    return 'expired';
  }
  return 'healthy';
});

const licenseErrorText = computed(() => errorMsg.value || licenseData.value?.errorMessage || '');

const activationTitle = computed(() =>
  isLicenseCheckDisabled.value ? '开发模式授权工作台' : '系统授权激活中心'
);

const activationDescription = computed(() =>
  isLicenseCheckDisabled.value
    ? '当前环境已关闭许可证强校验，适合本地联调与演示验证。你仍可以上传正式许可证，预演真实授权状态。'
    : '上传有效许可证后，系统将立即完成验签、写盘、内存刷新，并解锁中台与业务端的全部授权能力。'
);

const activationConsoleBadge = computed(() =>
  isLicenseCheckDisabled.value ? 'Development Authorization' : 'Offline License Activation'
);

const activationModeTag = computed(() =>
  isLicenseCheckDisabled.value ? '开发模式' : '正式激活'
);

const activationConsoleTitle = computed(() =>
  isLicenseCheckDisabled.value ? '上传正式许可证进行联调预演' : '上传许可证并激活系统'
);

const activationConsoleDescription = computed(() =>
  isLicenseCheckDisabled.value
    ? '页面仍会按照正式流程完成预览、落盘和状态刷新，便于你提前检查授权内容是否正确。'
    : '上传后将自动完成签名校验与系统状态切换。若文件无效，页面会保留当前状态并给出具体错误。'
);

const activationSubmitLabel = computed(() =>
  isLicenseCheckDisabled.value ? '上传并预演授权文件' : '上传并激活许可证'
);

const activationFooterHint = computed(() =>
  isLicenseCheckDisabled.value
    ? '开发模式下不会强制拦截业务访问，但建议尽早用正式许可证完成一次完整验证。'
    : '建议在交付当天由实施人员与客户共同完成上传，以便即时核对客户编码、版本与有效期。'
);

const activationMetrics = computed<SummaryMetric[]>(() => [
  { label: '授权文件格式', value: '.LIC / RSA 签名' },
  { label: '状态刷新方式', value: '上传即生效' },
  { label: '默认校验范围', value: isLicenseCheckDisabled.value ? '开发跳过' : '全系统生效' }
]);

const activationSteps: NarrativeStep[] = [
  {
    index: '01',
    title: '选择离线签发的许可证文件',
    description: '支持实施工具导出的标准 .lic 文件，页面会先完成格式与签名校验。'
  },
  {
    index: '02',
    title: '系统写入授权文件并刷新缓存',
    description: '后端会先校验再落盘，避免无效文件覆盖现有授权状态。'
  },
  {
    index: '03',
    title: '中台与业务端立即读取最新授权',
    description: '配额、有效期与模块标签会同步更新，无需手工重启应用。'
  }
];

const activationTips: NarrativeTip[] = [
  {
    title: '建议同时核对客户编码与产品版本',
    description: '上传前先确认许可证对应的客户编码、产品版本与本次交付环境一致。'
  },
  {
    title: '更新授权前可先走预览对比',
    description: '已激活状态下上传新许可证会先进入对比视图，便于发现配额降级或模块变更。'
  }
];

const selectedFileSummary = computed<SelectedFileSummary | null>(() => {
  if (!selectedFile.value) {
    return null;
  }
  const segments = selectedFile.value.name.split('.');
  const extension = segments.length > 1 ? `.${segments[segments.length - 1].toLowerCase()}` : '未知格式';
  return {
    name: selectedFile.value.name,
    size: formatFileSize(selectedFile.value.size),
    extension
  };
});

const daysLeft = computed(() => {
  const expire = parseDateOnly(licenseData.value?.expireDate);
  if (!expire) {
    return null;
  }
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.ceil((expire.getTime() - today.getTime()) / DAY_MS);
});

const daysDisplayValue = computed(() => {
  if (daysLeft.value === null) {
    return '--';
  }
  return String(Math.max(daysLeft.value, 0));
});

const daysLeftText = computed(() => {
  if (daysLeft.value === null) {
    return '未登记到期时间';
  }
  if (daysLeft.value < 0) {
    return `已过期 ${Math.abs(daysLeft.value)} 天`;
  }
  if (daysLeft.value === 0) {
    return '今天到期';
  }
  if (daysLeft.value <= 30) {
    return `${daysLeft.value} 天内需更新`;
  }
  return `${daysLeft.value} 天`;
});

const ringColor = computed(() => {
  if (healthTone.value === 'expired') {
    return '#f53f3f';
  }
  if (healthTone.value === 'warning') {
    return '#ff7d00';
  }
  return '#14c9c9';
});

const daysRingStyle = computed(() => {
  const ratio =
    daysLeft.value === null
      ? 0.2
      : Math.max(0, Math.min(daysLeft.value / LICENSE_FULL_LIFECYCLE_DAYS, 1));
  const degree = 360 * ratio;
  return {
    background: `conic-gradient(${ringColor.value} 0deg ${degree}deg, rgba(15, 23, 42, 0.10) ${degree}deg 360deg)`
  };
});

const customerMonogram = computed(() => {
  const name = licenseData.value?.customerName?.trim();
  return name ? name.slice(0, 1).toUpperCase() : 'B';
});

const heroStatusLabel = computed(() => {
  if (isLicenseCheckDisabled.value) {
    return '开发模式';
  }
  return isExpired.value ? '授权已过期' : '许可证有效';
});

const heroModeLabel = computed(() => (licenseData.value?.enabled === false ? '校验关闭' : '实时校验中'));

const heroStatusDescription = computed(() => {
  if (isLicenseCheckDisabled.value) {
    return '当前环境未强制校验许可证，适合作为本地调试或功能联调环境。';
  }
  if (isExpired.value) {
    return '当前许可证已经过期，建议立即更新新许可证，避免业务能力继续受限。';
  }
  return '当前许可证已经加载到运行时内存，配额与授权信息将对中台和业务端立即生效。';
});

const heroMetaTags = computed(() => {
  const tags = [
    licenseData.value?.licenseId || '未生成许可证 ID',
    licenseData.value?.customerCode || '未登记客户编码',
    `v${licenseData.value?.productVersion || '-'}`
  ];
  if (licenseData.value?.enabled === false) {
    tags.push('开发模式跳过强校验');
  }
  return tags;
});

const lifecycleCards = computed<LifecycleCard[]>(() => [
  {
    label: '签发日期',
    value: licenseData.value?.issueDate || '-',
    hint: '标识授权正式生效的起始日期'
  },
  {
    label: '到期日期',
    value: licenseData.value?.expireDate || '-',
    hint: '到期后系统仍可展示页面，但授权会被标记为失效'
  },
  {
    label: '文件状态',
    value: licenseData.value?.filePath ? '已写入本地' : '尚未落盘',
    hint: licenseData.value?.filePath || '当前无有效文件路径'
  }
]);

const overviewFacts = computed<FactRow[]>(() => [
  {
    label: '许可证 ID',
    value: licenseData.value?.licenseId || '-',
    hint: '唯一标识本次授权文件',
    mono: true
  },
  {
    label: '客户名称',
    value: licenseData.value?.customerName || '-',
    hint: '当前授权归属客户'
  },
  {
    label: '客户编码',
    value: licenseData.value?.customerCode || '-',
    hint: '可与离线签发记录交叉核对',
    mono: true
  },
  {
    label: '产品版本',
    value: licenseData.value?.productVersion || '-',
    hint: '建议与当前部署版本保持一致'
  },
  {
    label: '授权备注',
    value: licenseData.value?.remark || '未填写',
    hint: '用于记录交付说明、升级说明或客户特殊约束'
  }
]);

const quotaMetrics = computed<LicenseQuotaMetric[]>(() => {
  const rawItems = [
    {
      label: '最大 API 账号数',
      value: licenseData.value?.maxApiAccounts,
      display: formatQuota(licenseData.value?.maxApiAccounts),
      hint: '控制可创建并启用的 API 账号总量'
    },
    {
      label: '单账号用户数',
      value: licenseData.value?.maxUsersPerAccount,
      display: formatQuota(licenseData.value?.maxUsersPerAccount),
      hint: '预留给未来按账号绑定业务用户的扩展字段'
    },
    {
      label: '业务用户上限',
      value: licenseData.value?.maxTotalUsers,
      display: formatQuota(licenseData.value?.maxTotalUsers),
      hint: '当前前台业务系统使用用户总量上限'
    }
  ];

  const finiteValues = rawItems
    .map(item => item.value)
    .filter((value): value is number => typeof value === 'number' && value > 0);
  const maxFiniteValue = finiteValues.length ? Math.max(...finiteValues) : 1;

  return rawItems.map(item => {
    if (item.value === undefined || item.value === null) {
      return { ...item, percent: 0 };
    }
    if (item.value === 0) {
      return { ...item, percent: 100 };
    }
    return {
      ...item,
      percent: Math.max(
        QUOTA_VISUAL_MIN_PERCENT,
        Math.min(100, Math.round((item.value / maxFiniteValue) * 100))
      )
    };
  });
});

const featureTags = computed<FeatureTag[]>(() => {
  const colors: FeatureTag['tone'][] = ['blue', 'teal', 'gold', 'cyan'];
  return (licenseData.value?.features || []).map((feature, index) => {
    const tone = colors[index % colors.length];
    return {
      key: feature,
      label: formatFeatureLabel(feature),
      tone,
      color:
        tone === 'blue'
          ? 'arcoblue'
          : tone === 'teal'
            ? 'green'
            : tone === 'gold'
              ? 'orange'
              : 'cyan'
    };
  });
});

const diagnosticItems = computed<DiagnosticCard[]>(() => [
  {
    label: '许可证校验',
    value: isLicenseCheckDisabled.value ? '已关闭' : '已开启',
    hint: isLicenseCheckDisabled.value ? '当前环境不会因许可证失效阻断访问' : '所有受控页面都会先检查许可证状态',
    tone: isLicenseCheckDisabled.value ? 'warning' : 'healthy'
  },
  {
    label: '当前状态',
    value: isExpired.value ? '已过期' : '运行中',
    hint: isExpired.value ? '建议尽快上传新许可证' : '授权内容已同步到运行时内存',
    tone: isExpired.value ? 'expired' : 'healthy'
  },
  {
    label: '模块数量',
    value: `${licenseData.value?.features?.length || 0} 项`,
    hint: '当前许可证内登记的业务模块标签数量',
    tone: licenseData.value?.features?.length ? 'healthy' : 'slate'
  },
  {
    label: '更新方式',
    value: '先预览后覆盖',
    hint: '更新时会先展示新旧差异，并由后端自动备份旧文件',
    tone: 'slate'
  }
]);

const supportTips: NarrativeTip[] = [
  {
    title: '建议更新前先核对降级风险',
    description: '如果新许可证减少了模块或配额，页面会在对比弹窗中明确提示，避免误覆盖。'
  },
  {
    title: '许可证文件路径建议纳入交付文档',
    description: '运维同事可依据路径快速确认当前实例所加载的授权文件来源。'
  }
];

const footerBadges = computed<FooterBadge[]>(() => {
  const items: FooterBadge[] = [];
  if (licenseData.value?.remark) {
    items.push({
      value: licenseData.value.remark,
      tone: 'gold',
      icon: IconInfoCircle
    });
  }
  if (licenseData.value?.filePath) {
    items.push({
      value: licenseData.value.filePath,
      tone: 'slate',
      icon: IconFolder
    });
  }
  if (licenseData.value?.licenseId) {
    items.push({
      value: `授权编号：${licenseData.value.licenseId}`,
      tone: 'blue',
      icon: IconFile
    });
  }
  return items;
});

const compareBasicRows = computed<CompareRow[]>(() => [
  {
    key: 'licenseId',
    label: '许可证 ID',
    current: licenseData.value?.licenseId || '-',
    next: previewData.value?.licenseId || '-',
    changed: (licenseData.value?.licenseId || '-') !== (previewData.value?.licenseId || '-')
  },
  {
    key: 'customerName',
    label: '客户名称',
    current: licenseData.value?.customerName || '-',
    next: previewData.value?.customerName || '-',
    changed: (licenseData.value?.customerName || '-') !== (previewData.value?.customerName || '-')
  },
  {
    key: 'customerCode',
    label: '客户编码',
    current: licenseData.value?.customerCode || '-',
    next: previewData.value?.customerCode || '-',
    changed: (licenseData.value?.customerCode || '-') !== (previewData.value?.customerCode || '-')
  },
  {
    key: 'productVersion',
    label: '产品版本',
    current: licenseData.value?.productVersion || '-',
    next: previewData.value?.productVersion || '-',
    changed: (licenseData.value?.productVersion || '-') !== (previewData.value?.productVersion || '-')
  },
  {
    key: 'expireDate',
    label: '到期日期',
    current: licenseData.value?.expireDate || '-',
    next: previewData.value?.expireDate || '-',
    changed: (licenseData.value?.expireDate || '-') !== (previewData.value?.expireDate || '-')
  }
]);

const compareQuotaRows = computed<CompareQuotaRow[]>(() => {
  const items = [
    {
      key: 'maxApiAccounts',
      label: '最大 API 账号数',
      currentRaw: licenseData.value?.maxApiAccounts,
      nextRaw: previewData.value?.maxApiAccounts
    },
    {
      key: 'maxUsersPerAccount',
      label: '单账号用户数',
      currentRaw: licenseData.value?.maxUsersPerAccount,
      nextRaw: previewData.value?.maxUsersPerAccount
    },
    {
      key: 'maxTotalUsers',
      label: '业务用户上限',
      currentRaw: licenseData.value?.maxTotalUsers,
      nextRaw: previewData.value?.maxTotalUsers
    }
  ];

  return items.map(item => {
    const current = formatQuota(item.currentRaw);
    const next = formatQuota(item.nextRaw);
    const currentComparable = item.currentRaw === 0 ? Number.POSITIVE_INFINITY : item.currentRaw ?? null;
    const nextComparable = item.nextRaw === 0 ? Number.POSITIVE_INFINITY : item.nextRaw ?? null;

    let direction: CompareQuotaRow['direction'] = 'same';
    if (currentComparable !== null && nextComparable !== null && currentComparable !== nextComparable) {
      direction = nextComparable > currentComparable ? 'up' : 'down';
    }

    return {
      key: item.key,
      label: item.label,
      current,
      next,
      changed: current !== next,
      direction
    };
  });
});

const addedFeatures = computed(() => {
  const current = new Set(licenseData.value?.features || []);
  return (previewData.value?.features || []).filter(feature => !current.has(feature));
});

const removedFeatures = computed(() => {
  const next = new Set(previewData.value?.features || []);
  return (licenseData.value?.features || []).filter(feature => !next.has(feature));
});

const currentFeatureTags = computed<FeatureTag[]>(() =>
  (licenseData.value?.features || []).map(feature => ({
    key: feature,
    label: formatFeatureLabel(feature),
    tone: 'blue',
    color: removedFeatures.value.includes(feature) ? 'red' : 'arcoblue'
  }))
);

const nextFeatureTags = computed<FeatureTag[]>(() => {
  const nextFeatures = previewData.value?.features || [];
  const tags: FeatureTag[] = nextFeatures.map(feature => ({
    key: feature,
    label: addedFeatures.value.includes(feature) ? `+ ${formatFeatureLabel(feature)}` : formatFeatureLabel(feature),
    tone: addedFeatures.value.includes(feature) ? 'teal' : 'blue',
    color: addedFeatures.value.includes(feature) ? 'green' : 'arcoblue'
  }));
  return tags.concat(
    removedFeatures.value.map<FeatureTag>(feature => ({
      key: `removed-${feature}`,
      label: `× ${formatFeatureLabel(feature)}`,
      tone: 'gold',
      color: 'red'
    }))
  );
});

const downgradeWarnings = computed(() => {
  const warnings: string[] = [];
  removedFeatures.value.forEach(feature => {
    warnings.push(`功能模块「${formatFeatureLabel(feature)}」将被移除`);
  });
  compareQuotaRows.value.forEach(row => {
    if (row.direction === 'down') {
      warnings.push(`${row.label}将从 ${row.current} 降至 ${row.next}`);
    }
  });
  return warnings;
});

const compareSummary = computed(() => {
  if (!previewData.value) {
    return '请核对新旧许可证差异后再继续更新。';
  }
  if (!downgradeWarnings.value.length) {
    return '新许可证不存在降级风险，可直接确认更新。';
  }
  return '检测到部分配额或模块发生变化，请先确认是否符合本次交付预期。';
});

const resetActivationSelection = () => {
  fileList.value = [];
  selectedFile.value = null;
};

const resetCompareSelection = () => {
  compareVisible.value = false;
  previewData.value = null;
  pendingFile.value = null;
  replaceFileList.value = [];
};

/**
 * 查询许可证状态。
 * 页面首屏与“刷新状态”按钮统一调用此方法，避免状态写入逻辑分散。
 */
const loadLicenseStatus = async () => {
  try {
    const response = await fetchLicenseStatus();
    licenseData.value = response.data;
    errorMsg.value = '';
  } catch {
    licenseData.value = null;
  }
};

/**
 * 初次激活场景的文件选择。
 */
const handleFileChange = (items: FileItem[]) => {
  fileList.value = items;
  selectedFile.value = extractFirstFile(items);
  errorMsg.value = '';
};

/**
 * 上传并激活许可证。
 * 上传成功后重置路由层的许可证缓存，并在短暂提示后跳回登录页重新进入管理端。
 */
const handleUpload = async () => {
  if (!selectedFile.value) {
    return;
  }

  uploading.value = true;
  errorMsg.value = '';
  try {
    const response = await uploadLicense(selectedFile.value);
    licenseData.value = response.data;
    Message.success(isLicenseCheckDisabled.value ? '许可证上传成功，已完成开发模式预演' : '许可证上传成功，系统已激活');
    resetLicenseCache();
    resetActivationSelection();

    if (!isLicenseCheckDisabled.value) {
      window.setTimeout(() => {
        window.location.href = '/admin/login';
      }, 1200);
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : '许可证上传失败';
    errorMsg.value = message;
  } finally {
    uploading.value = false;
  }
};

/**
 * 更新许可证时先走预览对比，不直接覆盖当前文件。
 */
const handleReplaceFile = async (items: FileItem[]) => {
  const file = extractFirstFile(items);
  if (!file) {
    replaceFileList.value = [];
    return;
  }

  pendingFile.value = file;
  previewing.value = true;
  try {
    const response = await previewLicense(file);
    previewData.value = response.data;
    compareVisible.value = true;
  } catch (error) {
    const message = error instanceof Error ? error.message : '许可证文件预览失败';
    Message.error(message);
    resetCompareSelection();
  } finally {
    previewing.value = false;
  }
};

/**
 * 确认更新许可证。
 * 后端会先备份旧许可证，再原子替换并刷新运行时状态。
 */
const confirmUpdate = async () => {
  if (!pendingFile.value) {
    return;
  }

  uploading.value = true;
  try {
    const response = await updateLicense(pendingFile.value);
    licenseData.value = response.data;
    resetLicenseCache();
    resetCompareSelection();
    Message.success('许可证更新成功，旧许可证已自动备份');
  } catch (error) {
    const message = error instanceof Error ? error.message : '许可证更新失败';
    Message.error(message);
  } finally {
    uploading.value = false;
  }
};

const closeCompare = () => {
  resetCompareSelection();
};

/**
 * 删除许可证并重置为未激活状态。
 */
const handleReset = async () => {
  resetting.value = true;
  try {
    const response = await resetLicense();
    licenseData.value = response.data;
    resetLicenseCache();
    resetActivationSelection();
    resetCompareSelection();
    Message.success('许可证已删除，系统已重置为未激活状态');
  } catch (error) {
    const message = error instanceof Error ? error.message : '重置失败';
    Message.error(message);
  } finally {
    resetting.value = false;
  }
};

onMounted(() => {
  loadLicenseStatus();
});
</script>

<style scoped lang="less">
.license-page-shell {
  position: relative;
  min-height: 100%;
  padding: 24px;
  overflow: auto;
  background:
    linear-gradient(180deg, #f4f8ff 0%, #f7fafc 46%, #f9fbff 100%),
    radial-gradient(circle at top left, rgba(20, 201, 201, 0.12), transparent 34%),
    radial-gradient(circle at 85% 0%, rgba(22, 93, 255, 0.12), transparent 28%);

  &__glow {
    position: absolute;
    inset: auto;
    width: 320px;
    height: 320px;
    border-radius: 50%;
    filter: blur(120px);
    pointer-events: none;
    opacity: 0.6;

    &--left {
      top: 32px;
      left: -120px;
      background: rgba(20, 201, 201, 0.22);
    }

    &--right {
      top: 180px;
      right: -100px;
      background: rgba(22, 93, 255, 0.18);
    }
  }
}

.panel-surface {
  position: relative;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 32px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.94)),
    radial-gradient(circle at top right, rgba(22, 93, 255, 0.08), transparent 30%);
  box-shadow:
    0 22px 60px rgba(15, 23, 42, 0.08),
    0 4px 18px rgba(15, 23, 42, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(14px);
}

.panel-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 700;
  color: #0f766e;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.license-activation {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(400px, 0.88fr);
  gap: 24px;
}

.activation-story,
.activation-console {
  padding: 36px;
}

.activation-story {
  display: flex;
  flex-direction: column;
  gap: 28px;

  &__title {
    margin: 0;
    font-size: 40px;
    line-height: 1.1;
    font-weight: 900;
    color: #0f172a;
    letter-spacing: -0.04em;
  }

  &__desc {
    margin: 0;
    max-width: 720px;
    font-size: 15px;
    line-height: 1.9;
    color: #64748b;
  }

  &__metrics {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 14px;
  }

  &__timeline,
  &__tips {
    display: grid;
    gap: 14px;
  }
}

.story-metric {
  padding: 18px 20px;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(238, 247, 255, 0.92));
  border: 1px solid rgba(191, 219, 254, 0.6);

  strong {
    display: block;
    font-size: 24px;
    font-weight: 800;
    color: #0f172a;
    letter-spacing: -0.04em;
  }

  span {
    display: block;
    margin-top: 8px;
    font-size: 12px;
    color: #64748b;
  }
}

.story-step,
.story-tip {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.story-step__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(22, 93, 255, 0.14), rgba(20, 201, 201, 0.18));
  color: #165dff;
  font-weight: 800;
  flex-shrink: 0;
}

.story-step__content,
.story-tip > div {
  display: grid;
  gap: 4px;

  strong {
    font-size: 15px;
    color: #0f172a;
  }

  p {
    margin: 0;
    font-size: 13px;
    line-height: 1.8;
    color: #64748b;
  }
}

.story-tip {
  background: rgba(244, 251, 255, 0.85);

  .arco-icon {
    margin-top: 2px;
    font-size: 18px;
    color: #14c9c9;
    flex-shrink: 0;
  }
}

.activation-console {
  display: flex;
  flex-direction: column;
  gap: 24px;

  &__head,
  &__hero,
  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }

  &__hero {
    align-items: flex-start;

    h2 {
      margin: 0 0 8px;
      font-size: 26px;
      font-weight: 800;
      color: #0f172a;
      letter-spacing: -0.03em;
    }

    p {
      margin: 0;
      font-size: 13px;
      line-height: 1.8;
      color: #64748b;
    }
  }
}

.console-badge,
.console-mode-tag,
.hero-status-pill,
.hero-mode-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.console-badge {
  background: rgba(20, 201, 201, 0.12);
  color: #0f766e;
}

.console-mode-tag {
  border: 1px solid rgba(203, 213, 225, 0.92);
  color: #334155;

  &.tone-healthy {
    background: rgba(0, 180, 42, 0.08);
    border-color: rgba(0, 180, 42, 0.22);
    color: #00b42a;
  }

  &.tone-warning {
    background: rgba(255, 125, 0, 0.08);
    border-color: rgba(255, 125, 0, 0.22);
    color: #ff7d00;
  }

  &.tone-expired {
    background: rgba(245, 63, 63, 0.08);
    border-color: rgba(245, 63, 63, 0.2);
    color: #f53f3f;
  }
}

.console-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 68px;
  height: 68px;
  border-radius: 24px;
  background: linear-gradient(135deg, #165dff 0%, #14c9c9 100%);
  color: #fff;
  font-size: 30px;
  box-shadow: 0 18px 36px rgba(22, 93, 255, 0.26);
  flex-shrink: 0;
}

.console-alert {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 18px;
  font-size: 13px;
  font-weight: 600;

  &.tone-healthy {
    background: rgba(0, 180, 42, 0.08);
    color: #00b42a;
  }

  &.tone-warning {
    background: rgba(255, 125, 0, 0.08);
    color: #ff7d00;
  }

  &.tone-expired {
    background: rgba(245, 63, 63, 0.08);
    color: #f53f3f;
  }
}

.upload-dropzone {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 26px 22px;
  border-radius: 28px;
  border: 2px dashed rgba(22, 93, 255, 0.24);
  background:
    linear-gradient(180deg, rgba(248, 251, 255, 0.92), rgba(242, 248, 255, 0.86)),
    radial-gradient(circle at top left, rgba(20, 201, 201, 0.12), transparent 30%);
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(22, 93, 255, 0.42);
    box-shadow: 0 18px 32px rgba(22, 93, 255, 0.12);
  }

  &.is-ready {
    border-color: rgba(20, 201, 201, 0.48);
    background:
      linear-gradient(180deg, rgba(239, 255, 252, 0.98), rgba(244, 251, 255, 0.94)),
      radial-gradient(circle at top left, rgba(20, 201, 201, 0.16), transparent 32%);
  }

  &__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 62px;
    height: 62px;
    border-radius: 22px;
    background: rgba(255, 255, 255, 0.96);
    color: #165dff;
    font-size: 26px;
    box-shadow: 0 10px 24px rgba(22, 93, 255, 0.12);
    flex-shrink: 0;
  }

  &__content {
    display: grid;
    gap: 6px;
    text-align: left;

    strong {
      font-size: 16px;
      color: #0f172a;
    }

    p {
      margin: 0;
      font-size: 12px;
      line-height: 1.8;
      color: #64748b;
    }
  }
}

.console-hint {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: #64748b;
}

.activation-submit-btn {
  height: 48px !important;
  border-radius: 18px !important;
  font-weight: 700 !important;
  background: linear-gradient(135deg, #165dff 0%, #14c9c9 100%) !important;
  border: none !important;
  box-shadow: 0 16px 28px rgba(22, 93, 255, 0.22) !important;
}

.license-dashboard {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.dashboard-hero {
  padding: 26px 28px 28px;
  overflow: hidden;

  &.is-expired {
    background:
      linear-gradient(180deg, rgba(255, 247, 247, 0.98), rgba(255, 241, 241, 0.94)),
      radial-gradient(circle at top right, rgba(245, 63, 63, 0.12), transparent 32%);
  }

  &__bar,
  &__main {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
  }

  &__main {
    margin-top: 22px;
    align-items: stretch;
  }

  &__timeline {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 14px;
    margin-top: 22px;
  }
}

.hero-status-group,
.hero-actions,
.hero-meta-tags {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-status-pill {
  &.tone-healthy {
    background: rgba(0, 180, 42, 0.12);
    color: #00b42a;
  }

  &.tone-warning {
    background: rgba(255, 125, 0, 0.12);
    color: #ff7d00;
  }

  &.tone-expired {
    background: rgba(245, 63, 63, 0.12);
    color: #f53f3f;
  }
}

.hero-mode-pill {
  background: rgba(15, 23, 42, 0.04);
  color: #334155;
}

.hero-action-btn {
  min-width: 116px;
  height: 40px !important;
  border-radius: 999px !important;
  font-weight: 700 !important;
  border-color: rgba(203, 213, 225, 0.96) !important;
  background: rgba(255, 255, 255, 0.88) !important;

  &--primary {
    border: none !important;
    background: linear-gradient(135deg, #165dff 0%, #14c9c9 100%) !important;
    box-shadow: 0 12px 28px rgba(22, 93, 255, 0.2) !important;
  }

  &--danger {
    color: #f53f3f !important;
  }
}

.hero-customer {
  display: flex;
  align-items: flex-start;
  gap: 18px;
  min-width: 0;
  flex: 1;

  &__avatar {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 84px;
    height: 84px;
    border-radius: 28px;
    background:
      linear-gradient(135deg, rgba(22, 93, 255, 0.16), rgba(20, 201, 201, 0.18)),
      #fff;
    color: #165dff;
    font-size: 34px;
    font-weight: 900;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
    flex-shrink: 0;
  }

  &__content {
    min-width: 0;

    h1 {
      margin: 10px 0 8px;
      font-size: 34px;
      line-height: 1.04;
      font-weight: 900;
      color: #0f172a;
      letter-spacing: -0.05em;
    }

    p {
      margin: 0;
      max-width: 720px;
      font-size: 14px;
      line-height: 1.85;
      color: #64748b;
    }
  }
}

.hero-meta-tag {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(203, 213, 225, 0.92);
  font-size: 12px;
  color: #334155;
}

.hero-countdown-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 246px;
  padding: 18px;
  border-radius: 28px;
  background: linear-gradient(160deg, #0f172a 0%, #162033 100%);
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.2);
  flex-shrink: 0;

  &__ring {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 138px;
    height: 138px;
    margin: 0 auto;
    border-radius: 50%;
    padding: 12px;
  }

  &__ring-core {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    border-radius: 50%;
    background: radial-gradient(circle at top, #1e293b 0%, #0f172a 100%);
    color: #fff;

    strong {
      font-size: 34px;
      line-height: 1;
      font-weight: 900;
      letter-spacing: -0.04em;
    }

    span {
      margin-top: 6px;
      font-size: 12px;
      color: rgba(255, 255, 255, 0.56);
    }
  }

  &__meta {
    display: grid;
    gap: 6px;
    margin-top: 18px;
    text-align: center;

    strong {
      font-size: 20px;
      font-weight: 800;
    }

    .tone-healthy {
      color: #14c9c9;
    }

    .tone-warning {
      color: #ffb454;
    }

    .tone-expired {
      color: #ff7875;
    }
  }

  &__label {
    font-size: 12px;
    letter-spacing: 0.16em;
    text-transform: uppercase;
    color: rgba(255, 255, 255, 0.46);
  }
}

.hero-timeline-card {
  padding: 16px 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(226, 232, 240, 0.92);

  span {
    font-size: 11px;
    letter-spacing: 0.14em;
    text-transform: uppercase;
    color: #94a3b8;
  }

  strong {
    display: block;
    margin-top: 10px;
    font-size: 18px;
    font-weight: 800;
    color: #0f172a;
  }

  p {
    margin: 8px 0 0;
    font-size: 12px;
    line-height: 1.8;
    color: #64748b;
  }
}

.dashboard-warning {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 18px;
  font-size: 13px;
  font-weight: 700;

  &.tone-healthy {
    color: #00b42a;
    background: rgba(0, 180, 42, 0.08);
  }

  &.tone-warning {
    color: #ff7d00;
    background: rgba(255, 125, 0, 0.08);
  }

  &.tone-expired {
    color: #f53f3f;
    background: rgba(245, 63, 63, 0.08);
  }
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 18px;
}

.dashboard-panel {
  padding: 24px;

  &--identity,
  &--quota {
    grid-column: span 4;
  }

  &--features,
  &--operations {
    grid-column: span 6;
  }
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;

  h2 {
    margin: 8px 0 0;
    font-size: 24px;
    font-weight: 900;
    color: #0f172a;
    letter-spacing: -0.04em;
  }

  &__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 56px;
    border-radius: 20px;
    font-size: 24px;
    flex-shrink: 0;

    &.tone-blue {
      background: rgba(22, 93, 255, 0.1);
      color: #165dff;
    }

    &.tone-teal {
      background: rgba(20, 201, 201, 0.12);
      color: #14c9c9;
    }

    &.tone-gold {
      background: rgba(255, 125, 0, 0.12);
      color: #ff7d00;
    }

    &.tone-slate {
      background: rgba(15, 23, 42, 0.06);
      color: #334155;
    }
  }
}

.fact-list,
.quota-list {
  display: grid;
  gap: 12px;
}

.fact-row,
.quota-card,
.diagnostic-card,
.feature-empty__helper {
  border-radius: 24px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.82);
}

.fact-row {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr);
  gap: 14px;
  padding: 18px;

  &__label {
    font-size: 12px;
    color: #64748b;
    line-height: 1.8;
  }

  &__value {
    display: grid;
    gap: 4px;

    strong {
      font-size: 16px;
      line-height: 1.6;
      color: #0f172a;
      word-break: break-all;

      &.mono {
        font-family: 'JetBrains Mono', 'Consolas', monospace;
        font-size: 14px;
      }
    }

    small {
      font-size: 12px;
      line-height: 1.8;
      color: #94a3b8;
    }
  }
}

.quota-card {
  padding: 18px;

  &__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 14px;

    strong {
      display: block;
      font-size: 16px;
      color: #0f172a;
    }

    span {
      display: block;
      margin-top: 6px;
      font-size: 12px;
      line-height: 1.8;
      color: #64748b;
    }

    b {
      font-size: 22px;
      color: #165dff;
      line-height: 1;
    }
  }

  &__track {
    position: relative;
    height: 8px;
    overflow: hidden;
    border-radius: 999px;
    background: rgba(226, 232, 240, 0.9);
  }

  &__fill {
    height: 100%;
    border-radius: inherit;
    background: linear-gradient(90deg, #165dff 0%, #14c9c9 100%);
    box-shadow: 0 4px 12px rgba(22, 93, 255, 0.24);
    transition: width 0.6s ease;
  }
}

.feature-cloud {
  display: grid;
  gap: 12px;
}

.feature-chip {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(226, 232, 240, 0.92);

  .arco-icon {
    font-size: 18px;
    flex-shrink: 0;
  }

  strong {
    display: block;
    font-size: 15px;
    color: #0f172a;
  }

  span {
    display: block;
    margin-top: 4px;
    font-size: 12px;
    color: #64748b;
  }

  &.tone-blue {
    .arco-icon {
      color: #165dff;
    }
  }

  &.tone-teal {
    .arco-icon {
      color: #14c9c9;
    }
  }

  &.tone-gold {
    .arco-icon {
      color: #ff7d00;
    }
  }

  &.tone-cyan {
    .arco-icon {
      color: #0ea5e9;
    }
  }
}

.feature-empty {
  padding: 8px 0 0;

  &__symbol {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 78px;
    height: 78px;
    border-radius: 28px;
    background: linear-gradient(135deg, rgba(22, 93, 255, 0.12), rgba(20, 201, 201, 0.18));
    color: #165dff;
    font-size: 30px;
  }

  &__helper {
    margin-top: 16px;
    padding: 18px;

    strong {
      display: block;
      font-size: 15px;
      color: #0f172a;
    }

    p {
      margin: 8px 0 0;
      font-size: 12px;
      line-height: 1.9;
      color: #64748b;
    }
  }
}

.diagnostic-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.diagnostic-card {
  padding: 18px;

  span {
    font-size: 11px;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    color: #94a3b8;
  }

  strong {
    display: block;
    margin-top: 10px;
    font-size: 22px;
    line-height: 1.15;
    font-weight: 900;
    letter-spacing: -0.04em;
  }

  p {
    margin: 8px 0 0;
    font-size: 12px;
    line-height: 1.8;
    color: #64748b;
  }

  &.tone-healthy strong {
    color: #14c9c9;
  }

  &.tone-warning strong {
    color: #ff7d00;
  }

  &.tone-expired strong {
    color: #f53f3f;
  }

  &.tone-slate strong {
    color: #334155;
  }
}

.tips-board {
  display: grid;
  gap: 12px;
  margin-top: 18px;

  &__item {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 16px 18px;
    border-radius: 20px;
    background: rgba(244, 251, 255, 0.84);
    border: 1px solid rgba(226, 232, 240, 0.92);

    .arco-icon {
      margin-top: 2px;
      color: #14c9c9;
      font-size: 18px;
      flex-shrink: 0;
    }

    strong {
      display: block;
      font-size: 14px;
      color: #0f172a;
    }

    p {
      margin: 4px 0 0;
      font-size: 12px;
      line-height: 1.8;
      color: #64748b;
    }
  }
}

.dashboard-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.footer-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.6;
  word-break: break-all;

  .arco-icon {
    flex-shrink: 0;
  }

  &.tone-blue {
    background: rgba(22, 93, 255, 0.08);
    color: #165dff;
  }

  &.tone-teal {
    background: rgba(20, 201, 201, 0.12);
    color: #0f766e;
  }

  &.tone-gold {
    background: rgba(255, 125, 0, 0.08);
    color: #ff7d00;
  }

  &.tone-slate {
    background: rgba(15, 23, 42, 0.05);
    color: #475569;
    font-family: 'JetBrains Mono', 'Consolas', monospace;
  }
}

.compare-shell {
  padding: 24px;

  &__header,
  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }

  &__title {
    display: flex;
    align-items: flex-start;
    gap: 14px;

    h3 {
      margin: 8px 0 6px;
      font-size: 24px;
      font-weight: 900;
      color: #0f172a;
      letter-spacing: -0.04em;
    }

    p {
      margin: 0;
      font-size: 13px;
      line-height: 1.8;
      color: #64748b;
    }
  }

  &__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 56px;
    border-radius: 20px;
    background: linear-gradient(135deg, #165dff 0%, #14c9c9 100%);
    color: #fff;
    font-size: 22px;
    flex-shrink: 0;
  }

  &__close {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border: none;
    border-radius: 999px;
    background: rgba(15, 23, 42, 0.05);
    color: #64748b;
    cursor: pointer;
    transition: background 0.2s ease, color 0.2s ease;

    &:hover {
      background: rgba(15, 23, 42, 0.1);
      color: #0f172a;
    }
  }

  &__warning {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-top: 18px;
    padding: 14px 16px;
    border-radius: 18px;
    background: rgba(255, 125, 0, 0.08);
    color: #ff7d00;
    font-size: 13px;
    font-weight: 700;
  }

  &__footer {
    margin-top: 20px;
    align-items: flex-end;
  }

  &__footer-tip {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    max-width: 520px;
    font-size: 12px;
    line-height: 1.8;
    color: #64748b;
  }
}

.compare-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin-top: 18px;
}

.compare-card {
  border-radius: 28px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  overflow: hidden;

  &--current {
    background: linear-gradient(180deg, rgba(245, 249, 255, 0.96), rgba(255, 255, 255, 0.96));
  }

  &--next {
    background: linear-gradient(180deg, rgba(240, 255, 250, 0.96), rgba(255, 255, 255, 0.96));
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 18px 20px;
    border-bottom: 1px solid rgba(226, 232, 240, 0.92);

    span {
      font-size: 18px;
      font-weight: 800;
      color: #0f172a;
    }

    small {
      font-size: 12px;
      color: #64748b;
      letter-spacing: 0.14em;
      text-transform: uppercase;
    }
  }

  &__body {
    display: grid;
    gap: 18px;
    padding: 20px;
  }
}

.compare-block {
  display: grid;
  gap: 8px;

  &__title {
    font-size: 11px;
    letter-spacing: 0.14em;
    text-transform: uppercase;
    color: #94a3b8;
    font-weight: 700;
  }
}

.compare-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(226, 232, 240, 0.9);

  &.is-different {
    border-color: rgba(22, 93, 255, 0.26);
    box-shadow: inset 0 0 0 1px rgba(22, 93, 255, 0.06);
  }

  &__label {
    font-size: 12px;
    color: #64748b;
  }

  strong {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    font-weight: 800;
    color: #0f172a;
    text-align: right;
  }

  &__next {
    color: #165dff;

    &.up {
      color: #00b42a;
    }

    &.down {
      color: #f53f3f;
    }
  }
}

.compare-feature-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.compare-feature-tag {
  border-radius: 999px !important;
  padding-inline: 10px !important;
  min-height: 30px;
  font-size: 12px !important;
}

.compare-feature-empty {
  font-size: 12px;
  color: #94a3b8;
}

.compare-shell__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 1440px) {
  .license-activation {
    grid-template-columns: 1fr;
  }

  .dashboard-panel--identity,
  .dashboard-panel--quota,
  .dashboard-panel--features,
  .dashboard-panel--operations {
    grid-column: span 6;
  }
}

@media (max-width: 1100px) {
  .dashboard-hero__main {
    flex-direction: column;
  }

  .hero-countdown-card {
    width: 100%;
  }

  .dashboard-hero__timeline,
  .compare-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .license-page-shell {
    padding: 16px;
  }

  .activation-story,
  .activation-console,
  .dashboard-panel,
  .dashboard-hero,
  .compare-shell {
    padding: 20px;
  }

  .activation-story__metrics,
  .diagnostic-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-panel--identity,
  .dashboard-panel--quota,
  .dashboard-panel--features,
  .dashboard-panel--operations {
    grid-column: auto;
  }

  .fact-row {
    grid-template-columns: 1fr;
  }

  .compare-shell__header,
  .compare-shell__footer,
  .activation-console__head,
  .activation-console__hero,
  .activation-console__footer,
  .dashboard-hero__bar {
    flex-direction: column;
    align-items: stretch;
  }

  .compare-shell__footer {
    align-items: stretch;
  }

  .compare-shell__actions,
  .hero-actions {
    flex-wrap: wrap;
  }

  .compare-shell__footer-tip {
    max-width: none;
  }

  .hero-customer {
    flex-direction: column;
  }
}
</style>

<style lang="less">
.license-compare-modal__wrap {
  .arco-modal-body {
    padding: 0;
  }
}
</style>
