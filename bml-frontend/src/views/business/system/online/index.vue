<template>
  <div class="ops-page">
    <a-card class="query-card" :bordered="false">
      <a-form :model="queryParams" layout="inline">
        <a-form-item label="用户名"><a-input v-model="queryParams.username" allow-clear placeholder="请输入用户名" /></a-form-item>
        <a-form-item><a-space><a-button type="primary" @click="loadData"><template #icon><icon-search /></template>查询</a-button><a-button @click="handleReset"><template #icon><icon-refresh /></template>重置</a-button></a-space></a-form-item>
      </a-form>
    </a-card>
    <a-card class="table-card" :bordered="false">
      <template #title>在线用户</template>
      <template #extra><a-button @click="loadData"><template #icon><icon-refresh /></template>刷新</a-button></template>
      <a-table row-key="userKey" :loading="loading" :data="tableData" :pagination="false">
        <template #columns>
          <a-table-column title="用户名" data-index="username" :width="180" />
          <a-table-column title="用户ID" data-index="userId" :width="140" />
          <a-table-column title="登录时间" data-index="loginTime" :width="180" />
          <a-table-column title="过期时间" data-index="expireTime" :width="180" />
          <a-table-column title="剩余秒数" data-index="ttlSeconds" :width="120" />
          <a-table-column title="会话标识" data-index="userKey" ellipsis tooltip />
          <a-table-column title="操作" :width="120" align="center" fixed="right"><template #cell="{ record }"><a-button size="mini" type="text" status="danger" v-if="hasPermission('system:online:forceLogout')" @click="kickRow(record)">强退</a-button></template></a-table-column>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { useButtonPermission } from '../../../../composables/useButtonPermission';
import { fetchOnlineUsers, forceLogoutOnlineUser, type OnlineUserVO } from '../../../../api/system';

const { hasPermission } = useButtonPermission();
const loading = ref(false);
const tableData = ref<OnlineUserVO[]>([]);
const queryParams = reactive({ username: '' });
async function loadData() { loading.value = true; try { const res = await fetchOnlineUsers({ username: queryParams.username || undefined }) as any; tableData.value = res.data || []; } finally { loading.value = false; } }
function handleReset() { queryParams.username = ''; loadData(); }
function kickRow(record: OnlineUserVO) { Modal.warning({ title: '确认强制下线', content: `确定强制“${record.username}”下线吗？`, hideCancel: false, async onOk() { await forceLogoutOnlineUser(record.userKey); Message.success('已强制下线'); await loadData(); } }); }
onMounted(loadData);
</script>

<style scoped>
.ops-page { padding: 18px; min-height: 100%; background: linear-gradient(180deg, #f8fbff 0%, #f3f7fb 100%); }
.query-card { margin-bottom: 16px; border-radius: 16px; }
.table-card { border-radius: 16px; }
:deep(.arco-table-th) { background: #f5f8ff; font-weight: 700; }
</style>
