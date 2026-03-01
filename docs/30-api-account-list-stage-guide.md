# API账号管理列表区维护指南

## 1. 文档目标

本文档用于说明 `API账号管理` 页面中“账号列表”区域的当前实现方式、交互规则和后续维护方法。

当前列表区已经收口为“单行主信息列表 + 右侧操作 + 双击弹窗详情”的结构，目标如下：

1. 列表一行只承载一个 API 账号的高频主信息，保证扫读效率。
2. 低频治理字段不再塞回列表，而是统一放到详情弹窗中查看。
3. 行右侧保留高频操作，方便运营、授权和排障。
4. 继续保持列模型驱动和通用容器复用，降低后续改版成本。

## 2. 核心代码位置

- 页面入口：`bml-frontend/src/views/api/ApiAccountManage.vue`
- 列表容器：`bml-frontend/src/components/governance/GovernanceListStage.vue`
- 详情弹窗：`bml-frontend/src/components/api-account/ApiAccountPreviewModal.vue`
- 通用表格列模型：`bml-frontend/src/composables/useTableColumns.ts`

## 3. 当前交互规则

当前列表区遵循以下规则：

1. 一行只显示一个账号。
2. 列表只展示主信息，不展示完整治理明细。
3. 用户双击任意一行，会弹出账号详情弹窗。
4. 弹窗右上角保留 `编辑账号 / 接口授权 / 回调日志 / 更多操作`。
5. 列表右侧操作区保留高频动作：
   - `详情`
   - `编辑`
   - `授权`
   - `更多`
6. `更多` 菜单中放低频但仍需快捷触达的动作：
   - `回调日志`
   - `重置密钥`
   - `删除账号`

## 4. 当前列表列设计

当前账号列表保留 6 个业务列：

1. `账号信息`
2. `业务系统 / 负责人`
3. `接入概览`
4. `状态 / 授权`
5. `最近更新`
6. `操作`

列配置集中维护在 `ApiAccountManage.vue` 的 `accountTableColumnModel` 中：

```ts
const accountTableColumnModel = defineTableColumns<AccountColumnKind>([
  { key: 'account', title: '账号信息', kind: 'account', width: 260 },
  { key: 'system', title: '业务系统 / 负责人', kind: 'system', width: 230 },
  { key: 'access', title: '接入概览', kind: 'access', width: 230 },
  { key: 'status', title: '状态 / 授权', kind: 'status', width: 150 },
  { key: 'updateTime', title: '最近更新', kind: 'updateTime', width: 180 },
  { key: 'actions', title: '操作', kind: 'actions', width: 240, fixed: 'right' }
]);
```

## 5. 各列承载内容

### 5.1 账号信息

展示内容：

1. 账号名称
2. 账号类型标签
3. 系统账号 ID

设计原则：

1. 首列必须能一眼识别账号主体。
2. 不再展示 AccessKey、备注、白名单等低频字段。

### 5.2 业务系统 / 负责人

展示内容：

1. 业务系统名称
2. 业务系统编码
3. 负责人名称

设计原则：

1. 业务归属和责任人必须放在同一视线区域，方便定位。
2. 联系方式等更低频内容进入弹窗查看。

### 5.3 接入概览

展示内容：

1. 接入环境标签
2. 客户端数量标签
3. 客户端摘要
4. 回调配置摘要
5. 限流值

设计原则：

1. 只展示“能快速判断账号接入形态”的信息。
2. 不展开完整客户端清单和完整白名单内容。

### 5.4 状态 / 授权

展示内容：

1. 启用 / 停用状态
2. 已授权接口数量

设计原则：

1. 让运营在列表里快速判断账号是否可用。
2. 不把完整授权明细塞进列表。

### 5.5 最近更新

展示内容：

1. 最近更新时间
2. 到期时间摘要

设计原则：

1. 只保留时间维度的关键信息。
2. 创建时间等次级时间字段进入弹窗查看。

### 5.6 操作

展示内容：

1. 主按钮：`详情`
2. 次按钮：`编辑`
3. 次按钮：`授权`
4. 菜单：`回调日志 / 重置密钥 / 删除账号`

设计原则：

1. 高频动作显式展示。
2. 危险动作放入二级菜单并继续保留确认弹窗。
3. 操作区必须阻止双击事件冒泡，避免误弹详情。

## 6. 详情弹窗设计规则

详情弹窗由 `ApiAccountPreviewModal.vue` 承载，定位是“低频字段统一查看入口”。

弹窗内容分为 4 层：

1. 标题区：账号名、ID、类型、状态
2. 右上角操作区：编辑、授权、回调日志、更多操作
3. 指标卡区：授权数、启用授权、客户端范围、白名单条数
4. 信息区：
   - 基础信息
   - 接入配置
   - 治理字段

后续新增字段时，优先判断是否属于低频治理字段：

1. 是：优先加到弹窗
2. 否：再评估是否能进入列表主信息列

## 7. 后续维护顺序

如果后续继续调整列表区，请按以下顺序处理：

1. 先判断是“容器外壳改动”还是“列信息改动”。
2. 容器外壳优先修改 `GovernanceListStage.vue`。
3. 业务列优先修改 `accountTableColumnModel` 和对应 `kind` 分支。
4. 详情字段优先修改 `accountPreviewStats`、`accountPreviewBasicFacts`、`accountPreviewAccessFacts`、`accountPreviewGovernanceFacts`。
5. 如果只是调整动作逻辑，优先复用页面中的统一方法：
   - `openAccountPreviewModalById`
   - `openAuthorizationDrawerById`
   - `openCallbackLogDrawerById`
   - `handleResetSecretById`
   - `handleDeleteAccountById`

## 8. 不建议的做法

1. 重新把列表改回“单元格里堆很多卡片”的重型结构。
2. 在列表区顶部重新堆标题、摘要、说明和工具带。
3. 把完整白名单、完整客户端清单、备注、回调地址全文重新塞回列表行。
4. 为列表动作单独再写一套和弹窗动作不一致的处理逻辑。
5. 删除危险操作的确认弹窗。

## 9. 验证方式

完成调整后，至少执行以下验证：

1. 运行 `npm run build`
2. 检查列表一行是否只显示一个账号的主信息
3. 检查右侧操作按钮是否可正常执行
4. 检查双击任意一行是否能弹出详情弹窗
5. 检查详情弹窗右上角动作是否正常打开编辑、授权和回调日志
6. 检查重置密钥和删除账号是否仍有确认提示
7. 检查分页、筛选和列表刷新是否未受影响
