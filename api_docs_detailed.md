# 项目接口极度详细说明

该文档通过代码静态分析生成，包含请求参数、数据模型(DTO)详情等核心信息。

## API账号授权管理 (SysApiAccountAuthorizationController)
### 获取账号授权快照
- **请求路径**: `/account/{id}/authorization`
- **请求方式**: `GET`
- **控制器方法**: `getAuthorization()`
- **返回类型**: `Result<ApiAccountAuthorizationVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `id` | `Long` | `@PathVariable` | - |
---

### 保存账号授权
- **请求路径**: `/account/{id}/authorization`
- **请求方式**: `PUT`
- **控制器方法**: `saveAuthorization()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `id` | `Long` | `@PathVariable` | - |
| `command` | `SaveApiAccountAuthorizationCommand` | `@RequestBody` | - |

> **SaveApiAccountAuthorizationCommand 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `apiIds` | `List<Long>` | 授权接口ID列表 |

---

## API账号回调日志管理 (SysApiAccountCallbackController)
### 分页查询指定账号的回调日志
- **请求路径**: `/account/{id}/callback-log/page`
- **请求方式**: `GET`
- **控制器方法**: `pageLogs()`
- **返回类型**: `Result<ApiCallbackLogPageVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `id` | `Long` | `@PathVariable` | - |
| `query` | `ApiCallbackLogPageQuery` | `` | - |

> **ApiCallbackLogPageQuery 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `callbackStatus` | `Integer` | 回调状态：0待执行 1重试中 2成功 3失败 |

---

### 发送测试回调
- **请求路径**: `/account/{id}/callback/test`
- **请求方式**: `POST`
- **控制器方法**: `triggerTestCallback()`
- **返回类型**: `Result<ApiCallbackLogVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `id` | `Long` | `@PathVariable` | - |
---

### 手动重试回调日志
- **请求路径**: `/account/callback-log/{logId}/retry`
- **请求方式**: `POST`
- **控制器方法**: `retryCallback()`
- **返回类型**: `Result<ApiCallbackLogVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `logId` | `Long` | `@PathVariable` | - |
---

## API账号管理 (SysApiAccountController)
### 分页查询API账号
- **请求路径**: `/account/page`
- **请求方式**: `GET`
- **控制器方法**: `page()`
- **返回类型**: `Result<PageResult<SysApiAccountVO>>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `query` | `SysApiAccountPageQuery` | `` | - |

> **SysApiAccountPageQuery 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `accountName` | `String` | 账号名称，支持模糊匹配 |
| `description` | `String` | 账号用途描述，支持模糊匹配 |
| `accountId` | `Long` | 账号ID |
| `accessKey` | `String` | AccessKey |
| `ownerName` | `String` | 负责人 |
| `ownerContact` | `String` | 负责人联系方式 |
| `systemName` | `String` | 系统名称 |
| `systemCode` | `String` | 系统编码 |
| `accountType` | `Integer` | 账号类型：1-内部账号，2-外部账号 |
| `clientType` | `String` | 调用客户端类型代码，例如 web、app、mini_program |
| `systemKeyword` | `String` | 业务系统关键字，支持匹配系统名称或系统编码 |
| `accessEnvironment` | `String` | 接入环境代码：test/staging/production |
| `signVersion` | `String` | 签名版本 |
| `allowedScope` | `String` | 授权范围标签，精确匹配单个 scope |
| `callbackUrl` | `String` | 回调地址 |
| `remark` | `String` | 备注 |
| `ipKeyword` | `String` | 白名单IP关键字 |
| `textMatchMode` | `String` | 字符匹配模式：fuzzy-模糊，exact-精准 |
| `rateLimitMin` | `Integer` | 限流下限 |
| `rateLimitMax` | `Integer` | 限流上限 |
| `expireTimeStart` | `String` | 过期开始时间，格式：yyyy-MM-dd HH:mm:ss |
| `expireTimeEnd` | `String` | 过期结束时间，格式：yyyy-MM-dd HH:mm:ss |
| `createTimeStart` | `String` | 创建开始时间，格式：yyyy-MM-dd HH:mm:ss |
| `createTimeEnd` | `String` | 创建结束时间，格式：yyyy-MM-dd HH:mm:ss |
| `updateTimeStart` | `String` | 更新开始时间，格式：yyyy-MM-dd HH:mm:ss |
| `updateTimeEnd` | `String` | 更新结束时间，格式：yyyy-MM-dd HH:mm:ss |
| `status` | `Integer` | 状态：1-启用，0-停用 |

---

### 查询API账号详情
- **请求路径**: `/account/{id}`
- **请求方式**: `GET`
- **控制器方法**: `getInfo()`
- **返回类型**: `Result<SysApiAccountDetailVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `id` | `Long` | `@PathVariable` | - |
---

### 按 ID 复制 API 账号副本
- **请求路径**: `/account/{id}/copy`
- **请求方式**: `GET`
- **控制器方法**: `copy()`
- **返回类型**: `Result<SysApiAccountDetailVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `id` | `Long` | `@PathVariable` | - |
---

### 新增API账号
- **请求路径**: `/account`
- **请求方式**: `POST`
- **控制器方法**: `create()`
- **返回类型**: `Result<ApiCredentialVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `command` | `CreateSysApiAccountCommand` | `@RequestBody` | - |

> **CreateSysApiAccountCommand 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `accountName` | `String` | 账号名称 |
| `description` | `String` | 账号用途描述，用于说明该账号的业务场景和使用目的 |
| `accountType` | `Integer` | 账号类型：1-内部账号，2-外部账号 |
| `clientTypes` | `List<String>` | 调用客户端类型代码集合，例如 web、app、mini_program |
| `ownerName` | `String` | 接入方负责人 |
| `ownerContact` | `String` | 负责人联系方式 |
| `systemName` | `String` | 业务系统名称 |
| `systemCode` | `String` | 业务系统编码 |
| `accessEnvironment` | `String` | 接入环境代码：test/staging/production |
| `ipWhitelist` | `List<String>` | IP 白名单，支持单个 IP 或 CIDR |
| `signVersion` | `String` | 签名算法版本，例如 v1 |
| `allowedScopes` | `List<String>` | 授权范围标签集合，例如 read、write、admin |
| `callbackUrl` | `String` | 业务回调地址 |
| `rateLimit` | `Integer` | 每分钟限流阈值 |
| `expireTime` | `LocalDateTime` | 过期时间 |
| `status` | `Integer` | 状态：1-启用，0-停用 |
| `remark` | `String` | 备注 |

---

### 修改API账号
- **请求路径**: `/account/{id}`
- **请求方式**: `PUT`
- **控制器方法**: `update()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `id` | `Long` | `@PathVariable` | - |
| `command` | `UpdateSysApiAccountCommand` | `@RequestBody` | - |

> **UpdateSysApiAccountCommand 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `accountName` | `String` | 账号名称 |
| `description` | `String` | 账号用途描述，用于说明该账号的业务场景和使用目的 |
| `accountType` | `Integer` | 账号类型：1-内部账号，2-外部账号 |
| `clientTypes` | `List<String>` | 调用客户端类型代码集合，例如 web、app、mini_program |
| `ownerName` | `String` | 接入方负责人 |
| `ownerContact` | `String` | 负责人联系方式 |
| `systemName` | `String` | 业务系统名称 |
| `systemCode` | `String` | 业务系统编码 |
| `accessEnvironment` | `String` | 接入环境代码：test/staging/production |
| `ipWhitelist` | `List<String>` | IP 白名单，支持单个 IP 或 CIDR |
| `signVersion` | `String` | 签名算法版本，例如 v1 |
| `allowedScopes` | `List<String>` | 授权范围标签集合，例如 read、write、admin |
| `callbackUrl` | `String` | 业务回调地址 |
| `rateLimit` | `Integer` | 每分钟限流阈值 |
| `expireTime` | `LocalDateTime` | 过期时间 |
| `status` | `Integer` | 状态：1-启用，0-停用 |
| `remark` | `String` | 备注 |

---

### 修改API账号状态
- **请求路径**: `/account/{id}/status`
- **请求方式**: `PUT`
- **控制器方法**: `updateStatus()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `id` | `Long` | `@PathVariable` | - |
| `command` | `UpdateSysApiAccountStatusCommand` | `@RequestBody` | - |

> **UpdateSysApiAccountStatusCommand 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `status` | `Integer` | 状态(1正常 0禁用) |

---

### 删除API账号
- **请求路径**: `/account/{id}`
- **请求方式**: `DELETE`
- **控制器方法**: `remove()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `id` | `Long` | `@PathVariable` | - |
---

### 重置API账号密钥
- **请求路径**: `/account/{id}/secret/reset`
- **请求方式**: `PUT`
- **控制器方法**: `resetSecret()`
- **返回类型**: `Result<ApiCredentialVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `id` | `Long` | `@PathVariable` | - |
---

### 获取API账号列表(兼容旧接口)
- **请求路径**: `/account/list`
- **请求方式**: `GET`
- **控制器方法**: `list()`
- **返回类型**: `Result<List<SysApiAccountVO>>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `dto` | `SysApiAccountDTO` | `` | - |

> **SysApiAccountDTO 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | API 账号传输对象 |
| `accountName` | `String` | 账号名称 |
| `description` | `String` | 账号用途描述 |
| `accountType` | `Integer` | 账号类型：1-内部账号，2-外部账号 |
| `clientTypes` | `List<String>` | 调用客户端类型代码集合 |
| `clientType` | `String` | 查询时使用的单个客户端类型代码 |
| `ownerName` | `String` | 接入方负责人 |
| `ownerContact` | `String` | 负责人联系方式 |
| `systemName` | `String` | 业务系统名称 |
| `systemCode` | `String` | 业务系统编码 |
| `systemKeyword` | `String` | 业务系统关键字，用于兼容旧接口查询系统名称或系统编码 |
| `accessEnvironment` | `String` | 接入环境代码：test/staging/production |
| `ipWhitelist` | `List<String>` | IP 白名单，支持单个 IP 或 CIDR |
| `signVersion` | `String` | 签名算法版本，例如 v1 |
| `allowedScopes` | `List<String>` | 授权范围标签集合 |
| `callbackUrl` | `String` | 业务回调地址 |
| `rateLimit` | `Integer` | 每分钟限流阈值 |
| `expireTime` | `LocalDateTime` | 过期时间 |
| `status` | `Integer` | 状态：1-启用，0-停用 |
| `remark` | `String` | 备注 |

---

### 新增API账号(兼容旧接口)
- **请求路径**: `/account/add`
- **请求方式**: `POST`
- **控制器方法**: `add()`
- **返回类型**: `Result<ApiCredentialVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `dto` | `SysApiAccountDTO` | `@RequestBody` | - |

> **SysApiAccountDTO 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | API 账号传输对象 |
| `accountName` | `String` | 账号名称 |
| `description` | `String` | 账号用途描述 |
| `accountType` | `Integer` | 账号类型：1-内部账号，2-外部账号 |
| `clientTypes` | `List<String>` | 调用客户端类型代码集合 |
| `clientType` | `String` | 查询时使用的单个客户端类型代码 |
| `ownerName` | `String` | 接入方负责人 |
| `ownerContact` | `String` | 负责人联系方式 |
| `systemName` | `String` | 业务系统名称 |
| `systemCode` | `String` | 业务系统编码 |
| `systemKeyword` | `String` | 业务系统关键字，用于兼容旧接口查询系统名称或系统编码 |
| `accessEnvironment` | `String` | 接入环境代码：test/staging/production |
| `ipWhitelist` | `List<String>` | IP 白名单，支持单个 IP 或 CIDR |
| `signVersion` | `String` | 签名算法版本，例如 v1 |
| `allowedScopes` | `List<String>` | 授权范围标签集合 |
| `callbackUrl` | `String` | 业务回调地址 |
| `rateLimit` | `Integer` | 每分钟限流阈值 |
| `expireTime` | `LocalDateTime` | 过期时间 |
| `status` | `Integer` | 状态：1-启用，0-停用 |
| `remark` | `String` | 备注 |

---

### 修改API账号(兼容旧接口)
- **请求路径**: `/account/edit`
- **请求方式**: `PUT`
- **控制器方法**: `edit()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `dto` | `SysApiAccountDTO` | `@RequestBody` | - |

> **SysApiAccountDTO 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | API 账号传输对象 |
| `accountName` | `String` | 账号名称 |
| `description` | `String` | 账号用途描述 |
| `accountType` | `Integer` | 账号类型：1-内部账号，2-外部账号 |
| `clientTypes` | `List<String>` | 调用客户端类型代码集合 |
| `clientType` | `String` | 查询时使用的单个客户端类型代码 |
| `ownerName` | `String` | 接入方负责人 |
| `ownerContact` | `String` | 负责人联系方式 |
| `systemName` | `String` | 业务系统名称 |
| `systemCode` | `String` | 业务系统编码 |
| `systemKeyword` | `String` | 业务系统关键字，用于兼容旧接口查询系统名称或系统编码 |
| `accessEnvironment` | `String` | 接入环境代码：test/staging/production |
| `ipWhitelist` | `List<String>` | IP 白名单，支持单个 IP 或 CIDR |
| `signVersion` | `String` | 签名算法版本，例如 v1 |
| `allowedScopes` | `List<String>` | 授权范围标签集合 |
| `callbackUrl` | `String` | 业务回调地址 |
| `rateLimit` | `Integer` | 每分钟限流阈值 |
| `expireTime` | `LocalDateTime` | 过期时间 |
| `status` | `Integer` | 状态：1-启用，0-停用 |
| `remark` | `String` | 备注 |

---

### 删除API账号(兼容旧接口)
- **请求路径**: `/account/remove/{id}`
- **请求方式**: `DELETE`
- **控制器方法**: `removeLegacy()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `id` | `Long` | `@PathVariable` | - |
---

### 重置密钥(兼容旧接口)
- **请求路径**: `/account/{id}/reset`
- **请求方式**: `PUT`
- **控制器方法**: `resetSecretLegacy()`
- **返回类型**: `Result<ApiCredentialVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `id` | `Long` | `@PathVariable` | - |
---

## 开放接口目录管理 (SysOpenApiRegistryController)
### 查询开放接口目录树
- **请求路径**: `/openapi/registry/tree`
- **请求方式**: `GET`
- **控制器方法**: `tree()`
- **返回类型**: `Result<List<OpenApiGroupVO>>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `query` | `OpenApiRegistryTreeQuery` | `` | - |

> **OpenApiRegistryTreeQuery 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `keyword` | `String` | 关键词，匹配接口名称、路径、描述 |
| `method` | `String` | HTTP方法 |
| `status` | `Integer` | 状态(1正常 0停用) |
| `moduleName` | `String` | 模块名称 |

---

### 同步开放接口目录
- **请求路径**: `/openapi/registry/sync`
- **请求方式**: `POST`
- **控制器方法**: `sync()`
- **返回类型**: `Result<OpenApiRegistrySyncResultVO>`

**请求参数**: 无

---

## 企业开放接口-企业档案 (OpenEnterpriseCompanyController)
### 分页查询企业档案
- **请求路径**: `/open/api/enterprise/company/page`
- **请求方式**: `GET`
- **控制器方法**: `page()`
- **返回类型**: `Result<PageResult<OpenEnterpriseCompanyVO>>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `query` | `EnterpriseCompanyPageQuery` | `` | - |

> **EnterpriseCompanyPageQuery 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `keyword` | `String` | 关键字，支持企业名称、企业编码、联系人模糊匹配 |
| `status` | `Integer` | 企业状态：1-启用，0-停用 |
| `industry` | `String` | 所属行业 |

---

### 查询企业档案详情
- **请求路径**: `/open/api/enterprise/company/{companyId}`
- **请求方式**: `GET`
- **控制器方法**: `detail()`
- **返回类型**: `Result<OpenEnterpriseCompanyVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `companyId` | `Long` | `@PathVariable` | - |
---

## 企业开放接口-业务概览 (OpenEnterpriseDashboardController)
### 查询企业管理业务概览
- **请求路径**: `/open/api/enterprise/dashboard/summary`
- **请求方式**: `GET`
- **控制器方法**: `summary()`
- **返回类型**: `Result<OpenEnterpriseDashboardVO>`

**请求参数**: 无

---

## 企业开放接口-系统账号 (OpenEnterpriseSystemAccountController)
### 分页查询企业系统账号
- **请求路径**: `/open/api/enterprise/system-account/page`
- **请求方式**: `GET`
- **控制器方法**: `page()`
- **返回类型**: `Result<PageResult<OpenEnterpriseSystemAccountVO>>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `query` | `EnterpriseSystemAccountPageQuery` | `` | - |

> **EnterpriseSystemAccountPageQuery 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `keyword` | `String` | 关键字，支持账号、姓名、所属企业模糊匹配 |
| `status` | `Integer` | 账号状态：1-启用，0-停用 |
| `accountType` | `String` | 账号类型，例如：admin、operator、auditor |
| `companyId` | `Long` | 所属企业ID |

---

### 查询企业系统账号详情
- **请求路径**: `/open/api/enterprise/system-account/{accountId}`
- **请求方式**: `GET`
- **控制器方法**: `detail()`
- **返回类型**: `Result<OpenEnterpriseSystemAccountVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `accountId` | `Long` | `@PathVariable` | - |
---

## 认证中心 (AuthController)
### 用户登录
- **请求路径**: `/auth/login`
- **请求方式**: `POST`
- **控制器方法**: `login()`
- **返回类型**: `Result<TokenVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `loginBody` | `LoginBody` | `@RequestBody` | - |

> **LoginBody 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `username` | `String` | Username |
| `password` | `String` | Password |

---

### 刷新Token
- **请求路径**: `/auth/refresh`
- **请求方式**: `POST`
- **控制器方法**: `refresh()`
- **返回类型**: `Result<TokenVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `dto` | `RefreshTokenDTO` | `@RequestBody` | - |

> **RefreshTokenDTO 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `refreshToken` | `String` | 刷新令牌（RefreshToken） |

---

### 用户登出
- **请求路径**: `/auth/logout`
- **请求方式**: `POST`
- **控制器方法**: `logout()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `request` | `HttpServletRequest` | `` | - |
---

### 获取当前用户信息
- **请求路径**: `/auth/info`
- **请求方式**: `GET`
- **控制器方法**: `getInfo()`
- **返回类型**: `Result<UserInfoVO>`

**请求参数**: 无

---

### 获取路由菜单
- **请求路径**: `/auth/routers`
- **请求方式**: `GET`
- **控制器方法**: `getRouters()`
- **返回类型**: `Result<List<RouterVO>>`

**请求参数**: 无

---

## 系统告警通知接口 (SysAlertController)
### 获取未读的告警总数
- **请求路径**: `/system/alert/unread-count`
- **请求方式**: `GET`
- **控制器方法**: `getUnreadCount()`
- **返回类型**: `Result<Long>`

**请求参数**: 无

---

### 获取最近的系统监控告警列表
- **请求路径**: `/system/alert/list`
- **请求方式**: `GET`
- **控制器方法**: `getRecentAlerts()`
- **返回类型**: `Result<List<SysAlertVO>>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| - | `@Parameter(description = "返回条数，默认50"` | - | 无法精确解析参数 |
---

### 增量轮询 — 获取指定 ID 之后的最新告警
- **请求路径**: `/system/alert/latest`
- **请求方式**: `GET`
- **控制器方法**: `getLatestAlerts()`
- **返回类型**: `Result<List<SysAlertVO>>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| - | `@Parameter(description = "上次已知的最大告警 ID，首次可不传"` | - | 无法精确解析参数 |
---

### 将指定的告警置为已读
- **请求路径**: `/system/alert/read/{id}`
- **请求方式**: `PUT`
- **控制器方法**: `markAsRead()`
- **返回类型**: `Result<Boolean>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| - | `@PathVariable("id"` | - | 无法精确解析参数 |
---

### 将所有未读告警一键置为已读
- **请求路径**: `/system/alert/read-all`
- **请求方式**: `PUT`
- **控制器方法**: `markAllAsRead()`
- **返回类型**: `Result<Integer>`

**请求参数**: 无

---

### 获取存在告警的日期列表
- **请求路径**: `/system/alert/dates`
- **请求方式**: `GET`
- **控制器方法**: `getAlertDates()`
- **返回类型**: `Result<List<String>>`

**请求参数**: 无

---

### 按日期查询告警列表
- **请求路径**: `/system/alert/by-date`
- **请求方式**: `GET`
- **控制器方法**: `getAlertsByDate()`
- **返回类型**: `Result<List<SysAlertVO>>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| - | `@Parameter(description = "查询日期 (yyyy-MM-dd` | - | 无法精确解析参数 |
---

### 删除指定的告警记录
- **请求路径**: `/system/alert/{id}`
- **请求方式**: `DELETE`
- **控制器方法**: `deleteAlert()`
- **返回类型**: `Result<Boolean>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| - | `@PathVariable("id"` | - | 无法精确解析参数 |
---

## 部门管理 (SysDeptController)
### 获取部门树列表
- **请求路径**: `/system/dept/list`
- **请求方式**: `GET`
- **控制器方法**: `list()`
- **返回类型**: `Result<List<SysDeptVO>>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `dto` | `SysDeptDTO` | `` | - |

> **SysDeptDTO 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 部门ID |
| `parentId` | `Long` | 父部门ID |
| `deptName` | `String` | 部门名称 |
| `sort` | `Integer` | 显示顺序 |
| `leader` | `String` | 负责人 |
| `phone` | `String` | 联系电话 |
| `email` | `String` | 邮箱 |
| `status` | `Integer` | 状态 (1:正常 0:停用) |

---

### 根据部门编号获取详细信息
- **请求路径**: `/system/dept/{deptId}`
- **请求方式**: `GET`
- **控制器方法**: `getInfo()`
- **返回类型**: `Result<SysDeptVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `deptId` | `Long` | `@PathVariable` | - |
---

### 新增部门
- **请求路径**: `/system/dept`
- **请求方式**: `POST`
- **控制器方法**: `add()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `dto` | `SysDeptDTO` | `@RequestBody` | - |

> **SysDeptDTO 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 部门ID |
| `parentId` | `Long` | 父部门ID |
| `deptName` | `String` | 部门名称 |
| `sort` | `Integer` | 显示顺序 |
| `leader` | `String` | 负责人 |
| `phone` | `String` | 联系电话 |
| `email` | `String` | 邮箱 |
| `status` | `Integer` | 状态 (1:正常 0:停用) |

---

### 修改部门
- **请求路径**: `/system/dept`
- **请求方式**: `PUT`
- **控制器方法**: `edit()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `dto` | `SysDeptDTO` | `@RequestBody` | - |

> **SysDeptDTO 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 部门ID |
| `parentId` | `Long` | 父部门ID |
| `deptName` | `String` | 部门名称 |
| `sort` | `Integer` | 显示顺序 |
| `leader` | `String` | 负责人 |
| `phone` | `String` | 联系电话 |
| `email` | `String` | 邮箱 |
| `status` | `Integer` | 状态 (1:正常 0:停用) |

---

### 删除部门
- **请求路径**: `/system/dept/{deptId}`
- **请求方式**: `DELETE`
- **控制器方法**: `remove()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `deptId` | `Long` | `@PathVariable` | - |
---

## 菜单管理 (SysMenuController)
### 获取菜单列表
- **请求路径**: `/system/menu/list`
- **请求方式**: `GET`
- **控制器方法**: `list()`
- **返回类型**: `Result<List<SysMenuVO>>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `dto` | `SysMenuDTO` | `` | - |

> **SysMenuDTO 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 菜单ID |
| `parentId` | `Long` | 父菜单ID |
| `menuName` | `String` | 菜单名称 |
| `menuType` | `String` | 菜单类型 (M:目录 C:菜单 B:按钮) |
| `path` | `String` | 路由路径 |
| `component` | `String` | 组件路径 |
| `perms` | `String` | 权限标识 |
| `icon` | `String` | 菜单图标 |
| `sort` | `Integer` | 显示顺序 |
| `visible` | `Integer` | 是否显示 (1:显示 0:隐藏) |
| `status` | `Integer` | 状态 (1:正常 0:停用) |
| `isFrame` | `Integer` | 是否外链 (1:是 0:否) |
| `remark` | `String` | 备注 |

---

### 根据菜单编号获取详细信息
- **请求路径**: `/system/menu/{menuId}`
- **请求方式**: `GET`
- **控制器方法**: `getInfo()`
- **返回类型**: `Result<SysMenuVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `menuId` | `Long` | `@PathVariable` | - |
---

### 新增菜单
- **请求路径**: `/system/menu`
- **请求方式**: `POST`
- **控制器方法**: `add()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `dto` | `SysMenuDTO` | `@RequestBody` | - |

> **SysMenuDTO 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 菜单ID |
| `parentId` | `Long` | 父菜单ID |
| `menuName` | `String` | 菜单名称 |
| `menuType` | `String` | 菜单类型 (M:目录 C:菜单 B:按钮) |
| `path` | `String` | 路由路径 |
| `component` | `String` | 组件路径 |
| `perms` | `String` | 权限标识 |
| `icon` | `String` | 菜单图标 |
| `sort` | `Integer` | 显示顺序 |
| `visible` | `Integer` | 是否显示 (1:显示 0:隐藏) |
| `status` | `Integer` | 状态 (1:正常 0:停用) |
| `isFrame` | `Integer` | 是否外链 (1:是 0:否) |
| `remark` | `String` | 备注 |

---

### 修改菜单
- **请求路径**: `/system/menu`
- **请求方式**: `PUT`
- **控制器方法**: `edit()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `dto` | `SysMenuDTO` | `@RequestBody` | - |

> **SysMenuDTO 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 菜单ID |
| `parentId` | `Long` | 父菜单ID |
| `menuName` | `String` | 菜单名称 |
| `menuType` | `String` | 菜单类型 (M:目录 C:菜单 B:按钮) |
| `path` | `String` | 路由路径 |
| `component` | `String` | 组件路径 |
| `perms` | `String` | 权限标识 |
| `icon` | `String` | 菜单图标 |
| `sort` | `Integer` | 显示顺序 |
| `visible` | `Integer` | 是否显示 (1:显示 0:隐藏) |
| `status` | `Integer` | 状态 (1:正常 0:停用) |
| `isFrame` | `Integer` | 是否外链 (1:是 0:否) |
| `remark` | `String` | 备注 |

---

### 删除菜单
- **请求路径**: `/system/menu/{menuId}`
- **请求方式**: `DELETE`
- **控制器方法**: `remove()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `menuId` | `Long` | `@PathVariable` | - |
---

## 角色管理 (SysRoleController)
### 获取角色列表
- **请求路径**: `/system/role/list`
- **请求方式**: `GET`
- **控制器方法**: `list()`
- **返回类型**: `Result<List<SysRoleVO>>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `dto` | `SysRoleDTO` | `` | - |

> **SysRoleDTO 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 角色ID (更新时必填) |
| `roleName` | `String` | 角色名称 |
| `roleCode` | `String` | 角色编码 |
| `sort` | `Integer` | 显示顺序 |
| `dataScope` | `Integer` | 数据范围 (1:全部 2:本组织及下级 3:仅本组织 4:本部门及下级 5:仅本部门 6:仅本人 7:自定义) |
| `status` | `Integer` | 状态 (1:正常 0:停用) |
| `remark` | `String` | 备注 |
| `menuIds` | `List<Long>` | 菜单组 |

---

### 根据角色编号获取详细信息
- **请求路径**: `/system/role/{roleId}`
- **请求方式**: `GET`
- **控制器方法**: `getInfo()`
- **返回类型**: `Result<SysRoleVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `roleId` | `Long` | `@PathVariable` | - |
---

### 新增角色
- **请求路径**: `/system/role`
- **请求方式**: `POST`
- **控制器方法**: `add()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `dto` | `SysRoleDTO` | `@RequestBody` | - |

> **SysRoleDTO 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 角色ID (更新时必填) |
| `roleName` | `String` | 角色名称 |
| `roleCode` | `String` | 角色编码 |
| `sort` | `Integer` | 显示顺序 |
| `dataScope` | `Integer` | 数据范围 (1:全部 2:本组织及下级 3:仅本组织 4:本部门及下级 5:仅本部门 6:仅本人 7:自定义) |
| `status` | `Integer` | 状态 (1:正常 0:停用) |
| `remark` | `String` | 备注 |
| `menuIds` | `List<Long>` | 菜单组 |

---

### 修改角色
- **请求路径**: `/system/role`
- **请求方式**: `PUT`
- **控制器方法**: `edit()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `dto` | `SysRoleDTO` | `@RequestBody` | - |

> **SysRoleDTO 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 角色ID (更新时必填) |
| `roleName` | `String` | 角色名称 |
| `roleCode` | `String` | 角色编码 |
| `sort` | `Integer` | 显示顺序 |
| `dataScope` | `Integer` | 数据范围 (1:全部 2:本组织及下级 3:仅本组织 4:本部门及下级 5:仅本部门 6:仅本人 7:自定义) |
| `status` | `Integer` | 状态 (1:正常 0:停用) |
| `remark` | `String` | 备注 |
| `menuIds` | `List<Long>` | 菜单组 |

---

### 删除角色
- **请求路径**: `/system/role/{roleId}`
- **请求方式**: `DELETE`
- **控制器方法**: `remove()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `roleId` | `Long` | `@PathVariable` | - |
---

## 服务器监控资源获取接口 (SysServerMonitorController)
### 全量抓取服务器、JVM、磁盘最新硬件物理探针态
- **请求路径**: `/system/monitor/server`
- **请求方式**: `GET`
- **控制器方法**: `getServerInfo()`
- **返回类型**: `Result<ServerInfoVO>`

**请求参数**: 无

---

### 强制发送JVM垃圾回收指令(GC)，尝试释放内存
- **请求路径**: `/system/monitor/gc`
- **请求方式**: `POST`
- **控制器方法**: `cleanJvmMemory()`
- **返回类型**: `Result<String>`

**请求参数**: 无

---

## 用户管理 (SysUserController)
### 获取用户列表
- **请求路径**: `/system/user/list`
- **请求方式**: `GET`
- **控制器方法**: `list()`
- **返回类型**: `Result<List<SysUserVO>>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `dto` | `SysUserDTO` | `` | - |

> **SysUserDTO 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 用户ID (更新时必填) |
| `username` | `String` | 用户名 |
| `nickname` | `String` | 用户昵称 |
| `email` | `String` | 用户邮箱 |
| `phone` | `String` | 手机号码 |
| `gender` | `Integer` | 用户性别 (0:未知 1:男 2:女) |
| `avatar` | `String` | 头像地址 |
| `password` | `String` | 密码 (创建时必填) |
| `status` | `Integer` | 状态 (1:正常 0:停用) |
| `deptId` | `Long` | 部门ID |
| `roleIds` | `List<Long>` | 角色ID列表 |
| `postIds` | `List<Long>` | 岗位ID列表 |
| `remark` | `String` | 备注 |

---

### 根据用户编号获取详细信息
- **请求路径**: `/system/user/{userId}`
- **请求方式**: `GET`
- **控制器方法**: `getInfo()`
- **返回类型**: `Result<SysUserVO>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `userId` | `Long` | `@PathVariable` | - |
---

### 新增用户
- **请求路径**: `/system/user`
- **请求方式**: `POST`
- **控制器方法**: `add()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `dto` | `SysUserDTO` | `@RequestBody` | - |

> **SysUserDTO 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 用户ID (更新时必填) |
| `username` | `String` | 用户名 |
| `nickname` | `String` | 用户昵称 |
| `email` | `String` | 用户邮箱 |
| `phone` | `String` | 手机号码 |
| `gender` | `Integer` | 用户性别 (0:未知 1:男 2:女) |
| `avatar` | `String` | 头像地址 |
| `password` | `String` | 密码 (创建时必填) |
| `status` | `Integer` | 状态 (1:正常 0:停用) |
| `deptId` | `Long` | 部门ID |
| `roleIds` | `List<Long>` | 角色ID列表 |
| `postIds` | `List<Long>` | 岗位ID列表 |
| `remark` | `String` | 备注 |

---

### 修改用户
- **请求路径**: `/system/user`
- **请求方式**: `PUT`
- **控制器方法**: `edit()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `dto` | `SysUserDTO` | `@RequestBody` | - |

> **SysUserDTO 数据结构**:

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 用户ID (更新时必填) |
| `username` | `String` | 用户名 |
| `nickname` | `String` | 用户昵称 |
| `email` | `String` | 用户邮箱 |
| `phone` | `String` | 手机号码 |
| `gender` | `Integer` | 用户性别 (0:未知 1:男 2:女) |
| `avatar` | `String` | 头像地址 |
| `password` | `String` | 密码 (创建时必填) |
| `status` | `Integer` | 状态 (1:正常 0:停用) |
| `deptId` | `Long` | 部门ID |
| `roleIds` | `List<Long>` | 角色ID列表 |
| `postIds` | `List<Long>` | 岗位ID列表 |
| `remark` | `String` | 备注 |

---

### 删除用户
- **请求路径**: `/system/user/{userId}`
- **请求方式**: `DELETE`
- **控制器方法**: `remove()`
- **返回类型**: `Result<Void>`

**请求参数**:

| 参数名 | 类型 | 注解 | 描述 |
| --- | --- | --- | --- |
| `userId` | `Long` | `@PathVariable` | - |
---

