共分析到 **61** 个API接口。
# 项目接口完整说明 (Project APIs)\n\n## OpenAPI 示例接口 (OpenApiDemoController)
**基础路径 (Base Path)**: `/open/api`

| 接口描述 (Summary) | 详细说明 (Description) | 请求方法 (Method) | 完整路径 (Path) |
| --- | --- | --- | --- |
| 连通性检测 | 用于验证 OpenAPI 签名链路 | `GET` | `/open/api/ping` |

## API账号授权管理 (SysApiAccountAuthorizationController)
**基础路径 (Base Path)**: `/account`

| 接口描述 (Summary) | 详细说明 (Description) | 请求方法 (Method) | 完整路径 (Path) |
| --- | --- | --- | --- |
| 获取账号授权快照 | - | `GET` | `/account/{id}/authorization` |
| 保存账号授权 | - | `PUT` | `/account/{id}/authorization` |

## API账号回调日志管理 (SysApiAccountCallbackController)
**基础路径 (Base Path)**: `/account`

| 接口描述 (Summary) | 详细说明 (Description) | 请求方法 (Method) | 完整路径 (Path) |
| --- | --- | --- | --- |
| 分页查询指定账号的回调日志 | - | `GET` | `/account/{id}/callback-log/page` |
| 发送测试回调 | - | `POST` | `/account/{id}/callback/test` |
| 手动重试回调日志 | - | `POST` | `/account/callback-log/{logId}/retry` |

## API账号管理 (SysApiAccountController)
**基础路径 (Base Path)**: `/account`

| 接口描述 (Summary) | 详细说明 (Description) | 请求方法 (Method) | 完整路径 (Path) |
| --- | --- | --- | --- |
| 分页查询API账号 | - | `GET` | `/account/page` |
| 查询API账号详情 | - | `GET` | `/account/{id}` |
| 按 ID 复制 API 账号副本 | - | `GET` | `/account/{id}/copy` |
| 新增API账号 | - | `POST` | `/account` |
| 修改API账号 | - | `PUT` | `/account/{id}` |
| 修改API账号状态 | - | `PUT` | `/account/{id}/status` |
| 删除API账号 | - | `DELETE` | `/account/{id}` |
| 重置API账号密钥 | - | `PUT` | `/account/{id}/secret/reset` |
| 获取API账号列表(兼容旧接口) | - | `GET` | `/account/list` |
| 新增API账号(兼容旧接口) | - | `POST` | `/account/add` |
| 修改API账号(兼容旧接口) | - | `PUT` | `/account/edit` |
| 删除API账号(兼容旧接口) | - | `DELETE` | `/account/remove/{id}` |
| 重置密钥(兼容旧接口) | - | `PUT` | `/account/{id}/reset` |

## 开放接口目录管理 (SysOpenApiRegistryController)
**基础路径 (Base Path)**: `/openapi/registry`

| 接口描述 (Summary) | 详细说明 (Description) | 请求方法 (Method) | 完整路径 (Path) |
| --- | --- | --- | --- |
| 查询开放接口目录树 | - | `GET` | `/openapi/registry/tree` |
| 同步开放接口目录 | - | `POST` | `/openapi/registry/sync` |

## 企业开放接口-企业档案 (OpenEnterpriseCompanyController)
**基础路径 (Base Path)**: `/open/api/enterprise/company`

| 接口描述 (Summary) | 详细说明 (Description) | 请求方法 (Method) | 完整路径 (Path) |
| --- | --- | --- | --- |
| 分页查询企业档案 | 后续企业管理前台可通过该接口查询企业列表 | `GET` | `/open/api/enterprise/company/page` |
| 查询企业档案详情 | 根据企业ID返回企业档案详情 | `GET` | `/open/api/enterprise/company/{companyId}` |

## 企业开放接口-业务概览 (OpenEnterpriseDashboardController)
**基础路径 (Base Path)**: `/open/api/enterprise/dashboard`

| 接口描述 (Summary) | 详细说明 (Description) | 请求方法 (Method) | 完整路径 (Path) |
| --- | --- | --- | --- |
| 查询企业管理业务概览 | 为企业管理前台提供首页概览数据样板 | `GET` | `/open/api/enterprise/dashboard/summary` |

## 企业开放接口-系统账号 (OpenEnterpriseSystemAccountController)
**基础路径 (Base Path)**: `/open/api/enterprise/system-account`

| 接口描述 (Summary) | 详细说明 (Description) | 请求方法 (Method) | 完整路径 (Path) |
| --- | --- | --- | --- |
| 分页查询企业系统账号 | 为企业管理前台提供系统账号列表查询样板 | `GET` | `/open/api/enterprise/system-account/page` |
| 查询企业系统账号详情 | 根据系统账号ID返回企业账号详情 | `GET` | `/open/api/enterprise/system-account/{accountId}` |

## 认证中心 (AuthController)
> 登录、登出、Token刷新、用户信息获取

**基础路径 (Base Path)**: `/auth`

| 接口描述 (Summary) | 详细说明 (Description) | 请求方法 (Method) | 完整路径 (Path) |
| --- | --- | --- | --- |
| 用户登录 | 账号密码登录，返回AccessToken和RefreshToken | `POST` | `/auth/login` |
| 刷新Token | 使用RefreshToken获取新的AccessToken | `POST` | `/auth/refresh` |
| 用户登出 | 清除登录状态，使Token失效 | `POST` | `/auth/logout` |
| 获取当前用户信息 | 返回用户信息、角色列表和权限标识列表 | `GET` | `/auth/info` |
| 获取路由菜单 | 返回当前用户的菜单树，用于前端路由生成 | `GET` | `/auth/routers` |

## 系统告警通知接口 (SysAlertController)
**基础路径 (Base Path)**: `/system/alert`

| 接口描述 (Summary) | 详细说明 (Description) | 请求方法 (Method) | 完整路径 (Path) |
| --- | --- | --- | --- |
| 获取未读的告警总数 | - | `GET` | `/system/alert/unread-count` |
| 获取最近的系统监控告警列表 | - | `GET` | `/system/alert/list` |
| 增量轮询 — 获取指定 ID 之后的最新告警 | - | `GET` | `/system/alert/latest` |
| 将指定的告警置为已读 | - | `PUT` | `/system/alert/read/{id}` |
| 将所有未读告警一键置为已读 | - | `PUT` | `/system/alert/read-all` |
| 获取存在告警的日期列表 | - | `GET` | `/system/alert/dates` |
| 按日期查询告警列表 | - | `GET` | `/system/alert/by-date` |
| 删除指定的告警记录 | - | `DELETE` | `/system/alert/{id}` |

## 部门管理 (SysDeptController)
**基础路径 (Base Path)**: `/system/dept`

| 接口描述 (Summary) | 详细说明 (Description) | 请求方法 (Method) | 完整路径 (Path) |
| --- | --- | --- | --- |
| 获取部门树列表 | - | `GET` | `/system/dept/list` |
| 根据部门编号获取详细信息 | - | `GET` | `/system/dept/{deptId}` |
| 新增部门 | - | `POST` | `/system/dept` |
| 修改部门 | - | `PUT` | `/system/dept` |
| 删除部门 | - | `DELETE` | `/system/dept/{deptId}` |

## 菜单管理 (SysMenuController)
**基础路径 (Base Path)**: `/system/menu`

| 接口描述 (Summary) | 详细说明 (Description) | 请求方法 (Method) | 完整路径 (Path) |
| --- | --- | --- | --- |
| 获取菜单列表 | - | `GET` | `/system/menu/list` |
| 根据菜单编号获取详细信息 | - | `GET` | `/system/menu/{menuId}` |
| 新增菜单 | - | `POST` | `/system/menu` |
| 修改菜单 | - | `PUT` | `/system/menu` |
| 删除菜单 | - | `DELETE` | `/system/menu/{menuId}` |

## 角色管理 (SysRoleController)
**基础路径 (Base Path)**: `/system/role`

| 接口描述 (Summary) | 详细说明 (Description) | 请求方法 (Method) | 完整路径 (Path) |
| --- | --- | --- | --- |
| 获取角色列表 | - | `GET` | `/system/role/list` |
| 根据角色编号获取详细信息 | - | `GET` | `/system/role/{roleId}` |
| 新增角色 | - | `POST` | `/system/role` |
| 修改角色 | - | `PUT` | `/system/role` |
| 删除角色 | - | `DELETE` | `/system/role/{roleId}` |

## 服务器监控资源获取接口 (SysServerMonitorController)
**基础路径 (Base Path)**: `/system/monitor`

| 接口描述 (Summary) | 详细说明 (Description) | 请求方法 (Method) | 完整路径 (Path) |
| --- | --- | --- | --- |
| 全量抓取服务器、JVM、磁盘最新硬件物理探针态 | - | `GET` | `/system/monitor/server` |
| 强制发送JVM垃圾回收指令(GC)，尝试释放内存 | - | `POST` | `/system/monitor/gc` |

## 用户管理 (SysUserController)
**基础路径 (Base Path)**: `/system/user`

| 接口描述 (Summary) | 详细说明 (Description) | 请求方法 (Method) | 完整路径 (Path) |
| --- | --- | --- | --- |
| 获取用户列表 | - | `GET` | `/system/user/list` |
| 根据用户编号获取详细信息 | - | `GET` | `/system/user/{userId}` |
| 新增用户 | - | `POST` | `/system/user` |
| 修改用户 | - | `PUT` | `/system/user` |
| 删除用户 | - | `DELETE` | `/system/user/{userId}` |

