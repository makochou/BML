<template>
  <div class="page-wrapper audit-setting-page">
    <GovernanceCompactQueryPanel
      eyebrow="LOG LIFECYCLE"
      title="日志设置 / 归档策略"
      description="配置审计日志在线保留周期、自动归档、冷存储位置与自动清理策略，支撑等保与合规审计。"
      density="ultra"
      theme="aurora"
      :meta-items="metaItems"
    >
      <template #actions>
        <a-button :loading="loading" @click="loadSetting"><template #icon><icon-refresh /></template>刷新</a-button>
        <a-button type="primary" :loading="saving" @click="handleSave"><template #icon><icon-save /></template>保存策略</a-button>
      </template>
    </GovernanceCompactQueryPanel>

    <a-row :gutter="16" class="setting-layout">
      <a-col :xs="24" :lg="14">
        <a-card class="setting-card" :bordered="false" title="归档策略配置">
          <a-form :model="form" layout="vertical">
            <a-form-item label="在线保留天数" field="onlineRetentionDays">
              <a-input-number v-model="form.onlineRetentionDays" :min="1" :max="3650" mode="button" class="setting-input" />
            </a-form-item>
            <a-form-item label="自动归档" field="archiveEnabled">
              <a-switch v-model="form.archiveEnabled"><template #checked>启用</template><template #unchecked>关闭</template></a-switch>
            </a-form-item>
            <a-form-item label="归档存储位置" field="archiveStorage">
              <a-input v-model="form.archiveStorage" placeholder="local://data/audit-archive 或对象存储路径" allow-clear />
            </a-form-item>
            <a-form-item label="超过保留周期自动清理" field="autoCleanEnabled">
              <a-switch v-model="form.autoCleanEnabled"><template #checked>启用</template><template #unchecked>关闭</template></a-switch>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="10">
        <a-card class="setting-card strategy-card" :bordered="false" title="生命周期说明">
          <a-timeline>
            <a-timeline-item label="实时写入">登录、操作、异常与风险事件进入在线审计数据。</a-timeline-item>
            <a-timeline-item label="在线查询">近 {{ form.onlineRetentionDays }} 天保留在在线库中，支持快速检索与详情穿透。</a-timeline-item>
            <a-timeline-item label="冷归档">{{ form.archiveEnabled ? '自动归档已启用，历史日志将写入冷存储。' : '自动归档未启用，可先手动导出留档。' }}</a-timeline-item>
            <a-timeline-item label="安全清理">{{ form.autoCleanEnabled ? '超过保留周期后允许自动清理。' : '自动清理未启用，保留人工确认机制。' }}</a-timeline-item>
          </a-timeline>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconRefresh, IconSave } from '@arco-design/web-vue/es/icon';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import { fetchAuditArchiveSetting, saveAuditArchiveSetting, type AuditArchiveSettingVO } from '../../../../api/system';

const loading = ref(false);
const saving = ref(false);
const form = reactive<AuditArchiveSettingVO>({
  onlineRetentionDays: 180,
  archiveEnabled: false,
  archiveStorage: 'local://data/audit-archive',
  autoCleanEnabled: false,
});

const metaItems = computed(() => [
  { label: '在线保留', value: `${form.onlineRetentionDays}天`, tone: 'blue' as const },
  { label: '自动归档', value: form.archiveEnabled ? '已启用' : '未启用', tone: form.archiveEnabled ? 'green' as const : 'gold' as const },
  { label: '自动清理', value: form.autoCleanEnabled ? '已启用' : '未启用', tone: form.autoCleanEnabled ? 'green' as const : 'gold' as const },
]);

const loadSetting = async () => {
  loading.value = true;
  try {
    const res = await fetchAuditArchiveSetting() as any;
    Object.assign(form, res.data || {});
  } finally {
    loading.value = false;
  }
};

const handleSave = async () => {
  saving.value = true;
  try {
    await saveAuditArchiveSetting({ ...form });
    Message.success('审计归档策略已保存');
    await loadSetting();
  } finally {
    saving.value = false;
  }
};

onMounted(loadSetting);
</script>

<style scoped>
.audit-setting-page { min-height: 100%; }
.setting-layout { margin-top: 16px; }
.setting-card { min-height: 420px; border-radius: 18px; background: rgba(255,255,255,0.92); box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08); }
.setting-input { width: 240px; }
.strategy-card :deep(.arco-timeline-item-content) { color: var(--color-text-2); line-height: 1.7; }
</style>
