<template>
  <div class="ops-page">
    <a-card class="query-card" :bordered="false">
      <a-form :model="queryParams" layout="inline">
        <a-form-item label="Key 匹配"><a-input v-model="queryParams.pattern" allow-clear placeholder="如 login_tokens:*" /></a-form-item>
        <a-form-item><a-space><a-button type="primary" @click="loadData"><template #icon><icon-search /></template>查询</a-button><a-button @click="handleReset"><template #icon><icon-refresh /></template>重置</a-button></a-space></a-form-item>
      </a-form>
    </a-card>
    <a-row :gutter="16" class="stat-row">
      <a-col :span="6"><a-card :bordered="false"><div class="stat-title">Key 数量</div><div class="stat-value">{{ overview?.dbSize || 0 }}</div></a-card></a-col>
      <a-col :span="6"><a-card :bordered="false"><div class="stat-title">Redis 版本</div><div class="stat-value">{{ overview?.redisVersion || '-' }}</div></a-card></a-col>
      <a-col :span="6"><a-card :bordered="false"><div class="stat-title">内存占用</div><div class="stat-value">{{ overview?.usedMemoryHuman || '-' }}</div></a-card></a-col>
      <a-col :span="6"><a-card :bordered="false"><div class="stat-title">客户端</div><div class="stat-value">{{ overview?.connectedClients || '-' }}</div></a-card></a-col>
    </a-row>
    <a-card class="table-card" :bordered="false">
      <template #title>缓存键列表</template>
      <template #extra><a-space><a-input v-model="clearPrefixValue" allow-clear placeholder="清理前缀" style="width: 180px" /><a-button status="danger" :disabled="!hasPermission('system:cache:clear')" @click="clearPrefixRows">按前缀清理</a-button></a-space></template>
      <a-table row-key="key" :loading="loading" :data="keyRows" :pagination="{ pageSize: 12 }">
        <template #columns><a-table-column title="Key" data-index="key" ellipsis tooltip /><a-table-column title="操作" :width="120" align="center"><template #cell="{ record }"><a-button size="mini" type="text" status="danger" :disabled="!hasPermission('system:cache:remove')" @click="deleteRow(record.key)">删除</a-button></template></a-table-column></template>
      </a-table>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { useButtonPermission } from '../../../../composables/useButtonPermission';
import { clearCachePrefix, deleteCacheKey, fetchCacheOverview, type CacheVO } from '../../../../api/system';

const { hasPermission } = useButtonPermission();
const loading = ref(false);
const overview = ref<CacheVO | null>(null);
const clearPrefixValue = ref('');
const queryParams = reactive({ pattern: '*' });
const keyRows = computed(() => (overview.value?.keys || []).map(key => ({ key })));
async function loadData() { loading.value = true; try { const res = await fetchCacheOverview({ pattern: queryParams.pattern || '*' }) as any; overview.value = res.data; } finally { loading.value = false; } }
function handleReset() { queryParams.pattern = '*'; loadData(); }
function deleteRow(key: string) { Modal.warning({ title: '确认删除缓存', content: `确定删除 Key：${key}？`, hideCancel: false, async onOk() { await deleteCacheKey(key); Message.success('删除成功'); await loadData(); } }); }
function clearPrefixRows() { if (!clearPrefixValue.value) { Message.warning('请输入清理前缀'); return; } Modal.warning({ title: '确认清理缓存', content: `确定清理前缀“${clearPrefixValue.value}*”下的缓存？`, hideCancel: false, async onOk() { await clearCachePrefix(clearPrefixValue.value); Message.success('清理成功'); await loadData(); } }); }
onMounted(loadData);
</script>

<style scoped>
.ops-page { padding: 18px; min-height: 100%; background: linear-gradient(180deg, #f8fbff 0%, #f3f7fb 100%); }
.query-card, .table-card, .stat-row :deep(.arco-card) { border-radius: 16px; }
.query-card, .stat-row { margin-bottom: 16px; }
:deep(.arco-table-th) { background: #f5f8ff; font-weight: 700; }
.stat-title { color: #6b778c; font-size: 13px; }
.stat-value { margin-top: 8px; color: #1f2d3d; font-size: 22px; font-weight: 700; }
</style>
