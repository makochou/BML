<template>
  <div class="app-container">
    <a-card class="general-card" title="应用接入管理">
      <template #extra>
        <a-button type="primary" status="success" @click="openCreateModal">
          <template #icon><icon-plus /></template>
          新建应用
        </a-button>
      </template>

      <a-table :data="appList" :loading="loading" :pagination="false">
        <template #columns>
          <a-table-column title="应用名称" data-index="name" />
          <a-table-column title="AppKey (应用ID)" data-index="appId">
             <template #cell="{ record }">
                <a-typography-text copyable>{{ record.appId }}</a-typography-text>
             </template>
          </a-table-column>
          <a-table-column title="AppSecret (密钥)" data-index="appSecret">
             <template #cell="{ record }">
                <a-typography-text v-if="record.showSecret" copyable>{{ record.appSecret }}</a-typography-text>
                <span v-else>******</span>
                <a-button type="text" size="mini" @click="record.showSecret = !record.showSecret">
                    {{ record.showSecret ? '隐藏' : '显示' }}
                </a-button>
             </template>
          </a-table-column>
          <a-table-column title="状态" data-index="status">
            <template #cell="{ record }">
              <a-tag :color="record.status === 1 ? 'green' : 'red'">
                {{ record.status === 1 ? '正常' : '停用' }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="操作">
            <template #cell="{ record }">
              <a-space>
                <a-popconfirm content="确定要重置密钥吗？旧密钥将立即失效！" @ok="handleReset(record)">
                    <a-button type="text" status="warning" size="small">重置密钥</a-button>
                </a-popconfirm>
                <a-popconfirm content="确定要删除该应用吗？" @ok="handleDelete(record)">
                    <a-button type="text" status="danger" size="small">删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </a-table-column>
        </template>
      </a-table>
    </a-card>

    <!-- 新建应用弹窗 -->
    <a-modal v-model:visible="visible" title="新建应用" @ok="handleCreate">
      <a-form :model="form">
        <a-form-item field="name" label="应用名称" required>
          <a-input v-model="form.name" placeholder="请输入应用名称，如：CRM系统" />
        </a-form-item>
        <a-form-item field="remark" label="备注">
          <a-textarea v-model="form.remark" placeholder="请输入用途说明" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconPlus } from '@arco-design/web-vue/es/icon';
import request from '../../utils/request';

const loading = ref(false);
const visible = ref(false);
const appList = ref<any[]>([]);
const form = reactive({ name: '', remark: '' });

const fetchList = async () => {
    loading.value = true;
    try {
        const { data } = await request.get('/api/app/list');
        appList.value = data.map((item: any) => ({ ...item, showSecret: false }));
    } finally {
        loading.value = false;
    }
};

const openCreateModal = () => {
    form.name = '';
    form.remark = '';
    visible.value = true;
};

const handleCreate = async () => {
    if (!form.name) {
        Message.warning('请输入应用名称');
        return false;
    }
    await request.post('/api/app', form);
    Message.success('创建成功');
    visible.value = false;
    fetchList();
};

const handleReset = async (record: any) => {
    await request.put(`/api/app/${record.id}/reset`);
    Message.success('密钥重置成功');
    fetchList();
};

const handleDelete = async (record: any) => {
    await request.delete(`/api/app/${record.id}`);
    Message.success('删除成功');
    fetchList();
};

onMounted(fetchList);
</script>

<style scoped>
.app-container {
    padding: 20px;
}
</style>
