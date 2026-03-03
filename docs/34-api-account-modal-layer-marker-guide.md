# API账号新建弹窗 ①③ 层级标记维护指南

## 1. 目标

为“新建API账号”弹窗提供清晰的视觉层级标记，确保开发、测试、产品在沟通时能统一指向：

1. `①` 一级：弹窗标题层
2. `③` 三级：业务字段分组层（置于 `①` 上方）

## 2. 代码落点

1. 弹窗页面：`bml-frontend/src/views/api/ApiAccountManage.vue`
2. 通用分组渲染器：`bml-frontend/src/components/governance/GovernanceFormSections.vue`

## 3. 设计实现（最新）

### 3.1 一级标记（标题层）

在弹窗标题主区保留一级标记，并与三级标记一起展示：

```vue
<div class="account-modal-title__markers">
  <div class="account-layer-marker account-layer-marker--lv3">
    <span class="account-layer-marker__index">③</span>
    <span class="account-layer-marker__text">三级：业务字段分组</span>
  </div>
  <div class="account-layer-marker account-layer-marker--lv1">
    <span class="account-layer-marker__index">①</span>
    <span class="account-layer-marker__text">一级：弹窗标题层</span>
  </div>
</div>
```

### 3.2 二级标记处理

按最新需求，已移除 `②`（表单总览层）标记，避免视觉噪音与层级歧义。

### 3.3 三级标记（业务字段分组层）

三级标记由标题区统一承载，并放在一级标记上方，形成“全局层级指引”。

## 4. 组件通用化规范

`GovernanceFormSections` 仍保留可选层级 props（默认空字符串，不影响现有页面）：

1. `headingLevelMark?: string`
2. `headingLevelText?: string`

当前页面已不再使用该 props，但该能力保留用于后续页面扩展。

## 5. 回归检查

1. 打开“新建API账号”弹窗，确认能看到 `③` 在上、`①` 在下的标记顺序。
2. 确认页面上不再出现 `②` 标记。
3. 使用鼠标滚轮/滚动条上下滚动时，确认 `①` 标记与标题层保持固定不动。
4. 确认右侧页面滚动条不再出现，滚动仅由弹窗内部承载区处理。
5. 切换到非新建场景（编辑等），确认页面不受影响。
6. 在移动端宽度下验证标记文案不溢出。
7. 执行 `npm run build` 确认编译通过。
