# API账号治理页复用开发指南

## 1. 文档目标

本文档用于说明本次新增的两类前端复用能力：

- `useTableColumns` 组合式函数
- `GovernanceWorkbenchShell` 治理工作台骨架组件

适用场景：

- 账号列表
- 账号详情 / 编辑
- 凭证查看
- 接口授权
- 回调日志
- 后续新增的治理工作台页面

## 2. useTableColumns 组合式函数

### 2.1 文件位置

- `bml-frontend/src/composables/useTableColumns.ts`

### 2.2 设计目的

在治理类页面中，表格列通常具备以下共同点：

- 都需要固定列模型结构
- 都存在普通文本列的空值兜底
- 都需要统一处理省略和 tooltip 规则
- 都需要为复杂单元格保留 `kind` 渲染语义

为避免每个页面重复手写这些能力，统一抽出 `useTableColumns`。

### 2.3 核心能力

`useTableColumns` 当前提供以下能力：

1. 通过 `defineTableColumns` 声明列模型，保留更稳定的类型推断。
2. 通过 `columns` 返回给模板直接循环渲染的标准列配置。
3. 自动补齐 `ellipsis` 与 `tooltip` 的默认规则。
4. 通过 `getPlainText` 统一处理普通字段空值显示。

### 2.4 标准接入方式

```ts
import { defineTableColumns, useTableColumns } from '../../composables/useTableColumns';

type DemoColumnKind = 'name' | 'status' | 'actions' | 'plain';

const demoTableColumnModel = defineTableColumns<DemoColumnKind>([
  { key: 'name', title: '名称', kind: 'name', width: 220 },
  { key: 'remark', title: '备注', kind: 'plain', width: 240, dataIndex: 'remark', ellipsis: true, tooltip: true },
  { key: 'actions', title: '操作', kind: 'actions', width: 180, fixed: 'right' }
]);

const { columns: demoTableColumns, getPlainText } = useTableColumns(demoTableColumnModel);
```

### 2.5 模板渲染建议

推荐模板写法如下：

```vue
<a-table>
  <template #columns>
    <a-table-column
      v-for="column in demoTableColumns"
      :key="column.key"
      :title="column.title"
      :data-index="column.dataIndex"
      :width="column.width"
      :fixed="column.fixed"
    >
      <template #cell="{ record }">
        <span v-if="column.kind === 'name'">{{ record.name }}</span>
        <span v-else-if="column.kind === 'actions'">...</span>
        <span v-else>{{ getPlainText(record, column.dataIndex, '-') }}</span>
      </template>
    </a-table-column>
  </template>
</a-table>
```

### 2.6 后续扩展规范

后续新增列时，建议按以下规范执行：

1. 先补列模型，不要先改模板。
2. 普通字段优先复用 `plain`。
3. 需要特殊表现时再新增新的 `kind`。
4. 操作列统一放右侧，保证治理页交互一致。
5. 时间类字段尽量复用统一格式化方法，避免每页各自处理。

## 3. GovernanceWorkbenchShell 骨架组件

### 3.1 文件位置

- `bml-frontend/src/components/governance/GovernanceWorkbenchShell.vue`

### 3.2 设计目的

治理类页面在视觉和结构上高度相似，通常都包含：

- 顶部 Hero 说明区
- 摘要指标卡
- 左侧事实信息或筛选区
- 右侧主工作区
- 底部动作区

过去每个弹窗、抽屉都单独维护一套结构，后续扩展成本高。现在统一抽成一个工作台骨架组件，多个页面共享同一套结构语言。

### 3.3 当前已落地场景

当前已经接入该骨架的页面场景包括：

1. 新建 / 编辑账号
2. 凭证查看
3. 接口授权
4. 回调日志

### 3.4 组件插槽说明

`GovernanceWorkbenchShell` 提供以下主要插槽：

1. `titleBadge`
作用：用于在标题右侧补充状态徽标或风险提示。

2. `heroActions`
作用：用于放置顶部主操作按钮，如保存、关闭、测试回调等。

3. `aside`
作用：用于放置账号画像、筛选器、联调建议、注意事项等侧栏内容。

4. 默认插槽
作用：用于放置主表单、表格、树结构、明细区等主体内容。

5. `footer`
作用：用于放置底部确认动作与保存说明。

### 3.5 主题说明

组件当前支持以下主题：

- `emerald`
- `ocean`
- `teal`
- `violet`

推荐的业务语义映射如下：

1. `emerald`
适用于账号治理、配置编排、编辑表单。

2. `ocean`
适用于接口授权、接口目录、能力开通。

3. `teal`
适用于日志、回调、运维排障。

4. `violet`
适用于凭证交付、安全展示、敏感信息查看。

### 3.6 Hero 显隐控制

`GovernanceWorkbenchShell` 新增了 `hideHero` 属性，用于按场景隐藏顶部 Hero 区：

1. `hideHero=false`（默认）
   - 展示顶部 Hero、标签和统计卡，适合编辑/治理场景。
2. `hideHero=true`
   - 隐藏顶部 Hero，仅保留主布局与底部动作，适合新增流程的轻量录入场景。

### 3.7 标准接入方式

```vue
<GovernanceWorkbenchShell
  eyebrow="Authorization Governance Studio"
  title="接口授权"
  description="为单个账号配置调用能力。"
  :tags="authorizationHeroTags"
  :stats="authorizationHeroStats"
  theme="ocean"
  sticky-hero
>
  <template #heroActions>
    <a-button>关闭工作台</a-button>
    <a-button type="primary">保存授权</a-button>
  </template>

  <template #aside>
    <section>账号画像</section>
    <section>筛选器</section>
  </template>

  <section>摘要卡</section>
  <section>授权树</section>
</GovernanceWorkbenchShell>
```

新增场景可直接开启轻量模式：

```vue
<GovernanceWorkbenchShell
  :hide-hero="isCreateMode"
  ...
>
  <template v-if="!isCreateMode" #aside>
    <!-- 编辑模式展示侧栏 -->
  </template>
</GovernanceWorkbenchShell>
```

### 3.8 新增治理页的推荐步骤

当后续新增“账号详情”“凭证审计”“授权审批记录”“回调分析”等页面时，推荐按以下顺序实现：

1. 明确 `eyebrow`、`title`、`description`。
2. 抽取顶部 `tags` 和 `stats`。
3. 先整理左侧 `aside` 的事实信息与筛选器。
4. 再整理右侧主区域的表单、表格、树或详情区。
5. 如有保存、确认或批量动作，再补 `footer`。

### 3.9 开发注意事项

1. 视觉结构统一后，尽量不要在页面中重新发明另一套 Hero 或侧栏骨架。
2. 如果只是替换文案、摘要卡、动作按钮，应优先通过 props 和 slots 处理。
3. 如果后续出现新的治理场景，先判断是否能复用现有骨架，再决定是否新增专用组件。
4. 页面自己的样式只负责业务内容区，不负责重新定义整套工作台外框。

## 4. 本次页面中的实际落点

本次 `ApiAccountManage.vue` 已完成以下收口：

1. 账号列表表格使用 `useTableColumns`
2. 回调日志表格使用 `useTableColumns`
3. 新建 / 编辑账号接入 `GovernanceWorkbenchShell`，并在新增模式下启用 `hideHero`
4. 凭证查看接入 `GovernanceWorkbenchShell`
5. 接口授权接入 `GovernanceWorkbenchShell`
6. 回调日志接入 `GovernanceWorkbenchShell`

这意味着后续如果再新增治理场景，优先复用现有组合式函数和工作台骨架即可，无需从零搭模板结构。

## 5. 新建弹窗紧凑化与可拖拽改造说明

### 5.1 改造目标

本节针对“新建API账号”场景新增三项能力：

1. 弹窗支持拖拽（减少遮挡，便于边看列表边录入）。
2. 弹窗上下保留可视留白，不贴浏览器顶部或底部。
3. 新建模式使用紧凑多列字段布局，提高首屏录入效率。

### 5.2 代码落点

1. 弹窗页面：`bml-frontend/src/views/api/ApiAccountManage.vue`
2. 表单 schema：`bml-frontend/src/composables/useApiAccountFormSchema.ts`

### 5.3 弹窗接入规范

`a-modal` 推荐使用以下统一参数：

1. `draggable`：启用拖拽。
2. `align-center`：以居中方式打开弹窗，减少“顶边贴合”感。
3. `modal-class` 与 `body-class`：统一注入样式钩子，用于控制最大高度和内部滚动。
4. `width`：按模式动态控制，新建模式优先紧凑宽度。

### 5.4 表单紧凑布局规范

`useApiAccountFormSchema` 新增 `compactMode` 可选参数，页面根据模式开启：

1. 基础归属分区：`columns` 由 `2` 调整为 `3`（仅紧凑模式）。
2. 接入策略分区：`columns` 由 `2` 调整为 `3`（仅紧凑模式）。
3. 回调分区：由 `single` 切为 `grid + columns=2`（仅紧凑模式）。
4. 备注字段：紧凑模式下 `colSpan=2`，同时文本域行数收敛为 `2-4` 行。

这样可在一屏内展示更多输入项，同时保留 schema 驱动方式，便于后续字段扩展和跨页面复用。

### 5.5 样式治理规范

建议保持以下样式边界：

1. 弹窗“边界与滚动”只在页面级处理（`ApiAccountManage.vue` 的 `modal-class/body-class`）。
2. 字段“列数与组件属性”只在 schema 层处理（`useApiAccountFormSchema.ts`）。
3. 不在业务模板中硬编码列数和间距，避免后续改版牵连模板结构。

### 5.6 验证清单

每次调整后建议执行：

1. `npm run build`（前端类型和构建校验）。
2. 手工验证新建弹窗拖拽、滚动、字段换行与移动端降列。
3. 验证编辑模式仍维持原有可读布局，不受新建紧凑模式影响。

### 5.7 视口交互增强（拖拽/四向缩放/全屏）

本次在 `ApiAccountManage.vue` 增加了统一的弹窗视口控制实现：

1. 标题栏拖拽：按住标题栏可移动弹窗位置。
2. 四边与四角缩放：靠近边缘后拖动，可按方向调整宽高。
3. 全屏切换：标题栏按钮支持“全屏/还原”。
4. 安全留白：通过统一 margin 限制，确保弹窗不会贴住浏览器上下边缘。

实现要点：

1. 视口状态统一收口到 `accountModalViewport`（宽、高、位移、全屏）。
2. 指针状态统一收口到 `accountModalPointerState`（动作类型、起始坐标、起始尺寸）。
3. 使用 `clampAccountModalViewport()` 做边界收敛，防止拖拽缩放后越界。
4. 通过 `watch(accountModal.visible)` 统一绑定/解绑 DOM 监听，避免内存泄漏。
