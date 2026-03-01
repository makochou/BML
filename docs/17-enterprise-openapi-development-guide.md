# 企业开放接口开发指南

## 1. 目标

企业业务前台访问入口：

- `http://localhost:5173/`

BML 中台管理入口：

- `http://localhost:5173/admin`

本指南说明如何在后端继续新增企业业务开放接口，并让这些接口自动纳入 BML 中台的 API 账号授权、白名单治理和回调日志体系。

## 2. 当前开放接口范围

企业开放接口统一放在：

- `/api/open/api/enterprise/**`

当前项目已提供示例控制器：

- `OpenEnterpriseDashboardController`
- `OpenEnterpriseCompanyController`
- `OpenEnterpriseSystemAccountController`

这些接口会在中台页面中以“开放接口目录”的形式出现，供 API 账号授权使用。

## 3. 开发规范

### 3.1 路径规范

所有对外开放接口必须：

- 放在 `/open/api/**`
- 由中台 API 账号授权后才能访问
- 不应复用中台管理接口 `/api/**`

推荐路径风格：

- `/open/api/enterprise/dashboard/summary`
- `/open/api/enterprise/company/page`
- `/open/api/enterprise/company/{companyId}`

### 3.2 认证规范

开放接口不使用中台后台的 JWT Token，而是使用 API 账号签名校验：

- `X-Bml-App-Key`
- `X-Bml-Sign-Version`
- `X-Bml-Timestamp`
- `X-Bml-Nonce`
- `X-Bml-Sign`

### 3.3 授权规范

新接口开发完成后，必须确保：

1. 路径在 `/open/api/**` 下
2. 请求方法明确
3. 重启后端或手工点击“同步接口目录”
4. 在中台里把接口授权给目标 API 账号

## 4. 与 API 账号治理的关系

### 4.1 按环境独立白名单

每个 API 账号都可以分别维护：

- 测试白名单
- 预发白名单
- 生产白名单

运行时系统会根据账号当前 `accessEnvironment` 选择真正生效的白名单。

企业开放接口在被调用时，会自动执行：

- 账号有效性校验
- 接口授权校验
- 签名版本校验
- 当前环境白名单校验

### 4.2 回调日志与重试

如果企业业务存在异步通知场景，例如：

- 企业档案审核结果通知
- 企业系统账号开通完成通知
- 业务处理状态回调

建议统一复用 `sys_api_callback_log` 与 `SysApiCallbackLogService`，不要由各业务模块自行重复实现日志和重试逻辑。

推荐做法：

1. 业务模块产生回调事件
2. 组装回调快照
3. 交给统一回调服务发送
4. 失败后进入系统重试队列
5. 在中台中查看日志、手动重试、排查错误

## 5. 新增企业开放接口的标准流程

1. 在企业模块新增 `/open/api/enterprise/**` 控制器方法
2. 为接口补充明确的请求方法和说明
3. 重启后端或在中台点击“同步接口目录”
4. 在 `/admin/api/account` 创建或编辑 API 账号
5. 维护负责人、业务系统、客户端、环境、白名单、签名版本、回调地址
6. 在“接口授权”工作台中勾选新接口
7. 联调时验证：
   - 签名正确时可访问
   - 白名单不命中时被拒绝
   - 未授权接口被拒绝

## 6. 示例请求

```http
GET /api/open/api/enterprise/dashboard/summary HTTP/1.1
Host: localhost:8080
X-Bml-App-Key: demo-ak
X-Bml-Sign-Version: v1
X-Bml-Timestamp: 1767264000000
X-Bml-Nonce: 9f7b44a2d0c14e77a6fd
X-Bml-Sign: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

## 7. 企业业务场景下的回调建议

如果后续企业系统要接收 BML 的业务回调，建议提前约定：

- 回调地址由中台 API 账号管理页统一登记
- 生产与测试环境使用不同地址
- 回调接口返回 2xx 才视为成功
- 客户接入方应具备幂等处理能力

### 7.1 推荐记录的信息

- 系统账号 ID
- 账号名称
- 业务系统编码
- 当前接入环境
- 签名版本
- 生效白名单
- 回调地址

## 8. 常见问题

### 8.1 为什么新接口没有出现在中台授权树

通常是以下原因之一：

- 路径不在 `/open/api/**`
- 请求方法不明确
- 接口目录未同步
- 被放到了测试路径 `/open/api/test/**`

### 8.2 为什么账号授权了仍然访问失败

优先检查：

- 签名是否正确
- `X-Bml-Sign-Version` 是否与账号配置一致
- 请求来源 IP 是否命中当前环境白名单
- 账号是否停用或过期

### 8.3 为什么测试环境能调，生产环境不能调

这通常与按环境独立维护白名单有关。需要确认：

- 账号当前 `accessEnvironment`
- 生产环境白名单是否已配置
- 生产出口 IP 是否与登记一致

## 9. 开发建议

如果后续企业管理系统会持续增加开放接口，建议遵循以下结构：

- 控制器：只负责参数接收与统一响应
- 服务层：承载企业业务逻辑
- 回调逻辑：统一接入回调日志服务
- 授权控制：统一交给 API 账号授权体系

这样可以保证：

- 开放接口自动进入中台授权目录
- API 账号治理信息完整
- 安全策略统一
- 回调联调与故障排查成本更低
