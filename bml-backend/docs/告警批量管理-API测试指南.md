# 告警批量管理 API 测试指南

## 📋 测试准备

### 1. 环境要求

- 后端服务已启动
- 数据库已初始化
- 拥有测试账号和权限

### 2. 获取访问令牌

首先需要登录获取 JWT Token：

```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

响应示例：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

将返回的 token 保存到环境变量：
```bash
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 🧪 API 测试用例

### 测试用例 1：按类型批量标记已读

#### 1.1 准备测试数据

首先创建一些测试告警：

```bash
# 创建第一条告警
curl -X POST "http://localhost:8080/system/alert" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "alertType": "LICENSE_CHANGE",
    "alertTitle": "API 账号上限升级",
    "alertContent": "许可证更新：最大 API 账号数从 5 变更为 10。",
    "alertLevel": "INFO",
    "readStatus": 0
  }'

# 创建第二条告警
curl -X POST "http://localhost:8080/system/alert" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "alertType": "LICENSE_CHANGE",
    "alertTitle": "业务用户上限升级",
    "alertContent": "许可证更新：最大业务用户数从 100 变更为 200。",
    "alertLevel": "INFO",
    "readStatus": 0
  }'

# 创建第三条告警
curl -X POST "http://localhost:8080/system/alert" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "alertType": "LICENSE_CHANGE",
    "alertTitle": "API 账号上限升级",
    "alertContent": "许可证更新：最大 API 账号数从 10 变更为 15。",
    "alertLevel": "INFO",
    "readStatus": 0
  }'
```

#### 1.2 查询未读告警数量

```bash
curl -X GET "http://localhost:8080/system/alert/unread-count" \
  -H "Authorization: Bearer $TOKEN"
```

预期响应：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 3
}
```

#### 1.3 执行按类型批量标记已读

```bash
curl -X PUT "http://localhost:8080/system/alert/read-by-type?alertType=LICENSE_CHANGE" \
  -H "Authorization: Bearer $TOKEN"
```

预期响应：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 3
}
```

#### 1.4 验证结果

再次查询未读告警数量：

```bash
curl -X GET "http://localhost:8080/system/alert/unread-count" \
  -H "Authorization: Bearer $TOKEN"
```

预期响应：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 0
}
```

---

### 测试用例 2：按类型批量删除

#### 2.1 准备测试数据

创建一些测试告警（同测试用例 1）

#### 2.2 查询告警列表

```bash
curl -X GET "http://localhost:8080/system/alert/list?limit=50" \
  -H "Authorization: Bearer $TOKEN"
```

记录返回的告警数量。

#### 2.3 执行按类型批量删除

```bash
curl -X DELETE "http://localhost:8080/system/alert/delete-by-type?alertType=LICENSE_CHANGE" \
  -H "Authorization: Bearer $TOKEN"
```

预期响应：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 3
}
```

#### 2.4 验证结果

再次查询告警列表：

```bash
curl -X GET "http://localhost:8080/system/alert/list?limit=50" \
  -H "Authorization: Bearer $TOKEN"
```

预期：返回的告警列表中不再包含 `LICENSE_CHANGE` 类型的告警。

---

### 测试用例 3：按标题批量标记已读

#### 3.1 准备测试数据

创建多条相同标题的告警：

```bash
# 创建第一条
curl -X POST "http://localhost:8080/system/alert" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "alertType": "LICENSE_CHANGE",
    "alertTitle": "API 账号上限升级",
    "alertContent": "许可证更新：最大 API 账号数从 5 变更为 10。",
    "alertLevel": "INFO",
    "readStatus": 0
  }'

# 创建第二条（相同标题）
curl -X POST "http://localhost:8080/system/alert" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "alertType": "LICENSE_CHANGE",
    "alertTitle": "API 账号上限升级",
    "alertContent": "许可证更新：最大 API 账号数从 10 变更为 15。",
    "alertLevel": "INFO",
    "readStatus": 0
  }'

# 创建第三条（不同标题）
curl -X POST "http://localhost:8080/system/alert" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "alertType": "LICENSE_CHANGE",
    "alertTitle": "业务用户上限升级",
    "alertContent": "许可证更新：最大业务用户数从 100 变更为 200。",
    "alertLevel": "INFO",
    "readStatus": 0
  }'
```

#### 3.2 执行按标题批量标记已读

```bash
curl -X PUT "http://localhost:8080/system/alert/read-by-title?alertTitle=API%20账号上限升级" \
  -H "Authorization: Bearer $TOKEN"
```

预期响应：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 2
}
```

#### 3.3 验证结果

查询未读告警数量：

```bash
curl -X GET "http://localhost:8080/system/alert/unread-count" \
  -H "Authorization: Bearer $TOKEN"
```

预期响应：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1
}
```

（只剩下"业务用户上限升级"这一条未读告警）

---

### 测试用例 4：按标题批量删除

#### 4.1 准备测试数据

创建多条相同标题的告警（同测试用例 3）

#### 4.2 执行按标题批量删除

```bash
curl -X DELETE "http://localhost:8080/system/alert/delete-by-title?alertTitle=API%20账号上限升级" \
  -H "Authorization: Bearer $TOKEN"
```

预期响应：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 2
}
```

#### 4.3 验证结果

查询告警列表：

```bash
curl -X GET "http://localhost:8080/system/alert/list?limit=50" \
  -H "Authorization: Bearer $TOKEN"
```

预期：返回的告警列表中不再包含"API 账号上限升级"标题的告警。

---

## 🔍 边界测试

### 测试用例 5：不存在的类型

```bash
curl -X PUT "http://localhost:8080/system/alert/read-by-type?alertType=NON_EXISTENT_TYPE" \
  -H "Authorization: Bearer $TOKEN"
```

预期响应：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 0
}
```

### 测试用例 6：不存在的标题

```bash
curl -X DELETE "http://localhost:8080/system/alert/delete-by-title?alertTitle=不存在的标题" \
  -H "Authorization: Bearer $TOKEN"
```

预期响应：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 0
}
```

### 测试用例 7：空参数

```bash
curl -X PUT "http://localhost:8080/system/alert/read-by-type?alertType=" \
  -H "Authorization: Bearer $TOKEN"
```

预期响应：
```json
{
  "code": 400,
  "message": "参数错误：alertType 不能为空"
}
```

### 测试用例 8：特殊字符

```bash
curl -X PUT "http://localhost:8080/system/alert/read-by-title?alertTitle=%E7%89%B9%E6%AE%8A%E5%AD%97%E7%AC%A6%20%21%40%23%24%25" \
  -H "Authorization: Bearer $TOKEN"
```

预期：正常处理，返回匹配的记录数。

---

## 🔒 权限测试

### 测试用例 9：无权限用户

使用没有 `monitor:alert:edit` 权限的用户登录：

```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "guest",
    "password": "guest123"
  }'
```

使用返回的 token 执行批量操作：

```bash
curl -X PUT "http://localhost:8080/system/alert/read-by-type?alertType=LICENSE_CHANGE" \
  -H "Authorization: Bearer $GUEST_TOKEN"
```

预期响应：
```json
{
  "code": 403,
  "message": "权限不足"
}
```

---

## 📊 性能测试

### 测试用例 10：批量操作 100 条记录

#### 10.1 准备测试数据

使用脚本批量创建 100 条告警：

```bash
for i in {1..100}; do
  curl -X POST "http://localhost:8080/system/alert" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"alertType\": \"PERFORMANCE_TEST\",
      \"alertTitle\": \"性能测试告警 $i\",
      \"alertContent\": \"这是第 $i 条性能测试告警\",
      \"alertLevel\": \"INFO\",
      \"readStatus\": 0
    }"
done
```

#### 10.2 执行批量操作并测量时间

```bash
time curl -X PUT "http://localhost:8080/system/alert/read-by-type?alertType=PERFORMANCE_TEST" \
  -H "Authorization: Bearer $TOKEN"
```

预期：
- 响应时间 < 1 秒
- 返回 `data: 100`

---

## 🧹 清理测试数据

测试完成后，清理所有测试数据：

```bash
# 删除所有测试告警
curl -X DELETE "http://localhost:8080/system/alert/delete-by-type?alertType=LICENSE_CHANGE" \
  -H "Authorization: Bearer $TOKEN"

curl -X DELETE "http://localhost:8080/system/alert/delete-by-type?alertType=PERFORMANCE_TEST" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 📝 测试检查清单

### 功能测试

- [ ] 按类型批量标记已读 - 正常场景
- [ ] 按类型批量删除 - 正常场景
- [ ] 按标题批量标记已读 - 正常场景
- [ ] 按标题批量删除 - 正常场景
- [ ] 不存在的类型 - 边界场景
- [ ] 不存在的标题 - 边界场景
- [ ] 空参数 - 异常场景
- [ ] 特殊字符 - 边界场景

### 权限测试

- [ ] 有权限用户可以执行批量操作
- [ ] 无权限用户无法执行批量操作
- [ ] 未登录用户无法访问接口

### 性能测试

- [ ] 批量操作 100 条记录的响应时间
- [ ] 批量操作 1000 条记录的响应时间
- [ ] 并发批量操作的性能表现

### 数据一致性测试

- [ ] 批量标记已读后，未读数量正确
- [ ] 批量删除后，记录不再出现在列表中
- [ ] 逻辑删除正确执行（deleted 字段为 1）
- [ ] 操作日志正确记录

---

## 🐛 常见问题排查

### 问题 1：401 Unauthorized

**原因**：Token 过期或无效

**解决**：重新登录获取新的 Token

### 问题 2：403 Forbidden

**原因**：用户没有相应的权限

**解决**：
1. 检查用户是否拥有 `monitor:alert:edit` 或 `monitor:alert:remove` 权限
2. 联系管理员分配权限

### 问题 3：返回的记录数为 0

**原因**：
1. 数据库中没有匹配的记录
2. 参数拼写错误
3. URL 编码问题

**解决**：
1. 先查询告警列表，确认数据存在
2. 检查参数拼写是否正确
3. 对中文参数进行 URL 编码

### 问题 4：响应时间过长

**原因**：
1. 数据库索引缺失
2. 数据量过大
3. 服务器性能不足

**解决**：
1. 检查数据库索引是否正确创建
2. 优化查询条件
3. 升级服务器配置

---

## 📞 技术支持

如有任何问题，请联系：

- **技术支持邮箱**：support@bml.com
- **在线客服**：系统右下角的客服图标
- **开发文档**：https://docs.bml.com

---

**文档版本**：V1.0  
**最后更新**：2026-04-30  
**作者**：BML Team
