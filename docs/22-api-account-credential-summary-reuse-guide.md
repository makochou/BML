# API账号凭证交付与详情摘要复用说明

## 1. 文档目标

本文档说明本次新增的两层复用能力：

1. 凭证查看弹窗独立业务组件
2. 账号详情页授权摘要与回调摘要组合式数据模型

本次改造目标是继续减少管理页模板体积，同时保证“查看凭证”“查看详情”“跳转授权”“跳转回调日志”这一整套治理链路保持统一的视觉语言和统一的数据卡片规范。

## 2. 新增内容

### 2.1 凭证交付组件

文件位置：

- `bml-frontend/src/components/api-account/ApiCredentialDeliveryModal.vue`

职责：

- 统一承载新建账号和重置密钥后的凭证交付弹窗
- 统一展示 Hero 指标、保存动作、签名请求头、密钥卡片、账号画像和环境白名单
- 统一处理单字段复制与整套凭证复制

组件输入：

- `visible`：弹窗是否显示
- `payload`：后端返回的凭证载荷

组件输出：

- `update:visible`：关闭弹窗时回传最新显示状态

适用场景：

- API账号管理页
- 后续独立凭证查看页
- 后续其他需要“一次性安全交付密钥”的账号治理页面

### 2.2 详情摘要组合式函数

文件位置：

- `bml-frontend/src/composables/useApiAccountDetailSummaries.ts`

职责：

- 统一生成账号详情 Hero 指标
- 统一生成授权摘要指标
- 统一生成回调摘要指标

输入参数：

- `detail`
- `authorizationSnapshot`
- `callbackSummary`

返回结果：

- `detailHeroStats`
- `detailAuthorizationStats`
- `detailCallbackStats`

这样做的好处是：后续如果再新增“应用详情页”“第三方接入详情页”这类同结构页面，只需要替换数据源，不需要再重复维护一套相同的指标映射。

## 3. 管理页接入方式

当前 `ApiAccountManage.vue` 已改为直接接入 `ApiCredentialDeliveryModal`：

```vue
<ApiCredentialDeliveryModal
  v-model:visible="credentialModal.visible"
  :payload="credentialModal.payload"
/>
```

这样处理后，管理页不再直接维护：

1. 凭证弹窗模板结构
2. 凭证交付区文案与卡片映射
3. 复制单字段与复制整套凭证逻辑
4. 凭证弹窗专属样式

维护建议：

- 要改凭证展示结构，优先改 `ApiCredentialDeliveryModal.vue`
- 不要再回 `ApiAccountManage.vue` 中内联堆叠凭证弹窗模板

## 4. 详情页接入方式

当前 `ApiAccountDetail.vue` 已改为通过组合式函数生成摘要指标：

```ts
const { detailHeroStats, detailAuthorizationStats, detailCallbackStats } =
  useApiAccountDetailSummaries({
    detail,
    authorizationSnapshot,
    callbackSummary
  });
```

这样处理后，详情页中授权摘要和回调摘要不再直接写死在页面脚本里，后续要统一调整卡片文案、指标顺序或色彩语义时，只需要改一处。

## 5. 推荐维护顺序

### 5.1 如果要改凭证弹窗

优先修改：

- `bml-frontend/src/components/api-account/ApiCredentialDeliveryModal.vue`

常见改法：

1. 改顶部交付提示：修改 `heroTags`
2. 改保存动作说明：修改 `safetyGuide`
3. 改请求头说明：修改 `requestHeaders`
4. 改摘要卡或画像卡：修改 `summaryCards`、`profileCards`

### 5.2 如果要改详情摘要卡

优先修改：

- `bml-frontend/src/composables/useApiAccountDetailSummaries.ts`

常见改法：

1. 改授权摘要口径：修改 `detailAuthorizationStats`
2. 改回调摘要口径：修改 `detailCallbackStats`
3. 改详情 Hero 指标：修改 `detailHeroStats`

## 6. 开发规范说明

本次改造遵循以下原则：

1. 不新增任何额外依赖，继续使用现有 Vue 组合式能力和 Arco Design 组件体系
2. 展示卡片统一复用 `GovernanceWorkbenchShell / GovernancePanel / GovernanceFactGrid`
3. 复制、摘要、白名单展示均收口到独立组件或组合式函数，避免页面层继续膨胀
4. 所有变更均保持现有接口契约不变，不影响后端统一响应和统一返回结构

## 7. 建议的后续扩展

如果后续继续推进 API账号治理页面的复用，可以优先考虑：

1. 把凭证交付区的密钥卡再拆成更细的 `CredentialKeyCard` 组件
2. 把详情页摘要组合式函数继续扩展为“详情页工作台数据工厂”
3. 把授权摘要与回调摘要的卡片文案做成可配置常量，支持不同业务域快速换肤
