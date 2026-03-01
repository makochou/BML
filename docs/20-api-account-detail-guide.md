# API账号详情页开发与维护指南

## 1. 目标

本次新增了正式的 `API账号详情页`，用于承接账号治理的“查看”场景，并与现有管理页形成如下闭环：

1. 查看账号详情
2. 跳转编辑账号
3. 跳转接口授权
4. 跳转回调日志
5. 跳转重置并查看新凭证

## 2. 页面位置

- 详情页文件：`bml-frontend/src/views/api/ApiAccountDetail.vue`
- 路由定义：`bml-frontend/src/router/index.ts`

详情页路由为：

- `/admin/api/account/:id`

该路由为隐藏路由，不进入左侧菜单，但可从账号列表“详情”按钮直接进入。

## 3. 页面结构

详情页完全复用治理工作台骨架：

- 顶部 Hero：账号总体状态与治理指标
- 左侧侧栏：账号画像与治理建议
- 主区摘要：核心治理摘要
- 主区能力：接口授权现状、回调运行概况
- 主区配置：环境白名单
- 主区运维：最近回调日志

## 4. 复用的通用组件

本次详情页直接复用了以下组件：

1. `GovernanceWorkbenchShell`
2. `GovernancePanel`
3. `GovernanceStatGrid`
4. `GovernanceFactGrid`
5. `useTableColumns`

这样做的意义是：

- 查看页与管理工作台保持完全一致的视觉语言
- 摘要卡、画像卡、侧栏面板都使用统一结构
- 后续新增类似治理详情页时可直接套用

## 5. 数据来源

详情页当前并行读取三类数据：

1. `fetchApiAccountDetail`
作用：获取账号基础信息、白名单、状态、限流和归属信息。

2. `fetchAuthorizationSnapshot`
作用：获取当前账号的接口授权摘要。

3. `fetchApiCallbackLogs`
作用：获取最近回调日志与回调摘要。

其中回调日志默认只读取第一页的 5 条记录，用于详情页快速预览。

## 6. 动作闭环说明

详情页顶部提供以下动作按钮：

1. `返回列表`
跳回 `/admin/api/account`

2. `编辑账号`
跳回管理页，并带上：

- `action=edit`
- `accountId=当前账号ID`

3. `接口授权`
跳回管理页，并带上：

- `action=authorization`
- `accountId=当前账号ID`

4. `回调日志`
跳回管理页，并带上：

- `action=callback`
- `accountId=当前账号ID`

5. `重置并查看新凭证`
跳回管理页，并带上：

- `action=reset-secret`
- `accountId=当前账号ID`

## 7. 管理页路由动作协议

为了让详情页与管理页形成治理闭环，`ApiAccountManage.vue` 已支持解析路由动作参数：

- `edit`
- `authorization`
- `callback`
- `reset-secret`

管理页会在加载完成后自动识别 `action + accountId`：

1. 自动打开编辑弹窗
2. 自动打开授权抽屉
3. 自动打开回调日志抽屉
4. 自动重置密钥并弹出凭证窗口

动作执行完成后，会自动清理路由参数，避免页面刷新时重复触发。

## 8. 后续维护建议

### 8.1 如果要新增详情区块

建议优先新增到以下区域之一：

1. `detailOverviewStats`
适合新增摘要指标。

2. `detailFactCards`
适合新增账号画像信息。

3. `detailAuthorizationStats`
适合新增授权维度统计。

4. `detailCallbackStats`
适合新增回调运维维度统计。

### 8.2 如果要新增详情页操作按钮

优先判断是否属于已有闭环：

- 编辑
- 授权
- 日志
- 凭证

如果属于已有闭环，建议继续走“跳回管理页并自动打开对应工作台”的模式，避免在详情页重复维护同一套复杂弹窗逻辑。

### 8.3 如果后续需要独立详情工作台

当详情页的交互越来越复杂时，可以进一步把以下能力单独抽成业务组件：

1. 最近回调日志预览表
2. 环境白名单卡片组
3. 授权摘要面板
4. 回调摘要面板

但在当前阶段，优先复用现有通用组件即可。
