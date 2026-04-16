<template>
    <div class="license-page">
        <!-- ═══════════════ 未激活状态 ═══════════════ -->
        <div v-if="!licenseData?.activated" class="activation-scene">
            <div class="activation-glass-card">
                <!-- 顶部光晕装饰 -->
                <div class="glow-ring"></div>

                <div class="activation-icon">
                    <icon-safe :size="44" />
                </div>
                <h1 class="activation-title">系统许可证激活</h1>
                <p class="activation-desc">请上传有效的许可证文件以激活系统全部功能</p>

                <div class="upload-zone">
                    <a-upload
                        :auto-upload="false"
                        :limit="1"
                        accept=".lic"
                        draggable
                        :file-list="fileList"
                        @change="handleFileChange"
                    >
                        <template #upload-button>
                            <div class="drop-area" :class="{ 'has-file': selectedFile }">
                                <div class="drop-icon-ring">
                                    <icon-upload :size="28" />
                                </div>
                                <p class="drop-title">{{ selectedFile ? selectedFile.name : '点击或拖拽文件到此处' }}</p>
                                <p class="drop-hint">仅支持 .lic 格式许可证文件</p>
                            </div>
                        </template>
                    </a-upload>
                </div>

                <button
                    class="activate-btn"
                    :class="{ disabled: !selectedFile || uploading }"
                    :disabled="!selectedFile || uploading"
                    @click="handleUpload"
                >
                    <span v-if="uploading" class="btn-spinner"></span>
                    <icon-safe v-else />
                    <span>{{ uploading ? '正在激活...' : '上传并激活许可证' }}</span>
                </button>

                <transition name="fade-slide">
                    <div v-if="errorMsg" class="error-toast">
                        <icon-exclamation-circle />
                        <span>{{ errorMsg }}</span>
                    </div>
                </transition>
            </div>
        </div>

        <!-- ═══════════════ 已激活 — Bento 仪表盘（单屏无滚动） ═══════════════ -->
        <div v-else class="dash">
            <!-- ── 上层 Bento：渐变主信息卡 + 暗色天数环卡 ── -->
            <div class="dash-top">
                <!-- 主信息卡（渐变背景） -->
                <div class="hero" :class="bannerClass">
                    <!-- 顶栏：状态标签 + 操作按钮 -->
                    <div class="hero-bar">
                        <span class="hero-tag" :class="bannerClass">
                            <icon-check-circle v-if="!licenseData?.expired" :size="13" />
                            <icon-exclamation-circle v-else :size="13" />
                            {{ licenseData?.expired ? '已过期' : '许可证有效' }}
                        </span>
                        <div class="hero-btns">
                            <a-upload :auto-upload="false" :limit="1" :show-file-list="false" :file-list="replaceFileList" accept=".lic" @change="handleReplaceFile">
                                <template #upload-button>
                                    <a-button type="primary" size="small" :loading="previewing" class="hbtn hbtn-primary">
                                        <template #icon><icon-upload /></template>更新许可证
                                    </a-button>
                                </template>
                            </a-upload>
                            <a-button size="small" class="hbtn hbtn-default" @click="loadLicenseStatus">
                                <template #icon><icon-refresh /></template>刷新
                            </a-button>
                            <a-popconfirm content="确定要删除许可证并重置系统吗？系统将回到未激活状态。" type="warning" @ok="handleReset">
                                <a-button size="small" status="danger" :loading="resetting" class="hbtn hbtn-danger">
                                    <template #icon><icon-delete /></template>重置
                                </a-button>
                            </a-popconfirm>
                        </div>
                    </div>
                    <!-- 主体：客户名称 + 许可证元信息 -->
                    <div class="hero-main">
                        <h2 class="hero-name">{{ licenseData?.customerName || '-' }}</h2>
                        <div class="hero-meta">
                            <code>{{ licenseData?.licenseId || '-' }}</code>
                            <i class="dot-sep"></i>
                            <span>{{ licenseData?.customerCode || '-' }}</span>
                            <i class="dot-sep"></i>
                            <span>v{{ licenseData?.productVersion || '-' }}</span>
                        </div>
                    </div>
                    <!-- 底栏：签发 / 到期日期 -->
                    <div class="hero-dates">
                        <div class="hd-item">
                            <span class="hd-label">签发日期</span>
                            <span class="hd-val">{{ licenseData?.issueDate || '-' }}</span>
                        </div>
                        <div class="hd-divider"></div>
                        <div class="hd-item">
                            <span class="hd-label">到期日期</span>
                            <span class="hd-val" :class="{ 'hd-expired': licenseData?.expired }">{{ licenseData?.expireDate || '-' }}</span>
                        </div>
                    </div>
                </div>

                <!-- 天数环卡（暗色背景，视觉焦点） -->
                <div class="days-card">
                    <div class="days-ring">
                        <svg viewBox="0 0 100 100">
                            <circle cx="50" cy="50" r="40" fill="none" stroke="rgba(255,255,255,0.08)" stroke-width="7" />
                            <circle cx="50" cy="50" r="40" fill="none" :stroke="ringColor" stroke-width="7"
                                stroke-linecap="round" :stroke-dasharray="ringDash"
                                transform="rotate(-90 50 50)" class="ring-animate" />
                        </svg>
                        <div class="days-center">
                            <span class="days-num" :class="daysLeftClass">{{ daysLeft !== null ? (daysLeft < 0 ? 0 : daysLeft) : '-' }}</span>
                            <span class="days-unit">天</span>
                        </div>
                    </div>
                    <span class="days-label">剩余有效期</span>
                    <span class="days-sub" :class="daysLeftClass">{{ daysLeftText }}</span>
                </div>
            </div>

            <!-- ── 下层：三列详细信息卡 ── -->
            <div class="dash-bottom">
                <!-- 基本信息 -->
                <div class="detail-card">
                    <div class="dc-head"><div class="dc-dot dc-blue"></div><span class="dc-title">基本信息</span></div>
                    <div class="dc-body">
                        <div class="kv-row" v-for="item in baseInfo" :key="item.label">
                            <span class="kv-label">{{ item.label }}</span>
                            <span class="kv-value" :class="{ mono: item.mono }">{{ item.value }}</span>
                        </div>
                    </div>
                </div>
                <!-- 配额限制 -->
                <div class="detail-card">
                    <div class="dc-head"><div class="dc-dot dc-green"></div><span class="dc-title">配额限制</span></div>
                    <div class="dc-body">
                        <div class="quota-row" v-for="q in quotaInfo" :key="q.label">
                            <div class="quota-header">
                                <span class="quota-name">{{ q.label }}</span>
                                <span class="quota-num">{{ q.display }}</span>
                            </div>
                            <div class="quota-track">
                                <div class="quota-fill" :style="{ width: q.percent + '%' }"></div>
                            </div>
                        </div>
                    </div>
                </div>
                <!-- 授权模块 -->
                <div class="detail-card">
                    <div class="dc-head"><div class="dc-dot dc-orange"></div><span class="dc-title">授权模块</span></div>
                    <div class="dc-body dc-features">
                        <span v-for="feature in licenseData?.features || []" :key="feature" class="feat-chip">
                            <icon-check-circle class="feat-ok" />{{ featureLabel(feature) }}
                        </span>
                        <span v-if="!licenseData?.features?.length" class="feat-empty">
                            <icon-info-circle :size="13" /> 暂无授权模块
                        </span>
                    </div>
                </div>
            </div>

            <!-- ── 底部元信息 ── -->
            <div v-if="licenseData?.remark || licenseData?.filePath" class="dash-foot">
                <span v-if="licenseData?.remark" class="foot-tag"><icon-file :size="11" /> {{ licenseData.remark }}</span>
                <span v-if="licenseData?.filePath" class="foot-tag foot-path"><icon-folder :size="11" /> {{ licenseData.filePath }}</span>
            </div>
        </div>

        <!-- ═══════════════ 许可证对比弹窗（紧凑双栏设计） ═══════════════ -->
        <a-modal
            v-model:visible="compareVisible"
            :width="880"
            :mask-closable="false"
            :footer="false"
            :header="false"
            class="compare-modal"
            modal-class="cmp-modal-wrap"
        >
            <div class="cmp">
                <!-- 弹窗标题栏 -->
                <div class="cmp-header">
                    <div class="cmp-title-row">
                        <div class="cmp-title-icon"><icon-swap :size="18" /></div>
                        <div>
                            <h3 class="cmp-title">许可证更新确认</h3>
                            <p class="cmp-subtitle">请核对新旧许可证差异后确认更新</p>
                        </div>
                    </div>
                    <icon-close class="cmp-close" @click="closeCompare" />
                </div>

                <!-- 降级警告（仅在有降级时显示） -->
                <div v-if="downgradeWarnings.length > 0" class="cmp-warn">
                    <icon-exclamation-circle-fill :size="14" />
                    <span>检测到降级：{{ downgradeWarnings.join('；') }}</span>
                </div>

                <!-- 双栏对比主体 -->
                <div class="cmp-body">
                    <!-- 左栏：当前许可证 -->
                    <div class="cmp-col">
                        <div class="cmp-col-head cmp-cur">
                            <icon-safe :size="14" />
                            <span>当前许可证</span>
                        </div>
                        <div class="cmp-card">
                            <div class="cmp-sec-title">基本信息</div>
                            <div v-for="field in basicFields" :key="'c-' + field.key" class="cmp-kv" :class="{ 'cmp-row-diff': field.current !== field.next }">
                                <span class="cmp-k">{{ field.label }}</span>
                                <span class="cmp-v">{{ field.current }}</span>
                            </div>
                            <div class="cmp-sec-title cmp-sec-gap">配额限制</div>
                            <div v-for="field in quotaFields" :key="'cq-' + field.key" class="cmp-kv" :class="{ 'cmp-row-diff': field.current !== field.next }">
                                <span class="cmp-k">{{ field.label }}</span>
                                <span class="cmp-v">{{ field.current }}</span>
                            </div>
                            <div class="cmp-sec-title cmp-sec-gap">授权模块</div>
                            <div class="cmp-tags">
                                <a-tag v-for="f in licenseData?.features || []" :key="'cf-' + f" size="small" color="arcoblue" class="cmp-tag"
                                    :class="{ 'cmp-tag-removed': removedFeatures.includes(f) }"
                                >{{ featureLabel(f) }}<template v-if="removedFeatures.includes(f)"> ×</template></a-tag>
                                <span v-if="!licenseData?.features?.length" class="cmp-empty">无</span>
                            </div>
                        </div>
                    </div>

                    <!-- 中间箭头 -->
                    <div class="cmp-arrow-col">
                        <icon-arrow-right :size="20" />
                    </div>

                    <!-- 右栏：新许可证 -->
                    <div class="cmp-col">
                        <div class="cmp-col-head cmp-new">
                            <icon-upload :size="14" />
                            <span>新许可证</span>
                        </div>
                        <div class="cmp-card">
                            <div class="cmp-sec-title">基本信息</div>
                            <div v-for="field in basicFields" :key="'n-' + field.key" class="cmp-kv" :class="{ 'cmp-row-diff': field.current !== field.next }">
                                <span class="cmp-k">{{ field.label }}</span>
                                <span class="cmp-v" :class="{ 'cmp-changed': field.current !== field.next }">
                                    <icon-swap v-if="field.current !== field.next" class="cmp-diff-icon" :size="11" />
                                    {{ field.next }}
                                </span>
                            </div>
                            <div class="cmp-sec-title cmp-sec-gap">配额限制</div>
                            <div v-for="field in quotaFields" :key="'nq-' + field.key" class="cmp-kv" :class="{ 'cmp-row-diff': field.current !== field.next }">
                                <span class="cmp-k">{{ field.label }}</span>
                                <span class="cmp-v" :class="{ 'cmp-changed': field.current !== field.next, 'cmp-down': field.direction === 'down' }">
                                    <icon-arrow-up v-if="field.direction === 'up'" class="cmp-dir-up" :size="11" />
                                    <icon-arrow-down v-else-if="field.direction === 'down'" class="cmp-dir-down" :size="11" />
                                    <icon-swap v-else-if="field.current !== field.next" class="cmp-diff-icon" :size="11" />
                                    {{ field.next }}
                                </span>
                            </div>
                            <div class="cmp-sec-title cmp-sec-gap">授权模块</div>
                            <div class="cmp-tags">
                                <a-tag v-for="f in previewData?.features || []" :key="'nf-' + f" size="small" :color="isFeatureAdded(f) ? 'green' : 'arcoblue'" class="cmp-tag"
                                    :class="{ 'cmp-tag-added': isFeatureAdded(f) }"
                                >
                                    <template v-if="isFeatureAdded(f)">+ </template>{{ featureLabel(f) }}
                                </a-tag>
                                <a-tag v-for="f in removedFeatures" :key="'rm-' + f" size="small" color="red" class="cmp-tag cmp-tag-removed">× {{ featureLabel(f) }}</a-tag>
                                <span v-if="!previewData?.features?.length && !removedFeatures.length" class="cmp-empty">无</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 底部操作栏 -->
                <div class="cmp-footer">
                    <a-button class="cmp-btn-cancel" @click="closeCompare">取消</a-button>
                    <a-button type="primary" :loading="uploading" class="cmp-btn-confirm" @click="confirmUpdate">
                        <template #icon><icon-check-circle /></template>
                        确认更新
                    </a-button>
                </div>
            </div>
        </a-modal>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import type { FileItem } from '@arco-design/web-vue';
import { fetchLicenseStatus, previewLicense, uploadLicense, updateLicense, resetLicense, type LicenseStatus } from '../../../api/license';
import { resetLicenseCache } from '../../../router';

const licenseData = ref<LicenseStatus | null>(null);
const uploading = ref(false);
const previewing = ref(false);
const resetting = ref(false);
const errorMsg = ref('');
const fileList = ref<FileItem[]>([]);
const selectedFile = ref<File | null>(null);
/** 更新上传组件的文件列表（用于控制 a-upload 内部状态，避免 limit=1 后按钮消失） */
const replaceFileList = ref<FileItem[]>([]);

/** 许可证对比弹窗 */
const compareVisible = ref(false);
const previewData = ref<LicenseStatus | null>(null);
const pendingFile = ref<File | null>(null);

/**
 * 功能模块中文标签映射。
 * 许可证授权控制的是前台业务端模块（非中台管理），
 * 前台业务模块尚未开发，待开发后在此补充。
 */
const FEATURE_LABELS: Record<string, string> = {};

const featureLabel = (key: string) => FEATURE_LABELS[key] || key;

const formatQuota = (val?: number) => {
    if (val === undefined || val === null) return '-';
    return val === 0 ? '不限' : String(val);
};

/** 计算剩余天数 */
const daysLeft = computed(() => {
    if (!licenseData.value?.expireDate) return null;
    const expire = new Date(licenseData.value.expireDate);
    const now = new Date();
    return Math.ceil((expire.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
});

const daysLeftText = computed(() => {
    const d = daysLeft.value;
    if (d === null) return '-';
    if (d < 0) return `已过期 ${Math.abs(d)} 天`;
    if (d === 0) return '今天到期';
    return `${d} 天`;
});

const daysLeftClass = computed(() => {
    const d = daysLeft.value;
    if (d === null) return '';
    if (d < 0) return 'expired';
    if (d <= 30) return 'warning';
    return 'healthy';
});

/** 状态横幅样式 */
const bannerClass = computed(() => licenseData.value?.expired ? 'banner-expired' : 'banner-valid');

/** 基本信息列表 */
const baseInfo = computed(() => [
    { label: '许可证 ID', value: licenseData.value?.licenseId || '-', mono: true },
    { label: '客户名称', value: licenseData.value?.customerName || '-', mono: false },
    { label: '客户编码', value: licenseData.value?.customerCode || '-', mono: true },
    { label: '产品版本', value: licenseData.value?.productVersion || '-', mono: false },
]);

/** 配额信息（带进度百分比） */
const quotaInfo = computed(() => {
    const items = [
        { label: '最大 API 账号', raw: licenseData.value?.maxApiAccounts },
        { label: '单账号用户数', raw: licenseData.value?.maxUsersPerAccount },
        { label: '业务用户数', raw: licenseData.value?.maxTotalUsers },
    ];
    return items.map(item => {
        const display = formatQuota(item.raw);
        // 0 = 不限 → 100%；有值 → 按 100 封顶映射
        const percent = item.raw === undefined || item.raw === null
            ? 0
            : item.raw === 0 ? 100 : Math.min((item.raw / 100) * 100, 100);
        return { ...item, display, percent };
    });
});

/** 剩余天数进度环参数（SVG 圆半径 r=40） */
const RING_CIRCUMFERENCE = 2 * Math.PI * 40; // r=40
const ringColor = computed(() => {
    const d = daysLeft.value;
    if (d === null) return 'rgba(0,0,0,0.1)';
    if (d < 0) return '#f53f3f';
    if (d <= 30) return '#ff7d00';
    return '#00b42a';
});
const ringDash = computed(() => {
    const d = daysLeft.value;
    if (d === null) return `0 ${RING_CIRCUMFERENCE}`;
    // 以 365 天为满圈
    const ratio = Math.max(0, Math.min(d / 365, 1));
    const filled = ratio * RING_CIRCUMFERENCE;
    return `${filled} ${RING_CIRCUMFERENCE - filled}`;
});

/** 加载许可证状态 */
const loadLicenseStatus = async () => {
    try {
        const res = await fetchLicenseStatus() as any;
        licenseData.value = res.data;
        errorMsg.value = '';
    } catch {
        licenseData.value = null;
    }
};

/** 文件选择处理 */
const handleFileChange = (files: FileItem[]) => {
    fileList.value = files;
    selectedFile.value = files.length > 0 ? files[0].file as File : null;
    errorMsg.value = '';
};

/** 上传许可证 */
const handleUpload = async () => {
    if (!selectedFile.value) return;
    uploading.value = true;
    errorMsg.value = '';

    try {
        const res = await uploadLicense(selectedFile.value) as any;
        licenseData.value = res.data;
        Message.success('许可证上传成功，系统已激活');
        resetLicenseCache();
        fileList.value = [];
        selectedFile.value = null;
        // 激活成功后跳转登录页
        setTimeout(() => { window.location.href = '/admin/login'; }, 1500);
    } catch (err: any) {
        errorMsg.value = err?.message || '许可证上传失败';
    } finally {
        uploading.value = false;
    }
};

/** 更新（替换）许可证 — 先预览对比再确认 */
const handleReplaceFile = async (files: FileItem[]) => {
    if (files.length === 0) return;
    const file = files[0].file as File;
    pendingFile.value = file;
    previewing.value = true;

    try {
        const res = await previewLicense(file) as any;
        previewData.value = res.data;
        compareVisible.value = true;
    } catch (err: any) {
        Message.error(err?.message || '许可证文件验证失败');
        replaceFileList.value = [];
    } finally {
        previewing.value = false;
    }
};

/** 确认更新许可证（使用专用的 update 接口，后端自动备份旧许可证） */
const confirmUpdate = async () => {
    if (!pendingFile.value) return;
    uploading.value = true;

    try {
        const res = await updateLicense(pendingFile.value) as any;
        licenseData.value = res.data;
        resetLicenseCache();
        compareVisible.value = false;
        pendingFile.value = null;
        previewData.value = null;
        replaceFileList.value = [];
        Message.success('许可证更新成功，旧许可证已自动备份');
    } catch (err: any) {
        Message.error(err?.message || '许可证更新失败');
    } finally {
        uploading.value = false;
    }
};

/** 对比字段：基本信息 */
const basicFields = computed(() => [
    { key: 'licenseId', label: '许可证 ID', current: licenseData.value?.licenseId || '-', next: previewData.value?.licenseId || '-' },
    { key: 'customerName', label: '客户名称', current: licenseData.value?.customerName || '-', next: previewData.value?.customerName || '-' },
    { key: 'productVersion', label: '产品版本', current: licenseData.value?.productVersion || '-', next: previewData.value?.productVersion || '-' },
    { key: 'issueDate', label: '签发日期', current: licenseData.value?.issueDate || '-', next: previewData.value?.issueDate || '-' },
    { key: 'expireDate', label: '到期日期', current: licenseData.value?.expireDate || '-', next: previewData.value?.expireDate || '-' },
]);

/** 对比字段：配额 */
const quotaFields = computed(() => {
    const fields = [
        { key: 'maxApiAccounts', label: '最大 API 账号', currentRaw: licenseData.value?.maxApiAccounts, nextRaw: previewData.value?.maxApiAccounts },
        { key: 'maxUsersPerAccount', label: '单账号用户数', currentRaw: licenseData.value?.maxUsersPerAccount, nextRaw: previewData.value?.maxUsersPerAccount },
        { key: 'maxTotalUsers', label: '业务用户数', currentRaw: licenseData.value?.maxTotalUsers, nextRaw: previewData.value?.maxTotalUsers },
    ];
    return fields.map(f => {
        const current = formatQuota(f.currentRaw);
        const next = formatQuota(f.nextRaw);
        let direction: 'up' | 'down' | 'same' = 'same';
        if (f.currentRaw !== undefined && f.nextRaw !== undefined && f.currentRaw !== f.nextRaw) {
            // 0 表示不限，视为最大值
            const cv = f.currentRaw === 0 ? Infinity : f.currentRaw;
            const nv = f.nextRaw === 0 ? Infinity : f.nextRaw;
            direction = nv > cv ? 'up' : nv < cv ? 'down' : 'same';
        }
        return { ...f, current, next, direction };
    });
});

/** 功能模块差异分析 */
const removedFeatures = computed(() => {
    const current = new Set(licenseData.value?.features || []);
    const next = new Set(previewData.value?.features || []);
    return [...current].filter(f => !next.has(f));
});

const isFeatureAdded = (feature: string) => {
    const current = new Set(licenseData.value?.features || []);
    return !current.has(feature);
};

/** 降级警告 */
const downgradeWarnings = computed(() => {
    const warnings: string[] = [];
    for (const f of removedFeatures.value) {
        warnings.push(`功能模块「${featureLabel(f)}」将被移除`);
    }
    for (const q of quotaFields.value) {
        if (q.direction === 'down') {
            warnings.push(`${q.label}配额从 ${q.current} 降为 ${q.next}`);
        }
    }
    return warnings;
});

/** 关闭对比弹窗并重置上传组件状态（避免按钮消失） */
const closeCompare = () => {
    compareVisible.value = false;
    replaceFileList.value = [];
    pendingFile.value = null;
    previewData.value = null;
};

/** 重置许可证（删除文件并重置状态） */
const handleReset = async () => {
    resetting.value = true;
    try {
        const res = await resetLicense() as any;
        licenseData.value = res.data;
        resetLicenseCache();
        Message.success('许可证已删除，系统已重置为未激活状态');
    } catch (err: any) {
        Message.error(err?.message || '重置失败');
    } finally {
        resetting.value = false;
    }
};

onMounted(() => {
    loadLicenseStatus();
});
</script>

<style scoped lang="less">
/* ═══════════════════════════════════════════════════════
   许可证管理页面 — 紧凑单页设计 + Vision Pro 毛玻璃风格
   ═══════════════════════════════════════════════════════ */
.license-page {
    height: 100%;
    overflow: hidden;
    padding: 0;
}

/* ═══════════════ 未激活状态 ═══════════════ */
.activation-scene {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    padding: 24px;
    background:
        radial-gradient(ellipse at 20% 0%, rgba(22, 93, 255, 0.08) 0%, transparent 60%),
        radial-gradient(ellipse at 80% 100%, rgba(114, 46, 209, 0.06) 0%, transparent 60%);
}

.activation-glass-card {
    position: relative;
    background: rgba(255, 255, 255, 0.72);
    backdrop-filter: blur(40px) saturate(180%);
    -webkit-backdrop-filter: blur(40px) saturate(180%);
    border: 1px solid rgba(255, 255, 255, 0.6);
    border-radius: 28px;
    padding: 48px 44px 40px;
    max-width: 480px;
    width: 100%;
    text-align: center;
    box-shadow:
        0 24px 80px rgba(22, 93, 255, 0.10),
        0 4px 16px rgba(0, 0, 0, 0.03),
        inset 0 0 0 1px rgba(255, 255, 255, 0.4);
    overflow: hidden;
}

.glow-ring {
    position: absolute;
    top: -60px;
    left: 50%;
    transform: translateX(-50%);
    width: 200px;
    height: 200px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(22, 93, 255, 0.18) 0%, transparent 70%);
    pointer-events: none;
}

.activation-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 72px;
    height: 72px;
    border-radius: 22px;
    background: linear-gradient(135deg, #165dff 0%, #722ed1 100%);
    color: #fff;
    margin-bottom: 20px;
    box-shadow: 0 12px 32px rgba(22, 93, 255, 0.35);
    position: relative;
    z-index: 1;
}

.activation-title {
    font-size: 24px;
    font-weight: 800;
    color: #1d2129;
    margin: 0 0 6px;
    letter-spacing: -0.3px;
}

.activation-desc {
    font-size: 13px;
    color: #86909c;
    margin: 0 0 28px;
}

/* 上传拖放区 */
.upload-zone {
    margin-bottom: 20px;
}

.drop-area {
    padding: 28px 20px;
    border: 2px dashed rgba(22, 93, 255, 0.25);
    border-radius: 16px;
    cursor: pointer;
    transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
    background: rgba(255, 255, 255, 0.5);

    &:hover {
        border-color: #165dff;
        background: rgba(22, 93, 255, 0.04);
        transform: translateY(-2px);
        box-shadow: 0 8px 24px rgba(22, 93, 255, 0.12);
    }

    &.has-file {
        border-color: #00b42a;
        background: rgba(0, 180, 42, 0.04);
    }
}

.drop-icon-ring {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: rgba(22, 93, 255, 0.08);
    color: #165dff;
    margin-bottom: 10px;
}

.drop-title {
    font-size: 13px;
    font-weight: 500;
    color: #4e5969;
    margin: 0 0 4px;
}

.drop-hint {
    font-size: 12px;
    color: #c0c4cc;
    margin: 0;
}

/* 激活按钮 */
.activate-btn {
    width: 100%;
    height: 48px;
    border: none;
    border-radius: 14px;
    background: linear-gradient(135deg, #165dff 0%, #722ed1 100%);
    color: #fff;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
    box-shadow: 0 8px 24px rgba(22, 93, 255, 0.35);

    &:hover:not(.disabled) {
        transform: translateY(-2px);
        box-shadow: 0 12px 32px rgba(22, 93, 255, 0.45);
    }

    &.disabled {
        opacity: 0.5;
        cursor: not-allowed;
        box-shadow: none;
    }
}

.btn-spinner {
    width: 18px;
    height: 18px;
    border: 2px solid rgba(255, 255, 255, 0.3);
    border-top-color: #fff;
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.error-toast {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    margin-top: 16px;
    padding: 10px 16px;
    background: rgba(245, 63, 63, 0.08);
    border: 1px solid rgba(245, 63, 63, 0.15);
    border-radius: 12px;
    color: #f53f3f;
    font-size: 13px;
}

.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.3s; }
.fade-slide-enter-from, .fade-slide-leave-to { opacity: 0; transform: translateY(-8px); }

/* ═══════════════ 已激活 — Bento 仪表盘 ═══════════════ */
.dash {
    display: flex;
    flex-direction: column;
    height: 100%;
    padding: 16px 20px;
    gap: 14px;
    overflow: hidden;
}

/* ── 上层：主信息卡 + 天数环卡 ── */
.dash-top {
    display: flex;
    gap: 14px;
    flex-shrink: 0;
}

/* 主信息卡（渐变背景） */
.hero {
    flex: 1;
    border-radius: 16px;
    padding: 18px 22px;
    display: flex;
    flex-direction: column;
    gap: 14px;

    &.banner-valid {
        background: linear-gradient(135deg, #edf4ff 0%, #f2eeff 100%);
        border: 1px solid rgba(22, 93, 255, 0.10);
    }

    &.banner-expired {
        background: linear-gradient(135deg, #fff2f0 0%, #ffe8e5 100%);
        border: 1px solid rgba(245, 63, 63, 0.10);
    }
}

.hero-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.hero-tag {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 3px 12px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 600;

    &.banner-valid {
        background: rgba(0, 180, 42, 0.12);
        color: #00b42a;
    }
    &.banner-expired {
        background: rgba(245, 63, 63, 0.12);
        color: #f53f3f;
    }
}

.hero-btns {
    display: flex;
    gap: 8px;
}

.hbtn {
    border-radius: 20px !important;
    font-weight: 600 !important;
    padding: 0 16px !important;
    height: 32px !important;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1) !important;

    &:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
    }
}

.hbtn-primary {
    background: linear-gradient(135deg, #165dff 0%, #722ed1 100%) !important;
    border: none !important;
    box-shadow: 0 3px 12px rgba(22, 93, 255, 0.35) !important;

    &:hover {
        box-shadow: 0 6px 20px rgba(22, 93, 255, 0.45) !important;
    }
}

.hbtn-default {
    background: rgba(255, 255, 255, 0.85) !important;
    border: 1px solid rgba(0, 0, 0, 0.08) !important;
    color: #4e5969 !important;
    backdrop-filter: blur(8px);

    &:hover {
        background: #fff !important;
        color: #165dff !important;
        border-color: rgba(22, 93, 255, 0.3) !important;
    }
}

.hbtn-danger {
    background: rgba(255, 255, 255, 0.85) !important;
    border: 1px solid rgba(245, 63, 63, 0.2) !important;
    color: #f53f3f !important;

    &:hover {
        background: rgba(245, 63, 63, 0.08) !important;
        border-color: rgba(245, 63, 63, 0.4) !important;
        box-shadow: 0 4px 12px rgba(245, 63, 63, 0.15) !important;
    }
}

.hero-main {
    padding: 2px 0;
}

.hero-name {
    font-size: 22px;
    font-weight: 800;
    color: #1d2129;
    margin: 0 0 6px;
    letter-spacing: -0.3px;
}

.hero-meta {
    display: flex;
    align-items: center;
    font-size: 12px;
    color: #86909c;

    code {
        font-family: 'JetBrains Mono', 'Consolas', monospace;
        font-size: 11px;
        background: rgba(0, 0, 0, 0.04);
        padding: 1px 6px;
        border-radius: 4px;
    }
}

.dot-sep {
    display: inline-block;
    width: 3px;
    height: 3px;
    border-radius: 50%;
    background: #c0c4cc;
    margin: 0 8px;
    font-style: normal;
}

.hero-dates {
    display: flex;
    align-items: center;
    padding-top: 12px;
    border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.hd-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.hd-label {
    font-size: 11px;
    color: #86909c;
}

.hd-val {
    font-size: 14px;
    font-weight: 600;
    color: #1d2129;

    &.hd-expired { color: #f53f3f; }
}

.hd-divider {
    width: 1px;
    height: 28px;
    background: rgba(0, 0, 0, 0.06);
    margin: 0 20px;
}

/* 天数环卡（暗色背景） */
.days-card {
    width: 190px;
    flex-shrink: 0;
    border-radius: 16px;
    background: linear-gradient(150deg, #1d2433 0%, #141821 100%);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 20px 16px;
    gap: 4px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.days-ring {
    position: relative;
    width: 110px;
    height: 110px;

    svg { width: 100%; height: 100%; }
}

.ring-animate {
    transition: stroke-dasharray 1s cubic-bezier(0.4, 0, 0.2, 1);
}

.days-center {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
}

.days-num {
    font-size: 30px;
    font-weight: 900;
    color: #fff;
    line-height: 1;

    &.healthy { color: #52c41a; }
    &.warning { color: #faad14; }
    &.expired { color: #ff4d4f; }
}

.days-unit {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.4);
    margin-top: 2px;
}

.days-label {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.4);
    margin-top: 4px;
}

.days-sub {
    font-size: 13px;
    font-weight: 700;

    &.healthy { color: #52c41a; }
    &.warning { color: #faad14; }
    &.expired { color: #ff4d4f; }
}

/* ── 下层三列 ── */
.dash-bottom {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 14px;
    flex: 1;
    min-height: 0;
}

.detail-card {
    background: #fff;
    border: 1px solid rgba(0, 0, 0, 0.06);
    border-radius: 14px;
    padding: 16px;
    display: flex;
    flex-direction: column;
    transition: box-shadow 0.3s ease;

    &:hover {
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
    }
}

.dc-head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 14px;
}

.dc-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;

    &.dc-blue { background: #165dff; }
    &.dc-green { background: #00b42a; }
    &.dc-orange { background: #ff7d00; }
}

.dc-title {
    font-size: 13px;
    font-weight: 700;
    color: #1d2129;
}

.dc-body {
    flex: 1;
    min-height: 0;
}

/* 键值对行 */
.kv-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 7px 0;

    & + & {
        border-top: 1px solid rgba(0, 0, 0, 0.04);
    }
}

.kv-label {
    font-size: 12px;
    color: #86909c;
    white-space: nowrap;
}

.kv-value {
    font-size: 12px;
    color: #1d2129;
    font-weight: 500;
    text-align: right;
    word-break: break-all;

    &.mono {
        font-family: 'JetBrains Mono', 'Consolas', monospace;
        font-size: 11px;
    }
}

/* 配额行 */
.quota-row {
    padding: 7px 0;
    & + & {
        border-top: 1px solid rgba(0, 0, 0, 0.04);
    }
}

.quota-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 6px;
}

.quota-name {
    font-size: 12px;
    color: #86909c;
}

.quota-num {
    font-size: 12px;
    font-weight: 600;
    color: #1d2129;
}

.quota-track {
    height: 5px;
    border-radius: 3px;
    background: rgba(0, 0, 0, 0.04);
    overflow: hidden;
}

.quota-fill {
    height: 100%;
    border-radius: 3px;
    background: linear-gradient(90deg, #165dff, #722ed1);
    transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 授权模块 */
.dc-features {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-content: flex-start;
}

.feat-chip {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 5px 14px;
    border-radius: 8px;
    background: linear-gradient(135deg, rgba(22, 93, 255, 0.06) 0%, rgba(114, 46, 209, 0.04) 100%);
    border: 1px solid rgba(22, 93, 255, 0.10);
    font-size: 12px;
    font-weight: 500;
    color: #165dff;
    transition: all 0.25s;

    &:hover {
        background: rgba(22, 93, 255, 0.10);
        transform: translateY(-1px);
    }
}

.feat-ok {
    font-size: 13px;
    color: #00b42a;
}

.feat-empty {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #c0c4cc;
    font-size: 12px;
}

/* 底部元信息 */
.dash-foot {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
    flex-wrap: wrap;
}

.foot-tag {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 4px 12px;
    background: rgba(0, 0, 0, 0.025);
    border-radius: 6px;
    font-size: 11px;
    color: #86909c;
}

.foot-path {
    font-family: 'JetBrains Mono', 'Consolas', monospace;
    font-size: 10px;
    color: #b0b5c0;
    user-select: all;
}

/* ═══════════════ 对比弹窗（紧凑双栏） ═══════════════ */
.cmp {
    padding: 0;
}

/* 自定义标题栏 */
.cmp-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    padding: 20px 24px 16px;
}

.cmp-title-row {
    display: flex;
    align-items: center;
    gap: 12px;
}

.cmp-title-icon {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    background: linear-gradient(135deg, #165dff 0%, #722ed1 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    flex-shrink: 0;
}

.cmp-title {
    font-size: 16px;
    font-weight: 700;
    color: #1d2129;
    margin: 0;
}

.cmp-subtitle {
    font-size: 12px;
    color: #86909c;
    margin: 2px 0 0;
}

.cmp-close {
    font-size: 16px;
    color: #86909c;
    cursor: pointer;
    padding: 4px;
    border-radius: 6px;
    transition: all 0.2s;

    &:hover {
        background: rgba(0, 0, 0, 0.06);
        color: #1d2129;
    }
}

/* 降级警告条 */
.cmp-warn {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 24px 12px;
    padding: 8px 14px;
    background: rgba(255, 125, 0, 0.06);
    border: 1px solid rgba(255, 125, 0, 0.15);
    border-radius: 8px;
    font-size: 12px;
    color: #ff7d00;
}

/* 双栏对比主体 */
.cmp-body {
    display: flex;
    gap: 0;
    padding: 0 24px;
    align-items: stretch;
}

.cmp-col {
    flex: 1;
    min-width: 0;
}

.cmp-arrow-col {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    flex-shrink: 0;
    color: #c0c4cc;
}

/* 栏标题 */
.cmp-col-head {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 700;
    padding: 8px 14px;
    border-radius: 10px;
    margin-bottom: 10px;
}

.cmp-cur {
    background: rgba(22, 93, 255, 0.06);
    color: #165dff;
}

.cmp-new {
    background: rgba(0, 180, 42, 0.06);
    color: #00b42a;
}

/* 内容卡 */
.cmp-card {
    background: rgba(0, 0, 0, 0.018);
    border-radius: 12px;
    padding: 14px;
}

.cmp-sec-title {
    font-size: 10px;
    font-weight: 700;
    color: #86909c;
    text-transform: uppercase;
    letter-spacing: 1.5px;
    margin-bottom: 8px;
}

.cmp-sec-gap {
    margin-top: 14px;
}

/* 键值行 */
.cmp-kv {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 5px 0;

    & + & {
        border-top: 1px solid rgba(0, 0, 0, 0.04);
    }
}

.cmp-k {
    font-size: 12px;
    color: #86909c;
}

.cmp-v {
    font-size: 12px;
    color: #1d2129;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 4px;

    &.cmp-changed {
        color: #165dff;
        font-weight: 700;
    }

    &.cmp-down {
        color: #f53f3f;
        font-weight: 700;
    }
}

/* 行级差异标记 */
.cmp-row-diff {
    background: rgba(22, 93, 255, 0.04);
    border-radius: 6px;
    margin: 0 -6px;
    padding: 5px 6px !important;
}

.cmp-mark {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    flex-shrink: 0;
    margin-left: 6px;
}

.cmp-mark-same {
    color: #c0c4cc;
}

.cmp-mark-diff {
    background: rgba(22, 93, 255, 0.12);
    color: #165dff;
}

.cmp-diff-icon {
    color: #165dff;
    flex-shrink: 0;
}

.cmp-dir-up {
    color: #00b42a;
    flex-shrink: 0;
}

.cmp-dir-down {
    color: #f53f3f;
    flex-shrink: 0;
}

/* 标签差异标记 */
.cmp-tag-added {
    font-weight: 600 !important;
}

.cmp-tag-removed {
    text-decoration: line-through;
    opacity: 0.7;
}

/* 标签 */
.cmp-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
}

.cmp-tag {
    border-radius: 6px !important;
    font-size: 11px !important;
}

.cmp-empty {
    font-size: 12px;
    color: #c0c4cc;
}

/* 底部操作栏 */
.cmp-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    padding: 16px 24px 20px;
}

.cmp-btn-cancel {
    border-radius: 20px !important;
    padding: 0 20px !important;
    height: 36px !important;
}

.cmp-btn-confirm {
    border-radius: 20px !important;
    padding: 0 24px !important;
    height: 36px !important;
    font-weight: 600 !important;
    background: linear-gradient(135deg, #165dff 0%, #722ed1 100%) !important;
    border: none !important;
    box-shadow: 0 3px 12px rgba(22, 93, 255, 0.3) !important;

    &:hover {
        box-shadow: 0 6px 20px rgba(22, 93, 255, 0.4) !important;
    }
}

/* ── 响应式 ── */
@media (max-width: 1024px) {
    .dash-top {
        flex-direction: column;
    }
    .days-card {
        width: 100%;
        flex-direction: row;
        gap: 20px;
        padding: 16px 24px;
    }
    .days-ring {
        width: 80px;
        height: 80px;
    }
    .days-num { font-size: 22px; }
}
@media (max-width: 768px) {
    .dash-bottom {
        grid-template-columns: 1fr;
    }
    .hero-bar {
        flex-direction: column;
        align-items: flex-start;
        gap: 8px;
    }
}
</style>

<!-- 全局样式：覆盖 Arco Modal 默认 body padding -->
<style lang="less">
.cmp-modal-wrap .arco-modal-body {
    padding: 0;
}
</style>
