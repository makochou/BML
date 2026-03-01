# API 账号回调筛选 Schema 与表单校验复用说明

## 1. 文档目标

本文档说明本次新增的两项前端复用能力：

1. 回调日志抽屉筛选器改为 Schema 驱动
2. 账号新建/编辑表单校验规则抽成独立组合式函数

本次改造的目标是继续降低页面脚本复杂度，让“筛选字段定义”和“表单校验规则”都从业务页面中剥离出来，后续扩展时优先改配置和组合式函数，而不是直接改页面模板或重复复制校验逻辑。

## 2. 回调日志筛选 Schema

### 2.1 文件位置

- `bml-frontend/src/composables/useApiAccountCallbackLogFilterSchema.ts`
- `bml-frontend/src/components/api-account/ApiCallbackLogWorkbenchDrawer.vue`

### 2.2 当前职责

`useApiAccountCallbackLogFilterSchema.ts` 统一维护回调日志抽屉中的筛选字段，包括：

1. 字段标识
2. 字段标题
3. 组件类型
4. 下拉选项
5. 占位提示

当前已纳管字段：

1. `callbackStatus`

### 2.3 接入方式

回调日志抽屉内部不再手写 `a-form-item`，而是统一通过：

1. `GovernanceFormSections.vue`
2. `useApiAccountCallbackLogFilterSchema.ts`

完成渲染。

核心接入流程如下：

1. 父页面继续维护响应式筛选对象 `callbackLogFilters`
2. 抽屉组件接收 `filters`
3. 抽屉内部将 `filters` 作为 `model`
4. `GovernanceFormSections` 根据 schema 自动渲染筛选项

### 2.4 后续如何扩展筛选项

如果后续需要新增“业务类型 / 事件类型 / 响应码 / 时间范围”等条件，优先修改：

- `bml-frontend/src/composables/useApiAccountCallbackLogFilterSchema.ts`

建议步骤：

1. 在 `fields` 中新增字段定义
2. 选择对应的 `kind`
3. 补充 `componentProps`
4. 在父页面的查询参数里接入对应字段
5. 保持抽屉模板不变

## 3. 账号表单校验组合式函数

### 3.1 文件位置

- `bml-frontend/src/composables/useApiAccountFormValidation.ts`
- `bml-frontend/src/types/apiAccount.ts`
- `bml-frontend/src/views/api/ApiAccountManage.vue`

### 3.2 当前职责

`useApiAccountFormValidation.ts` 统一收口以下逻辑：

1. 必填校验
2. 业务系统编码格式校验
3. 回调地址合法性校验
4. 调用客户端去重
5. 环境白名单文本解析
6. 提交负载标准化与构建

这样页面提交时只需要关心两件事：

1. 触发校验
2. 使用校验成功后返回的标准 payload

### 3.3 当前暴露的方法

#### 3.3.1 `validateAndBuildPayload(form)`

职责：

1. 校验账号表单
2. 标准化字符串字段
3. 生成可直接提交后端的 `SaveApiAccountPayload`

返回结构：

1. 校验失败：
   - `valid: false`
   - `message`
2. 校验成功：
   - `valid: true`
   - `payload`
   - `environmentIpWhitelist`

#### 3.3.2 `parseIpWhitelistInput(value)`

职责：

1. 将按行、逗号、分号输入的白名单文本解析为数组
2. 自动去空值
3. 自动去重

#### 3.3.3 `buildEnvironmentIpWhitelistPayload(textMap)`

职责：

1. 将三环境白名单文本统一转成结构化对象
2. 供提交负载和其他白名单统计逻辑复用

#### 3.3.4 `isValidCallbackUrl(value)`

职责：

1. 校验是否为合法的 `http / https` 地址

## 4. 当前页面如何使用

### 4.1 管理页提交逻辑

`ApiAccountManage.vue` 中的 `submitAccountForm` 现在只负责：

1. 调用 `validateAndBuildPayload(accountForm)`
2. 若失败则展示统一提示
3. 若成功则调用新增或更新接口

这样做的直接收益是：

1. 页面提交流程更短
2. 校验规则可在多个页面共享
3. 后续账号详情页若增加“快速编辑”能力时，可以直接复用同一套校验逻辑

### 4.2 表单模型类型

本次在 `apiAccount.ts` 中新增了：

1. `ApiAccountFormModel`
2. `ApiCallbackLogFilterModel`

这样后续新增页面时可以直接复用已有表单模型和筛选模型，减少重复手写类型。

## 5. 推荐维护方式

### 5.1 如果要新增账号字段校验

优先修改：

- `bml-frontend/src/composables/useApiAccountFormValidation.ts`

建议步骤：

1. 在 `validateAndBuildPayload` 中补充校验规则
2. 在标准化阶段补充默认值或格式转换
3. 如需提交后端，补充 `payload` 映射
4. 页面层只保留提示和调用逻辑

### 5.2 如果要改回调日志筛选器视觉

优先修改：

- `bml-frontend/src/components/api-account/ApiCallbackLogWorkbenchDrawer.vue`
- `bml-frontend/src/components/governance/GovernanceFormSections.vue`

不要再回父页面中手写回调日志筛选项。

## 6. 开发规范说明

本次改造遵循以下原则：

1. 不新增任何依赖
2. 不修改后端接口契约
3. Schema 负责字段定义，组件负责渲染，页面负责状态与动作
4. 校验逻辑统一收口，避免多个页面各写一套
5. 保持页面视觉语言与现有治理工作台一致

## 7. 后续建议

如果继续推进，建议优先考虑：

1. 把接口授权抽屉中的筛选器也继续向更细粒度的 schema 配置演进
2. 把账号表单的默认值工厂函数也抽成组合式能力，进一步减少页面初始化代码
3. 把白名单文本解析和统计规则继续沉淀到单独的治理工具模块
