# API账号详情摘要与表单Schema复用说明

## 1. 文档目标

本文档说明本次新增的两层复用能力：

1. 账号详情页核心摘要区独立业务组件
2. 新建/编辑账号表单区 Schema 配置模型

本次改造的目标是进一步减少页面模板体积，让“详情摘要”和“账号表单”都转为可维护、可扩展、可复用的结构。

## 2. 新增内容

### 2.1 账号详情摘要组件

文件位置：

- `bml-frontend/src/components/api-account/ApiAccountDetailSummaryPanels.vue`

职责：

- 统一承载详情页的核心治理摘要
- 统一承载授权摘要和回调摘要双栏区
- 统一对外暴露“打开授权工作台 / 打开回调日志”动作

组件输入：

- `overviewStats`
- `authorizationStats`
- `callbackStats`

组件输出：

- `openAuthorization`
- `openCallback`

适用场景：

- API账号详情页
- 后续其他接入账号详情页
- 需要同类“概览 + 授权 + 回调”摘要结构的治理页

### 2.2 通用治理表单分区渲染器

文件位置：

- `bml-frontend/src/components/governance/GovernanceFormSections.vue`

职责：

- 根据字段 schema 自动渲染表单分区
- 统一处理输入框、选择器、数字框、日期框和文本域
- 统一处理标题、描述、表单项壳层、辅助文案和响应式布局

适用场景：

- API账号新建/编辑表单
- 后续其他治理工作台中的配置表单

### 2.3 API账号表单 Schema

文件位置：

- `bml-frontend/src/composables/useApiAccountFormSchema.ts`

职责：

- 统一维护“基础归属信息 / 接入策略与安全配置 / 回调与补充说明”三大分区
- 统一维护字段组件类型、占位文案、组件参数和辅助说明
- 后续新增普通治理字段时优先修改 schema，而不是改模板

### 2.4 通用字段类型定义

文件位置：

- `bml-frontend/src/types/governance.ts`

新增模型：

- `GovernanceFieldKind`
- `GovernanceFormFieldSchema`
- `GovernanceFormSectionSchema`

## 3. 页面接入结果

### 3.1 账号详情页

当前 `ApiAccountDetail.vue` 已改为直接接入：

- `ApiAccountDetailSummaryPanels.vue`
- `useApiAccountDetailSummaries.ts`

这样处理后，详情页不再手写整块摘要模板结构，只负责传递指标数据和跳转动作。

### 3.2 账号管理页

当前 `ApiAccountManage.vue` 已改为直接接入：

- `GovernanceFormSections.vue`
- `useApiAccountFormSchema.ts`

这样处理后，基础信息、接入策略、回调说明都转为配置驱动；后续新增普通字段时，只需要修改 schema。

## 4. 维护方式

### 4.1 如果要改详情摘要区

优先修改：

- `bml-frontend/src/components/api-account/ApiAccountDetailSummaryPanels.vue`

适合修改的内容：

1. 卡片布局
2. 标题与说明文案
3. 双栏区按钮文案
4. 摘要卡的视觉样式

### 4.2 如果要改详情摘要指标口径

优先修改：

- `bml-frontend/src/composables/useApiAccountDetailSummaries.ts`

适合修改的内容：

1. Hero 指标口径
2. 授权摘要统计口径
3. 回调摘要统计口径

### 4.3 如果要新增账号表单字段

优先修改：

- `bml-frontend/src/composables/useApiAccountFormSchema.ts`

常见步骤：

1. 在对应分区的 `fields` 中新增字段配置
2. 选择字段类型，例如 `input / select / input-number / date-picker / textarea`
3. 在 `componentProps` 中补充占位符、选项、最大长度等参数
4. 如有辅助说明，使用 `helper`

说明：

- 普通字段不需要再回页面模板加 `a-form-item`
- 只有像“按环境维护 IP 白名单”这种强业务结构分区，才继续保留独立模板

## 5. 开发规范说明

本次改造遵循以下原则：

1. 不新增任何依赖，继续使用现有 Vue 组合式能力和 Arco Design 体系
2. 页面只负责业务状态与事件，复杂结构下沉到业务组件或 Schema
3. 字段模型与渲染器分离，后续扩展字段时不直接侵入页面模板
4. 不改后端接口契约，不影响统一响应和统一返回结构

## 6. 后续建议

如果继续推进这一套治理体系，可以优先考虑：

1. 把查询区筛选条件也改成 Schema 驱动
2. 把白名单治理区单独抽成 `ApiEnvironmentWhitelistEditor` 组件
3. 把详情页“白名单 + 最近日志”继续拼成完整的详情内容工作台组件
