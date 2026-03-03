# API账号新建页空白画布设计指南

## 1. 目标

在“新建API账号”流程暂不展示业务字段时，页面不能只是纯白占位，而要满足：

1. 视觉完整：具备层次、氛围和品牌感。
2. 信息明确：清楚表达“当前不展示内容”的状态。
3. 可复用：后续其他治理页可直接复用同一空白画布组件。

## 2. 改造结果

本次将新建态替换为通用组件 `EmptyWorkbenchCanvas`，并在 `ApiAccountManage.vue` 接入：

1. 使用 `pure` 纯装饰模式，不渲染任何文字内容。
2. 保留高质量视觉层（渐变、光晕、颗粒、漂浮体）维持页面质感。
3. 通过同一组件仍可在未来切回信息展示模式（非 `pure`）。

代码位置：

1. 组件：`bml-frontend/src/components/common/EmptyWorkbenchCanvas.vue`
2. 页面接入：`bml-frontend/src/views/api/ApiAccountManage.vue`

## 3. 组件能力设计

`EmptyWorkbenchCanvas` 采用 props 配置，避免页面写死：

1. `pure`：纯装饰模式（无内容）。
2. `eyebrow`：上方小标题。
3. `title`：主标题。
4. `description`：状态说明。
5. `tags`：标签数组。
6. `metrics`：底部指标卡数组（`label/value/hint`）。

当 `pure=true` 时，仅渲染视觉背景，不展示任意文案信息。

## 4. 视觉规范

组件内部统一定义：

1. 渐变底色 + 光晕层（Aurora）
2. 轻颗粒纹理（Grain）
3. 中央玻璃态主卡（Hero Card）
4. 两侧浮动光球（Orb 动效）
5. 响应式布局（桌面 3 列指标，移动端 1 列）

## 5. 页面接入示例

```vue
<EmptyWorkbenchCanvas
  class="account-create-blank-page"
  pure
/>
```

## 6. 回归检查

1. 打开“新建API账号”弹窗，确认新建区仅展示装饰画布，不出现任何正文内容。
2. 验证页面不展示业务字段与表单组件。
3. 验证顶部标题层（第①层）仍固定且可正常关闭弹窗。
4. 在 768px 以下检查样式是否自适应。
5. 执行 `npm run build`，确认构建通过。
