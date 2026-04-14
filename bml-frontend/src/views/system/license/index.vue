<template>
    <div class="license-page">
        <!-- 许可证未激活状态 -->
        <div v-if="!licenseData?.activated" class="license-activation">
            <div class="activation-card">
                <div class="activation-header">
                    <div class="icon-wrapper">
                        <icon-lock :size="48" />
                    </div>
                    <h1 class="activation-title">系统许可证激活</h1>
                    <p class="activation-subtitle">
                        请上传有效的许可证文件 (.lic) 以激活系统
                    </p>
                </div>

                <div class="upload-area">
                    <a-upload
                        :auto-upload="false"
                        :limit="1"
                        accept=".lic"
                        draggable
                        :file-list="fileList"
                        @change="handleFileChange"
                    >
                        <template #upload-button>
                            <div class="upload-trigger">
                                <icon-upload :size="32" />
                                <p class="upload-text">点击或拖拽许可证文件到此处</p>
                                <p class="upload-hint">仅支持 .lic 格式的许可证文件</p>
                            </div>
                        </template>
                    </a-upload>
                </div>

                <a-button
                    type="primary"
                    size="large"
                    long
                    :loading="uploading"
                    :disabled="!selectedFile"
                    class="upload-btn"
                    @click="handleUpload"
                >
                    <template #icon><icon-safe /></template>
                    上传并激活许可证
                </a-button>

                <div v-if="errorMsg" class="error-message">
                    <icon-exclamation-circle />
                    <span>{{ errorMsg }}</span>
                </div>
            </div>
        </div>

        <!-- 许可证已激活状态 -->
        <div v-else class="license-dashboard">
            <div class="dashboard-header">
                <div class="header-content">
                    <h2 class="page-title">
                        <icon-safe class="title-icon" />
                        许可证管理
                    </h2>
                    <a-tag v-if="licenseData?.expired" color="red" size="large">
                        <template #icon><icon-exclamation-circle /></template>
                        已过期
                    </a-tag>
                    <a-tag v-else color="green" size="large">
                        <template #icon><icon-check-circle /></template>
                        有效
                    </a-tag>
                </div>
            </div>

            <!-- 许可证信息卡片 -->
            <div class="info-grid">
                <div class="info-card primary">
                    <div class="card-header">
                        <icon-idcard class="card-icon" />
                        <span>基本信息</span>
                    </div>
                    <div class="card-body">
                        <div class="info-row">
                            <span class="label">许可证 ID</span>
                            <span class="value mono">{{ licenseData?.licenseId || '-' }}</span>
                        </div>
                        <div class="info-row">
                            <span class="label">客户名称</span>
                            <span class="value">{{ licenseData?.customerName || '-' }}</span>
                        </div>
                        <div class="info-row">
                            <span class="label">客户编码</span>
                            <span class="value mono">{{ licenseData?.customerCode || '-' }}</span>
                        </div>
                        <div class="info-row">
                            <span class="label">产品版本</span>
                            <span class="value">{{ licenseData?.productVersion || '-' }}</span>
                        </div>
                    </div>
                </div>

                <div class="info-card">
                    <div class="card-header">
                        <icon-calendar class="card-icon" />
                        <span>有效期</span>
                    </div>
                    <div class="card-body">
                        <div class="info-row">
                            <span class="label">签发日期</span>
                            <span class="value">{{ licenseData?.issueDate || '-' }}</span>
                        </div>
                        <div class="info-row">
                            <span class="label">到期日期</span>
                            <span class="value" :class="{ expired: licenseData?.expired }">
                                {{ licenseData?.expireDate || '-' }}
                            </span>
                        </div>
                        <div class="info-row">
                            <span class="label">剩余天数</span>
                            <span class="value" :class="daysLeftClass">{{ daysLeftText }}</span>
                        </div>
                    </div>
                </div>

                <div class="info-card">
                    <div class="card-header">
                        <icon-apps class="card-icon" />
                        <span>配额限制</span>
                    </div>
                    <div class="card-body">
                        <div class="info-row">
                            <span class="label">最大 API 账号</span>
                            <span class="value">{{ formatQuota(licenseData?.maxApiAccounts) }}</span>
                        </div>
                        <div class="info-row">
                            <span class="label">单账号用户数</span>
                            <span class="value">{{ formatQuota(licenseData?.maxUsersPerAccount) }}</span>
                        </div>
                        <div class="info-row">
                            <span class="label">全局用户数</span>
                            <span class="value">{{ formatQuota(licenseData?.maxTotalUsers) }}</span>
                        </div>
                    </div>
                </div>

                <div class="info-card">
                    <div class="card-header">
                        <icon-layers class="card-icon" />
                        <span>授权模块</span>
                    </div>
                    <div class="card-body features-body">
                        <a-tag
                            v-for="feature in licenseData?.features || []"
                            :key="feature"
                            color="arcoblue"
                            class="feature-tag"
                        >
                            {{ featureLabel(feature) }}
                        </a-tag>
                        <span v-if="!licenseData?.features?.length" class="empty-text">无</span>
                    </div>
                </div>
            </div>

            <!-- 备注 -->
            <div v-if="licenseData?.remark" class="remark-section">
                <icon-file class="remark-icon" />
                <span>{{ licenseData.remark }}</span>
            </div>

            <!-- 文件路径提示 -->
            <div v-if="licenseData?.filePath" class="file-path-section">
                <icon-folder class="file-path-icon" />
                <span class="file-path-label">许可证文件路径：</span>
                <code class="file-path-value">{{ licenseData.filePath }}</code>
            </div>

            <!-- 操作区域 -->
            <div class="action-section">
                <a-upload
                    :auto-upload="false"
                    :limit="1"
                    :show-file-list="false"
                    accept=".lic"
                    @change="handleReplaceFile"
                >
                    <template #upload-button>
                        <a-button type="primary" :loading="previewing">
                            <template #icon><icon-upload /></template>
                            更新许可证
                        </a-button>
                    </template>
                </a-upload>
                <a-button @click="loadLicenseStatus">
                    <template #icon><icon-refresh /></template>
                    刷新状态
                </a-button>
                <a-popconfirm
                    content="确定要删除许可证并重置系统吗？系统将回到未激活状态。"
                    type="warning"
                    @ok="handleReset"
                >
                    <a-button status="danger" :loading="resetting">
                        <template #icon><icon-delete /></template>
                        重置许可证
                    </a-button>
                </a-popconfirm>
            </div>
        </div>

        <!-- 许可证对比弹窗 -->
        <a-modal
            v-model:visible="compareVisible"
            title="许可证更新确认"
            :width="760"
            :mask-closable="false"
            :footer="false"
        >
            <div class="compare-container">
                <!-- 降级警告 -->
                <a-alert
                    v-if="downgradeWarnings.length > 0"
                    type="warning"
                    class="compare-alert"
                >
                    <template #title>检测到许可证降级</template>
                    <div>新许可证的授权范围小于当前许可证，以下将受到限制：</div>
                    <ul class="downgrade-list">
                        <li v-for="(warn, idx) in downgradeWarnings" :key="idx">{{ warn }}</li>
                    </ul>
                    <div class="downgrade-note">已有数据将保留，但相关功能的新建操作将被限制。</div>
                </a-alert>

                <!-- 对比表格 -->
                <div class="compare-grid">
                    <div class="compare-col">
                        <div class="compare-col-title current-title">
                            <icon-safe /> 当前许可证
                        </div>
                    </div>
                    <div class="compare-col">
                        <div class="compare-col-title new-title">
                            <icon-upload /> 新许可证
                        </div>
                    </div>
                </div>

                <div class="compare-section">
                    <div class="compare-section-title">基本信息</div>
                    <div v-for="field in basicFields" :key="field.key" class="compare-row">
                        <span class="compare-label">{{ field.label }}</span>
                        <span class="compare-value">{{ field.current }}</span>
                        <span class="compare-arrow">
                            <icon-arrow-right v-if="field.current !== field.next" />
                            <icon-minus v-else />
                        </span>
                        <span class="compare-value" :class="{ changed: field.current !== field.next }">
                            {{ field.next }}
                        </span>
                    </div>
                </div>

                <div class="compare-section">
                    <div class="compare-section-title">配额限制</div>
                    <div v-for="field in quotaFields" :key="field.key" class="compare-row">
                        <span class="compare-label">{{ field.label }}</span>
                        <span class="compare-value">{{ field.current }}</span>
                        <span class="compare-arrow" :class="quotaChangeClass(field)">
                            <icon-arrow-up v-if="field.direction === 'up'" />
                            <icon-arrow-down v-else-if="field.direction === 'down'" />
                            <icon-minus v-else />
                        </span>
                        <span class="compare-value" :class="{ changed: field.current !== field.next, downgrade: field.direction === 'down' }">
                            {{ field.next }}
                        </span>
                    </div>
                </div>

                <div class="compare-section">
                    <div class="compare-section-title">授权模块</div>
                    <div class="compare-features">
                        <div class="compare-feature-col">
                            <a-tag v-for="f in licenseData?.features || []" :key="f" color="arcoblue" class="feature-tag">{{ featureLabel(f) }}</a-tag>
                            <span v-if="!licenseData?.features?.length" class="empty-text">无</span>
                        </div>
                        <div class="compare-feature-arrow">
                            <icon-arrow-right />
                        </div>
                        <div class="compare-feature-col">
                            <a-tag
                                v-for="f in previewData?.features || []"
                                :key="f"
                                :color="isFeatureAdded(f) ? 'green' : 'arcoblue'"
                                class="feature-tag"
                            >{{ featureLabel(f) }}<template v-if="isFeatureAdded(f)"> (新增)</template></a-tag>
                            <a-tag
                                v-for="f in removedFeatures"
                                :key="'rm-' + f"
                                color="red"
                                class="feature-tag"
                            >{{ featureLabel(f) }} (移除)</a-tag>
                            <span v-if="!previewData?.features?.length && !removedFeatures.length" class="empty-text">无</span>
                        </div>
                    </div>
                </div>
            </div>

            <div class="compare-footer">
                <a-button @click="compareVisible = false">取消</a-button>
                <a-button type="primary" :loading="uploading" @click="confirmUpdate">
                    <template #icon><icon-check-circle /></template>
                    确认更新
                </a-button>
            </div>
        </a-modal>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import type { FileItem } from '@arco-design/web-vue';
import { fetchLicenseStatus, previewLicense, uploadLicense, resetLicense, type LicenseStatus } from '../../../api/license';
import { resetLicenseCache } from '../../../router';

const licenseData = ref<LicenseStatus | null>(null);
const uploading = ref(false);
const previewing = ref(false);
const resetting = ref(false);
const errorMsg = ref('');
const fileList = ref<FileItem[]>([]);
const selectedFile = ref<File | null>(null);

/** 许可证对比弹窗 */
const compareVisible = ref(false);
const previewData = ref<LicenseStatus | null>(null);
const pendingFile = ref<File | null>(null);

/** 功能模块中文标签映射 */
const FEATURE_LABELS: Record<string, string> = {
    api_gateway: 'API 网关',
    enterprise: '企业管理',
    system: '系统管理',
    monitor: '系统监控',
    alert: '告警通知'
};

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
    } finally {
        previewing.value = false;
    }
};

/** 确认更新许可证 */
const confirmUpdate = async () => {
    if (!pendingFile.value) return;
    uploading.value = true;

    try {
        const res = await uploadLicense(pendingFile.value) as any;
        licenseData.value = res.data;
        resetLicenseCache();
        compareVisible.value = false;
        pendingFile.value = null;
        previewData.value = null;
        Message.success('许可证更新成功');
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
        { key: 'maxTotalUsers', label: '全局用户数', currentRaw: licenseData.value?.maxTotalUsers, nextRaw: previewData.value?.maxTotalUsers },
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

const quotaChangeClass = (field: { direction: string }) => {
    if (field.direction === 'up') return 'quota-up';
    if (field.direction === 'down') return 'quota-down';
    return '';
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
.license-page {
    min-height: 100%;
    background: linear-gradient(135deg, #f0f4ff 0%, #e8ecf8 100%);
}

/* ── 未激活状态 ── */
.license-activation {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    padding: 24px;
}

.activation-card {
    background: #fff;
    border-radius: 20px;
    padding: 48px 40px;
    max-width: 520px;
    width: 100%;
    box-shadow: 0 8px 40px rgba(22, 93, 255, 0.08),
                0 2px 12px rgba(0, 0, 0, 0.04);
    text-align: center;
}

.activation-header {
    margin-bottom: 32px;
}

.icon-wrapper {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 88px;
    height: 88px;
    border-radius: 50%;
    background: linear-gradient(135deg, #165dff 0%, #4080ff 100%);
    color: #fff;
    margin-bottom: 20px;
    box-shadow: 0 8px 24px rgba(22, 93, 255, 0.3);
}

.activation-title {
    font-size: 24px;
    font-weight: 700;
    color: #1d2129;
    margin: 0 0 8px;
}

.activation-subtitle {
    font-size: 14px;
    color: #86909c;
    margin: 0;
}

.upload-area {
    margin-bottom: 24px;
}

.upload-trigger {
    padding: 40px 24px;
    border: 2px dashed #c9cdd4;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.3s;
    background: #fafbfc;

    &:hover {
        border-color: #165dff;
        background: #f0f4ff;
    }
}

.upload-text {
    font-size: 14px;
    color: #4e5969;
    margin: 12px 0 4px;
}

.upload-hint {
    font-size: 12px;
    color: #c0c4cc;
    margin: 0;
}

.upload-btn {
    height: 48px;
    font-size: 16px;
    border-radius: 10px;
    background: linear-gradient(135deg, #165dff 0%, #4080ff 100%);
}

.error-message {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    margin-top: 16px;
    padding: 12px 16px;
    background: #fff2f0;
    border-radius: 8px;
    color: #f53f3f;
    font-size: 13px;
}

/* ── 已激活状态 ── */
.license-dashboard {
    padding: 24px;
    max-width: 1200px;
    margin: 0 auto;
}

.dashboard-header {
    margin-bottom: 24px;
}

.header-content {
    display: flex;
    align-items: center;
    gap: 12px;
}

.page-title {
    font-size: 22px;
    font-weight: 700;
    color: #1d2129;
    margin: 0;
    display: flex;
    align-items: center;
    gap: 8px;
}

.title-icon {
    color: #165dff;
}

.info-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
    margin-bottom: 24px;
}

.info-card {
    background: #fff;
    border-radius: 16px;
    padding: 24px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
    transition: box-shadow 0.3s;

    &:hover {
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    }

    &.primary {
        border-left: 4px solid #165dff;
    }
}

.card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 600;
    color: #1d2129;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f2f3f5;
}

.card-icon {
    color: #165dff;
    font-size: 18px;
}

.info-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;

    & + & {
        border-top: 1px dashed #f2f3f5;
    }
}

.label {
    font-size: 13px;
    color: #86909c;
}

.value {
    font-size: 14px;
    color: #1d2129;
    font-weight: 500;

    &.mono {
        font-family: 'JetBrains Mono', 'Consolas', monospace;
        font-size: 13px;
    }

    &.expired {
        color: #f53f3f;
        font-weight: 600;
    }

    &.warning {
        color: #ff7d00;
        font-weight: 600;
    }

    &.healthy {
        color: #00b42a;
        font-weight: 600;
    }
}

.features-body {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.feature-tag {
    border-radius: 6px;
    font-size: 13px;
}

.empty-text {
    color: #c0c4cc;
    font-size: 13px;
}

.remark-section {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 16px 20px;
    background: #fff;
    border-radius: 12px;
    margin-bottom: 24px;
    font-size: 13px;
    color: #4e5969;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.remark-icon {
    color: #86909c;
    flex-shrink: 0;
    margin-top: 2px;
}

.file-path-section {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: #fff;
    border-radius: 10px;
    margin-bottom: 16px;
    font-size: 13px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.file-path-icon {
    color: #86909c;
    flex-shrink: 0;
}

.file-path-label {
    color: #86909c;
    white-space: nowrap;
}

.file-path-value {
    font-family: 'JetBrains Mono', 'Consolas', monospace;
    font-size: 12px;
    color: #165dff;
    background: #f0f4ff;
    padding: 2px 8px;
    border-radius: 4px;
    word-break: break-all;
    user-select: all;
}

.action-section {
    display: flex;
    gap: 12px;
}

/* ── 对比弹窗 ── */
.compare-container {
    padding: 0 4px;
}

.compare-alert {
    margin-bottom: 20px;
}

.downgrade-list {
    margin: 8px 0 4px 16px;
    padding: 0;
    li {
        font-size: 13px;
        color: #ff7d00;
        line-height: 1.8;
    }
}

.downgrade-note {
    font-size: 12px;
    color: #86909c;
    margin-top: 4px;
}

.compare-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    margin-bottom: 8px;
}

.compare-col-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 15px;
    font-weight: 600;
    padding: 12px 16px;
    border-radius: 10px;
}

.current-title {
    background: #f0f4ff;
    color: #165dff;
}

.new-title {
    background: #e8f5e9;
    color: #00b42a;
}

.compare-section {
    background: #fafbfc;
    border-radius: 12px;
    padding: 16px;
    margin-bottom: 12px;
}

.compare-section-title {
    font-size: 13px;
    font-weight: 600;
    color: #86909c;
    margin-bottom: 12px;
    text-transform: uppercase;
    letter-spacing: 1px;
}

.compare-row {
    display: grid;
    grid-template-columns: 100px 1fr 32px 1fr;
    align-items: center;
    gap: 8px;
    padding: 8px 0;

    & + & {
        border-top: 1px dashed #f2f3f5;
    }
}

.compare-label {
    font-size: 13px;
    color: #86909c;
}

.compare-value {
    font-size: 13px;
    color: #1d2129;
    font-weight: 500;

    &.changed {
        color: #165dff;
        font-weight: 600;
    }

    &.downgrade {
        color: #f53f3f;
        font-weight: 600;
    }
}

.compare-arrow {
    text-align: center;
    color: #c0c4cc;
    font-size: 14px;

    &.quota-up {
        color: #00b42a;
    }

    &.quota-down {
        color: #f53f3f;
    }
}

.compare-features {
    display: grid;
    grid-template-columns: 1fr 32px 1fr;
    align-items: flex-start;
    gap: 12px;
}

.compare-feature-col {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
}

.compare-feature-arrow {
    text-align: center;
    color: #c0c4cc;
    padding-top: 4px;
}

.compare-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 20px;
    padding-top: 16px;
    border-top: 1px solid #f2f3f5;
}

/* 响应式 */
@media (max-width: 768px) {
    .info-grid {
        grid-template-columns: 1fr;
    }
    .compare-row {
        grid-template-columns: 80px 1fr 24px 1fr;
    }
}
</style>
