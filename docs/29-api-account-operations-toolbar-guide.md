# GovernanceOperationToolbar 组件说明

## 1. 文档目标

本文档用于说明通用组件 `GovernanceOperationToolbar.vue` 的用途。

当前 `API账号管理` 页面已经不再使用该组件，而是回退为更极简的查询卡底部操作布局；但该组件仍可供后续其他页面复用。

## 2. 组件位置

- `bml-frontend/src/components/governance/GovernanceOperationToolbar.vue`

## 3. 适用场景

适用于以下页面：

1. 需要同时展示列表标题、摘要徽标和多组操作按钮。
2. 需要独立工作带而不是极简查询卡 footer。

## 4. 当前支持能力

1. 标题与说明文案。
2. 摘要徽标。
3. 左右分区动作插槽。
4. `card` 与 `embedded` 两种视觉变体。

## 5. 维护建议

如果后续某个页面需要恢复更完整的运营工具带，优先复用该组件，不建议重新手写一套列表头结构。
