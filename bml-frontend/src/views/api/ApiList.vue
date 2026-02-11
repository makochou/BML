<template>
  <div class="api-container">
    <a-card class="general-card" title="API 接口管理">
      <template #extra>
        <a-space>
          <a-button type="primary" status="success" @click="handleSync" :loading="syncLoading">
            <template #icon><icon-sync /></template>
            一键同步 API
          </a-button>
        </a-space>
      </template>
      
      <a-layout style="height: 70vh;">
        <a-layout-sider :width="280" style="padding-right: 16px; border-right: 1px solid var(--color-border-2);">
            <a-input-search placeholder="搜索分组..." v-model="groupSearch" style="margin-bottom: 12px;"/>
            <a-tree
                :data="groupData"
                :field-names="{ key: 'id', title: 'name', children: 'children' }"
                block-node
                @select="onGroupSelect"
            />
        </a-layout-sider>
        
        <a-layout-content style="padding-left: 16px;">
             <a-table :data="apiList" :pagination="false" :loading="loading">
                <template #columns>
                    <a-table-column title="接口名称" data-index="name" />
                    <a-table-column title="方法" data-index="method">
                        <template #cell="{ record }">
                            <a-tag :color="getMethodColor(record.method)">{{ record.method }}</a-tag>
                        </template>
                    </a-table-column>
                    <a-table-column title="路径" data-index="path" />
                    <a-table-column title="控制器" data-index="controller" />
                    <a-table-column title="操作">
                        <template #cell="{ record }">
                            <a-button type="text" size="small" @click="toDebug(record)">调试</a-button>
                        </template>
                    </a-table-column>
                </template>
             </a-table>
        </a-layout-content>
      </a-layout>
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
    padding: 20px;
}
</style>
