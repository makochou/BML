# BML 后端 API 接口详细说明

本文档列出 BML 后端所有 HTTP 接口的路径、方法、权限、请求与响应说明。  
**统一约定**：所有接口前缀为 `server.servlet.context-path`，即 **`/api`**；响应体统一为 `Result<T>` 结构（`code`、`message`、`data`、`timestamp`、`traceId`）。

---

## 一、通用说明

### 1.1 统一响应结构

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { },
  "timestamp": 1700000000000,
  "traceId": "f2f0f8b3b6f54d0ea3d41b3a3f4e10b4"
}
```

- **code**：业务状态码（200 成功，4xx/5xx 及 2xxx 为业务错误码）
- **message**：可读提示信息
- **data**：业务数据，失败时多为 null
- **timestamp**：服务端生成时间戳
- **traceId**：链路追踪 ID，便于排查

### 1.2 认证方式

- **管理端接口**：请求头 `Authorization: Bearer <accessToken>`，Token 来自登录或刷新接口。
- **开放接口（OpenAPI）**：请求头需携带 `X-Bml-App-Key`、`X-Bml-Timestamp`、`X-Bml-Nonce`、`X-Bml-Sign`（及可选 `X-Bml-Sign-Version`），走签名校验，详见 [08-authorization-design.md](./08-authorization-design.md)。

### 1.3 权限码规范

格式：`module:resource:action`。接口上的 `@PreAuthorize("@ss.hasPermi('xxx')")` 表示需要对应权限才能访问。

---

## 二、认证中心 `/api/auth`

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|------|------|
| POST | `/api/auth/login` | 用户登录 | 否 | 无 |
| POST | `/api/auth/refresh` | 刷新 AccessToken | 否 | 无 |
| POST | `/api/auth/logout` | 用户登出 | 是 | 无 |
| GET  | `/api/auth/info`   | 获取当前用户信息 | 是 | 无 |
| GET  | `/api/auth/routers` | 获取路由菜单（动态路由） | 是 | 无 |

---

### POST /api/auth/login — 用户登录

- **说明**：账号密码登录，成功后返回双令牌（AccessToken + RefreshToken）。
- **请求体**（application/json）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

- **响应 data**（TokenVO）：

| 字段 | 类型 | 说明 |
|------|------|------|
| accessToken | string | 访问令牌，请求头 `Authorization: Bearer {accessToken}` |
| refreshToken | string | 刷新令牌，用于调用 `/auth/refresh` |
| expiresIn | long | AccessToken 有效期（秒），如 7200 表示 2 小时 |

---

### POST /api/auth/refresh — 刷新 Token

- **说明**：AccessToken 过期时，用 RefreshToken 换取新的 AccessToken；RefreshToken 本身不更新。
- **请求体**（application/json）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| refreshToken | string | 是 | 登录时返回的 refreshToken |

- **响应 data**（TokenVO）：通常仅含 `accessToken`、`expiresIn`（刷新接口可能不再次返回 refreshToken，以实际实现为准）。

---

### POST /api/auth/logout — 用户登出

- **说明**：清除 Redis 中的用户会话，当前所有 Token 立即失效。
- **请求头**：`Authorization: Bearer {accessToken}`
- **请求体**：无
- **响应 data**：无（null）

---

### GET /api/auth/info — 获取当前用户信息

- **说明**：返回当前登录用户基本信息、角色与权限标识，供前端渲染用户信息和按钮权限。
- **请求头**：`Authorization: Bearer {accessToken}`
- **响应 data**（UserInfoVO）：

| 字段 | 类型 | 说明 |
|------|------|------|
| user | object | 用户基本信息（id、username、realName、avatar、status 等） |
| roles | Set\<string\> | 角色标识集合，如 ["admin","common"] |
| permissions | Set\<string\> | 权限标识集合，如 ["system:user:list","*:*:*"] |

---

### GET /api/auth/routers — 获取路由菜单

- **说明**：按当前用户权限返回菜单树，前端据此生成动态路由（name、path、component、redirect、meta、children）。
- **请求头**：`Authorization: Bearer {accessToken}`
- **响应 data**：`List<RouterVO>`

| 字段 | 类型 | 说明 |
|------|------|------|
| name | string | 路由名称 |
| path | string | 路由路径 |
| component | string | 组件路径，如 dashboard/Workplace、api/ApiAccountManage |
| redirect | string | 重定向地址 |
| hidden | boolean | 是否隐藏菜单 |
| alwaysShow | boolean | 是否总是显示根菜单 |
| meta | object | 元信息（title、icon、noCache 等） |
| children | List\<RouterVO\> | 子路由 |

---

## 三、API 账号管理 `/api/account`

以下接口均需 **JWT 认证**，并具备相应权限码。

### 3.1 账号 CRUD 与分页

| 方法 | 路径 | 说明 | 权限码 |
|------|------|------|--------|
| GET | `/api/account/page` | 分页查询 API 账号 | api:account:list |
| GET | `/api/account/{id}` | 查询 API 账号详情 | api:account:query |
| GET | `/api/account/{id}/copy` | 按 ID 复制账号（获取副本数据，用于新建） | api:account:add |
| POST | `/api/account` | 新增 API 账号 | api:account:add |
| PUT | `/api/account/{id}` | 修改 API 账号 | api:account:edit |
| PUT | `/api/account/{id}/status` | 修改 API 账号状态 | api:account:edit |
| DELETE | `/api/account/{id}` | 删除 API 账号 | api:account:remove |
| PUT | `/api/account/{id}/secret/reset` | 重置 API 账号密钥 | api:account:reset |

#### GET /api/account/page — 分页查询

- **Query 参数**（继承 PageQuery + SysApiAccountPageQuery）：
  - **pageNum**：页码，默认 1  
  - **pageSize**：每页条数，默认 10  
  - **orderByColumn**、**isAsc**：排序字段与方向  
  - **accountId**、**accountName**、**description**、**accessKey**、**ownerName**、**ownerContact**、**systemName**、**systemCode**、**systemKeyword**、**accountType**、**clientType**、**accessEnvironment**、**signVersion**、**allowedScope**、**callbackUrl**、**remark**、**ipKeyword**、**textMatchMode**（fuzzy/exact）、**status**、**rateLimitMin**、**rateLimitMax**、**expireTimeStart**、**expireTimeEnd**、**createTimeStart**、**createTimeEnd**、**updateTimeStart**、**updateTimeEnd**
- **响应 data**：`PageResult<SysApiAccountVO>`（records、total、pageNum、pageSize）

#### GET /api/account/{id} — 账号详情

- **路径参数**：id — 账号 ID  
- **响应 data**：SysApiAccountDetailVO（不含 secretKey；含治理字段、授权数量等）

#### GET /api/account/{id}/copy — 复制账号副本

- **说明**：返回一份可用于“新建”的账号数据（不含 id、accessKey、secretKey 等不可复制字段）。
- **响应 data**：SysApiAccountDetailVO

#### POST /api/account — 新增账号

- **请求体**：CreateSysApiAccountCommand  
  - **accountName**（必填）、**description**、**accountType**（1/2）、**clientTypes**、**ownerName**（必填）、**ownerContact**（必填）、**systemName**（必填）、**systemCode**（必填）、**accessEnvironment**（必填，test/staging/production）、**ipWhitelist**、**environmentIpWhitelist**、**signVersion**（必填）、**allowedScopes**、**callbackUrl**、**rateLimit**、**expireTime**、**status**、**remark**
- **响应 data**：ApiCredentialVO（含 **secretKey**，仅此接口及重置密钥返回，需一次性保存）

#### PUT /api/account/{id} — 修改账号

- **请求体**：UpdateSysApiAccountCommand（与创建字段类似，按需传递）
- **响应 data**：无

#### PUT /api/account/{id}/status — 修改状态

- **请求体**：`{ "status": 1 }`（1 正常，0 禁用）
- **响应 data**：无

#### PUT /api/account/{id}/secret/reset — 重置密钥

- **响应 data**：ApiCredentialVO（含新的 accessKey、**secretKey**，需一次性保存）

---

### 3.2 兼容旧路径（与 3.1 对应，权限相同）

| 方法 | 路径 | 说明 | 权限码 |
|------|------|------|--------|
| GET | `/api/account/list` | 获取账号列表（非分页） | api:account:list |
| POST | `/api/account/add` | 新增账号（兼容） | api:account:add |
| PUT | `/api/account/edit` | 修改账号（兼容） | api:account:edit |
| DELETE | `/api/account/remove/{id}` | 删除账号（兼容） | api:account:remove |
| PUT | `/api/account/{id}/reset` | 重置密钥（兼容） | api:account:reset |

---

### 3.3 授权与回调（同前缀 `/api/account`）

| 方法 | 路径 | 说明 | 权限码 |
|------|------|------|--------|
| GET | `/api/account/{id}/authorization` | 获取账号授权快照（可选接口树 + 已选 apiIds） | api:account:authorize |
| PUT | `/api/account/{id}/authorization` | 保存账号授权（批量 apiIds） | api:account:authorize |
| GET | `/api/account/{id}/callback-log/page` | 分页查询指定账号的回调日志 | api:account:query |
| POST | `/api/account/{id}/callback/test` | 发送测试回调 | api:account:edit |
| POST | `/api/account/callback-log/{logId}/retry` | 手动重试单条回调日志 | api:account:edit |

#### GET /api/account/{id}/authorization

- **响应 data**：ApiAccountAuthorizationVO（账号信息、selectedApiIds、分组树、统计摘要）

#### PUT /api/account/{id}/authorization

- **请求体**：`{ "apiIds": [1, 2, 3] }`（SaveApiAccountAuthorizationCommand）
- **响应 data**：无

#### GET /api/account/{id}/callback-log/page

- **Query**：pageNum、pageSize、**callbackStatus**（0 待执行 1 重试中 2 成功 3 失败）
- **响应 data**：ApiCallbackLogPageVO（分页列表 + 汇总统计）

#### POST /api/account/{id}/callback/test

- **响应 data**：ApiCallbackLogVO（本次测试回调记录）

#### POST /api/account/callback-log/{logId}/retry

- **路径参数**：logId — 回调日志 ID  
- **响应 data**：ApiCallbackLogVO

---

## 四、API 接口列表 `/api/api-list`

| 方法 | 路径 | 说明 | 权限码 |
|------|------|------|--------|
| GET | `/api/api-list/tree` | 查询 API 接口目录树（模块→业务资源→接口，用于列表页） | system:apiList:list |

- **Query**：keyword、method、status、moduleName（均可选）
- **响应 data**：`List<ApiCatalogTreeNodeVO>`，节点含 id、label、type（MODULE/RESOURCE/API）、children；API 类型节点含 apiId、httpMethod、apiUrl、description、status

---

## 五、开放接口目录 `/api/openapi/registry`

| 方法 | 路径 | 说明 | 权限码 |
|------|------|------|--------|
| GET | `/api/openapi/registry/tree` | 查询开放接口目录树 | api:account:authorize |
| POST | `/api/openapi/registry/sync` | 同步开放接口目录（从项目纳管接口扫描入库） | api:account:sync |

#### GET /api/openapi/registry/tree

- **Query**：OpenApiRegistryTreeQuery — **keyword**、**method**、**status**、**moduleName**
- **响应 data**：`List<OpenApiGroupVO>`（按模块/控制器分组的接口树）

#### POST /api/openapi/registry/sync

- **请求体**：无  
- **响应 data**：OpenApiRegistrySyncResultVO（totalDiscovered、insertedCount、updatedCount、disabledCount、skippedCount 等）

---

## 六、开放 API 示例（OpenAPI 签名访问）`/api/open/api`

以下为开放给外部系统调用的示例接口，需走 **OpenAPI 签名**（X-Bml-App-Key、时间戳、nonce、签名等）。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/open/api/ping` | 连通性检测，用于验证 OpenAPI 签名链路 |

---

## 七、企业管理扩展 `/api/open/api/enterprise/*`

为企业管理前台提供的示例/扩展接口，可被 **JWT 或 OpenAPI 签名** 访问（视安全配置）。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/open/api/enterprise/dashboard/summary` | 企业管理业务概览 |
| GET | `/api/open/api/enterprise/company/page` | 分页查询企业档案 |
| GET | `/api/open/api/enterprise/company/{companyId}` | 企业档案详情 |
| GET | `/api/open/api/enterprise/system-account/page` | 分页查询企业系统账号 |
| GET | `/api/open/api/enterprise/system-account/{accountId}` | 企业系统账号详情 |

---

## 八、系统管理 `/api/system/*`

### 7.1 用户 `/api/system/user`

| 方法 | 路径 | 说明 | 权限码 |
|------|------|------|--------|
| GET | `/api/system/user/list` | 获取用户列表 | system:user:list |
| GET | `/api/system/user/{userId}` | 根据用户编号获取详细信息 | system:user:query |
| POST | `/api/system/user` | 新增用户 | system:user:add |
| PUT | `/api/system/user` | 修改用户 | system:user:edit |
| DELETE | `/api/system/user/{userId}` | 删除用户 | system:user:remove |

### 7.2 角色 `/api/system/role`

| 方法 | 路径 | 说明 | 权限码 |
|------|------|------|--------|
| GET | `/api/system/role/list` | 获取角色列表 | system:role:list |
| GET | `/api/system/role/{roleId}` | 根据角色编号获取详细信息 | system:role:query |
| POST | `/api/system/role` | 新增角色 | system:role:add |
| PUT | `/api/system/role` | 修改角色 | system:role:edit |
| DELETE | `/api/system/role/{roleId}` | 删除角色 | system:role:remove |

### 7.3 菜单 `/api/system/menu`

| 方法 | 路径 | 说明 | 权限码 |
|------|------|------|--------|
| GET | `/api/system/menu/list` | 获取菜单列表 | system:menu:list |
| GET | `/api/system/menu/{menuId}` | 根据菜单编号获取详细信息 | system:menu:query |
| POST | `/api/system/menu` | 新增菜单 | system:menu:add |
| PUT | `/api/system/menu` | 修改菜单 | system:menu:edit |
| DELETE | `/api/system/menu/{menuId}` | 删除菜单 | system:menu:remove |

### 7.4 部门 `/api/system/dept`

| 方法 | 路径 | 说明 | 权限码 |
|------|------|------|--------|
| GET | `/api/system/dept/list` | 获取部门树列表 | system:dept:list |
| GET | `/api/system/dept/{deptId}` | 根据部门编号获取详细信息 | system:dept:query |
| POST | `/api/system/dept` | 新增部门 | system:dept:add |
| PUT | `/api/system/dept` | 修改部门 | system:dept:edit |
| DELETE | `/api/system/dept/{deptId}` | 删除部门 | system:dept:remove |

### 7.5 告警 `/api/system/alert`

| 方法 | 路径 | 说明 | 权限码 |
|------|------|------|--------|
| GET | `/api/system/alert/unread-count` | 获取未读告警总数 | monitor:alert:list |
| GET | `/api/system/alert/list` | 获取最近系统监控告警列表 | monitor:alert:list |
| GET | `/api/system/alert/latest` | 增量轮询 — 获取指定 ID 之后的最新告警 | monitor:alert:list |
| PUT | `/api/system/alert/read/{id}` | 将指定告警置为已读 | monitor:alert:edit |
| PUT | `/api/system/alert/read-all` | 将所有未读告警一键置为已读 | monitor:alert:edit |
| GET | `/api/system/alert/dates` | 获取存在告警的日期列表 | monitor:alert:list |
| GET | `/api/system/alert/by-date` | 按日期查询告警列表 | monitor:alert:list |
| DELETE | `/api/system/alert/{id}` | 删除指定告警记录 | monitor:alert:remove |

### 7.6 服务器监控 `/api/system/monitor`

| 方法 | 路径 | 说明 | 权限码 |
|------|------|------|--------|
| GET | `/api/system/monitor/server` | 全量抓取服务器、JVM、磁盘等硬件探针态 | monitor:server:list |
| POST | `/api/system/monitor/gc` | 强制触发 JVM 垃圾回收 | monitor:server:gc |

---

## 九、接口索引（按路径排序）

| 方法 | 完整路径 | 说明 |
|------|----------|------|
| POST | /api/auth/login | 登录 |
| POST | /api/auth/refresh | 刷新 Token |
| POST | /api/auth/logout | 登出 |
| GET | /api/auth/info | 当前用户信息 |
| GET | /api/auth/routers | 动态路由菜单 |
| GET | /api/api-list/tree | API 接口目录树（列表页） |
| GET | /api/account/page | 分页查询 API 账号 |
| GET | /api/account/list | 账号列表（兼容） |
| GET | /api/account/{id} | 账号详情 |
| GET | /api/account/{id}/copy | 复制账号副本 |
| GET | /api/account/{id}/authorization | 授权快照 |
| GET | /api/account/{id}/callback-log/page | 回调日志分页 |
| POST | /api/account | 新增账号 |
| POST | /api/account/add | 新增账号（兼容） |
| POST | /api/account/{id}/callback/test | 测试回调 |
| POST | /api/account/callback-log/{logId}/retry | 重试回调 |
| PUT | /api/account/{id} | 修改账号 |
| PUT | /api/account/edit | 修改账号（兼容） |
| PUT | /api/account/{id}/status | 修改状态 |
| PUT | /api/account/{id}/authorization | 保存授权 |
| PUT | /api/account/{id}/secret/reset | 重置密钥 |
| PUT | /api/account/{id}/reset | 重置密钥（兼容） |
| DELETE | /api/account/{id} | 删除账号 |
| DELETE | /api/account/remove/{id} | 删除账号（兼容） |
| GET | /api/openapi/registry/tree | 开放接口目录树 |
| POST | /api/openapi/registry/sync | 同步开放接口目录 |
| GET | /api/open/api/ping | OpenAPI 连通性检测 |
| GET | /api/open/api/enterprise/dashboard/summary | 企业概览 |
| GET | /api/open/api/enterprise/company/page | 企业分页 |
| GET | /api/open/api/enterprise/company/{companyId} | 企业详情 |
| GET | /api/open/api/enterprise/system-account/page | 系统账号分页 |
| GET | /api/open/api/enterprise/system-account/{accountId} | 系统账号详情 |
| GET | /api/system/user/list | 用户列表 |
| GET | /api/system/user/{userId} | 用户详情 |
| POST | /api/system/user | 新增用户 |
| PUT | /api/system/user | 修改用户 |
| DELETE | /api/system/user/{userId} | 删除用户 |
| GET | /api/system/role/list | 角色列表 |
| GET | /api/system/role/{roleId} | 角色详情 |
| POST | /api/system/role | 新增角色 |
| PUT | /api/system/role | 修改角色 |
| DELETE | /api/system/role/{roleId} | 删除角色 |
| GET | /api/system/menu/list | 菜单列表 |
| GET | /api/system/menu/{menuId} | 菜单详情 |
| POST | /api/system/menu | 新增菜单 |
| PUT | /api/system/menu | 修改菜单 |
| DELETE | /api/system/menu/{menuId} | 删除菜单 |
| GET | /api/system/dept/list | 部门树 |
| GET | /api/system/dept/{deptId} | 部门详情 |
| POST | /api/system/dept | 新增部门 |
| PUT | /api/system/dept | 修改部门 |
| DELETE | /api/system/dept/{deptId} | 删除部门 |
| GET | /api/system/alert/unread-count | 未读告警数 |
| GET | /api/system/alert/list | 告警列表 |
| GET | /api/system/alert/latest | 最新告警 |
| GET | /api/system/alert/dates | 告警日期列表 |
| GET | /api/system/alert/by-date | 按日期查告警 |
| PUT | /api/system/alert/read/{id} | 单条已读 |
| PUT | /api/system/alert/read-all | 全部已读 |
| DELETE | /api/system/alert/{id} | 删除告警 |
| GET | /api/system/monitor/server | 服务器监控数据 |
| POST | /api/system/monitor/gc | 触发 GC |

---

*文档与代码一致；若与 Swagger 或实际 Controller 有差异，以代码为准。*
