# 告警管理 API 文档

## 概述

告警管理模块提供系统告警的查询、标记已读、删除等功能。支持增量轮询机制，实现准实时的告警通知推送。

**基础路径**：`/system/alert`

**认证方式**：需要登录认证（JWT Token）

---

## 接口列表

### 1. 获取未读告警数量

**接口地址**：`GET /system/alert/unread-count`

**接口描述**：获取当前用户未读的告警总数，用于右上角告警图标的红点提示。

**请求参数**：无

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 5
}
```

**响应字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| data | Long | 未读告警数量 |

---

### 2. 获取最近的告警列表

**接口地址**：`GET /system/alert/recent`

**接口描述**：获取最近的告警列表，按创建时间倒序排列，用于告警中心的全量展示。

**请求参数**：

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| limit | Integer | 否 | 100 | 返回的告警数量 |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "alertType": "LICENSE_CHANGE",
      "alertLevel": "info",
      "alertTitle": "API 账号上限升级",
      "alertContent": "许可证更新：最大 API 账号数从 5 变更为 10。",
      "readStatus": 0,
      "createTime": "2026-04-30 10:30:00",
      "updateTime": "2026-04-30 10:30:00"
    },
    {
      "id": 2,
      "alertType": "CPU_HIGH",
      "alertLevel": "warning",
      "alertTitle": "CPU 使用率过高",
      "alertContent": "当前 CPU 使用率为 85%，已超过阈值 80%。",
      "readStatus": 1,
      "createTime": "2026-04-30 09:15:00",
      "updateTime": "2026-04-30 09:20:00"
    }
  ]
}
```

**响应字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 告警 ID |
| alertType | String | 告警类型（LICENSE_CHANGE / CPU_HIGH / MEMORY_HIGH / DISK_FULL / JVM_HIGH） |
| alertLevel | String | 告警级别（info / warning / error / critical） |
| alertTitle | String | 告警标题 |
| alertContent | String | 告警详情内容 |
| readStatus | Integer | 阅读状态（0:未读, 1:已读） |
| createTime | String | 创建时间 |
| updateTime | String | 更新时间 |

---

### 3. 增量轮询获取最新告警

**接口地址**：`GET /system/alert/latest`

**接口描述**：前端增量轮询核心接口。前端每 30 秒携带上一次拿到的最大 ID 进行轮询，后端仅返回该 ID 之后新产生的告警记录，实现低成本的准实时通知推送。

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| lastId | Long | 否 | 上次已知的最大告警 ID，为空时返回最近 20 条 |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 3,
      "alertType": "LICENSE_CHANGE",
      "alertLevel": "info",
      "alertTitle": "业务用户上限升级",
      "alertContent": "许可证更新：业务用户上限从 100 变更为 200。",
      "readStatus": 0,
      "createTime": "2026-04-30 11:00:00",
      "updateTime": "2026-04-30 11:00:00"
    }
  ]
}
```

**使用示例**：

```javascript
// 前端轮询逻辑
let lastAlertId = null;

setInterval(async () => {
  const response = await fetch(`/system/alert/latest?lastId=${lastAlertId || ''}`);
  const result = await response.json();
  
  if (result.data && result.data.length > 0) {
    // 显示新告警
    result.data.forEach(alert => {
      showNotification(alert);
    });
    
    // 更新 lastAlertId 为最新的告警 ID
    lastAlertId = Math.max(...result.data.map(a => a.id));
  }
}, 30000); // 每 30 秒轮询一次
```

---

### 4. 标记单条告警为已读

**接口地址**：`PUT /system/alert/{id}/read`

**接口描述**：将指定的告警标记为已读状态。

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 告警 ID（路径参数） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

---

### 5. 一键标记所有告警为已读

**接口地址**：`PUT /system/alert/read-all`

**接口描述**：将所有未读告警一键标记为已读状态。

**请求参数**：无

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 5
}
```

**响应字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| data | Integer | 实际更新的记录数 |

---

### 6. 删除告警

**接口地址**：`DELETE /system/alert/{id}`

**接口描述**：逻辑删除指定的告警记录。

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 告警 ID（路径参数） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

---

### 7. 获取存在告警的日期列表

**接口地址**：`GET /system/alert/dates`

**接口描述**：获取所有有告警的日期列表（去重、倒序），前端日期选择器据此高亮有告警的日期。

**请求参数**：无

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    "2026-04-30",
    "2026-04-29",
    "2026-04-28"
  ]
}
```

---

### 8. 按日期查询告警列表

**接口地址**：`GET /system/alert/by-date`

**接口描述**：查询指定日期当天的所有告警，按创建时间倒序排列。

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| date | String | 是 | 查询日期（格式：yyyy-MM-dd） |

**响应示例**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "alertType": "LICENSE_CHANGE",
      "alertLevel": "info",
      "alertTitle": "API 账号上限升级",
      "alertContent": "许可证更新：最大 API 账号数从 5 变更为 10。",
      "readStatus": 0,
      "createTime": "2026-04-30 10:30:00",
      "updateTime": "2026-04-30 10:30:00"
    }
  ]
}
```

---

## 告警类型说明

### 系统资源告警

| 类型 | 说明 | 级别 |
|------|------|------|
| CPU_HIGH | CPU 使用率过高 | warning / error |
| MEMORY_HIGH | 内存使用率过高 | warning / error |
| DISK_FULL | 磁盘空间不足 | warning / error |
| JVM_HIGH | JVM 堆内存使用率过高 | warning / error |

### 许可证变更告警

| 类型 | 说明 | 级别 |
|------|------|------|
| LICENSE_CHANGE | 许可证配额变更 | info |

所有许可证变更告警统一使用 `LICENSE_CHANGE` 类型，通过 `alertTitle` 区分具体的变更内容：

- API 账号上限升级
- API 账号上限降级
- 业务用户上限升级
- 业务用户上限降级
- API 来源用户上限升级
- API 来源用户上限降级
- 许可证有效期延期
- 许可证有效期缩短
- 授权功能模块新增
- 授权功能模块移除
- 新许可证已过期

---

## 告警级别说明

| 级别 | 说明 | 使用场景 |
|------|------|----------|
| info | 提示信息 | 配额升级、功能模块新增等正面变更 |
| warning | 警告信息 | 配额降级、资源使用率接近阈值 |
| error | 错误信息 | 许可证过期、资源使用率超过阈值 |
| critical | 严重错误 | 系统资源耗尽、服务不可用 |

---

## 智能去重机制

### 去重规则

在保存新告警前，系统会自动检查是否已存在相同类型（`alertType`）和标题（`alertTitle`）的未读告警：

- **如果存在**：更新该告警的内容（`alertContent`）和级别（`alertLevel`），保持未读状态
- **如果不存在**：创建新的告警记录

### 去重示例

**场景 1：首次创建告警**

```
请求：保存告警
- alertType: LICENSE_CHANGE
- alertTitle: API 账号上限升级
- alertContent: 许可证更新：最大 API 账号数从 5 变更为 10。

结果：创建新记录，ID = 1
```

**场景 2：更新现有未读告警**

```
请求：保存告警
- alertType: LICENSE_CHANGE
- alertTitle: API 账号上限升级
- alertContent: 许可证更新：最大 API 账号数从 10 变更为 15。

结果：更新现有记录（ID = 1），内容更新为最新值
```

**场景 3：已读告警不影响新告警**

```
前提：ID = 1 的告警已被标记为已读

请求：保存告警
- alertType: LICENSE_CHANGE
- alertTitle: API 账号上限升级
- alertContent: 许可证更新：最大 API 账号数从 15 变更为 20。

结果：创建新记录，ID = 2（因为 ID = 1 的告警已读）
```

### 去重优势

1. **避免重复提醒**：相同类型的变更只显示最新的一条告警
2. **减少信息冗余**：告警中心不会堆积大量重复告警
3. **提升用户体验**：用户只需关注最新的变更信息
4. **节省存储空间**：减少数据库中的重复记录

---

## 前端集成示例

### Vue 3 + Element Plus

```vue
<template>
  <div class="alert-center">
    <!-- 告警图标 + 未读数量 -->
    <el-badge :value="unreadCount" :hidden="unreadCount === 0">
      <el-icon @click="showAlertPanel"><Bell /></el-icon>
    </el-badge>

    <!-- 告警面板 -->
    <el-drawer v-model="drawerVisible" title="告警中心" size="400px">
      <div class="alert-list">
        <div
          v-for="alert in alerts"
          :key="alert.id"
          :class="['alert-item', `alert-${alert.alertLevel}`]"
        >
          <div class="alert-header">
            <span class="alert-title">{{ alert.alertTitle }}</span>
            <el-tag :type="getLevelType(alert.alertLevel)" size="small">
              {{ getLevelText(alert.alertLevel) }}
            </el-tag>
          </div>
          <div class="alert-content">{{ alert.alertContent }}</div>
          <div class="alert-footer">
            <span class="alert-time">{{ alert.createTime }}</span>
            <div class="alert-actions">
              <el-button
                v-if="alert.readStatus === 0"
                size="small"
                @click="markAsRead(alert.id)"
              >
                标记已读
              </el-button>
              <el-button size="small" @click="deleteAlert(alert.id)">
                删除
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="markAllAsRead">全部已读</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { Bell } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

const unreadCount = ref(0);
const alerts = ref([]);
const drawerVisible = ref(false);
let lastAlertId = null;
let pollingTimer = null;

// 获取未读数量
const fetchUnreadCount = async () => {
  const response = await fetch('/system/alert/unread-count');
  const result = await response.json();
  unreadCount.value = result.data;
};

// 获取最近的告警列表
const fetchRecentAlerts = async () => {
  const response = await fetch('/system/alert/recent?limit=50');
  const result = await response.json();
  alerts.value = result.data;
  if (alerts.value.length > 0) {
    lastAlertId = Math.max(...alerts.value.map(a => a.id));
  }
};

// 增量轮询获取最新告警
const pollLatestAlerts = async () => {
  const response = await fetch(`/system/alert/latest?lastId=${lastAlertId || ''}`);
  const result = await response.json();
  
  if (result.data && result.data.length > 0) {
    // 显示新告警通知
    result.data.forEach(alert => {
      ElMessage({
        type: getLevelType(alert.alertLevel),
        message: alert.alertTitle,
        duration: 5000,
      });
    });
    
    // 更新告警列表
    alerts.value.unshift(...result.data);
    lastAlertId = Math.max(...result.data.map(a => a.id));
    
    // 更新未读数量
    await fetchUnreadCount();
  }
};

// 标记单条告警为已读
const markAsRead = async (id) => {
  await fetch(`/system/alert/${id}/read`, { method: 'PUT' });
  const alert = alerts.value.find(a => a.id === id);
  if (alert) {
    alert.readStatus = 1;
  }
  await fetchUnreadCount();
};

// 一键标记所有告警为已读
const markAllAsRead = async () => {
  await fetch('/system/alert/read-all', { method: 'PUT' });
  alerts.value.forEach(alert => {
    alert.readStatus = 1;
  });
  unreadCount.value = 0;
  ElMessage.success('已全部标记为已读');
};

// 删除告警
const deleteAlert = async (id) => {
  await fetch(`/system/alert/${id}`, { method: 'DELETE' });
  alerts.value = alerts.value.filter(a => a.id !== id);
  await fetchUnreadCount();
  ElMessage.success('删除成功');
};

// 显示告警面板
const showAlertPanel = () => {
  drawerVisible.value = true;
  fetchRecentAlerts();
};

// 获取告警级别对应的 Element Plus 类型
const getLevelType = (level) => {
  const typeMap = {
    info: 'info',
    warning: 'warning',
    error: 'danger',
    critical: 'danger',
  };
  return typeMap[level] || 'info';
};

// 获取告警级别文本
const getLevelText = (level) => {
  const textMap = {
    info: '提示',
    warning: '警告',
    error: '错误',
    critical: '严重',
  };
  return textMap[level] || '提示';
};

// 组件挂载时启动轮询
onMounted(() => {
  fetchUnreadCount();
  fetchRecentAlerts();
  
  // 每 30 秒轮询一次
  pollingTimer = setInterval(pollLatestAlerts, 30000);
});

// 组件卸载时清除轮询
onUnmounted(() => {
  if (pollingTimer) {
    clearInterval(pollingTimer);
  }
});
</script>

<style scoped>
.alert-center {
  position: relative;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  background-color: #fff;
}

.alert-item.alert-info {
  border-left: 4px solid #409eff;
}

.alert-item.alert-warning {
  border-left: 4px solid #e6a23c;
}

.alert-item.alert-error,
.alert-item.alert-critical {
  border-left: 4px solid #f56c6c;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.alert-title {
  font-weight: 600;
  font-size: 14px;
}

.alert-content {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
  line-height: 1.5;
}

.alert-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.alert-time {
  font-size: 12px;
  color: #909399;
}

.alert-actions {
  display: flex;
  gap: 8px;
}
</style>
```

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或登录已过期 |
| 403 | 无权限访问 |
| 404 | 告警记录不存在 |
| 500 | 服务器内部错误 |

---

## 性能优化建议

### 1. 前端轮询优化

- **轮询间隔**：建议 30 秒，避免频繁请求
- **增量查询**：使用 `lastId` 参数，只获取新增告警
- **页面可见性**：页面不可见时暂停轮询，节省资源

```javascript
// 页面可见性优化
document.addEventListener('visibilitychange', () => {
  if (document.hidden) {
    // 页面隐藏时暂停轮询
    clearInterval(pollingTimer);
  } else {
    // 页面显示时恢复轮询
    pollingTimer = setInterval(pollLatestAlerts, 30000);
  }
});
```

### 2. 后端查询优化

- **索引优化**：已创建复合索引 `idx_alert_dedup`，支持高效查询
- **分页查询**：使用 `LIMIT` 限制返回数量，避免一次性加载过多数据
- **缓存策略**：可考虑使用 Redis 缓存未读数量，减少数据库查询

### 3. 数据清理策略

建议定期清理历史告警数据，避免数据库膨胀：

```sql
-- 删除 30 天前的已读告警
DELETE FROM sys_alert 
WHERE read_status = 1 
  AND create_time < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

---

**文档版本**：V1.0  
**最后更新**：2026-04-30  
**作者**：BML Team
