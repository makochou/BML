# API账号管理顶部概览 Deck 维护指南

## 1. 文档目标

本文档用于说明 `API账号管理` 页面顶部“治理横幅 + 摘要卡带”的维护方式。

本区域的目标不是简单展示数字，而是完成以下职责：

1. 在首屏快速说明当前页面职责。
2. 给出最常用的快捷操作入口。
3. 用紧凑方式展示核心治理指标与运营摘要。
4. 为后续其他治理页复用提供统一组件模板。

## 2. 代码位置

核心文件如下：

- 页面接入：`bml-frontend/src/views/api/ApiAccountManage.vue`
- 通用组件：`bml-frontend/src/components/governance/GovernanceOverviewDeck.vue`
- 类型定义：`bml-frontend/src/types/governance.ts`

## 3. 当前接入方式

页面当前通过如下方式接入：

```vue
<GovernanceOverviewDeck
  eyebrow="Project API Authorization Center"
  badge="全项目接口授权工作台"
  title="API账号管理"
  description="统一维护 API 账号、接入治理字段、按环境白名单、接口授权和回调日志重试，让开放能力治理更集中、更清晰。"
  :tags="heroCapabilityTags"
  :hero-stats="overviewHeroStats"
  :summary-cards="overviewSummaryCards"
  theme="ocean"
  max-width="1260px"
  align="center"
  compact
>
  <template #actions>
    <a-button type="primary">同步接口目录</a-button>
    <a-button>新建账号</a-button>
  </template>
</GovernanceOverviewDeck>
```

## 4. 组件职责

`GovernanceOverviewDeck.vue` 统一负责以下内容：

1. 眉题与主标题。
2. 描述文案。
3. 标签胶囊区。
4. 操作按钮区。
5. 顶部 KPI 指标区。
6. 底部摘要卡区。
7. 最大宽度、对齐方式与紧凑密度控制。
8. 桌面端与移动端的响应式布局。

## 5. 配置数据说明

### 5.1 能力标签

能力标签由 `heroCapabilityTags` 维护，建议保持短句形式，例如：

```ts
const heroCapabilityTags = ['统一纳管', '白名单隔离', '接口授权', '回调闭环'];
```

建议规范如下：

1. 每个标签尽量控制在 4 到 8 个字。
2. 优先描述“能力”而不是“实现细节”。
3. 避免过长文案导致首屏拥挤。

### 5.2 顶部 KPI

顶部 KPI 由 `overviewHeroStats` 维护，类型为 `WorkbenchStatCard[]`。

适合放入该区域的数据特征如下：

1. 能概括全局状态。
2. 数值短、辨识度高。
3. 对用户进入页面后的第一眼判断有帮助。

当前示例包括：

1. 纳管账号
2. 累计授权
3. 可授权接口

### 5.3 底部摘要卡

底部摘要卡由 `overviewSummaryCards` 维护，类型为 `GovernanceOverviewCard[]`。

适合放入该区域的数据特征如下：

1. 更偏运营观察。
2. 可以配合图标快速识别。
3. 与顶部 KPI 语义尽量不要重复。

当前示例包括：

1. 启用账号
2. 生产接入
3. 已配回调
4. 外部账号

## 6. 新增摘要卡的标准做法

如果后续要新增一个顶部或底部指标，建议按以下步骤处理：

1. 先确认该指标属于“顶部 KPI”还是“底部摘要卡”。
2. 在 `ApiAccountManage.vue` 中扩展对应 `computed` 数据。
3. 如需新增图标，统一从 `@arco-design/web-vue/es/icon` 引入。
4. 如需新增强调色，优先复用 `blue`、`green`、`teal`、`gold`、`violet`。

## 7. 布局控制参数

组件当前新增了三类通用布局参数：

1. `maxWidth`：控制概览 Deck 的最大宽度，避免铺满整页。
2. `align`：控制 Deck 的对齐方式，当前支持 `start` 和 `center`。
3. `compact`：开启紧凑模式后，会统一压缩留白、标题字号、按钮高度和摘要卡尺寸。

当前页面推荐接入参数如下：

```vue
<GovernanceOverviewDeck
  max-width="1260px"
  align="center"
  compact
/>
```

## 8. 视觉维护规范

本组件当前采用以下视觉规范：

1. 顶部采用高饱和蓝绿渐变治理横幅。
2. 摘要卡采用浅色玻璃感卡片。
3. 主按钮使用高亮渐变，次按钮使用白色胶囊按钮。
4. 通过“顶部强视觉 + 底部弱视觉”形成信息层级。

后续调整视觉时，请优先修改组件内部样式，不要在业务页面里零散追加覆盖样式。

## 9. 响应式规范

组件已内置以下响应式行为：

1. 中屏下顶部横幅会自动改为单列布局。
2. 窄屏下摘要卡会自动降为双列或单列。
3. 移动端下操作按钮会自动撑满宽度，避免点击区域过小。

## 10. 验证方式

本区域调整完成后，建议至少执行以下验证：

1. 执行 `npm run build`，确认编译通过。
2. 桌面端查看顶部横幅和摘要卡是否更紧凑。
3. 检查操作按钮与标签是否对齐且不拥挤。
4. 检查 1280px 以下是否能平滑换行。
5. 检查移动端下卡片和按钮是否出现溢出。
