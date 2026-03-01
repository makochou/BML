# API账号查询Schema与白名单编辑组件说明

## 1. 文档目标

本文档说明本次新增的两项复用能力：

1. 查询区筛选条件改为 Schema 驱动
2. 白名单编辑区下沉为独立 `ApiEnvironmentWhitelistEditor` 组件

本次改造的目标是继续降低页面模板复杂度，让“查询条件”和“白名单编辑”都能按统一模型扩展。

## 2. 新增内容

### 2.1 查询区 Schema

文件位置：

- `bml-frontend/src/composables/useApiAccountQuerySchema.ts`

职责：

- 统一维护账号查询区的筛选字段
- 统一维护占位文案、下拉选项和组件参数
- 后续新增查询条件时优先改这里，而不是回页面里新增表单项

当前纳管的查询字段：

1. 账号名称
2. 状态
3. 账号类型
4. 调用客户端
5. 业务系统
6. 接入环境

### 2.2 白名单编辑组件

文件位置：

- `bml-frontend/src/components/api-account/ApiEnvironmentWhitelistEditor.vue`

职责：

- 统一承载按环境维护 IP 白名单的完整编辑区
- 统一展示当前生效环境、当前生效条目、三环境卡片和输入框
- 统一处理环境白名单文本回写

组件输入：

- `modelValue`
- `accessEnvironment`
- `environmentOptions`

组件输出：

- `update:modelValue`

适用场景：

- API账号新建/编辑页
- 后续应用接入白名单治理页
- 其他需要同结构环境白名单编辑器的工作台

### 2.3 通用表单渲染器增强

文件位置：

- `bml-frontend/src/components/governance/GovernanceFormSections.vue`
- `bml-frontend/src/types/governance.ts`

本次增强点：

1. 新增 `variant`，支持 `card / embedded`
2. 新增 `hideHeader`
3. 查询区使用 `embedded` 变体，弹窗表单继续使用 `card` 变体

这样可以保证：

- 同一套 Schema 渲染能力可以覆盖不同页面壳层
- 查询区不会额外再嵌一层厚卡片
- 编辑弹窗仍保留更完整的工作台面板质感

## 3. 页面接入结果

### 3.1 查询区

当前 `ApiAccountManage.vue` 已通过：

- `GovernanceFormSections.vue`
- `useApiAccountQuerySchema.ts`

实现 schema 驱动查询表单。

### 3.2 白名单编辑区

当前 `ApiAccountManage.vue` 已通过：

- `ApiEnvironmentWhitelistEditor.vue`

替换原来的内联白名单模板。

## 4. 推荐维护方式

### 4.1 如果要新增查询条件

优先修改：

- `bml-frontend/src/composables/useApiAccountQuerySchema.ts`

常见步骤：

1. 在 `fields` 中新增字段
2. 选择对应组件类型
3. 补充 `componentProps`
4. 在查询请求参数里补充该字段的提交逻辑

### 4.2 如果要改白名单编辑样式或交互

优先修改：

- `bml-frontend/src/components/api-account/ApiEnvironmentWhitelistEditor.vue`

常见改法：

1. 改编辑区标题和说明
2. 改环境卡片布局
3. 改当前生效条目统计口径
4. 改输入提示与辅助说明

## 5. 开发规范说明

本次改造遵循以下原则：

1. 不新增任何依赖
2. Schema 负责字段定义，页面负责状态和查询动作
3. 白名单复杂结构独立下沉，避免继续堆积在页面模板中
4. 不改变现有后端接口契约和统一返回结构

## 6. 后续建议

如果继续推进，可以优先考虑：

1. 把回调日志筛选器也改成 Schema 驱动
2. 把账号创建校验规则抽成独立组合式函数
3. 把查询区摘要卡与筛选区一起抽成完整的查询工作台组件
