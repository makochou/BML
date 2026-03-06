# API 接口列表功能开发指南

## 1. 功能说明

「API 接口列表」是 BML 企业管理系统中用于**树形展示全量纳管 API** 的功能菜单，便于开发与运维人员查阅、对接接口。

### 1.1 展示层级

- **一级：模块**（如：系统管理、API 管理、企业管理）
- **二级：业务资源**（如：用户管理、角色管理、API 账号管理）
- **三级：具体接口**（如：用户列表、新增用户、修改用户、删除用户）

### 1.2 数据来源

数据来自表 `sys_api_registry`，与「开放接口目录」同步结果一致；同步逻辑见 [14-stage-c-implementation-guide.md](./14-stage-c-implementation-guide.md) 及 `SysOpenApiRegistryService.syncRegistry()`。  
模块与控制器的**中文展示名**由 `ApiCatalogDisplayNameSupport` 统一维护，便于扩展。

---

## 2. 后端实现要点

### 2.1 权限码

- **system:apiList:list** — 查询 API 接口目录树（用于列表页展示）

与 [03-permission-design.md](./03-permission-design.md) 中 `module:resource:action` 规范一致。

### 2.2 接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/api-list/tree | 查询 API 接口目录树 | system:apiList:list |

**Query 参数**（可选，与开放接口目录树一致）：

- `keyword`：关键词，匹配接口名称、路径、描述
- `method`：HTTP 方法
- `status`：状态（1 正常，0 停用）
- `moduleName`：模块名称

**响应 data**：树形结构 `List<ApiCatalogTreeNodeVO>`，节点类型为 `MODULE`、`RESOURCE`、`API`；仅 `API` 类型节点带 `apiId`、`httpMethod`、`apiUrl`、`description`、`status`。

### 2.3 扩展展示名

新增模块或控制器时，在 **ApiCatalogDisplayNameSupport** 中补充映射即可：

- 模块：`registerModuleDisplayName(moduleName, "展示名")`
- 控制器：`registerControllerDisplayName(controllerName, "展示名")`

未配置的项将回退为原始名称。

### 2.4 Flyway 迁移

- **V1.7.0__api_list_menu.sql**：插入「API接口列表」菜单（id=236）、权限码、并赋权超级管理员。

---

## 3. 前端实现要点

### 3.1 路由

- 路径：`/admin/api/list`
- 组件：`views/api/ApiList.vue`
- 由静态路由与后端菜单共同支持（菜单 path=api/list，component=api/ApiList）。

### 3.2 请求与类型

- **api/apiList.ts**：`fetchApiListTree(params?)` 请求 `/api-list/tree`
- **types/apiList.ts**：`ApiCatalogTreeNode`、`OpenApiRegistryTreeQuery` 与后端 VO 对齐

### 3.3 页面行为

- 无顶部大标题块，仅保留筛选栏与统计信息。
- 筛选：关键词搜索、HTTP 方法下拉、刷新按钮；统计：模块数、业务分组数、接口数。
- **两栏布局**：
  - **左栏（模块）**：模块列表，点击模块行可展开/收起，展开后在该栏内直接显示其下业务分组（如「API 管理」展开后显示 OpenAPI 示例、API 账号授权、API 账号管理等）；点击某一业务分组后，在中间栏展示该分组下的接口列表。
  - **中栏（接口列表栏）**：选中左栏某一业务分组后，在此栏展示该分组下的接口列表（方法、路径、描述等）；未选中时提示「请先选择左侧业务分组」。

---

## 4. 与开发规范的对齐

- **统一响应**：接口返回 `Result<List<ApiCatalogTreeNodeVO>>`，符合 [12-unified-response-exception-spec.md](./12-unified-response-exception-spec.md)
- **权限**：使用 `@PreAuthorize("@ss.hasPermi('system:apiList:list')")`，符合 [03-permission-design.md](./03-permission-design.md)
- **DTO/VO 分离**：树节点使用 VO，查询参数复用 `OpenApiRegistryTreeQuery`，符合 [06-technical-specification.md](./06-technical-specification.md)
- **版本与依赖**：无新增依赖，版本由父 POM 统一管理

---

## 5. 后续可扩展项

- 树节点支持「仅展示启用接口」开关
- 导出接口清单（Excel/Markdown）
- 从列表跳转到「API 账号授权」并定位到对应接口
