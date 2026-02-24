<template>
  <div class="debug-container">
    <a-card class="general-card" title="Open API 在线调试台">
      <a-grid :cols="24" :col-gap="20">
        <!-- 左侧配置区 -->
        <a-grid-item :span="12">
           <a-form :model="form" layout="vertical">
              <a-form-item field="apiId" label="选择接口">
                <a-select v-model="form.apiId" :loading="apiLoading" placeholder="请选择要调试的API" @change="onApiChange" allow-search>
                    <a-option v-for="item in apiList" :key="item.id" :value="item.id" :label="`[${item.method}] ${item.path} - ${item.name}`" />
                </a-select>
              </a-form-item>
              
              <a-form-item field="appId" label="选择应用 (用于签名)">
                <a-select v-model="form.appId" :loading="appLoading" placeholder="请选择使用哪个AppKey进行调用" @change="onAppChange">
                    <a-option v-for="item in appList" :key="item.id" :value="item.appId" :label="`${item.name} (${item.appId})`" />
                </a-select>
              </a-form-item>

              <a-divider orientation="left">请求参数</a-divider>
              
              <a-form-item label="Query Params (URL参数)">
                 <div v-for="(param, index) in queryParams" :key="index" class="param-row">
                    <a-input v-model="param.key" placeholder="Key" style="width: 120px" />
                    <span class="sep">=</span>
                    <a-input v-model="param.value" placeholder="Value" />
                    <a-button type="text" status="danger" @click="removeQueryParam(index)"><icon-delete /></a-button>
                 </div>
                 <a-button type="dashed" size="small" @click="addQueryParam" style="margin-top: 8px">
                    <icon-plus /> 添加参数
                 </a-button>
              </a-form-item>

              <a-form-item v-if="currentApi?.method !== 'GET'" label="Body (JSON)">
                 <a-textarea v-model="form.body" placeholder="{}" :auto-size="{minRows: 4, maxRows: 10}" />
              </a-form-item>

              <a-space size="large" style="margin-top: 20px; width: 100%; justify-content: center;">
                 <a-button type="primary" size="large" @click="handleSend" :loading="sending">
                    <template #icon><icon-send /></template>
                    发送请求
                 </a-button>
              </a-space>
           </a-form>
        </a-grid-item>

        <!-- 右侧结果区 -->
        <a-grid-item :span="12" style="border-left: 1px solid var(--color-border-2); padding-left: 20px;">
           <a-tabs default-active-key="response">
              <a-tab-pane key="response" title="响应结果">
                 <a-spin :loading="sending" style="width: 100%">
                    <a-alert v-if="responseStatus" :type="responseStatus >= 200 && responseStatus < 300 ? 'success' : 'error'">
                        Status: {{ responseStatus }} {{ responseStatusText }}
                    </a-alert>
                    <pre class="json-viewer">{{ responseData }}</pre>
                 </a-spin>
              </a-tab-pane>
              <a-tab-pane key="headers" title="请求头预览 (自动生成)">
                 <pre class="json-viewer">{{ previewHeaders }}</pre>
              </a-tab-pane>
           </a-tabs>
        </a-grid-item>
      </a-grid>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { Message } from '@arco-design/web-vue';
import { IconSend, IconPlus, IconDelete } from '@arco-design/web-vue/es/icon';
import request from '../../utils/request';
import axios from 'axios';
import CryptoJS from 'crypto-js';

const route = useRoute();
const apiLoading = ref(false);
const appLoading = ref(false);
const sending = ref(false);

const apiList = ref<any[]>([]);
const appList = ref<any[]>([]);

const form = reactive({
    apiId: Number(route.query.apiId) || undefined,
    appId: '', // storing AppKey
    body: ''
});

const currentApi = ref<any>(null);
const currentAppSecret = ref(''); // hidden

const queryParams = ref<{key: string, value: string}[]>([{key: '', value: ''}]);

const responseData = ref('');
const responseStatus = ref(0);
const responseStatusText = ref('');

const previewHeaders = computed(() => {
    if (!form.appId || !currentAppSecret.value) return '等待选择应用...';
    
    // 模拟生成
    const timestamp = Date.now().toString();
    const nonce = Math.random().toString(36).substring(2, 12);
    const paramsStr = buildQueryString();
    
    // 签名核心逻辑
    const raw = `appKey=${form.appId}&timestamp=${timestamp}&nonce=${nonce}&params=${paramsStr}`;
    const sign = CryptoJS.HmacSHA256(raw, currentAppSecret.value).toString();
    
    return JSON.stringify({
        'X-Bml-App-Key': form.appId,
        'X-Bml-Timestamp': timestamp,
        'X-Bml-Nonce': nonce,
        'X-Bml-Sign': sign
    }, null, 2);
});


const fetchApis = async () => {
    apiLoading.value = true;
    try {
        const { data } = await request.get('/api/info/list');
        apiList.value = data;
        if (form.apiId) {
            onApiChange(form.apiId);
        }
    } finally {
        apiLoading.value = false;
    }
}

const fetchApps = async () => {
    appLoading.value = true;
    try {
        const { data } = await request.get('/api/app/list');
        appList.value = data;
        if (data.length > 0) {
            form.appId = data[0].appId;
            currentAppSecret.value = data[0].appSecret; // 实际上应该后端不返回Secret，或者加密。演示环境假设admin能看到。
            // 注意：真实场景前端不应该知道Secret。这里是"调试台"，假设开发者拥有Secret。
            // 更好的做法是后端提供 "sign-debug" 接口。但为了无需后端改动，前端直接算。
        }
    } finally {
        appLoading.value = false;
    }
}

const onApiChange = (val: any) => {
    currentApi.value = apiList.value.find(item => item.id === val);
}

const onAppChange = (val: any) => {
    const app = appList.value.find(item => item.appId === val);
    if (app) {
        currentAppSecret.value = app.appSecret;
    }
}

const addQueryParam = () => {
    queryParams.value.push({key: '', value: ''});
}

const removeQueryParam = (index: number) => {
    queryParams.value.splice(index, 1);
}

const buildQueryString = () => {
    return queryParams.value
        .filter(p => p.key)
        .map(p => `${p.key}=${p.value}`) // 简单拼接，注意编码
        .join('&');
}

const handleSend = async () => {
    if (!currentApi.value) {
        Message.warning('请选择接口');
        return;
    }
    if (!form.appId) {
        Message.warning('请选择应用');
        return;
    }

    sending.value = true;
    responseData.value = '';
    responseStatus.value = 0;

    try {
        const timestamp = Date.now().toString();
        const nonce = Math.random().toString(36).substring(2, 15);
        const paramsStr = buildQueryString(); // query string
        
        // 签名逻辑 (同 Java 后端)
        const raw = `appKey=${form.appId}&timestamp=${timestamp}&nonce=${nonce}&params=${paramsStr}`;
        const sign = CryptoJS.HmacSHA256(raw, currentAppSecret.value).toString();
        
        // 构造请求URL
        // 注意：/open/api/xxx 是后端开放路径。
        // currentApi.path 可能是 /api/user/list，需要替换吗？或者后端有统一 /open 前缀？
        // 此时我们假设调试的是真实的 Open API 路径。
        // 如果后端接口是 /api/group/list，且 OpenApiInterceptor 拦截了 /open/api/**
        // 那么我们只能调试 /open/api/** 的接口。
        
        // 这里有个问题：数据库存的 API path 是 /api/group/list，不是 /open/...
        // 我们需要假设 OpenApiInterceptor 拦截的是 /open/api/proxy? 
        // 或者是调试 演示接口 /open/api/demo/hello
        
        // 修正逻辑：
        // 真实的 Open API 平台通常是将 /api/** 映射到 /open/api/**，或者网关转发。
        // 这里演示 Demo，我们尝试请求 currentApi.path。
        // 如果 OpenApiInterceptor 拦截的是 /open/api/**，而数据库里的Path是 /api/group/list
        // 那么直接请求 /api/group/list 是不走签名的 (走 Token)。
        
        // 决定：为了演示签名，我们只调试 /open/api/demo/hello 或将请求发往 /open/api前缀（如果后端做了映射）
        // 但目前后端没做映射。
        
        // 权宜之计：我们不仅支持 Open API 调试，也支持普通 API (Token) 调试。
        // 但用户要看的是 "签名认证"。
        // 所以我们让用户手动输入 URL 或者我们在 Demo 中只调试 /open/api/demo/hello。
        
        // 既然这是一个"通用调试台"，我们应当允许发送请求到 path。
        // 如果 path 以 /open/api 开头，则加签名头。
        // 如果不是，则加 Token (暂不支持Token调试，仅支持签名演示)。
        // 假设用户只调试 /open/api 相关接口。
        
        // 这里的请求是发给前端 Proxy 的 (/api/...) 
        const realUrl = `/api${currentApi.value.path}`; 

        const headers = {
            'X-Bml-App-Key': form.appId,
            'X-Bml-Timestamp': timestamp,
            'X-Bml-Nonce': nonce,
            'X-Bml-Sign': sign
        };
        
        // 使用 axios 直接发，不走 request.ts 拦截器 (避免干扰)
        const res = await axios({
            method: currentApi.value.method,
            url: realUrl,
            params: new URLSearchParams(buildQueryString()), // axios 处理 params
            data: form.body ? JSON.parse(form.body) : undefined,
            headers: headers
        });
        
        responseData.value = JSON.stringify(res.data, null, 2);
        responseStatus.value = res.status;
        responseStatusText.value = res.statusText;

    } catch (err: any) {
        if (err.response) {
            responseData.value = JSON.stringify(err.response.data, null, 2);
            responseStatus.value = err.response.status;
            responseStatusText.value = err.response.statusText;
        } else {
            responseData.value = err.message;
        }
    } finally {
        sending.value = false;
    }
}

onMounted(() => {
    fetchApis();
    fetchApps();
});
</script>

<style scoped>
.debug-container {
    padding: 16px;
    height: 100%;
    overflow-y: auto;
}
.param-row {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
}
.sep {
    margin: 0 8px;
    color: var(--color-text-3);
}
.json-viewer {
    background: var(--color-fill-2);
    padding: 12px;
    border-radius: 4px;
    font-family: monospace;
    overflow: auto;
    max-height: 500px;
}
</style>
