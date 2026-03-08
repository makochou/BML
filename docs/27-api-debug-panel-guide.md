# API 调试面板使用说明

## 1. 功能概述

在「API 接口列表」页面采用**两栏布局**：左侧为**接口目录**（模块 → 业务分组 → 接口 三级树），右侧为 **API 调试** 栏目，用于对选中的接口进行**调用、访问、调试**。交互方式类似 Postman：配置请求方法、路径、Query 参数、请求头、请求体后点击「发送」，即可在下方查看 HTTP 状态码、耗时、响应头与响应体。

- **入口**：后台管理 → **API 接口列表** 菜单。左侧为整合后的接口目录树，右侧为调试面板（占满剩余宽度，便于调试）。
- **数据来源**：接口目录树数据来自后端 `sys_api_registry` 目录树接口。
- **认证方式**：调试请求默认**自动携带当前登录用户的 JWT（Authorization: Bearer &lt;accessToken&gt;）**，与正常管理端请求一致；适用于需要登录态的管理类接口。  
  **说明**：需 OpenAPI 签名才能访问的接口（如部分 `/open/api/**`）在本面板中仅能使用当前会话调试；若要以外部身份调用，请使用独立客户端并按 OpenAPI 签名规范携带 appKey、timestamp、nonce、sign 等。

## 2. 使用步骤

### 2.1 选择要调试的接口

1. 在左侧「接口目录」中展开目标**模块**（如「API 管理」），其下会显示业务分组（如「OpenAPI 示例」「API 账号管理」等）。
2. 再展开某一**业务分组**，该分组下的接口会直接展示在该分组下方（三级树内联）。
3. **点击某一接口行**（带 HTTP 方法标签与路径的那一行），该行高亮，右侧调试面板会预填该接口的 **HTTP 方法** 与 **请求路径**。

### 2.2 配置请求（可选）

- **Query 参数**：在「Query 参数」标签页中可添加多行键值对，仅**启用且 key 非空**的行会拼接到请求 URL。可随时添加/删除行。
- **请求头**：在「请求头」标签页中可添加自定义 Header；勾选「自动携带当前登录 Token」时，会由调试客户端自动注入 `Authorization: Bearer <accessToken>`，无需手填。
- **请求体**：对 POST / PUT / PATCH，在「请求体」标签页中填写 JSON 字符串（如 `{"key": "value"}`）；留空表示不发送 body。GET/DELETE 一般不填。

### 2.3 发送请求与查看响应

1. 点击工具栏上的 **「发送」** 按钮。
2. 面板会显示**请求中**状态；请求结束后在下方「响应」区域展示：
   - **HTTP 状态码** 与 **状态文案**（如 200 OK）；
   - **请求耗时**（毫秒）；
   - **响应头**（可折叠）；
   - **响应体**（原始内容；若为 JSON 会自动格式化展示）。

若请求失败（网络错误或 4xx/5xx），同样会展示上述信息，便于排查；错误时会有简要 Message 提示。

## 3. 与统一响应、规范的对应关系

- **后端**：业务接口统一返回 `Result<T>`（code、message、data、timestamp、traceId 等），调试面板展示的是**原始 HTTP 响应**（status、headers、body），因此你会看到完整 JSON  body（即包含 code、message、data 的整包），与「统一响应、统一返回」规范一致。
- **前端**：调试请求通过 `getDebugHttpClient()` 发起的 axios 实例发送，该实例仅注入 Token、不做 401 自动刷新与统一错误弹窗，以便在面板内完整展示状态码与响应内容，便于调试。

## 4. 开发说明（便于后续扩展）

### 4.1 前端结构

| 路径 | 说明 |
|------|------|
| `src/views/api/ApiList.vue` | API 接口列表页；左侧接口目录树（模块 → 业务分组 → 接口 三级内联）、右侧调试面板两栏布局；维护 `selectedApi`、`expandedModuleIds`、`expandedResourceIds` 并传入调试面板。 |
| `src/components/api-debug/ApiDebugPanel.vue` | 调试面板组件：占位提示、请求配置（方法/路径/Query/Headers/Body）、发送、响应展示。 |
| `src/types/apiDebug.ts` | 调试面板用类型：`KeyValueRow`、`ApiDebugRequestConfig`、`ApiDebugResponseResult` 及 `createKeyValueRow` 等。 |
| `src/utils/request.ts` | 导出 `apiBaseURL`、`getDebugHttpClient()`；业务请求仍用默认 `request`，调试用独立 client 以保留原始响应。 |

### 4.2 扩展建议

- **环境切换**：若需多环境（如测试/预发/生产），可扩展 `apiBaseURL` 或为 `getDebugHttpClient()` 增加 baseURL 参数，由用户在面板选择。
- **历史记录**：可将每次请求的 method、path、params、headers、body 及响应状态存入本地或后端，便于「历史请求」列表与一键重放。
- **OpenAPI 签名**：若要在面板内以「API 账号」身份调用需签名的接口，可在前端按 OpenAPI 签名规范计算 sign，并在请求头中注入 appKey、timestamp、nonce、sign 等（需后端提供或复用现有签名逻辑）。

### 4.3 依赖与版本

- 本功能未新增后端依赖；前端仅使用现有 axios、Arco Design Vue、Vue 3 等，无需修改 `package.json` 或父 POM。
- 后端统一响应与异常规范见 `docs/12-unified-response-exception-spec.md`；前端请求层规范见 `docs/06-technical-specification.md`。

## 5. 常见问题

- **点击接口后右侧仍是「请选择接口」？**  
  请确认已在左侧**先展开模块、再展开业务分组**，然后点击**接口行**（带 HTTP 方法标签和路径的那一行），而不是模块名或业务分组名。

- **发送后 401？**  
  表示当前登录已过期或未登录，请重新登录后再试；调试面板不会自动刷新 Token。

- **OpenAPI 接口返回 403 或签名错误？**  
  以「当前用户 JWT」方式调用时，仅能访问允许该登录态访问的接口；若接口要求 OpenAPI 签名，需在外部使用 appKey/secret 按规范签名后调用，或后续在面板内扩展「API 账号 + 签名」模式。

- **响应体乱码或非 JSON？**  
  面板按原始响应 body 展示；若后端返回非 UTF-8 或非 JSON，会原样显示，可根据需要在前端做解码或格式检测扩展。
