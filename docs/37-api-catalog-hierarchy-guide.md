# API 接口目录层级映射文档说明

## 1. 目标

为了让「API 接口列表」左侧树中的接口名称更贴近业务（例如：**用户管理 — 用户新增**），同时又能在代码之外集中维护**菜单层级 → 接口**关系，本项目新增一份**接口目录层级映射文档**：

- 文档以 **JSON 文件** 形式存在，便于前端直接读取解析；
- 每一条记录明确一个接口的：
  - 上上级菜单（模块，如「API 管理」）；
  - 上级菜单（业务资源，如「API 账号管理」）；
  - 接口业务名称（如「新增 API 账号」）。
- 前端在渲染接口目录时，优先读取该文档中的配置作为**展示名称来源**。

> 当后续新增接口时，只需要在这份文档中补充一行配置，即可自动在前端列表中生效。

---

## 2. 文档存放位置与文件名

- **实际数据文件**：`bml-frontend/public/api-catalog-hierarchy.json`  
  - 由前端通过 `fetch('/api-catalog-hierarchy.json')` 读取；
  - Vite 构建时会原样拷贝到发布包根目录，线上也可以直接访问。
- **本说明文档**：`docs/37-api-catalog-hierarchy-guide.md`（即本文）

二者配合使用：

- JSON 文件承载「机器可读」的映射数据；
- Markdown 文档说明格式、维护规范和最佳实践。

---

## 3. JSON 文件格式

### 3.1 顶层结构

`api-catalog-hierarchy.json` 为一个对象（Object），**key 为接口标识，value 为层级信息**：

```json
{
  "GET /open/api/ping": {
    "topMenu": "API 管理",
    "parentMenu": "OpenAPI 示例",
    "apiName": "OpenAPI 签名链路健康检查"
  }
}
```

- **key**：`<HTTP 方法> + 空格 + <接口路径>`  
  - 例如：`GET /account/{id}`、`POST /account/callback-log/{logId}/retry`。
  - 必须与后端 `SysApiListController.tree()` 返回的 `httpMethod` 与 `apiUrl` 字段保持一致。
- **value**：`ApiDisplayMeta` 对象：
  - `topMenu`：上上级菜单名称（通常为左侧主菜单），例如 `"API 管理"`；
  - `parentMenu`：上级菜单名称（业务资源），例如 `"API 账号管理"`；
  - `apiName`：接口业务名称，例如 `"新增 API 账号"`；
  - `displayName`（可选）：若配置，前端会直接使用该字段作为完整展示名，忽略 `topMenu` / `parentMenu` / `apiName` 的拼接。

### 3.2 示例片段

当前已为部分 API 账号相关接口配置示例（节选）：

```json
{
  "GET /open/api/ping": {
    "topMenu": "API 管理",
    "parentMenu": "OpenAPI 示例",
    "apiName": "OpenAPI 签名链路健康检查"
  },
  "GET /account/{id}": {
    "topMenu": "API 管理",
    "parentMenu": "API 账号管理",
    "apiName": "查看 API 账号详情"
  },
  "POST /account": {
    "topMenu": "API 管理",
    "parentMenu": "API 账号管理",
    "apiName": "新增 API 账号"
  },
  "GET /account/{id}/authorization": {
    "topMenu": "API 管理",
    "parentMenu": "API 账号授权",
    "apiName": "查看账号授权快照"
  }
}
```

---

## 4. 前端如何使用该文档

前端 `views/api/ApiList.vue` 中的实现要点：

- 定义 `ApiDisplayMeta` 类型，与本 JSON 结构一一对应；
- 通过 `fetch('/api-catalog-hierarchy.json')` 在页面加载时读取文档，并缓存到 `apiDisplayMap`；
- 渲染接口行时优先调用：

```ts
const key = `${method} ${path}`; // 如 "GET /account/{id}"
const meta = apiDisplayMap[key];
```

并按如下优先级生成展示名称：

1. 若 `meta.displayName` 非空，直接使用；
2. 否则按 `topMenu — parentMenu — apiName` 拼接；
3. 若文档中无该接口或字段为空，则回退为「业务分组名称 + 接口 label/description/apiUrl」的默认策略。

详细代码可见 `getApiDisplayName` 函数内联注释。

---

## 5. 维护规范（新增 / 修改 / 删除）

### 5.1 新增接口时

当后端新增接口并纳入 `sys_api_registry` 后，需要同步更新本 JSON 文档：

1. 在 `docs/API接口详细说明.md` 中先补充该接口的**路径、方法与说明**；
2. 在 `bml-frontend/public/api-catalog-hierarchy.json` 中，新增一条对应记录：

```json
"GET /system/user/page": {
  "topMenu": "系统管理",
  "parentMenu": "用户管理",
  "apiName": "分页查询用户列表"
}
```

3. 执行前端构建并手工验证「API 接口列表」页面展示是否符合预期。

### 5.2 修改接口层级或文案时

- 若仅调整菜单层级展示（例如从「API 管理」改到「系统管理」），只需更新 `topMenu` 或 `parentMenu` 字段；
- 若希望展示更贴业务的名称，可以：
  - 调整 `apiName`，例如从 `"删除 API 账号"` 调整成 `"注销 API 账号"`；
  - 或者直接配置 `displayName`，例如 `"API 管理 — API 账号管理 — 注销 API 账号"`。

### 5.3 删除接口或废弃映射

- 当接口物理删除或从 `sys_api_registry` 中移除时，应同步从本 JSON 中删除对应 key，避免产生「孤儿映射」；
- 前端在找不到映射 key 时会自动回退为默认展示逻辑，不会报错。

---

## 6. 与现有文档和规范的关系

- 与 `docs/API接口详细说明.md` 保持一一对应，后者是「接口能力说明」，本文件是「接口在门户中的展示层级说明」；
- 与 `docs/36-api-list-feature-guide.md` 中的 API 列表功能说明互补，属于该功能的「展示层级配置」部分；
- 遵循统一响应、统一异常等规范，不改变后端返回结构，仅影响前端 UI 呈现。

---

## 7. 小结

- 通过在 `api-catalog-hierarchy.json` 中集中维护「菜单层级 → 接口」映射，可以：
  - 保证接口目录展示名称的一致性与可读性；
  - 降低前端硬编码、分散维护带来的成本；
  - 让后续新接口的接入路径明确：**先补文档，再补 JSON，最后开发页面**。
- 前端实现已做好降级处理；即使该文档暂未更新，也能保持现有默认展示，不影响功能可用性。

