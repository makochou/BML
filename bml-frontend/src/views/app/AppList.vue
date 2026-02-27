<template>
  <div class="app-container">
    <a-card class="general-card" title="应用接入管理">
      <template #extra>
        <a-button type="primary" status="success" @click="openCreateModal">
          <template #icon><icon-plus /></template>
          新建应用
        </a-button>
      </template>

      <div class="secret-notice">
        Secret 只会在创建或重置时展示一次，列表和详情不再返回明文密钥。
      </div>

      <a-table :data="appList" :loading="loading" :pagination="false">
        <template #columns>
          <a-table-column title="应用名称" data-index="accountName" />
          <a-table-column title="AppKey" data-index="accessKey">
             <template #cell="{ record }">
                <a-typography-text copyable>{{ record.accessKey }}</a-typography-text>
             </template>
          </a-table-column>
          <a-table-column title="状态" data-index="status">
            <template #cell="{ record }">
              <a-tag :color="record.status === 1 ? 'green' : 'red'">
                {{ record.status === 1 ? '正常' : '停用' }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="备注" data-index="remark" />
          <a-table-column title="创建时间" data-index="createTime" />
          <a-table-column title="操作">
            <template #cell="{ record }">
              <a-space>
                <a-popconfirm content="确定要重置密钥吗？旧密钥会立即失效。" @ok="handleReset(record)">
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

    <a-modal v-model:visible="createVisible" title="新建应用" @ok="handleCreate">
      <a-form :model="form">
        <a-form-item field="name" label="应用名称" required>
          <a-input v-model="form.name" placeholder="请输入应用名称，例如：CRM系统" />
        </a-form-item>
        <a-form-item field="remark" label="备注">
          <a-textarea v-model="form.remark" placeholder="请输入用途说明" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:visible="credentialVisible" title="请立即保存新凭证" :footer="false">
      <div class="credential-panel" v-if="latestCredential">
        <div class="credential-row">
          <span class="label">应用名称</span>
          <span>{{ latestCredential.accountName }}</span>
        </div>
        <div class="credential-row">
          <span class="label">AppKey</span>
          <a-typography-text copyable>{{ latestCredential.accessKey }}</a-typography-text>
        </div>
        <div class="credential-row secret-row">
          <span class="label">AppSecret</span>
          <a-typography-text copyable>{{ latestCredential.secretKey }}</a-typography-text>
        </div>
        <div class="credential-actions">
          <a-button type="primary" long @click="credentialVisible = false">我已保存</a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconPlus } from '@arco-design/web-vue/es/icon';
import request from '../../utils/request';

type AppRecord = {
    id: number;
    accountName: string;
    accessKey: string;
    status: number;
    remark?: string;
    createTime?: string;
};

type CredentialPayload = {
    id: number;
    accountName: string;
    accessKey: string;
    secretKey: string;
};

const loading = ref(false);
const createVisible = ref(false);
const credentialVisible = ref(false);
const appList = ref<AppRecord[]>([]);
const latestCredential = ref<CredentialPayload | null>(null);
const form = reactive({ name: '', remark: '' });

const fetchList = async () => {
    loading.value = true;
    try {
        const { data } = await request.get('/api/account/list');
        appList.value = data;
    } finally {
        loading.value = false;
    }
};

const openCreateModal = () => {
    form.name = '';
    form.remark = '';
    createVisible.value = true;
};

const showCredentialModal = (payload: CredentialPayload) => {
    latestCredential.value = payload;
    credentialVisible.value = true;
};

const handleCreate = async () => {
    if (!form.name) {
        Message.warning('请输入应用名称');
        return false;
    }
    const { data } = await request.post('/api/account/add', { accountName: form.name, remark: form.remark });
    Message.success('创建成功');
    createVisible.value = false;
    showCredentialModal(data);
    fetchList();
    return true;
};

const handleReset = async (record: AppRecord) => {
    const { data } = await request.put(`/api/account/${record.id}/reset`);
    Message.success('密钥重置成功');
    showCredentialModal(data);
    fetchList();
};

const handleDelete = async (record: AppRecord) => {
    await request.delete(`/api/account/remove/${record.id}`);
    Message.success('删除成功');
    fetchList();
};

onMounted(fetchList);
</script>

<style scoped>
.app-container {
    padding: 16px;
}

.general-card {
    border-radius: 4px;
    border: none;
}

.secret-notice {
    margin-bottom: 16px;
    padding: 12px 16px;
    border-radius: 8px;
    background: rgba(255, 196, 0, 0.12);
    color: #7a4a00;
    font-size: 13px;
}

.credential-panel {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.credential-row {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.credential-row .label {
    color: var(--color-text-3);
    font-size: 12px;
}

.secret-row :deep(.arco-typography) {
    word-break: break-all;
}

.credential-actions {
    margin-top: 8px;
}
</style>
