# API账号管理紧凑查询面板维护指南

## 1. 文档目标

本文档用于说明 `API账号管理` 页面查询区的当前实现方式。

当前查询区采用“主条件常显、次条件折叠、左下角展开、右下角查询/重置”的极简工作模式，目标如下：

1. 首屏只展示高频条件。
2. 低频条件通过底部左侧按钮按需展开。
3. 查询动作固定收口到底部右侧。
4. 保持 schema 驱动，方便后续继续扩展。

## 2. 核心代码位置

- 页面接入：`bml-frontend/src/views/api/ApiAccountManage.vue`
- 紧凑查询面板：`bml-frontend/src/components/governance/GovernanceCompactQueryPanel.vue`
- 通用表单分区：`bml-frontend/src/components/governance/GovernanceFormSections.vue`
- 查询 schema：`bml-frontend/src/composables/useApiAccountQuerySchema.ts`
- 主次条件拆分工具：`bml-frontend/src/composables/useGovernanceSectionPriority.ts`

## 3. 当前页面结构

当前查询区接入方式如下：

```vue
<GovernanceCompactQueryPanel class="query-panel" max-width="1260px">
  <template #note>
    <a-button @click="toggleQueryAdvanced">展开更多查询条件</a-button>
  </template>

  <template #footerActions>
    <a-button type="primary" @click="handleSearch">查询账号</a-button>
    <a-button @click="handleResetSearch">重置条件</a-button>
  </template>

  <a-form :model="queryForm" layout="vertical">
    <GovernanceFormSections
      :model="queryFormAsRecord"
      :sections="queryPrimarySections"
      variant="embedded"
    />
  </a-form>
</GovernanceCompactQueryPanel>
```

说明如下：

1. 常显条件通过 `queryPrimarySections` 渲染。
2. 扩展条件通过 `querySecondarySections` 渲染。
3. `#note` 插槽承载左下角展开入口。
4. `#footerActions` 插槽承载右下角查询与重置按钮。

## 4. 查询字段主次拆分规则

当前 6 个条件按优先级拆分为：

### 4.1 常显条件

1. 账号名称
2. 业务系统
3. 状态
4. 账号类型

### 4.2 折叠条件

1. 调用客户端
2. 接入环境

## 5. 维护规则

新增查询字段时，按以下顺序处理：

1. 在 `queryForm` 中新增字段。
2. 在接口查询参数中补充入参。
3. 在 `useApiAccountQuerySchema.ts` 中新增 schema。
4. 根据频率决定 `priority` 为 `primary` 或 `secondary`。
5. 在 `handleResetSearch` 中补充重置逻辑。

## 6. 不建议的做法

1. 在查询区中重新加入大段提示文字。
2. 在底部区域增加额外徽标、说明或工作带内容。
3. 在页面模板里直接手写新的查询控件结构。
4. 把低频条件重新全部塞回首屏。

## 7. 验证方式

1. 执行 `npm run build`。
2. 检查左下角是否为展开更多查询条件按钮。
3. 检查右下角是否只保留查询和重置按钮。
4. 检查展开扩展条件后的布局是否稳定。
5. 检查移动端是否能正常换行。
