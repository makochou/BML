<template>
  <div class="api-debug-panel">
    <!-- 未选中接口时的占位提示 -->
    <div v-if="!selectedApi" class="debug-placeholder">
      <div class="placeholder-icon">
        <icon-send />
      </div>
      <p class="placeholder-title">API 调试空间</p>
      <p class="placeholder-desc">从左侧接口列表栏点击任一接口，即可体验类 Postman 的专业级 API 调试功能。</p>
    </div>

    <!-- 已选中接口：请求配置 + 发送 + 响应展示 -->
    <div v-else class="postman-container">

      <!-- [高级] 统合的顶部请求栏 (Method + URL + Send) -->
      <div class="postman-request-bar-wrapper">
        <div class="postman-request-bar">
          <!-- HTTP 方法下拉框 -->
          <a-dropdown trigger="click" @select="handleMethodSelect">
            <div 
              class="method-trigger" 
              :class="`method-type-${form.method?.toLowerCase()}`"
            >
              <span class="method-text">{{ form.method || 'GET' }}</span>
              <icon-down class="method-icon-down" />
            </div>
            <template #content>
              <a-doption v-for="opt in methodOptions" :key="opt.value" :value="opt.value">
                <span :style="{ color: getMethodTagColor(String(opt.value)), fontWeight: 600 }">
                  {{ opt.label }}
                </span>
              </a-doption>
            </template>
          </a-dropdown>

          <!-- 柔性分隔线 -->
          <div class="bar-divider"></div>

          <!-- URL 输入框 -->
          <input
            v-model="form.path"
            class="url-input"
            placeholder="输入请求 URL，如 /system/user/list"
            spellcheck="false"
          />

          <!-- 发送按钮 -->
          <a-button
            type="primary"
            :loading="sending"
            class="send-btn"
            @click="sendRequest"
          >
            <template #icon>
              <icon-send v-if="!sending" />
            </template>
            发送
          </a-button>
        </div>
      </div>

      <!-- [中段] 请求配置面板 (Params, Headers, Body) -->
      <a-tabs type="line" class="postman-config-tabs" default-active-key="params" :animation="true" size="medium">
        <!-- 1. Query 参数 -->
        <a-tab-pane key="params" title="Params">
          <div class="postman-config-section">
            <div class="kv-header-hints">
              <span>Query 参数</span>
            </div>
            <div class="kv-grid">
              <!-- Grid Header -->
              <div class="kv-grid-header">
                <div class="kv-col check-col"></div>
                <div class="kv-col key-col">Key</div>
                <div class="kv-col val-col">Value</div>
                <div class="kv-col action-col"></div>
              </div>
              <!-- Grid Body -->
              <div v-for="(row, index) in form.queryParams" :key="row.id" class="kv-grid-row">
                <div class="kv-col check-col">
                  <a-checkbox v-model="row.enabled" />
                </div>
                <div class="kv-col key-col input-wrap">
                  <input v-model="row.key" placeholder="Key" @input="ensureLastEmptyRow(form.queryParams, index)" />
                </div>
                <div class="kv-col val-col input-wrap">
                  <input v-model="row.value" placeholder="Value" />
                </div>
                <div class="kv-col action-col">
                  <span class="delete-icon" @click="removeQueryParam(row, index)" v-if="index !== form.queryParams.length - 1">
                    <icon-close />
                  </span>
                </div>
              </div>
            </div>
          </div>
        </a-tab-pane>

        <!-- 2. 请求头 -->
        <a-tab-pane key="headers" title="Headers">
          <div class="postman-config-section">
            <div class="kv-header-hints space-between">
              <span>Headers 请求头</span>
              <a-checkbox v-model="form.attachToken">自动携带当前登录 Token</a-checkbox>
            </div>
            <div class="kv-grid">
              <!-- Grid Header -->
              <div class="kv-grid-header">
                <div class="kv-col check-col"></div>
                <div class="kv-col key-col">Key</div>
                <div class="kv-col val-col">Value</div>
                <div class="kv-col action-col"></div>
              </div>
              <!-- Grid Body -->
              <div v-for="(row, index) in form.headers" :key="row.id" class="kv-grid-row">
                <div class="kv-col check-col">
                  <a-checkbox v-model="row.enabled" />
                </div>
                <div class="kv-col key-col input-wrap">
                  <input v-model="row.key" placeholder="Key" @input="ensureLastEmptyRow(form.headers, index)" />
                </div>
                <div class="kv-col val-col input-wrap">
                  <input v-model="row.value" placeholder="Value" />
                </div>
                <div class="kv-col action-col">
                   <span class="delete-icon" @click="removeHeader(row, index)" v-if="index !== form.headers.length - 1">
                    <icon-close />
                  </span>
                </div>
              </div>
            </div>
          </div>
        </a-tab-pane>

        <!-- 3. 请求体 -->
        <a-tab-pane key="body" title="Body">
          <div class="postman-config-section">
            <div class="kv-header-hints">
              <span class="active-format">RAW</span>
              <span class="hint-text"> (仅 POST/PUT/PATCH 生效)</span>
            </div>
            <div class="body-editor-wrap">
              <textarea
                v-model="form.body"
                class="code-textarea"
                placeholder="在此输入 JSON (例如: {&quot;id&quot;: 1}) 或其他请求块..."
                spellcheck="false"
              ></textarea>
            </div>
          </div>
        </a-tab-pane>
      </a-tabs>

      <!-- 可拖拽分割条 (视觉模拟，使用边框实现) -->
      <div class="postman-horizontal-divider"></div>

      <!-- [下段] 响应区域 (Postman Style) -->
      <div class="postman-response-section">
        <!-- 响应状态顶栏 -->
        <div class="response-status-bar">
          <div class="status-left">
            <span class="section-title">Response</span>
          </div>
          <div class="status-right" v-if="lastResponse">
            <span class="meta-item">
              <span class="meta-label">Status:</span>
              <span class="meta-val status-val" :class="`status-${responseStatusColor(lastResponse?.status || 0)}`">
                {{ lastResponse?.status }} {{ lastResponse?.statusText }}
              </span>
            </span>
            <span class="meta-divider"></span>
            <span class="meta-item">
              <span class="meta-label">Time:</span>
              <span class="meta-val time-val">{{ lastResponse?.durationMs }} <small>ms</small></span>
            </span>
          </div>
        </div>

        <!-- 响应数据展示区 -->
        <template v-if="lastResponse">
          <a-tabs type="text" class="response-internal-tabs" default-active-key="body" size="small">
            <a-tab-pane key="body" title="Body">
              <div class="response-code-viewer">
                <pre><code>{{ lastResponse?.bodyText }}</code></pre>
              </div>
            </a-tab-pane>
            <a-tab-pane key="headers" title="Headers">
                <div class="kv-grid readonly-grid">
                  <div class="kv-grid-header">
                    <div class="kv-col key-col" style="flex: 0.3;">Key</div>
                    <div class="kv-col val-col" style="flex: 0.7;">Value</div>
                  </div>
                  <div v-for="h in (lastResponse?.responseHeaders || [])" :key="h.id" class="kv-grid-row">
                    <div class="kv-col key-col input-wrap"><span class="readonly-text">{{ h.key }}</span></div>
                    <div class="kv-col val-col input-wrap"><span class="readonly-text">{{ h.value }}</span></div>
                  </div>
               </div>
            </a-tab-pane>
          </a-tabs>
        </template>
        <div v-else class="postman-empty-response">
          <icon-layers class="empty-r-icon" />
          <p>Enter the URL and click <a-button type="text" @click="sendRequest">Send</a-button> to get a response</p>
        </div>
      </div>

    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, watch, reactive } from 'vue';
import { Message } from '@arco-design/web-vue';
import {
  IconSend,
  IconClose,
  IconDown,
  IconLayers
} from '@arco-design/web-vue/es/icon';
import { getDebugHttpClient } from '../../utils/request';
import type { ApiCatalogTreeNode } from '../../types/apiList';
import type { ApiDebugRequestConfig, ApiDebugResponseResult, KeyValueRow } from '../../types/apiDebug';
import { createKeyValueRow } from '../../types/apiDebug';
import { methodOptions, getMethodTagColor } from '../../utils/api-account-governance';

/**
 * 当前选中的 API 节点（type=API 的树节点），用于预填方法与路径。
 * 为 null 时展示占位提示。
 */
const props = defineProps<{
  selectedApi: ApiCatalogTreeNode | null;
  displayName?: string;
}>();

/** 表单：方法、路径、Query、Headers、Body */
const form = reactive<ApiDebugRequestConfig>({
  method: 'GET',
  path: '',
  queryParams: [],
  headers: [],
  attachToken: true,
  body: ''
});

/** 是否正在发送请求 */
const sending = ref(false);

/** 最近一次响应结果，用于展示 */
const lastResponse = ref<ApiDebugResponseResult | null>(null);

/** 根据选中接口预填方法与路径 */
function fillFromSelectedApi(api: ApiCatalogTreeNode | null) {
  if (!api || api.type !== 'API') {
    form.method = 'GET';
    form.path = '';
    form.queryParams = [];
    form.headers = [];
    form.body = '';
    lastResponse.value = null;
    return;
  }
  form.method = api.httpMethod || 'GET';
  form.path = (api.apiUrl || '').trim().replace(/^\s+/, '') || '';
  if (!form.path.startsWith('/')) {
    form.path = '/' + form.path;
  }
  // 保持至少一个空行以供输入
  form.queryParams = [createKeyValueRow()];
  form.headers = [createKeyValueRow()];
  form.body = '';
  lastResponse.value = null;
}

watch(
  () => props.selectedApi,
  (api) => {
    fillFromSelectedApi(api);
  },
  { immediate: true }
);

/**
 * 下拉菜单选择 HTTP 方法
 */
function handleMethodSelect(val: string | number | Record<string, any>) {
  form.method = String(val);
}

/**
 * 确保表格最后始终有一个空行用于新增（类似 Postman 体验）
 */
function ensureLastEmptyRow(list: KeyValueRow[], changedIndex: number) {
  const isLast = changedIndex === list.length - 1;
  const changedRow = list[changedIndex] as KeyValueRow | undefined;
  
  // 如果是修改的最后一行，且输入了值，则自动再追加一个新空行
  if (isLast && changedRow && (changedRow.key || changedRow.value)) {
    list.push(createKeyValueRow());
  }
}

/** 
 * 删除键值对行 
 */
function removeQueryParam(row: KeyValueRow, index: number) {
  // 如果是最后一行，其实是个空占位符，不应该删除
  if (index === form.queryParams.length - 1) return;
  const i = form.queryParams.findIndex((r) => r.id === row.id);
  if (i !== -1) form.queryParams.splice(i, 1);
}

function removeHeader(row: KeyValueRow, index: number) {
  if (index === form.headers.length - 1) return;
  const i = form.headers.findIndex((r) => r.id === row.id);
  if (i !== -1) form.headers.splice(i, 1);
}

/** 将 Query 键值对转为 axios params 对象（仅启用且 key 非空） */
function buildParams(): Record<string, string> {
  const params: Record<string, string> = {};
  for (const row of form.queryParams) {
    if (row.enabled && row.key.trim()) {
      params[row.key.trim()] = row.value;
    }
  }
  return params;
}

/**
 * 将 Headers 键值对转为 axios headers 对象（仅启用且 key 非空）。
 * 若勾选携带 Token，由 getDebugHttpClient 自动注入；
 * 若有请求体且用户未设置 Content-Type，则默认设为 application/json。
 */
function buildHeaders(hasBody: boolean): Record<string, string> {
  const headers: Record<string, string> = {};
  for (const row of form.headers) {
    if (row.enabled && row.key.trim()) {
      headers[row.key.trim()] = row.value;
    }
  }
  if (hasBody && !headers['Content-Type'] && !headers['content-type']) {
    headers['Content-Type'] = 'application/json';
  }
  return headers;
}

/** 解析请求体：空字符串视为无 body；非空则尝试 JSON，失败则原样发送 */
function buildBody(): string | undefined {
  const raw = (form.body || '').trim();
  if (!raw) return undefined;
  return raw;
}

/** 根据状态码返回 Arco 标签颜色 */
function responseStatusColor(status: number): string {
  if (status >= 200 && status < 300) return 'green';
  if (status >= 400 && status < 500) return 'orange';
  if (status >= 500) return 'red';
  return 'arcoblue';
}

/** 将 Axios 响应转为 ApiDebugResponseResult */
function toDebugResult(
  status: number,
  statusText: string,
  durationMs: number,
  responseHeaders: Record<string, string>,
  data: unknown
): ApiDebugResponseResult {
  const responseHeadersList: KeyValueRow[] = Object.entries(responseHeaders).map(([key, value]) =>
    createKeyValueRow({ key, value, enabled: true })
  );
  let bodyText: string;
  let isJson = false;
  if (data === undefined || data === null) {
    bodyText = '';
  } else if (typeof data === 'string') {
    try {
      const parsed = JSON.parse(data);
      bodyText = JSON.stringify(parsed, null, 2);
      isJson = true;
    } catch {
      bodyText = data;
    }
  } else {
    bodyText = JSON.stringify(data, null, 2);
    isJson = true;
  }
  return {
    status,
    statusText,
    durationMs,
    responseHeaders: responseHeadersList,
    bodyText,
    isJson
  };
}

/** 发送请求并更新 lastResponse */
async function sendRequest() {
  const path = (form.path || '').trim();
  if (!path) {
    Message.warning('请填写请求路径');
    return;
  }
  const client = getDebugHttpClient();
  const params = buildParams();
  const body = buildBody();
  const method = (form.method || 'GET').toUpperCase();
  const hasBody = ['POST', 'PUT', 'PATCH'].includes(method) && body !== undefined;
  const headers = buildHeaders(hasBody);

  sending.value = true;
  lastResponse.value = null;
  const start = Date.now();

  try {
    const res = await client.request({
      url: path.startsWith('/') ? path : `/${path}`,
      method,
      params: Object.keys(params).length ? params : undefined,
      headers: Object.keys(headers).length ? headers : undefined,
      data: hasBody ? body : undefined
    });
    const durationMs = Date.now() - start;
    const responseHeaders: Record<string, string> = {};
    if (res.headers && typeof res.headers === 'object') {
      for (const [k, v] of Object.entries(res.headers)) {
        if (typeof v === 'string') responseHeaders[k] = v;
      }
    }
    lastResponse.value = toDebugResult(
      res.status,
      res.statusText || '',
      durationMs,
      responseHeaders,
      res.data
    );
  } catch (err: unknown) {
    const durationMs = Date.now() - start;
    const axiosErr = err as { response?: { status: number; statusText: string; headers?: Record<string, string>; data?: unknown } };
    const status = axiosErr.response?.status ?? 0;
    const statusText = axiosErr.response?.statusText ?? 'Error';
    const responseHeaders: Record<string, string> = {};
    if (axiosErr.response?.headers && typeof axiosErr.response.headers === 'object') {
      for (const [k, v] of Object.entries(axiosErr.response.headers)) {
        if (typeof v === 'string') responseHeaders[k] = v;
      }
    }
    let bodyText = '';
    let bodyData: unknown = axiosErr.response?.data;
    if (bodyData !== undefined && bodyData !== null) {
      bodyText = typeof bodyData === 'string' ? bodyData : JSON.stringify(bodyData, null, 2);
    } else {
      bodyText = (err as Error)?.message || '请求失败';
    }
    lastResponse.value = {
      status,
      statusText,
      durationMs,
      responseHeaders: Object.entries(responseHeaders).map(([key, value]) =>
        createKeyValueRow({ key, value, enabled: true })
      ),
      bodyText,
      isJson: false
    };
    Message.error(`请求异常: ${status || (err as Error)?.message || '未知错误'}`);
  } finally {
    sending.value = false;
  }
}
</script>

<style scoped>
/**
 * API 调试面板整体样式
 * 全面白化，消除冗余色块，通过细致的边距与阴影打造 Postman 级别的专业感。
 */
.api-debug-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
}

/* =================================================================
   1. 统一风格的顶部请求大框 (Request Bar)
   ================================================================= */
.postman-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.postman-request-bar-wrapper {
  padding: 16px 20px 12px;
  background: #ffffff;
}

.postman-request-bar {
  display: flex;
  align-items: center;
  height: 44px;
  background: #f7f8fa;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
  transition: all 0.2s;
  overflow: hidden;
}

.postman-request-bar:hover, .postman-request-bar:focus-within {
  border-color: #c9cdd4;
  box-shadow: 0 2px 6px rgba(0,0,0,0.04);
}

.method-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px 0 16px;
  height: 100%;
  cursor: pointer;
  font-weight: 700;
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  user-select: none;
  min-width: 90px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 根据不同方法分配专属的“招牌”色系，参考 Postman 视觉规范 */
.method-type-get {
  color: #165dff;
  background-color: rgba(22, 93, 255, 0.05);
}
.method-type-get:hover { background-color: rgba(22, 93, 255, 0.1); }

.method-type-post {
  color: #00b42a;
  background-color: rgba(0, 180, 42, 0.05);
}
.method-type-post:hover { background-color: rgba(0, 180, 42, 0.1); }

.method-type-put {
  color: #ff7d00;
  background-color: rgba(255, 125, 0, 0.05);
}
.method-type-put:hover { background-color: rgba(255, 125, 0, 0.1); }

.method-type-delete {
  color: #f53f3f;
  background-color: rgba(245, 63, 63, 0.05);
}
.method-type-delete:hover { background-color: rgba(245, 63, 63, 0.1); }

.method-type-patch {
  color: #722ed1;
  background-color: rgba(114, 46, 209, 0.05);
}
.method-type-patch:hover { background-color: rgba(114, 46, 209, 0.1); }

.method-icon-down {
  margin-left: 8px;
  font-size: 10px;
  opacity: 0.8;
}

.bar-divider {
  width: 1px;
  height: 20px;
  background-color: #e5e6eb;
}

.url-input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 0 16px;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 13px;
  color: #1d2129;
  outline: none;
}

.url-input::placeholder {
  color: #86909c;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  font-size: 13px;
}

.send-btn {
  height: 100%;
  border-radius: 0;
  padding: 0 28px;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0.8px;
  background: linear-gradient(135deg, #165dff 0%, #00b42a 100%); /* 渐变色提升活力感 */
  border: none;
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.15);
}

.send-btn:hover {
  transform: translateY(0);
  background: linear-gradient(135deg, #3491fa 0%, #00d02f 100%);
  box-shadow: 0 4px 12px rgba(22, 93, 255, 0.2);
  filter: brightness(1.1);
}

.send-btn:active {
  transform: scale(0.96);
  opacity: 0.9;
}

.send-btn :deep(.arco-icon) {
  font-size: 16px;
  margin-right: 2px;
  transition: transform 0.3s;
}

.send-btn:hover :deep(.arco-icon) {
  transform: translateX(3px) rotate(-15deg);
}


/* =================================================================
   2. 中段请求配置表格 (KV Grid / Postman Style Tabs)
   ================================================================= */
.postman-config-tabs {
  max-height: 40vh;
  min-height: 150px;
  display: flex;
  flex-direction: column;
}

.postman-config-tabs :deep(.arco-tabs-nav) {
  padding: 0 20px;
  border-bottom: 1px solid #e5e6eb;
}

.postman-config-tabs :deep(.arco-tabs-nav-tab) {
  padding-bottom: 0;
}

.postman-config-tabs :deep(.arco-tabs-tab) {
  font-size: 13px;
  color: #4e5969;
  margin-right: 24px;
  padding: 8px 0;
}

.postman-config-tabs :deep(.arco-tabs-tab-active) {
  color: #1d2129;
  font-weight: 600;
}

.postman-config.kv-grid :deep(.arco-checkbox-checked .arco-checkbox-icon) {
    background-color: var(--bml-primary, #165dff);
    border-color: var(--bml-primary, #165dff);
}

.postman-config-tabs :deep(.arco-tabs-content) {
  padding: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.postman-config-tabs :deep(.arco-tabs-nav-ink) {
  background-color: #f77234; /* 采用经典 Postman 橙色小彩蛋 */
}

.postman-config-section {
  padding: 12px 20px 20px;
  display: flex;
  flex-direction: column;
}

.kv-header-hints {
  font-size: 12px;
  color: #86909c;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}

.kv-header-hints.space-between {
  justify-content: space-between;
}

/* KV 表格构造 */
.kv-grid {
  border: 1px solid #f2f3f5;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: all 0.3s;
}

.kv-grid:focus-within {
    border-color: #e5e6eb;
    box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}

.input-wrap:focus-within {
    border-color: var(--bml-primary, #165dff);
    box-shadow: 0 0 0 3px rgba(var(--bml-primary-rgb), 0.1);
    background: #fff;
}

.kv-grid-header {
  display: flex;
  background: #f7f8fa;
  border-bottom: 1px solid #f2f3f5;
  font-size: 11px;
  color: #86909c;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.kv-grid-row {
  display: flex;
  border-bottom: 1px solid #f7f8fa;
  background: #fff;
  transition: background-color 0.2s;
  position: relative;
}

.kv-grid-row:last-child {
  border-bottom: none;
}

.kv-grid-row:hover {
  background: #fdfdfe;
}

.kv-col {
  padding: 4px 8px;
  border-right: 1px solid #f7f8fa;
  display: flex;
  align-items: center;
}

.kv-col:last-child {
  border-right: none;
}

.check-col { width: 36px; justify-content: center; }
.key-col { flex: 1; }
.val-col { flex: 1; }
.action-col { width: 36px; justify-content: center; }

/* 单元格内部的隐形 input */
.input-wrap {
  padding: 0;
  flex: 1;
  display: flex;
}
.input-wrap input {
  width: 100%;
  height: 32px;
  border: none;
  background: transparent;
  outline: none;
  padding: 4px 12px;
  font-size: 13px;
  color: #1d2129;
  font-family: 'JetBrains Mono', 'SF Mono', 'Monaco', 'Consolas', monospace; /* Professional Mono */
  transition: all 0.2s;
  border-bottom: 2px solid transparent;
}

.input-wrap input:focus {
    background: rgba(22, 93, 255, 0.02);
    border-bottom-color: rgba(22, 93, 255, 0.4);
}

.input-wrap input::placeholder {
  color: #c9cdd4;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 11px;
}

.delete-icon {
  color: #c9cdd4;
  cursor: pointer;
  font-size: 12px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  opacity: 0;
  transition: all 0.2s;
}

.kv-grid-row:hover .delete-icon {
  opacity: 1;
}

.delete-icon:hover {
  color: #f53f3f;
  background: rgba(245, 63, 63, 0.08);
}

/* Header/Body tabs 特殊样式 */
.body-editor-wrap {
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  overflow: hidden;
}

.code-textarea {
  width: 100%;
  height: 200px;
  border: none;
  resize: vertical;
  padding: 12px;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 13px;
  color: #1d2129;
  outline: none;
  line-height: 1.5;
  background: #fdfdfe;
}

.active-format {
  color: #1d2129;
  font-weight: 500;
}

.hint-text {
  color: #c9cdd4;
}

/* =================================================================
   3. 分割线与下截响应面板 (Response Display)
   ================================================================= */
.postman-horizontal-divider {
  height: 4px;
  background: #f2f3f5;
  cursor: row-resize;
  border-top: 1px solid #e5e6eb;
  border-bottom: 1px solid #e5e6eb;
  flex-shrink: 0;
}

.postman-response-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #ffffff;
}

/* 响应上方状态栏 */
.response-status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px 8px;
  border-bottom: 1px solid #f2f3f5;
}

.section-title {
  font-size: 13px;
  color: #86909c;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-right {
  display: flex;
  align-items: center;
  font-size: 13px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.meta-label {
  color: #86909c;
}

.meta-val {
  font-weight: 500;
  font-family: 'Inter', sans-serif;
}

.status-green { color: #00b42a; }
.status-orange { color: #ff7d00; }
.status-red { color: #f53f3f; }

.time-val { color: #165dff; }
.time-val small { font-size: 11px; color: #86909c; font-weight: normal; margin-left: 2px; }

.meta-divider {
  width: 1px;
  height: 12px;
  background: #e5e6eb;
  margin: 0 16px;
}

/* 响应代码区 */
.response-internal-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.response-internal-tabs :deep(.arco-tabs-nav) {
  padding: 0 20px;
  height: 32px;
}

.response-internal-tabs :deep(.arco-tabs-content) {
  padding: 0;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.response-internal-tabs :deep(.arco-tabs-pane) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.response-code-viewer {
  flex: 1;
  margin: 0;
  overflow: auto;
  background: #1e1e1e; /* 极客深色底，类似 VS Code */
  padding: 16px;
}

.response-code-viewer pre {
  margin: 0;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 13px;
  color: #d4d4d4; /* 保护色 */
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 响应 Headers 的只读列表复用 KV grid 样式，但加些微调 */
.readonly-grid {
  margin: 16px 20px;
  border-radius: 4px;
}

.readonly-text {
  padding: 8px 12px;
  font-size: 13px;
  color: #4e5969;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Consolas', monospace;
  word-break: break-all;
}

/* 空状态占位图 */
.postman-empty-response {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #86909c;
}

.empty-r-icon {
  font-size: 48px;
  color: #e5e6eb;
  margin-bottom: 12px;
}
.postman-empty-response p {
  margin: 0;
  font-size: 13px;
}

/* 未选中时的外层占位 */
.debug-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}

.placeholder-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #fdfdfe;
  box-shadow: 0 8px 24px rgba(0,0,0,0.04);
  color: #165dff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  margin-bottom: 20px;
}

.placeholder-title {
  font-size: 18px;
  font-weight: 600;
  color: #1d2129;
  margin: 0 0 8px;
}

.placeholder-desc {
  font-size: 13px;
  color: #86909c;
  max-width: 280px;
  margin: 0;
  line-height: 1.6;
}
</style>
