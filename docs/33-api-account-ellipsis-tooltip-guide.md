# API账号管理列表截断文本悬浮展示维护指南

## 1. 目标

当列表列宽不足导致文本被截断时，鼠标悬浮可展示完整内容，保证：

1. 列表保持紧凑布局，不被长文本撑破。
2. 关键信息可完整查看，不需要强制拉宽列。
3. 方案可复用到其他表格页，减少重复实现。

## 2. 本次实现范围

页面：`bml-frontend/src/views/api/ApiAccountManage.vue`

已接入悬浮完整显示的列：

1. 账号名称
2. 账号ID
3. 业务系统
4. 客户端（列表显示紧凑摘要，悬浮显示完整客户端集合）

## 3. 通用组件设计

新增组件：`bml-frontend/src/components/common/EllipsisTooltipText.vue`

核心能力：

1. 自动检测文本是否发生视觉截断（`scrollWidth > clientWidth`）。
2. 仅在截断时启用 Tooltip，避免无意义悬浮层。
3. 提供统一变体：`text` / `primary` / `mono`。
4. 支持独立 `tooltipText`，用于“列表摘要 + 悬浮完整值”场景。

## 4. 关键参数说明

`EllipsisTooltipText` 入参：

1. `text`：单元格实际显示文本。
2. `tooltipText`：可选；悬浮展示内容。未传时默认等于 `text`。
3. `fallback`：空值兜底，默认 `-`。
4. `variant`：文本视觉风格（默认 `text`）。
5. `alwaysShowTooltip`：是否始终启用悬浮层（默认 `false`）。

## 5. 页面接入规范

示例：

```vue
<div class="table-field-cell">
  <EllipsisTooltipText
    :text="record.systemName || '-'"
  />
</div>
```

“摘要显示 + 完整悬浮”示例：

```vue
<EllipsisTooltipText
  :text="getCompactClientSummary(record.clientTypes)"
  :tooltip-text="getFullClientTypeSummary(record.clientTypes)"
/>
```

## 6. 为什么不用原生 `title`

统一使用 `a-tooltip` 的原因：

1. 视觉风格与当前 Arco 体系一致。
2. 在仅截断时显示，体验更可控。
3. 组件可继续扩展（多行内容、复制、主题等）。

## 7. 回归测试清单

1. 将“账号ID”列拉窄，确认文本被截断时悬浮显示完整 `#ID`。
2. 将“客户端”列拉窄，确认列表显示摘要，悬浮显示全部客户端文本。
3. 将列拉宽至可完整展示，确认悬浮层自动不再触发。
4. 切换分页、筛选、刷新页面后，悬浮行为保持正常。
5. 执行 `npm run build`，确保类型检查和构建通过。

## 8. 复用建议

后续其他列表页如存在“字段较长 + 列宽紧凑”场景，优先直接复用 `EllipsisTooltipText`，不要再散落写 `title` 或重复实现宽度检测逻辑。
