<template>
  <div class="api-container">
    <a-card class="general-card" :bordered="false">
      <template #title>
        <span class="card-title">API 接口管理</span>
      </template>
      <template #extra>
        <a-space>
          <a-button type="primary" status="success" @click="handleSync" :loading="syncLoading">
            <template #icon><icon-sync /></template>
            一键同步 API
          </a-button>
        </a-space>
      </template>
      
      <div class="layout-box">
        <div class="left-sider">
            <a-input-search placeholder="搜索分组..." v-model="groupSearch" class="search-input"/>
            <div class="tree-wrapper">
                <a-tree
                    :data="groupData"
                    :field-names="{ key: 'id', title: 'name', children: 'children' }"
                    block-node
                    @select="onGroupSelect"
                    class="custom-tree"
                />
            </div>
        </div>
        
        <div class="right-content">
             <a-table :data="apiList" :pagination="false" :loading="loading" :bordered="{wrapper: true, cell: true}">
                <template #columns>
                    <a-table-column title="接口名称" data-index="name" />
                    <a-table-column title="方法" data-index="method" :width="100">
                        <template #cell="{ record }">
                            <a-tag :color="getMethodColor(record.method)">{{ record.method }}</a-tag>
                        </template>
                    </a-table-column>
                    <a-table-column title="路径" data-index="path" />
                    <a-table-column title="控制器" data-index="controller" />
                    <a-table-column title="操作" :width="100" align="center">
                        <template #cell="{ record }">
                            <a-button type="text" size="small" @click="toDebug(record)">调试</a-button>
                        </template>
                    </a-table-column>
                </template>
             </a-table>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconSync } from '@arco-design/web-vue/es/icon';
import request from '../../utils/request';
import { useRouter } from 'vue-router';

const router = useRouter();
const loading = ref(false);
const syncLoading = ref(false);
const groupData = ref([]);
const apiList = ref([]);
const groupSearch = ref('');
const currentGroupId = ref<number | null>(null);

const fetchGroups = async () => {
    const { data } = await request.get('/api/group/list');
    groupData.value = data;
    // 默认选中第一个
    if (data.length > 0) {
        currentGroupId.value = data[0].id;
        fetchApis();
    }
};

const fetchApis = async () => {
    loading.value = true;
    try {
        const { data } = await request.get('/api/info/list', {
            params: { groupId: currentGroupId.value }
        });
        apiList.value = data;
    } finally {
        loading.value = false;
    }
};

const onGroupSelect = (keys: Array<any>) => {
    if (keys.length > 0) {
        currentGroupId.value = keys[0];
        fetchApis();
    }
};

const handleSync = async () => {
    syncLoading.value = true;
    try {
        const { msg } = await request.post('/api/app/sync');
        Message.success(msg);
        fetchGroups(); // 刷新
    } finally {
        syncLoading.value = false;
    }
};

const getMethodColor = (method: string) => {
    switch (method.toUpperCase()) {
        case 'GET': return 'blue';
        case 'POST': return 'green';
        case 'PUT': return 'orange';
        case 'DELETE': return 'red';
        default: return 'gray';
    }
};

const toDebug = (record: any) => {
    router.push({ name: 'ApiDebug', query: { apiId: record.id } });
}

onMounted(() => {
    fetchGroups();
});
</script>

<style scoped>
.api-container {
    padding: 0px 8px 8px 8px; /* Remove top padding completely, reduce side/bottom padding */
    height: 100%;
    display: flex;
    flex-direction: column;
}

.general-card {
    flex: 1;
    display: flex;
    flex-direction: column;
}
.general-card :deep(.arco-card-body) {
    flex: 1;
    padding: 16px;
    overflow: hidden;
}

.card-title {
    font-size: 16px;
    font-weight: 500;
}

.layout-box {
    display: flex;
    height: 100%;
    border: 1px solid var(--color-border-2);
    border-radius: 4px;
}

.left-sider {
    width: 260px;
    border-right: 1px solid var(--color-border-2);
    display: flex;
    flex-direction: column;
    background: var(--color-fill-1);
}

.search-input {
    padding: 12px;
    border-bottom: 1px solid var(--color-border-2);
}

.tree-wrapper {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
}

.right-content {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    background: #fff;
}

:deep(.arco-tree-node-selected .arco-tree-node-title) {
    color: rgb(var(--primary-6));
    font-weight: 500;
}
</style>
