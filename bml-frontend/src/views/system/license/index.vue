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

    <!-- ==================== 已激活：单屏授权仪表盘（无需滚动） ==================== -->
    <section v-else class="lv-dashboard">

      <!-- ── 顶部英雄条：左侧主信息区 + 右侧通栏倒计时卡（撑满整个 hero 高度） ── -->
      <div class="lv-hero" :class="{ 'lv-hero--expired': isExpired }">
        <!-- 左：主信息区（客户信息撑顶 + 状态胶囊置底） -->
        <div class="lv-hero__main">
          <!-- 顶部：客户信息（左）+ 操作按钮（右），横向两端对齐 -->
          <div class="lv-hero__bar">
            <div class="lv-customer">
              <div class="lv-avatar">{{ customerMonogram }}</div>
              <div class="lv-customer__info">
                <h1>{{ licenseData?.customerName || '未命名客户' }}</h1>
                <p>{{ heroStatusDescription }}</p>
                <div class="lv-tags">
                  <span v-for="t in heroMetaTags" :key="t" class="lv-tag">{{ t }}</span>
                </div>
              </div>
            </div>
            <div class="lv-actions">
              <a-upload :auto-upload="false" :limit="1" :show-file-list="false" :file-list="replaceFileList" accept=".lic" @change="handleReplaceFile">
                <template #upload-button>
                  <a-button type="primary" size="small" :loading="previewing" class="lv-act lv-act--primary">
                    <template #icon><icon-upload /></template>更新许可证
                  </a-button>
                </template>
              </a-upload>
              <a-button size="small" class="lv-act" @click="loadLicenseStatus">
                <template #icon><icon-refresh /></template>刷新状态
              </a-button>
              <a-button size="small" status="danger" class="lv-act lv-act--danger" @click="resetModalVisible = true">
                <template #icon><icon-delete /></template>重置
              </a-button>
            </div>
          </div>
          <!-- 底部：状态胶囊（挪到客户信息下方） -->
          <div class="lv-pills">
            <span class="lv-pill" :class="`lv-pill--${healthTone}`">
              <icon-check-circle v-if="!isExpired" /><icon-exclamation-circle v-else />
              {{ heroStatusLabel }}
            </span>
            <span class="lv-pill lv-pill--muted">{{ heroModeLabel }}</span>
          </div>
        </div>

        <!-- 右：倒计时卡片（通栏，高度撑满整个 hero，环内已含天数无需副标题） -->
        <div class="lv-ring-card">
          <span class="lv-ring-card__title">剩余有效期</span>
          <div class="lv-ring" :style="daysRingStyle">
            <div class="lv-ring__core">
              <strong>{{ daysDisplayValue }}</strong>
              <span>天</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 警告横幅 -->
      <div v-if="licenseData?.errorMessage" class="lv-alert" :class="`lv-alert--${healthTone}`">
        <icon-exclamation-circle-fill /><span>{{ licenseData.errorMessage }}</span>
      </div>

      <!-- ── 三列信息面板（flex: 1 撑满剩余空间） ── -->
      <div class="lv-grid">

        <!-- ── 左列：授权档案 + 生命周期 ── -->
        <div class="lv-card">
          <div class="lv-card__accent lv-card__accent--blue"></div>
          <div class="lv-card__head">
            <div><span class="lv-eyebrow">Identity</span><h2>授权档案</h2></div>
            <div class="lv-card__ico lv-ico--blue"><icon-safe /></div>
          </div>
          <div class="lv-card__scroll">
            <div class="lv-facts">
              <div v-for="f in overviewFacts" :key="f.label" class="lv-fact">
                <span>{{ f.label }}</span>
                <strong :class="{ mono: f.mono }">{{ f.value }}</strong>
              </div>
            </div>
            <!-- 生命周期（从 hero strip 移入） -->
            <div class="lv-sep"></div>
            <span class="lv-eyebrow">Lifecycle</span>
            <div class="lv-lc-list">
              <div v-for="c in lifecycleCards" :key="c.label" class="lv-lc-row">
                <span>{{ c.label }}</span>
                <strong>{{ c.value }}</strong>
              </div>
            </div>
          </div>
        </div>

        <!-- ── 中列：配额上限（进度条 + 已使用/授权/剩余三维数据） ── -->
        <div class="lv-card">
          <div class="lv-card__accent lv-card__accent--teal"></div>
          <div class="lv-card__head">
            <div><span class="lv-eyebrow">Quota</span><h2>配额上限</h2></div>
            <div class="lv-card__ico lv-ico--teal"><icon-thunderbolt /></div>
          </div>
          <div class="lv-card__scroll">
            <div class="lv-quota-list">
              <div v-for="m in quotaMetrics" :key="m.label" class="lv-quota-card">
                <!-- 标题行：配额名称 + 百分比 -->
                <div class="lv-quota-card__title">
                  <span>{{ m.label }}</span>
                  <strong :class="`lv-quota-card__pct--${m.tone}`">{{ m.max === 0 ? '不限' : `${m.percent}%` }}</strong>
                </div>
                <!-- 进度条 -->
                <div class="lv-quota-card__track">
                  <div
                    class="lv-quota-card__fill"
                    :class="`lv-quota-card__fill--${m.tone}`"
                    :style="{ width: `${m.max === 0 ? 100 : m.percent}%` }"
                  ></div>
                </div>
                <!-- 带启用/停用明细的统计区域 -->
                <div v-if="m.hasBreakdown" class="lv-quota-card__stats lv-quota-card__stats--4col">
                  <div class="lv-quota-stat">
                    <strong>{{ m.currentDisplay }}</strong>
                    <span>总数</span>
                  </div>
                  <div class="lv-quota-stat lv-quota-stat--primary">
                    <strong>{{ m.activeDisplay }}</strong>
                    <span>启用</span>
                  </div>
                  <div class="lv-quota-stat lv-quota-stat--warning">
                    <strong>{{ m.disabledDisplay }}</strong>
                    <span>停用</span>
                  </div>
                  <div class="lv-quota-stat lv-quota-stat--success">
                    <strong>{{ m.remainingDisplay }}</strong>
                    <span>剩余</span>
                  </div>
                </div>
                <!-- 无明细的简约统计 -->
                <div v-else class="lv-quota-card__stats">
                  <div class="lv-quota-stat">
                    <strong>{{ m.currentDisplay }}</strong>
                    <span>已使用</span>
                  </div>
                  <div class="lv-quota-stat lv-quota-stat--primary">
                    <strong>{{ m.maxDisplay }}</strong>
                    <span>授权上限</span>
                  </div>
                  <div class="lv-quota-stat lv-quota-stat--success">
                    <strong>{{ m.remainingDisplay }}</strong>
                    <span>剩余</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ── 右列：授权模块（列表式展示） ── -->
        <div class="lv-card">
          <div class="lv-card__accent lv-card__accent--gold"></div>
          <div class="lv-card__head">
            <div><span class="lv-eyebrow">Features</span><h2>授权模块</h2></div>
            <div class="lv-card__ico lv-ico--gold"><icon-apps /></div>
          </div>
          <div class="lv-card__scroll">
            <div v-if="featureTags.length" class="lv-feature-list">
              <div v-for="ft in featureTags" :key="ft.key" class="lv-feature-item" :class="`lv-feature-item--${ft.tone}`">
                <icon-check-circle-fill class="lv-feature-item__ico" />
                <span>{{ ft.label }}</span>
              </div>
            </div>
            <div v-else class="lv-empty-state">
              <div class="lv-empty-state__icon"><icon-info-circle /></div>
              <span>暂无授权模块</span>
              <p>当前许可证未配置功能模块授权</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部信息条 -->
      <div class="lv-footer">
        <span v-for="b in footerBadges" :key="b.value" class="lv-badge" :class="`lv-badge--${b.tone}`">
          <component :is="b.icon" />{{ b.value }}
        </span>
      </div>
    </section>

    <!-- ── 许可证更新对比弹窗（BmlModal：支持拖拽、缩放、全屏） ── -->
    <BmlModal
      v-model:visible="compareVisible"
      title="授权更新确认"
      :width="1080"
      :height="680"
      :min-width="700"
      :min-height="480"
    >
      <section class="compare-shell">

        <!-- 降级警告提示横幅 -->
        <div v-if="downgradeWarnings.length" class="compare-shell__warning">
          <icon-exclamation-circle-fill />
          <span>{{ downgradeWarnings.join('；') }}</span>
        </div>

        <!-- 对比表格：左右并排，紧凑行距避免溢出 -->
        <div class="compare-grid">
          <!-- 左栏：当前许可证 -->
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

          <!-- 右栏：新许可证 -->
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

      </section>
      <template #footer>
        <div class="compare-shell__footer-tip">
          <icon-lock />
          <span>系统将先备份旧许可证，再覆盖为新许可证并即时刷新内存状态。</span>
        </div>
        <div class="compare-shell__actions">
          <a-button size="large" @click="closeCompare">取消</a-button>
          <a-button
            type="primary"
            size="large"
            :loading="uploading"
            class="compare-confirm-btn"
            @click="confirmUpdate"
          >
            <template #icon><icon-check-circle /></template>
            确认更新许可证
          </a-button>
        </div>
      </template>
    </BmlModal>

    <!-- ==================== 危险操作：重置许可证确认弹窗 ==================== -->
    <BmlModal
      v-model:visible="resetModalVisible"
      title="系统高危操作确认"
      :width="500"
      :height="380"
      :min-width="400"
      :min-height="300"
    >
      <div class="lv-danger-modal__content">
        <div class="lv-danger-modal__icon">
          <icon-exclamation-circle-fill />
        </div>
        <div class="lv-danger-modal__desc">
          <p>您即将<strong>彻底删除</strong>当前系统中的授权许可证，并将整个 BML 系统重置为未激活的阻断状态。</p>
          <p>执行此操作后，所有前台业务系统将立即停止服务，直到您上传新的有效许可证为止。</p>
        </div>
      </div>
      <template #footer>
        <a-button
          size="large"
          shape="round"
          @click="resetModalVisible = false"
          :disabled="resetting"
        >
          我再想想
        </a-button>
        <a-button
          status="danger"
          type="primary"
          size="large"
          shape="round"
          :loading="resetting"
          @click="handleReset"
        >
          <template #icon><icon-delete /></template>
          确认强制重置
        </a-button>
      </template>
    </BmlModal>
  </div>
</template>

<script setup lang="ts">
/**
 * 许可证管理页面
 *
 * 重要说明：
 *   defineOptions({ name: 'LicenseManagement' }) 是 keep-alive 缓存的关键。
 *   组件 name 必须与路由配置中的 name 字段保持一致，
 *   否则 <keep-alive :include="cachedViews"> 无法匹配到该组件，
 *   导致切换标签页后页面内容被销毁、重新加载。
 */
defineOptions({ name: 'LicenseManagement' });

import { computed, h, onMounted, ref } from 'vue';
import { Message, Notification } from '@arco-design/web-vue';
import BmlModal from '../../../components/BmlModal.vue';
import type { FileItem } from '@arco-design/web-vue';
import {
  IconApps,
  IconArrowDown,
  IconArrowUp,
  IconCheckCircle,
  IconCheckCircleFill,
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
 * 构建许可证变更通知内容的富文本 VNode。
 *
 * 将纯文本变更条目渲染为带图标、色彩标记的结构化列表，
 * 每条变更独占一行，升级/降级/功能新增/移除/过期分别用不同图标区分。
 *
 * @param lines  变更条目文本数组（支持 ↑ ↓ ✚ ✖ ⚠ ⛔ 📅 前缀自动识别）
 * @returns      渲染后的 VNode
 */
function renderChangeItems(lines: string[]) {
  return h('div', { class: 'bml-notify-items' }, lines.map(line => {
    // 根据条目前缀自动选择图标和颜色类名
    let iconClass = 'item-info';
    if (line.startsWith('↑')) iconClass = 'item-up';
    else if (line.startsWith('↓') || line.startsWith('⚠')) iconClass = 'item-down';
    else if (line.startsWith('✚')) iconClass = 'item-add';
    else if (line.startsWith('✖')) iconClass = 'item-remove';
    else if (line.startsWith('⛔')) iconClass = 'item-expired';
    else if (line.startsWith('📅')) iconClass = 'item-date';

    return h('div', { class: `bml-notify-item ${iconClass}` }, [
      h('span', { class: 'item-dot' }),
      h('span', { class: 'item-text' }, line),
    ]);
  }));
}

/**
 * 构建首次激活通知内容的富文本 VNode。
 *
 * @param lines  授权概要条目数组
 * @returns      渲染后的 VNode
 */
function renderSummaryItems(lines: string[]) {
  return h('div', { class: 'bml-notify-summary' }, lines.map(line =>
    h('div', { class: 'bml-notify-summary-row' }, [
      h('span', { class: 'summary-dot' }),
      h('span', { class: 'summary-text' }, line),
    ])
  ));
}

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

/**
 * 配额指标数据模型。
 * 包含授权上限、当前使用量、剩余可新增量、进度百分比，
 * 供前端配额面板展示进度条和统计数字。
 */
interface LicenseQuotaMetric {
  /** 配额名称 */
  label: string;
  /** 授权上限原始值（0 = 不限） */
  max: number;
  /** 当前已创建总数（含所有状态） */
  current: number;
  /** 当前启用（活跃）数量 */
  active: number;
  /** 当前停用数量 */
  disabled: number;
  /** 剩余可新增数量（-1 = 不限） */
  remaining: number;
  /** 使用百分比（0-100，基于启用数 vs 授权上限） */
  percent: number;
  /** 授权上限显示文本 */
  maxDisplay: string;
  /** 当前总数显示文本 */
  currentDisplay: string;
  /** 启用数量显示文本 */
  activeDisplay: string;
  /** 停用数量显示文本 */
  disabledDisplay: string;
  /** 剩余量显示文本 */
  remainingDisplay: string;
  /** 进度色调 */
  tone: 'healthy' | 'warning' | 'danger';
  /** 提示信息 */
  hint: string;
  /** 是否有启用/停用明细（maxUsersPerAccount 无明细） */
  hasBreakdown: boolean;
}

/* DiagnosticCard 接口已移除 —— 系统诊断面板已删除 */

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
/* QUOTA_VISUAL_MIN_PERCENT 已移除 —— 配额面板改用真实使用百分比 */

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
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value.trim());
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

/* daysLeftText 已移除 —— 倒计时环内已直接显示天数，无需额外副标题 */

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

/**
 * 配额指标计算属性。
 * 根据后端返回的授权上限与当前使用量，计算剩余量、使用百分比和进度色调，
 * 供配额面板渲染进度条和统计数字。
 */
const quotaMetrics = computed<LicenseQuotaMetric[]>(() => {
  const items: Array<{
    label: string;
    max: number;
    current: number;
    active: number;
    hint: string;
    hasBreakdown: boolean;
  }> = [
    {
      label: '业务用户上限',
      max: licenseData.value?.maxTotalUsers ?? 0,
      current: licenseData.value?.currentTotalUsers ?? 0,
      active: licenseData.value?.activeTotalUsers ?? 0,
      hint: '当前前台业务系统使用用户总量上限',
      hasBreakdown: true
    },
    {
      label: '最大 API 账号数',
      max: licenseData.value?.maxApiAccounts ?? 0,
      current: licenseData.value?.currentApiAccounts ?? 0,
      active: licenseData.value?.activeApiAccounts ?? 0,
      hint: '控制可创建并启用的 API 账号总量',
      hasBreakdown: true
    },
    {
      label: '允许 API 账号调用新增的用户数',
      max: licenseData.value?.maxUsersPerAccount ?? 0,
      current: licenseData.value?.currentApiUsers ?? 0,
      active: licenseData.value?.activeApiUsers ?? 0,
      hint: '所有 API 账号通过接口可创建的累积活跃用户总额度',
      hasBreakdown: true
    }
  ];

  return items.map(item => {
    const isUnlimited = item.max === 0;
    const disabled = item.current - item.active;
    // 剩余量基于「启用数」计算：还能再启用/新增多少
    const remaining = isUnlimited ? -1 : Math.max(0, item.max - item.active);
    // 百分比也基于「启用数」，因为配额控制的是活跃资源
    const percent = isUnlimited ? 0 : (item.max > 0 ? Math.min(100, Math.round((item.active / item.max) * 100)) : 0);

    let tone: LicenseQuotaMetric['tone'] = 'healthy';
    if (!isUnlimited && percent >= 90) {
      tone = 'danger';
    } else if (!isUnlimited && percent >= 70) {
      tone = 'warning';
    }

    return {
      label: item.label,
      max: item.max,
      current: item.current,
      active: item.active,
      disabled,
      remaining,
      percent,
      maxDisplay: formatQuota(item.max),
      currentDisplay: String(item.current),
      activeDisplay: String(item.active),
      disabledDisplay: String(disabled),
      remainingDisplay: isUnlimited ? '不限' : String(remaining),
      tone,
      hint: item.hint,
      hasBreakdown: item.hasBreakdown
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

/* diagnosticItems 已移除 —— 系统诊断面板已从 UI 中删除 */

/* supportTips 已移除 —— 交付提示区域已合并至诊断面板 */

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
  /* 排列顺序与配额面板（quotaMetrics）保持一致：业务用户上限 → 最大 API 账号数 → 允许 API 账号调用新增的用户数 */
  const items = [
    {
      key: 'maxTotalUsers',
      label: '业务用户上限',
      currentRaw: licenseData.value?.maxTotalUsers,
      nextRaw: previewData.value?.maxTotalUsers
    },
    {
      key: 'maxApiAccounts',
      label: '最大 API 账号数',
      currentRaw: licenseData.value?.maxApiAccounts,
      nextRaw: previewData.value?.maxApiAccounts
    },
    {
      key: 'maxUsersPerAccount',
      label: '允许 API 账号调用新增的用户数',
      currentRaw: licenseData.value?.maxUsersPerAccount,
      nextRaw: previewData.value?.maxUsersPerAccount
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
    resetLicenseCache();
    resetActivationSelection();

    // ── 右上角弹出激活成功通知（包含授权概要信息） ──
    const data = response.data;
    const summaryLines: string[] = [];

    if (data?.customerName) {
      summaryLines.push(`客户：${data.customerName}`);
    }
    if (data?.maxApiAccounts !== undefined && data?.maxApiAccounts !== null) {
      summaryLines.push(`API 账号上限：${data.maxApiAccounts === 0 ? '不限' : data.maxApiAccounts}`);
    }
    if (data?.maxTotalUsers !== undefined && data?.maxTotalUsers !== null) {
      summaryLines.push(`业务用户上限：${data.maxTotalUsers === 0 ? '不限' : data.maxTotalUsers}`);
    }
    if (data?.expireDate) {
      summaryLines.push(`有效期至：${data.expireDate}`);
    }
    if (data?.features && data.features.length > 0) {
      summaryLines.push(`授权模块：${data.features.map(formatFeatureLabel).join('、')}`);
    }

    Notification.success({
      id: 'license-activate-notify',
      title: isLicenseCheckDisabled.value
        ? '许可证上传成功（开发模式预演）'
        : '系统授权激活成功',
      content: () => summaryLines.length > 0
        ? renderSummaryItems(summaryLines)
        : h('span', { style: 'color: #64748b; font-size: 13px;' }, '许可证已成功加载'),
      position: 'topRight',
      duration: isLicenseCheckDisabled.value ? 8000 : 3000,
      closable: true,
      class: 'bml-license-notify',
    });

    Message.success(isLicenseCheckDisabled.value ? '许可证上传成功，已完成开发模式预演' : '许可证上传成功，系统已激活');

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
 * <p>
 * 后端会先备份旧许可证，再原子替换并刷新运行时状态。
 * 更新成功后，根据新旧许可证的全量对比结果，在右上角弹出通知并写入告警中心。
 * </p>
 *
 * 通知维度（所有变更均会提示，不只是降级）：
 *   1. 配额降级 + 资源被自动冻结（业务用户 / API 账号 / API 来源用户）
 *   2. 配额升级（业务用户上限 / API 账号上限 / API 来源用户上限提升）
 *   3. 到期日变更（延期或缩短）
 *   4. 功能模块新增
 *   5. 功能模块移除
 *   6. 许可证已过期
 */
const confirmUpdate = async () => {
  if (!pendingFile.value) {
    return;
  }

  uploading.value = true;
  try {
    /**
     * ══ 关键：先快照对比数据，再覆盖 licenseData ══
     *
     * compareQuotaRows / compareBasicRows / addedFeatures / removedFeatures
     * 均为 computed，内部比较 licenseData（当前）vs previewData（新）。
     * 如果先执行 `licenseData.value = response.data`，计算属性会立即重算，
     * 变成"新 vs 新"，所有 changed 都为 false，导致右上角通知永远不弹出。
     *
     * 因此必须在覆盖 licenseData 之前，先将对比结果快照到局部变量。
     */
    const snapshotQuotaRows = compareQuotaRows.value.map(r => ({ ...r }));
    const snapshotBasicRows = compareBasicRows.value.map(r => ({ ...r }));
    const snapshotAddedFeatures = [...addedFeatures.value];
    const snapshotRemovedFeatures = [...removedFeatures.value];

    const response = await updateLicense(pendingFile.value);

    // 现在安全地更新当前许可证数据
    licenseData.value = response.data;
    resetLicenseCache();

    const data = response.data;

    // ── 收集所有变更条目（使用快照数据，不再依赖 computed） ──
    const changeLines: string[] = [];

    // 1. 配额降级 + 资源被自动冻结（后端执行结果，优先展示）
    if (data?.frozenUserCount && data.frozenUserCount > 0) {
      changeLines.push(`⚠ 业务用户上限降级：已自动停用 ${data.frozenUserCount} 个最近创建的用户`);
    }
    if (data?.frozenApiAccountCount && data.frozenApiAccountCount > 0) {
      changeLines.push(`⚠ API 账号上限降级：已自动停用 ${data.frozenApiAccountCount} 个最近创建的 API 账号`);
    }
    if (data?.frozenApiUserCount && data.frozenApiUserCount > 0) {
      changeLines.push(`⚠ API 来源用户上限降级：已自动停用 ${data.frozenApiUserCount} 个最近由 API 创建的用户`);
    }

    // 2. 配额变更（升级 / 降级，基于快照数据）
    snapshotQuotaRows.forEach(row => {
      if (row.changed) {
        const arrow = row.direction === 'up' ? '↑' : row.direction === 'down' ? '↓' : '→';
        const tag = row.direction === 'up' ? '升级' : row.direction === 'down' ? '降级' : '变更';
        changeLines.push(`${arrow} ${row.label}${tag}：${row.current} → ${row.next}`);
      }
    });

    // 3. 到期日变更
    const expireRow = snapshotBasicRows.find(r => r.key === 'expireDate');
    if (expireRow?.changed) {
      changeLines.push(`📅 有效期变更：${expireRow.current} → ${expireRow.next}`);
    }

    // 4. 功能模块新增
    if (snapshotAddedFeatures.length > 0) {
      changeLines.push(`✚ 新增授权模块：${snapshotAddedFeatures.map(formatFeatureLabel).join('、')}`);
    }

    // 5. 功能模块移除
    if (snapshotRemovedFeatures.length > 0) {
      changeLines.push(`✖ 移除授权模块：${snapshotRemovedFeatures.map(formatFeatureLabel).join('、')}`);
    }

    // 6. 许可证已过期
    if (data?.expired) {
      changeLines.push('⛔ 注意：新许可证已过期，系统功能将受限');
    }

    // ── 弹出右上角通知（富文本渲染） ──
    if (changeLines.length > 0) {
      /**
       * 有任何变更时，在右上角弹出 Notification。
       * 包含降级时用 warning 类型（橙色），纯升级时用 info 类型（蓝色）。
       * 判断依据：是否有资源被冻结或模块被移除。
       * 使用 renderChangeItems() 将变更条目渲染为带图标的结构化列表。
       */
      const hasDowngrade =
        (data?.frozenUserCount ?? 0) > 0 ||
        (data?.frozenApiAccountCount ?? 0) > 0 ||
        (data?.frozenApiUserCount ?? 0) > 0 ||
        snapshotRemovedFeatures.length > 0 ||
        data?.expired;

      const notifyType = hasDowngrade ? 'warning' : 'info';

      Notification[notifyType]({
        id: 'license-change-notify',
        title: `许可证已更新（${changeLines.length} 项变更）`,
        content: () => renderChangeItems(changeLines),
        position: 'topRight',
        duration: 12000,
        closable: true,
        class: 'bml-license-notify',
      });
    } else {
      // 即使没有检测到具体变更项，也弹出成功通知（保底）
      const fallbackLines = [
        `客户：${data?.customerName || '-'}`,
        `有效期至：${data?.expireDate || '-'}`,
      ];
      Notification.success({
        id: 'license-change-notify',
        title: '许可证已更新',
        content: () => renderSummaryItems(fallbackLines),
        position: 'topRight',
        duration: 6000,
        closable: true,
        class: 'bml-license-notify',
      });
    }

    // 清空对比面板数据
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

const resetModalVisible = ref(false);

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
    resetModalVisible.value = false;
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
  height: 100%;
  padding: 14px;
  overflow: hidden; /* 页面外壳不出滚动条，内部 dashboard/activation 自行处理 */
  display: flex;
  flex-direction: column;
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
  /* 激活场景允许自身滚动（当内容超过视口时），保持页面外壳无横条 */
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
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
.console-mode-tag {
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

/* 旧仪表盘样式已被 lv-* 样式取代 — 以下为清理后保留的占位注释 */
/* [已清理] .license-dashboard → .lv-dashboard */
/* [已清理] .dashboard-hero → .lv-hero */
/* [已清理] .hero-* → .lv-pill, .lv-act, .lv-customer, .lv-ring-card */
/* [已清理] .hero-timeline-card → .lv-strip-cell */

/* [已清理] .hero-timeline-card, .dashboard-warning, .dashboard-grid, .dashboard-panel */
/* [已清理] .panel-header, .fact-list, .quota-list, .fact-row */

/* [已清理] .quota-card, .feature-cloud, .feature-chip, .feature-empty */
/* [已清理] .diagnostic-grid, .diagnostic-card, .tips-board */
/* [已清理] .dashboard-footer, .footer-badge */

/* ==================== 单屏仪表盘样式 ==================== */
.lv-dashboard {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.lv-hero {
  position: relative;
  /* 横向布局：左主信息区 + 右倒计时卡 */
  display: flex;
  align-items: stretch;
  gap: 12px;
  padding: 8px 10px;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(248, 251, 255, 0.95)),
    radial-gradient(circle at top right, rgba(22, 93, 255, 0.08), transparent 30%);
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.06), 0 2px 8px rgba(15, 23, 42, 0.03);
  backdrop-filter: blur(12px);
  overflow: hidden;

  &--expired {
    background:
      linear-gradient(180deg, rgba(255, 248, 248, 0.98), rgba(255, 243, 243, 0.95)),
      radial-gradient(circle at top right, rgba(245, 63, 63, 0.1), transparent 30%);
  }

  /* 左侧主信息区：客户信息靠左、上下居中 */
  &__main {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 6px;
    padding: 0 4px 0 6px;
  }

  /* 顶部行：客户信息（左）+ 操作按钮（右顶部对齐） */
  &__bar {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }
}

.lv-pills {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.lv-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;

  &--healthy { background: rgba(0, 180, 42, 0.1); color: #00b42a; }
  &--warning { background: rgba(255, 125, 0, 0.1); color: #ff7d00; }
  &--expired { background: rgba(245, 63, 63, 0.1); color: #f53f3f; }
  &--muted { background: rgba(15, 23, 42, 0.04); color: #475569; }
}

.lv-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.lv-act {
  border-radius: 999px !important;
  font-weight: 600 !important;

  &--primary {
    border: none !important;
    background: linear-gradient(135deg, #165dff 0%, #14c9c9 100%) !important;
    box-shadow: 0 6px 16px rgba(22, 93, 255, 0.18) !important;
  }

  &--danger {
    color: #f53f3f !important;
  }
}

.lv-customer {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  flex: 1;

  &__info {
    min-width: 0;

    h1 {
      margin: 0 0 2px;
      font-size: 20px;
      font-weight: 900;
      color: #0f172a;
      letter-spacing: -0.03em;
      line-height: 1.2;
    }

    p {
      margin: 0;
      font-size: 12px;
      line-height: 1.5;
      color: #64748b;
      max-width: 480px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}

.lv-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background:
    linear-gradient(135deg, rgba(22, 93, 255, 0.14), rgba(20, 201, 201, 0.16)),
    #fff;
  color: #165dff;
  font-size: 20px;
  font-weight: 900;
  flex-shrink: 0;
}

.lv-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  font-weight: 700;
  color: #0f766e;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.lv-tags {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.lv-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(203, 213, 225, 0.8);
  font-size: 11px;
  color: #334155;
}

/* 倒计时卡：纵向通栏，高度撑满 hero，紧凑尺寸 */
.lv-ring-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  align-self: stretch;
  min-width: 130px;
  padding: 8px 14px;
  border-radius: 14px;
  background:
    linear-gradient(160deg, #0f172a 0%, #1a2438 100%),
    radial-gradient(circle at top, rgba(20, 201, 201, 0.2), transparent 55%);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.22);
  flex-shrink: 0;

  /* 标题：剩余有效期 */
  &__title {
    font-size: 9px;
    font-weight: 700;
    letter-spacing: 0.14em;
    text-transform: uppercase;
    color: rgba(255, 255, 255, 0.5);
  }

}

/* 倒计时环：72px 直径，紧凑精致 */
.lv-ring {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  padding: 7px;
  flex-shrink: 0;

  &__core {
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
      font-size: 22px;
      font-weight: 900;
      line-height: 1;
      letter-spacing: -0.04em;
    }

    span {
      margin-top: 2px;
      font-size: 9px;
      letter-spacing: 0.08em;
      color: rgba(255, 255, 255, 0.5);
    }
  }
}

.lv-t--healthy { color: #14c9c9; }
.lv-t--warning { color: #ffb454; }
.lv-t--expired { color: #ff7875; }

/* .lv-strip-cell 已移除 — 生命周期信息已合并到左面板 */

.lv-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;

  &--healthy { background: rgba(0, 180, 42, 0.08); color: #00b42a; }
  &--warning { background: rgba(255, 125, 0, 0.08); color: #ff7d00; }
  &--expired { background: rgba(245, 63, 63, 0.08); color: #f53f3f; }
}

.lv-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  flex: 1;
  min-height: 0;
}

.lv-card {
  position: relative;
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.85);
  background: rgba(255, 255, 255, 0.92);
  box-shadow:
    0 4px 20px rgba(15, 23, 42, 0.05),
    0 1px 3px rgba(15, 23, 42, 0.03);
  overflow: hidden;
  transition: box-shadow 0.25s ease;

  &:hover {
    box-shadow:
      0 8px 28px rgba(15, 23, 42, 0.08),
      0 2px 6px rgba(15, 23, 42, 0.04);
  }

  /* 顶部彩色渐变条：区分不同面板的视觉标识 */
  &__accent {
    height: 3px;
    flex-shrink: 0;

    &--blue  { background: linear-gradient(90deg, #165dff, #6aa1ff); }
    &--teal  { background: linear-gradient(90deg, #14c9c9, #6ee7b7); }
    &--gold  { background: linear-gradient(90deg, #ff7d00, #fbbf24); }
  }

  &__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 10px;
    padding: 10px 14px 6px;

    h2 {
      margin: 2px 0 0;
      font-size: 16px;
      font-weight: 800;
      color: #0f172a;
      letter-spacing: -0.02em;
    }
  }

  &__ico {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: 12px;
    font-size: 16px;
    flex-shrink: 0;
  }

  /* 内容滚动区：卡片内容超出时可滚动，隐藏滚动条 */
  &__scroll {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 0 14px 10px;
    scrollbar-width: none;
    &::-webkit-scrollbar { display: none; }
  }
}

.lv-ico {
  &--blue { background: rgba(22, 93, 255, 0.1); color: #165dff; }
  &--teal { background: rgba(20, 201, 201, 0.12); color: #14c9c9; }
  &--gold { background: rgba(255, 125, 0, 0.1); color: #ff7d00; }
}

.lv-facts {
  display: grid;
  gap: 5px;
}

.lv-fact {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 10px;
  background: rgba(248, 251, 255, 0.8);
  border: 1px solid rgba(226, 232, 240, 0.6);
  transition: background 0.15s ease;

  &:hover {
    background: rgba(241, 245, 249, 0.95);
  }

  span {
    font-size: 12px;
    color: #64748b;
    white-space: nowrap;
  }

  strong {
    font-size: 12px;
    font-weight: 700;
    color: #0f172a;
    text-align: right;
    word-break: break-all;

    &.mono {
      font-family: 'JetBrains Mono', 'Consolas', monospace;
      font-size: 11px;
    }
  }
}

/* 配额卡片列表 */
.lv-quota-list {
  display: grid;
  gap: 8px;
}

/* 单个配额卡片：标题行 + 进度条 + 三列统计 */
.lv-quota-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 14px;
  border-radius: 14px;
  background:
    linear-gradient(135deg, rgba(20, 201, 201, 0.03), rgba(22, 93, 255, 0.03)),
    rgba(248, 251, 255, 0.85);
  border: 1px solid rgba(226, 232, 240, 0.65);
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(15, 23, 42, 0.06);
  }

  /* 标题行：名称（左）+ 百分比（右） */
  &__title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;

    span {
      font-size: 13px;
      font-weight: 700;
      color: #334155;
    }

    strong {
      font-size: 13px;
      font-weight: 800;
    }
  }

  /* 百分比色调 */
  &__pct--healthy { color: #14c9c9; }
  &__pct--warning { color: #ff7d00; }
  &__pct--danger  { color: #f53f3f; }

  /* 进度条轨道 */
  &__track {
    height: 8px;
    border-radius: 999px;
    background: rgba(15, 23, 42, 0.06);
    overflow: hidden;
  }

  /* 进度条填充 */
  &__fill {
    height: 100%;
    border-radius: 999px;
    transition: width 0.6s cubic-bezier(0.22, 1, 0.36, 1);

    &--healthy {
      background: linear-gradient(90deg, #14c9c9, #6ee7b7);
      box-shadow: 0 0 8px rgba(20, 201, 201, 0.3);
    }
    &--warning {
      background: linear-gradient(90deg, #ff7d00, #fbbf24);
      box-shadow: 0 0 8px rgba(255, 125, 0, 0.3);
    }
    &--danger {
      background: linear-gradient(90deg, #f53f3f, #ff7875);
      box-shadow: 0 0 8px rgba(245, 63, 63, 0.3);
    }
  }

  /* 三列统计行 */
  &__stats {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;

    /* 四列明细布局 */
    &--4col {
      grid-template-columns: repeat(4, 1fr);
      gap: 4px;

      .lv-quota-stat {
        padding: 4px 0;
        strong {
          font-size: 15px;
        }
        span {
          transform: scale(0.9);
          white-space: nowrap;
        }
      }
    }
  }
}

/* 单个统计项：居中数字 + 下方标签 */
.lv-quota-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 5px 0;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(226, 232, 240, 0.5);

  strong {
    font-size: 16px;
    font-weight: 900;
    line-height: 1;
    letter-spacing: -0.03em;
    color: #0f172a;
  }

  span {
    font-size: 10px;
    font-weight: 600;
    color: #94a3b8;
    letter-spacing: 0.02em;
  }

  /* 授权上限：渐变蓝文字 */
  &--primary strong {
    background: linear-gradient(135deg, #165dff 0%, #14c9c9 100%);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
  }

  /* 剩余量：绿色文字 */
  &--success strong {
    color: #00b42a;
  }

  /* 停用/异常量：橙色文字 */
  &--warning strong {
    color: #ff7d00;
  }
}

/* 授权模块列表：卡片式布局，每项带状态图标 */
.lv-feature-list {
  display: grid;
  gap: 8px;
}

.lv-feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid transparent;
  font-size: 13px;
  font-weight: 600;
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 14px rgba(15, 23, 42, 0.06);
  }

  &__ico {
    font-size: 16px;
    flex-shrink: 0;
  }

  &--blue {
    background: rgba(22, 93, 255, 0.06);
    border-color: rgba(22, 93, 255, 0.12);
    color: #165dff;
  }
  &--teal {
    background: rgba(20, 201, 201, 0.08);
    border-color: rgba(20, 201, 201, 0.15);
    color: #0f766e;
  }
  &--gold {
    background: rgba(255, 125, 0, 0.06);
    border-color: rgba(255, 125, 0, 0.12);
    color: #ff7d00;
  }
  &--cyan {
    background: rgba(14, 165, 233, 0.06);
    border-color: rgba(14, 165, 233, 0.12);
    color: #0ea5e9;
  }
}

/* 空状态：图标 + 主文本 + 副描述居中 */
.lv-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex: 1;
  min-height: 140px;
  text-align: center;

  &__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 48px;
    border-radius: 16px;
    background: rgba(15, 23, 42, 0.04);
    color: #94a3b8;
    font-size: 22px;
    margin-bottom: 4px;
  }

  span {
    font-size: 13px;
    font-weight: 600;
    color: #64748b;
  }

  p {
    margin: 0;
    font-size: 12px;
    color: #94a3b8;
  }
}

.lv-sep {
  height: 1px;
  margin: 10px 0 6px;
  background: rgba(226, 232, 240, 0.8);
}

/* .lv-diag / .lv-diag-cell / .lv-dc 已移除 —— 系统诊断面板已删除 */

/* 生命周期列表（左面板下部） */
.lv-lc-list {
  display: grid;
  gap: 4px;
  margin-top: 6px;
}

.lv-lc-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 10px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  background: rgba(248, 251, 255, 0.8);
  transition: background 0.15s ease;

  &:hover {
    background: rgba(241, 245, 249, 0.95);
  }

  span {
    font-size: 12px;
    color: #64748b;
    white-space: nowrap;
  }

  strong {
    font-size: 12px;
    font-weight: 700;
    color: #0f172a;
    text-align: right;
  }
}

.lv-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  flex-shrink: 0;
}

.lv-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 10px;
  line-height: 1.4;
  word-break: break-all;

  .arco-icon { flex-shrink: 0; font-size: 12px; }

  &--blue { background: rgba(22, 93, 255, 0.07); color: #165dff; }
  &--teal { background: rgba(20, 201, 201, 0.1); color: #0f766e; }
  &--gold { background: rgba(255, 125, 0, 0.07); color: #ff7d00; }
  &--slate {
    background: rgba(15, 23, 42, 0.04);
    color: #475569;
    font-family: 'JetBrains Mono', 'Consolas', monospace;
  }
}

/* ============================================================
   许可证对比弹窗样式（紧凑布局，单屏全部展示）
   ============================================================ */
.compare-shell {
  padding: 0;

  &__header,
  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }

  &__title {
    display: flex;
    align-items: center;
    gap: 12px;

    h3 {
      margin: 0 0 2px;
      font-size: 20px;
      font-weight: 900;
      color: #0f172a;
      letter-spacing: -0.03em;
    }

    p {
      margin: 0;
      font-size: 12px;
      line-height: 1.6;
      color: #64748b;
    }
  }

  &__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    border-radius: 14px;
    background: linear-gradient(135deg, #165dff 0%, #14c9c9 100%);
    color: #fff;
    font-size: 18px;
    flex-shrink: 0;
  }

  &__close {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border: none;
    border-radius: 999px;
    background: rgba(15, 23, 42, 0.05);
    color: #64748b;
    cursor: pointer;
    transition: background 0.2s ease, color 0.2s ease;
    flex-shrink: 0;

    &:hover {
      background: rgba(15, 23, 42, 0.1);
      color: #0f172a;
    }
  }

  &__warning {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 12px;
    padding: 10px 14px;
    border-radius: 12px;
    background: rgba(255, 125, 0, 0.08);
    color: #ff7d00;
    font-size: 12px;
    font-weight: 700;
  }

  /* 底部操作栏：突出确认按钮，与内容区有明确分隔 */
  &__footer {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid rgba(226, 232, 240, 0.8);
    align-items: center;
  }

  &__footer-tip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    max-width: 480px;
    font-size: 12px;
    line-height: 1.6;
    color: #94a3b8;
  }
}

/* 对比网格：两列并排 */
.compare-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 14px;
}

/* 对比卡片（当前 / 新） */
.compare-card {
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  overflow: hidden;

  &--current {
    background: linear-gradient(180deg, rgba(245, 249, 255, 0.96), rgba(255, 255, 255, 0.96));
  }

  &--next {
    background: linear-gradient(180deg, rgba(236, 255, 246, 0.96), rgba(255, 255, 255, 0.96));
    border-color: rgba(20, 201, 201, 0.22);
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 12px 16px;
    border-bottom: 1px solid rgba(226, 232, 240, 0.92);

    span {
      font-size: 15px;
      font-weight: 800;
      color: #0f172a;
    }

    small {
      font-size: 11px;
      color: #94a3b8;
      letter-spacing: 0.12em;
      text-transform: uppercase;
    }
  }

  &__body {
    display: grid;
    gap: 12px;
    padding: 14px 16px;
  }
}

/* 对比区块标题 */
.compare-block {
  display: grid;
  gap: 5px;

  &__title {
    font-size: 10px;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    color: #94a3b8;
    font-weight: 700;
    margin-bottom: 1px;
  }
}

/* 对比行：紧凑设计 */
.compare-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(226, 232, 240, 0.85);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &.is-different {
    border-color: rgba(22, 93, 255, 0.28);
    box-shadow: inset 0 0 0 1px rgba(22, 93, 255, 0.06);
    background: rgba(245, 249, 255, 0.7);
  }

  &__label {
    font-size: 12px;
    color: #64748b;
    white-space: nowrap;
  }

  strong {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 13px;
    font-weight: 800;
    color: #0f172a;
    text-align: right;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
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

/* 授权模块标签列表 */
.compare-feature-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.compare-feature-tag {
  border-radius: 999px !important;
  padding-inline: 8px !important;
  min-height: 26px;
  font-size: 11px !important;
}

.compare-feature-empty {
  font-size: 12px;
  color: #94a3b8;
}

/* 底部操作按钮区域 */
.compare-shell__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

/* 确认更新按钮：渐变背景 + 发光阴影 + 悬浮放大效果 */
.compare-confirm-btn {
  min-width: 180px;
  height: 40px !important;
  border-radius: 12px !important;
  background: linear-gradient(135deg, #165dff 0%, #0fc6c6 100%) !important;
  border: none !important;
  font-size: 15px !important;
  font-weight: 700 !important;
  letter-spacing: 0.02em;
  box-shadow: 0 4px 18px rgba(22, 93, 255, 0.35),
              0 1px 4px rgba(22, 93, 255, 0.18) !important;
  transition: transform 0.2s ease, box-shadow 0.2s ease !important;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 24px rgba(22, 93, 255, 0.45),
                0 2px 6px rgba(22, 93, 255, 0.22) !important;
  }

  &:active {
    transform: translateY(0);
  }
}

@media (max-width: 1440px) {
  .license-activation {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1200px) {
  .lv-grid {
    grid-template-columns: 1fr 1fr;
  }

  .compare-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .license-page-shell {
    padding: 10px;
    overflow: auto;
    height: auto;
    min-height: 100%;
  }

  .lv-dashboard {
    overflow: visible;
  }

  .lv-grid {
    grid-template-columns: 1fr;
  }

  .lv-hero__bar,
  .lv-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .lv-customer {
    flex-direction: column;
  }

  .activation-story,
  .activation-console {
    padding: 16px;
  }

  .compare-shell {
    padding: 0;
  }

  .activation-story__metrics {
    grid-template-columns: 1fr;
  }

  .compare-shell__header,
  .compare-shell__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .compare-shell__footer-tip {
    max-width: none;
  }
}
</style>

<style lang="less">
/* 全局样式（非 scoped）：控制 Arco Modal 容器的内边距和尺寸 */
.license-compare-modal__wrap {
  /* 弹窗圆角样式 */
  border-radius: 20px;
  overflow: hidden;

  .arco-modal-body {
    padding: 0;
    /* 限制弹窗最大高度为视口 92%，确保不需要页面级滚动 */
    max-height: 92vh;
    overflow-y: auto;
  }
}

/* ================== 重置许可证危险弹窗样式 ================== */
.lv-danger-modal {
  border-radius: 20px !important;
  overflow: hidden;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.15) !important;
  
  .arco-modal-body {
    padding: 0 !important;
  }

  &__content {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 40px 32px;
    background: linear-gradient(180deg, #fff 0%, #fefcfc 100%);
    text-align: center;
  }

  &__icon {
    font-size: 56px;
    color: #f53f3f;
    margin-bottom: 20px;
    filter: drop-shadow(0 8px 16px rgba(245, 63, 63, 0.25));
    animation: lv-pulse-danger 2s infinite cubic-bezier(0.4, 0, 0.2, 1);
  }

  &__title {
    margin: 0 0 16px;
    font-size: 22px;
    font-weight: 900;
    color: #1d2129;
    letter-spacing: -0.02em;
  }

  &__desc {
    margin: 0 0 32px;
    font-size: 14px;
    line-height: 1.6;
    color: #4e5969;
    background: #fff1f0;
    border: 1px solid #ffccc7;
    border-radius: 12px;
    padding: 16px 20px;
    
    p {
      margin: 0;
      & + p {
        margin-top: 8px;
        color: #f53f3f;
        font-weight: 600;
      }
    }
  }

  &__actions {
    display: flex;
    gap: 16px;
    width: 100%;
    
    .lv-danger-btn {
      flex: 1;
      height: 44px;
      font-size: 15px;
      font-weight: 700;
      transition: all 0.3s ease;
      
      &--cancel {
        background: #f2f3f5;
        border: none;
        color: #4e5969;
        
        &:hover {
          background: #e5e6eb;
          color: #1d2129;
        }
      }
      
      &--confirm {
        background: linear-gradient(135deg, #f53f3f 0%, #ff7875 100%);
        box-shadow: 0 6px 16px rgba(245, 63, 63, 0.25);
        border: none;
        color: #fff;
        
        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 8px 20px rgba(245, 63, 63, 0.35);
        }
        
        &:active {
          transform: translateY(1px);
        }
      }
    }
  }
}

@keyframes lv-pulse-danger {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}
</style>

<!--
  ══════════════════════════════════════════════════════════════════════
  全局样式：美化右上角授权系统提示通知弹窗
  ──────────────────────────────────────────────────────────────────────
  说明：
    Arco Design 的 Notification 组件挂载在 document.body 下，
    不在当前组件的 DOM 树内，因此 <style scoped> 无法命中它。
    必须使用不带 scoped 的 <style> 块才能覆盖其样式。

    为避免污染全局，所有选择器都以 .bml-license-notify 为命名空间前缀，
    该类名通过 Notification 的 class 选项注入（见 confirmUpdate 函数）。

  使用方式：
    在调用 Notification 时传入 class: 'bml-license-notify'，
    即可自动应用以下所有增强样式。
  ══════════════════════════════════════════════════════════════════════
-->
<style>
/*
 * ══════════════════════════════════════════════════════════════
 *  BML 许可证变更通知 — 全局样式（Premium 卡片风格）
 * ══════════════════════════════════════════════════════════════
 *
 *  Arco Notification 挂载在 document.body 下，不在当前组件 DOM 树内，
 *  必须使用不带 scoped 的 <style> 块覆盖样式。
 *  所有选择器以 .bml-license-notify 为命名空间前缀，避免污染全局。
 *
 *  设计特点：
 *  - 毛玻璃背景 + 大圆角 + 悬浮阴影（与系统整体设计语言一致）
 *  - 左侧 4px 色条区分通知类型（success/info/warning）
 *  - 变更条目使用彩色圆点 + 分行列表，升级/降级/新增/移除一目了然
 *  - 流畅的入场动画（从右上角滑入）
 */

/* ── 通知容器整体 ── */
.bml-license-notify.arco-notification {
  min-width: 380px;
  max-width: 460px;
  border-radius: 16px !important;
  background: rgba(255, 255, 255, 0.96) !important;
  backdrop-filter: blur(20px) saturate(1.4);
  -webkit-backdrop-filter: blur(20px) saturate(1.4);
  box-shadow:
    0 12px 40px -8px rgba(15, 23, 42, 0.18),
    0 4px 12px rgba(15, 23, 42, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.8) !important;
  border: 1px solid rgba(226, 232, 240, 0.7) !important;
  overflow: hidden;
  padding: 20px 22px 18px !important;
  animation: bmlLicenseNotifySlideIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

/* ── success 类型：左侧翠绿色条 ── */
.bml-license-notify.arco-notification-success {
  border-left: 4px solid #00b42a !important;
}
/* ── info 类型：左侧品牌蓝色条 ── */
.bml-license-notify.arco-notification-info {
  border-left: 4px solid #165dff !important;
}
/* ── warning 类型：左侧琥珀橙色条 ── */
.bml-license-notify.arco-notification-warning {
  border-left: 4px solid #ff7d00 !important;
}

/* ── 隐藏 Arco 默认的左侧类型图标（由彩色竖条替代） ── */
.bml-license-notify .arco-notification-icon {
  display: none !important;
}

/* ── 通知标题 ── */
.bml-license-notify .arco-notification-title {
  font-size: 15px !important;
  font-weight: 700 !important;
  color: #0f172a !important;
  line-height: 1.5;
  letter-spacing: 0.02em;
  margin-left: 0 !important;
}

/* ── 通知正文容器 ── */
.bml-license-notify .arco-notification-content {
  margin-top: 10px !important;
  margin-left: 0 !important;
}

/* ── 关闭按钮 ── */
.bml-license-notify .arco-notification-close-btn {
  color: #94a3b8 !important;
  top: 16px !important;
  right: 16px !important;
  transition: all 0.2s;
}
.bml-license-notify .arco-notification-close-btn:hover {
  color: #334155 !important;
  transform: rotate(90deg);
}

/* ══════════════════════════════════════════════════════════════
 *  变更条目列表（confirmUpdate 使用 renderChangeItems 渲染）
 * ══════════════════════════════════════════════════════════════ */

.bml-notify-items {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.bml-notify-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 7px 12px;
  border-radius: 8px;
  background: rgba(241, 245, 249, 0.6);
  transition: background 0.15s;
}
.bml-notify-item:hover {
  background: rgba(226, 232, 240, 0.5);
}

/* 圆点指示器 */
.bml-notify-item .item-dot {
  flex-shrink: 0;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-top: 6px;
}

/* 文字 */
.bml-notify-item .item-text {
  font-size: 13px;
  line-height: 1.6;
  color: #334155;
  word-break: break-all;
}

/* ── 各类型圆点颜色 ── */
.bml-notify-item.item-up .item-dot { background: #00b42a; box-shadow: 0 0 6px rgba(0, 180, 42, 0.4); }
.bml-notify-item.item-down .item-dot { background: #f53f3f; box-shadow: 0 0 6px rgba(245, 63, 63, 0.4); }
.bml-notify-item.item-add .item-dot { background: #165dff; box-shadow: 0 0 6px rgba(22, 93, 255, 0.4); }
.bml-notify-item.item-remove .item-dot { background: #ff7d00; box-shadow: 0 0 6px rgba(255, 125, 0, 0.4); }
.bml-notify-item.item-expired .item-dot { background: #f53f3f; box-shadow: 0 0 6px rgba(245, 63, 63, 0.5); }
.bml-notify-item.item-date .item-dot { background: #722ed1; box-shadow: 0 0 6px rgba(114, 46, 209, 0.4); }
.bml-notify-item.item-info .item-dot { background: #86909c; }

/* ── 各类型文字颜色微调 ── */
.bml-notify-item.item-up .item-text { color: #0e7a2e; }
.bml-notify-item.item-down .item-text { color: #cb2634; }
.bml-notify-item.item-expired .item-text { color: #cb2634; font-weight: 600; }

/* ══════════════════════════════════════════════════════════════
 *  概要列表（handleUpload 使用 renderSummaryItems 渲染）
 * ══════════════════════════════════════════════════════════════ */

.bml-notify-summary {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bml-notify-summary-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 0;
}

.bml-notify-summary-row .summary-dot {
  flex-shrink: 0;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #00b42a;
  opacity: 0.7;
}

.bml-notify-summary-row .summary-text {
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}

/* ── 入场动画 ── */
@keyframes bmlLicenseNotifySlideIn {
  from {
    opacity: 0;
    transform: translateX(20px) translateY(-8px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateX(0) translateY(0) scale(1);
  }
}
</style>
