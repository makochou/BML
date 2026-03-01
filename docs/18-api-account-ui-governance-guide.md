# API账号管理前端界面治理总说明

## 1. 文档目标

本文档用于说明 `API账号管理` 页面当前的整体前端结构、视觉分层和维护规则。

当前页面已经收口为 5 个核心区块：

1. 顶部概览 Deck
2. 紧凑查询卡片
3. 单行主信息账号列表
4. 账号详情预览弹窗
5. 授权与回调工作台

整体目标如下：

1. 首屏信息分层清楚，不堆重复摘要。
2. 查询区紧凑、清爽，只保留必要交互。
3. 列表区只做主信息浏览和快捷处理。
4. 低频治理字段统一进入弹窗或工作台查看。
5. 页面内所有重交互入口尽量复用同一套状态和动作方法。

## 2. 核心代码位置

- 页面入口：`bml-frontend/src/views/api/ApiAccountManage.vue`
- 顶部概览：`bml-frontend/src/components/governance/GovernanceOverviewDeck.vue`
- 查询卡片：`bml-frontend/src/components/governance/GovernanceCompactQueryPanel.vue`
- 表单字段布局：`bml-frontend/src/components/governance/GovernanceFormSections.vue`
- 列表容器：`bml-frontend/src/components/governance/GovernanceListStage.vue`
- 账号详情弹窗：`bml-frontend/src/components/api-account/ApiAccountPreviewModal.vue`
- 接口授权抽屉：`bml-frontend/src/components/api-account/ApiAuthorizationWorkbenchDrawer.vue`
- 回调日志抽屉：`bml-frontend/src/components/api-account/ApiCallbackLogWorkbenchDrawer.vue`

## 3. 页面分区说明

### 3.1 顶部概览 Deck

顶部概览用于承载：

1. 页面标题与定位说明
2. 治理标签
3. 核心指标
4. 顶部主操作

维护原则：

1. 只放全局性摘要，不放查询区和列表区的局部说明。
2. 指标与摘要卡必须避免重复表达同一组信息。

### 3.2 紧凑查询卡片

当前查询区采用“常用条件常显、低频条件折叠”的模式：

1. 首屏展示常用条件
2. 左下角保留“展开更多查询条件”
3. 右下角保留“查询账号 / 重置条件”
4. 不再放额外说明文案、工具条和结果摘要

维护原则：

1. 高频字段放 `primary`
2. 低频字段放 `secondary`
3. 查询区只负责筛选，不承载列表运营信息

### 3.3 单行主信息账号列表

当前列表区采用“无头部容器 + 单行主信息表”的模式：

1. 一行展示一个 API 账号
2. 列表保留主信息和右侧操作
3. 双击整行打开详情弹窗
4. 不再把低频字段塞进列表

当前列表区保留以下列：

1. `账号信息`
2. `业务系统 / 负责人`
3. `接入概览`
4. `状态 / 授权`
5. `最近更新`
6. `操作`

### 3.4 账号详情预览弹窗

详情弹窗用于承载列表不再展示的低频治理字段。

弹窗结构如下：

1. 标题区：账号名、ID、账号类型、状态
2. 右上角操作区：编辑账号、接口授权、回调日志、更多操作
3. 指标概览区：授权数、客户端范围、白名单数量等
4. 信息分区：
   - 基础信息
   - 接入配置
   - 治理字段

维护原则：

1. 列表中删掉的字段优先进入这里，而不是重新加回列表。
2. 弹窗右上角动作必须和列表右侧动作保持一致的业务入口。

### 3.5 授权与回调工作台

接口授权和回调日志继续保持工作台设计语言：

1. 抽屉式承载
2. 顶部概览与事实卡
3. 中部业务主区
4. 底部保存或处理动作

## 4. 推荐维护顺序

后续调整页面时，优先按以下顺序处理：

1. 先判断改动属于哪一层：
   - 顶部概览
   - 查询区
   - 列表区
   - 详情弹窗
   - 工作台抽屉
2. 如果是查询字段变化，优先修改：
   - `queryForm`
   - `useApiAccountQuerySchema.ts`
   - `splitGovernanceSectionsByPriority`
3. 如果是列表列变化，优先修改：
   - `AccountColumnKind`
   - `accountTableColumnModel`
   - 对应 `kind` 单元格渲染分支
4. 如果是低频字段变化，优先修改：
   - `accountPreviewStats`
   - `accountPreviewBasicFacts`
   - `accountPreviewAccessFacts`
   - `accountPreviewGovernanceFacts`
5. 如果是动作入口变化，优先复用：
   - `openAccountPreviewModalById`
   - `openAuthorizationDrawerById`
   - `openCallbackLogDrawerById`
   - `handleResetSecretById`
   - `handleDeleteAccountById`

## 5. 不建议的做法

1. 重新把查询区做回重型运营工具带。
2. 重新把列表改回十几列、需要大范围横向滚动的宽表。
3. 在列表行内堆大量白名单、回调、备注和授权细节。
4. 把列表动作和弹窗动作拆成两套不同逻辑。
5. 为临时需求直接在模板中硬编码字段，不维护列模型和卡片模型。

## 6. 验证方式

完成页面调整后，至少执行以下验证：

1. 运行 `npm run build`
2. 检查顶部概览区在桌面端和移动端是否布局正常
3. 检查查询区展开/收起是否正常
4. 检查列表是否保持“一行一个账号”的主信息结构
5. 检查右侧操作区是否可正常点击
6. 检查双击一行是否弹出详情弹窗
7. 检查弹窗右上角动作是否能正常打开编辑、授权和回调日志
8. 检查授权抽屉和回调日志抽屉是否未受影响
