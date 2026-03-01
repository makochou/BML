# API账号治理业务组件说明

## 1. 文档目标

本文档用于说明本次新增的 API 账号治理业务组件，包括：

1. 接口授权抽屉组件
2. 回调日志抽屉组件
3. 环境白名单卡片组组件
4. 回调日志表组件
5. 凭证交付弹窗组件
6. 账号详情摘要组件
7. 环境白名单编辑组件
8. API账号治理展示工具模块

## 2. 组件清单

### 2.1 接口授权抽屉

文件位置：

- `bml-frontend/src/components/api-account/ApiAuthorizationWorkbenchDrawer.vue`

职责：

- 统一承载接口授权抽屉结构
- 统一 Hero、账号画像、筛选器、模块批量授权、摘要卡和授权树布局
- 通过事件向父页面回传保存、全选、清空和树勾选结果

适用场景：

- API账号管理页
- 后续独立授权页面
- 其他需要复用同一套授权树交互的治理页面

### 2.2 回调日志抽屉

文件位置：

- `bml-frontend/src/components/api-account/ApiCallbackLogWorkbenchDrawer.vue`

职责：

- 统一承载回调日志与重试抽屉结构
- 统一 Hero、账号画像、筛选器、联调建议、摘要卡和日志表布局
- 通过事件向父页面回传查询、重置、分页、测试回调与重试操作

适用场景：

- API账号管理页
- 后续回调运维页面

### 2.3 环境白名单卡片组

文件位置：

- `bml-frontend/src/components/api-account/ApiEnvironmentWhitelistCards.vue`

职责：

- 统一展示测试、预发、生产三套白名单
- 自动突出当前生效环境
- 统一未限制和空状态文案

适用场景：

- 账号详情页
- 凭证查看页
- 后续环境治理详情页

### 2.4 回调日志表

文件位置：

- `bml-frontend/src/components/api-account/ApiCallbackLogTable.vue`

职责：

- 统一渲染回调日志表格
- 通过 `mode` 区分“完整表格”和“详情预览”
- 统一状态标签、重试次数、时间字段和操作列

适用场景：

- 回调日志抽屉
- 账号详情页最近回调日志预览

### 2.5 展示工具模块

### 2.5 凭证交付弹窗

文件位置：

- `bml-frontend/src/components/api-account/ApiCredentialDeliveryModal.vue`

职责：

- 统一承载创建账号和重置密钥后的凭证交付弹窗
- 统一展示保存动作、签名请求头、密钥卡、账号画像和环境白名单
- 统一处理单字段复制和整套凭证复制

适用场景：

- API账号管理页
- 后续独立凭证交付页
- 其他需要一次性交付密钥的治理页面

### 2.6 展示工具模块

### 2.6 账号详情摘要组件

文件位置：

- `bml-frontend/src/components/api-account/ApiAccountDetailSummaryPanels.vue`

职责：

- 统一承载详情页核心治理摘要、授权摘要和回调摘要
- 统一暴露跳转授权工作台和回调日志的动作入口

适用场景：

- API账号详情页
- 后续其他接入账号详情页

### 2.7 展示工具模块

### 2.7 环境白名单编辑组件

文件位置：

- `bml-frontend/src/components/api-account/ApiEnvironmentWhitelistEditor.vue`

职责：

- 统一承载测试、预发、生产三环境白名单编辑区
- 统一展示当前生效环境和条目统计
- 统一处理文本白名单输入回写

适用场景：

- API账号新建/编辑页
- 后续其他接入账号白名单治理页

### 2.8 展示工具模块

文件位置：

- `bml-frontend/src/utils/api-account-governance.ts`

职责：

- 统一提供账号类型、客户端、环境、状态相关选项
- 统一提供环境标签、回调状态、方法标签等展示规则
- 统一处理环境白名单取值逻辑

## 3. 管理页接入结果

当前 `ApiAccountManage.vue` 已完成以下接入：

1. 使用 `ApiAuthorizationWorkbenchDrawer` 承载接口授权抽屉
2. 使用 `ApiCallbackLogWorkbenchDrawer` 承载回调日志抽屉
3. 使用 `ApiCredentialDeliveryModal` 承载凭证交付弹窗
4. 使用 `ApiEnvironmentWhitelistEditor` 承载白名单编辑区
5. 使用 `GovernancePanel / GovernanceStatGrid / GovernanceFactGrid` 缩减页面模板体积

这样做的直接收益是：

- 管理页模板明显变薄
- 抽屉结构独立，后续易于单独维护
- 新增治理场景时可直接复用业务组件

## 4. 详情页接入结果

当前 `ApiAccountDetail.vue` 已完成以下接入：

1. 使用 `ApiEnvironmentWhitelistCards` 展示环境白名单
2. 使用 `ApiCallbackLogTable` 展示最近回调日志预览
3. 使用 `ApiAccountDetailSummaryPanels` 展示核心摘要、授权摘要和回调摘要
4. 保留详情页对编辑、授权、回调、凭证动作的治理闭环跳转

## 5. 推荐维护方式

### 5.1 如果要改接口授权抽屉

优先修改：

- `ApiAuthorizationWorkbenchDrawer.vue`

不要再回 `ApiAccountManage.vue` 中直接堆模板结构。

### 5.2 如果要改回调日志抽屉

优先修改：

- `ApiCallbackLogWorkbenchDrawer.vue`
- `ApiCallbackLogTable.vue`

### 5.3 如果要改白名单展示

优先修改：

- `ApiEnvironmentWhitelistCards.vue`

### 5.4 如果要改标签、环境和状态映射

优先修改：

- `api-account-governance.ts`

避免多个页面分别维护自己的环境和状态文案。

## 6. 后续建议

如果后续继续推进 API 账号治理页的组件化，可以优先考虑继续拆分以下业务组件：

1. 授权树工具条组件
2. 模块批量授权卡片组件
3. 账号详情摘要区组件
4. 凭证交付密钥卡组件
