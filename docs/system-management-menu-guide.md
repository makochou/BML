# 系统管理菜单分组开发指南

## 一、目标结构

业务系统的“系统管理”模块按五个业务域组织：

```text
组织与权限
├── 机构管理
├── 部门管理
├── 岗位管理
├── 用户管理
└── 角色与权限

基础配置
├── 菜单管理
├── 字典管理
├── 参数配置
├── 系统设置
└── 文件管理

消息中心
└── 通知公告

安全审计
├── 审计总览
├── 登录日志
├── 操作日志
├── 异常日志
├── 风险告警
├── 在线用户
└── 归档策略

运维管理
├── 系统监控
├── 缓存管理
├── 定时任务
└── 任务日志
```

## 二、前端接入规范

### 1. 路由配置

业务系统静态路由统一维护在：

```text
bml-frontend/src/router/index.ts
```

新增业务侧系统管理页面时，需要在 `BusinessRoot` 的 `children` 中添加路由，并填写：

- `path`：业务侧访问路径，统一以 `system/` 开头。
- `name`：唯一路由名称，用于侧边栏跳转和标签页缓存。
- `component`：页面组件路径。
- `meta.title`：页面标题。
- `meta.parentTitle`：菜单分组名称。
- `meta.permission`：页面显示所需的 `list` 权限。

### 2. 侧边栏配置

业务系统侧边栏统一维护在：

```text
bml-frontend/src/layout/BusinessLayout.vue
```

新增菜单项时，只需要补充 `sidebarMenuConfig`，模板层无需改动。每个子菜单建议使用如下结构：

```ts
{
  routeName: 'SystemDemo',
  title: '示例菜单',
  icon: IconApps,
  permission: 'system:demo:list'
}
```

### 3. 权限控制

前端按钮级权限统一使用：

```text
bml-frontend/src/composables/useButtonPermission.ts
```

页面内按钮建议使用：

```vue
<a-button :disabled="!hasPermission('system:demo:add')">新增</a-button>
```

## 三、后端菜单与权限规范

### 1. 菜单数据来源

业务系统菜单授权数据维护在 `sys_menu` 表，角色授权维护在 `sys_role_menu` 表。

### 2. Flyway 迁移规范

数据库变更统一放在：

```text
bml-backend/bml-app/src/main/resources/db/migration
```

命名格式：

```text
V版本号__英文描述.sql
```

例如：

```text
V2.12.0__reorganize_business_system_management_menus.sql
```

### 3. 菜单 ID 约定

当前业务侧系统管理相关 ID：

| ID | 类型 | 名称 | 说明 |
| --- | --- | --- | --- |
| 4 | M | 系统管理 | 业务系统根目录 |
| 60 | M | 基础配置 | 菜单、字典、参数、系统设置、文件 |
| 70 | M | 消息中心 | 通知公告 |
| 71 | M | 安全审计 | 审计、日志、风险、在线用户 |
| 72 | M | 运维管理 | 监控、缓存、任务 |
| 73 | C | 系统监控 | 业务侧系统监控入口 |

历史菜单管理 `id=43` 与按钮 `431-434` 已删除，后续禁止复用。

## 四、统一接口规范

### 1. 统一响应

后端控制器统一返回：

```java
Result<T>
```

分页接口统一返回：

```java
Result<PageResult<T>>
```

### 2. Controller 规范

业务控制器建议继承：

```java
BaseController
```

新增、修改、删除等写操作建议加：

```java
@OperationLog(title = "模块名称", businessType = BusinessType.INSERT)
```

接口权限统一使用：

```java
@PreAuthorize("@ss.hasPermi('system:demo:list')")
```

### 3. Service 规范

服务接口建议继承项目统一基础服务：

```java
BaseService<Entity>
```

分页查询统一返回 `PageResult<VO>`，入参使用 DTO 或 Query 对象，出参使用 VO，不直接暴露实体。

## 五、页面设计规范

新增页面建议遵循当前业务侧视觉规范：

- 使用 Arco Design Vue 组件。
- 顶部使用卡片式筛选区或品牌标题区。
- 表格使用分页、加载状态、空状态。
- 危险操作使用二次确认。
- 异步操作使用 `Message.success` 或统一错误处理。
- 表格列若使用 `<a-table-column>`，必须放在 `<template #columns>` 内，避免 Arco 表格列不渲染。

## 六、构建验证

每次调整菜单、路由、权限或后端接口后，必须执行：

```powershell
npm run build
```

工作目录：

```text
bml-frontend
```

后端编译验证：

```powershell
mvn -f bml-backend/pom.xml -DskipTests compile
```

工作目录：

```text
BML 仓库根目录
```

## 七、当前验证状态

本次菜单重组后已验证：

- 前端 `npm run build` 通过。
- 后端 `mvn -f bml-backend/pom.xml -DskipTests compile` 通过。
