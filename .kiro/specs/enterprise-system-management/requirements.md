# 需求文档

## 简介

本文档描述企业管理系统（BML）系统管理模块的功能需求。该模块是整个企业管理平台的基础支撑层，为后续采购、销售、库存等业务模块提供统一的组织架构、权限控制和用户管理能力。

系统管理模块包含六个子功能：机构管理、部门管理、岗位管理、角色管理、用户管理、菜单管理。

技术栈：
- 前端：Vue 3 + TypeScript + Arco Design（@arco-design/web-vue）
- 后端：Spring Boot 3.3.6 + MyBatis-Plus 3.5.9 + MariaDB
- 前端路径：`bml-frontend/src/views/business/system/`

---

## 词汇表

- **System**：系统管理模块整体，包含机构、部门、岗位、角色、用户、菜单六个子模块
- **OrgManager**：机构管理子系统，负责维护企业集团/公司/分公司等树形机构结构
- **DeptManager**：部门管理子系统，负责维护归属于机构的多级树形部门结构
- **PostManager**：岗位管理子系统，负责维护企业岗位信息
- **RoleManager**：角色管理子系统，负责维护角色及其菜单权限、数据权限
- **UserManager**：用户管理子系统，负责维护用户账号及其组织归属、角色分配
- **MenuManager**：菜单管理子系统，负责维护系统菜单树及权限标识
- **SysOrg**：机构实体，字段包括 id、parentId、ancestors、orgName、orgCode、orgType、creditCode、legalPerson、registeredCapital、establishDate、sort、leader、phone、email、province、city、district、address、businessScope、status、remark、dataIsolation
- **SysDept**：部门实体，字段包括 id、parentId、orgId、ancestors、deptName、deptCode、deptType、funcType、sort、leader、phone、email、status
- **SysPost**：岗位实体，字段包括 id、postCode、postName、orgId、postCategory、postLevel、sort、status、remark
- **SysRole**：角色实体，字段包括 id、roleName、roleCode、sort、dataScope、status、remark、menuIds
- **SysUser**：用户实体，字段包括 id、username、password、realName、nickname、email、phone、gender、avatar、status、orgId、deptId、postId、employeeNo、entryDate、loginIp、loginDate、remark
- **SysMenu**：菜单实体，字段包括 id、parentId、menuName、menuType、path、component、perms、icon、sort、visible、status、isFrame、remark
- **DataIsolationType**：机构数据隔离模式枚举（0:共享 1:完全隔离 2:汇总共享 3:同级互通 4:按模块隔离）
- **DataScopeType**：角色数据权限范围枚举（1:全部数据 2:所在机构及下级 3:仅本机构 4:所在部门及下级 5:仅本部门 6:仅本人 7:自定义）
- **OrgType**：机构类型枚举（1:集团 2:公司 3:分公司 4:子公司 5:办事处 6:事业部）
- **DeptType**：部门类型枚举（1:事业部 2:中心 3:部门 4:小组）
- **MenuType**：菜单类型枚举（M:目录 C:菜单 F:按钮）
- **UserStatus**：用户状态枚举（1:正常 0:停用 2:锁定）
- **TreeNode**：树形结构节点，包含 children 子节点列表
- **AncestorPath**：祖级路径字符串，以逗号分隔的 ID 链，如 "0,100,101"，用于快速查询子树
- **DataScopeAspect**：后端数据权限 AOP 切面，根据当前用户角色的 DataScopeType 自动过滤查询结果
- **BmlModal**：前端可拖拽、可缩放、支持全屏的弹窗组件
- **GovernanceCompactQueryPanel**：前端查询面板组件，density="ultra" theme="aurora"
- **GovernanceListStage**：前端列表容器组件，density="ultra" body-fill

---

## 需求

### 需求 1：机构管理 — 树形结构维护

**用户故事：** 作为系统管理员，我希望能够维护集团/公司/分公司等多级机构树形结构，以便清晰描述企业组织层级关系。

#### 验收标准

1. THE OrgManager SHALL 以树形结构展示所有机构，支持无限层级嵌套，每个节点显示机构名称、机构编码、机构类型、负责人、联系电话、数据隔离模式、排序、状态、创建时间字段。
2. WHEN 管理员点击"新增机构"按钮，THE OrgManager SHALL 弹出新增表单，表单包含上级机构（树形选择）、机构名称（必填）、机构编码（必填）、机构类型（必填，枚举 OrgType）、数据隔离模式（必填，枚举 DataIsolationType）、负责人、排序、状态、备注字段。
3. WHEN 管理员在机构列表行点击"新增"按钮，THE OrgManager SHALL 弹出新增表单并预填该行机构 ID 为上级机构。
4. WHEN 管理员点击"编辑"按钮，THE OrgManager SHALL 弹出编辑表单并回填该机构所有字段。
5. WHEN 管理员提交新增或编辑表单且必填字段通过校验，THE OrgManager SHALL 调用后端接口保存数据并刷新列表。
6. IF 管理员提交表单时必填字段为空，THEN THE OrgManager SHALL 在对应字段下方显示校验错误提示，阻止提交。
7. WHEN 管理员确认删除机构，THE OrgManager SHALL 调用后端删除接口并刷新列表。
8. THE OrgManager SHALL 在新增/编辑表单中通过分 Tab 页（基本信息、工商信息、联系与地址）展示机构的完整字段，包括统一社会信用代码、法定代表人、注册资本、成立日期、经营范围、省市区、详细地址。
9. WHEN 管理员在查询面板输入机构名称、机构编码、机构类型、状态条件并点击查询，THE OrgManager SHALL 按条件过滤并重新加载机构树。
10. THE OrgManager SHALL 默认展开所有树节点。

---

### 需求 2：机构管理 — 数据隔离模式配置

**用户故事：** 作为系统管理员，我希望能够为每个机构独立配置数据隔离模式，以便控制不同机构之间的数据可见性边界。

#### 验收标准

1. THE OrgManager SHALL 支持为每个机构配置以下五种 DataIsolationType 之一：共享（0）、完全隔离（1）、汇总共享（2）、同级互通（3）、按模块隔离（4）。
2. THE OrgManager SHALL 在机构列表中以彩色标签（Tag）展示每个机构的数据隔离模式，不同模式使用不同颜色区分（共享:arcoblue、完全隔离:orangered、汇总共享:gold、同级互通:green、按模块隔离:purple）。
3. THE OrgManager SHALL 在编辑表单的数据隔离模式下拉框中显示每种模式的完整说明文字（如"共享 — 上级可查看下级数据"）。
4. WHEN 机构的 DataIsolationType 为 ISOLATED（完全隔离），THE DataScopeAspect SHALL 在执行数据权限过滤时排除该机构的数据，即使查询者拥有"所在机构及下级"权限也不可见该机构数据。
5. WHEN 机构的 DataIsolationType 为 SIBLING_SHARED（同级互通），THE DataScopeAspect SHALL 允许同一父机构下的兄弟机构互相查看数据。

---

### 需求 3：部门管理 — 树形结构维护

**用户故事：** 作为系统管理员，我希望能够维护归属于机构的多级树形部门结构，以便描述企业内部的组织单元。

#### 验收标准

1. THE DeptManager SHALL 以树形结构展示所有部门，每个节点显示部门名称、部门编码、所属机构、部门类型、职能分类、负责人、联系电话、排序、状态、创建时间字段。
2. WHEN 管理员新增或编辑部门，THE DeptManager SHALL 提供所属机构（树形选择）、上级部门（树形选择）、部门名称（必填）、部门编码、部门类型（枚举 DeptType，默认"部门"）、职能分类（枚举，含管理/研发/销售/财务/人事/行政/生产/采购/仓储）、负责人、联系电话、邮箱、排序、状态字段。
3. WHEN 管理员在部门列表行点击"新增"按钮，THE DeptManager SHALL 弹出新增表单并预填该行部门 ID 为上级部门。
4. WHEN 管理员提交新增或编辑表单且必填字段通过校验，THE DeptManager SHALL 调用后端接口保存数据并刷新列表。
5. IF 管理员提交表单时部门名称为空，THEN THE DeptManager SHALL 显示校验错误提示，阻止提交。
6. WHEN 管理员确认删除部门，THE DeptManager SHALL 调用后端删除接口并刷新列表。
7. WHEN 管理员在查询面板输入部门名称、状态条件并点击查询，THE DeptManager SHALL 按条件过滤并重新加载部门树。
8. THE DeptManager SHALL 默认展开所有树节点。

---

### 需求 4：岗位管理 — 岗位信息维护

**用户故事：** 作为系统管理员，我希望能够维护企业岗位信息，以便在用户管理中为用户分配岗位。

#### 验收标准

1. THE PostManager SHALL 以分页列表展示所有岗位，每行显示岗位编码、岗位名称、所属机构、岗位类别、岗位级别、排序、状态、创建时间字段。
2. WHEN 管理员新增或编辑岗位，THE PostManager SHALL 提供岗位编码（必填）、岗位名称（必填）、所属机构（树形选择，可为空表示全局岗位）、岗位类别（枚举，含管理类/技术类/行政类/财务类/销售类/生产类）、岗位级别（如 P1~P10 技术序列、M1~M5 管理序列）、排序、状态、备注字段。
3. WHEN 管理员提交新增或编辑表单且必填字段通过校验，THE PostManager SHALL 调用后端接口保存数据并刷新列表。
4. IF 管理员提交表单时岗位编码或岗位名称为空，THEN THE PostManager SHALL 显示校验错误提示，阻止提交。
5. WHEN 管理员确认删除岗位，THE PostManager SHALL 调用后端删除接口并刷新列表。
6. WHEN 管理员在查询面板输入岗位名称、岗位编码、所属机构、岗位类别、状态条件并点击查询，THE PostManager SHALL 按条件过滤并重新加载列表。
7. THE PostManager SHALL 支持分页，每页默认显示 20 条记录，并提供页码切换和每页条数选择。

---

### 需求 5：角色管理 — 角色与权限维护

**用户故事：** 作为系统管理员，我希望能够维护角色信息并为角色分配菜单权限和数据权限，以便通过角色控制用户的功能访问范围和数据访问范围。

#### 验收标准

1. THE RoleManager SHALL 以列表展示所有角色，每行显示角色名称、角色编码、数据权限（彩色标签）、排序、状态、创建时间、备注字段。
2. WHEN 管理员新增或编辑角色，THE RoleManager SHALL 通过分 Tab 页（基本信息、菜单权限、数据权限）提供角色名称（必填）、角色编码（必填）、排序、状态、备注、菜单权限树（多选）、数据权限范围（枚举 DataScopeType）字段。
3. THE RoleManager SHALL 支持以下七种 DataScopeType：全部数据（1）、所在机构及下级（2）、仅本机构（3）、所在部门及下级（4）、仅本部门（5）、仅本人（6）、自定义（7）。
4. THE RoleManager SHALL 在数据权限 Tab 页中以说明文字描述每种数据权限类型的含义，包括与机构数据隔离模式的交互关系。
5. WHEN 管理员在菜单权限 Tab 页选择菜单，THE RoleManager SHALL 以树形多选控件（tree-checkable）展示菜单树，支持父子节点独立勾选（tree-check-strictly）。
6. WHEN 管理员提交新增或编辑表单且必填字段通过校验，THE RoleManager SHALL 调用后端接口保存角色及其菜单 ID 列表，并刷新列表。
7. IF 管理员提交表单时角色名称或角色编码为空，THEN THE RoleManager SHALL 显示校验错误提示，阻止提交。
8. WHEN 管理员确认删除角色，THE RoleManager SHALL 调用后端删除接口并刷新列表。
9. WHEN 管理员在查询面板输入角色名称、角色编码、状态条件并点击查询，THE RoleManager SHALL 按条件过滤并重新加载列表。
10. THE RoleManager SHALL 在列表中以不同颜色标签展示数据权限类型（全部数据:red、所在机构及下级:purple、仅本机构:arcoblue、所在部门及下级:cyan、仅本部门:green、仅本人:orangered、自定义:gold）。

---

### 需求 6：用户管理 — 用户账号维护

**用户故事：** 作为系统管理员，我希望能够维护用户账号信息，并为用户分配机构、部门、岗位和角色，以便用户能够登录系统并获得相应权限。

#### 验收标准

1. THE UserManager SHALL 以分页列表展示所有用户，每行显示用户名、昵称、工号、所属机构、部门、岗位、手机号、状态、入职日期、创建时间字段。
2. WHEN 管理员新增用户，THE UserManager SHALL 通过分 Tab 页（账号信息、组织与岗位）提供用户名（必填）、昵称（必填）、密码（新增时必填）、手机号、邮箱、性别、状态、角色（多选）、备注、所属机构（树形选择）、所属部门（树形选择）、岗位（下拉选择）、工号、入职日期字段。
3. WHEN 管理员编辑用户，THE UserManager SHALL 回填用户所有字段，且用户名字段不可修改（disabled）。
4. WHEN 管理员提交新增或编辑表单且必填字段通过校验，THE UserManager SHALL 调用后端接口保存数据并刷新列表。
5. IF 管理员提交新增表单时用户名、昵称或密码为空，THEN THE UserManager SHALL 显示校验错误提示，阻止提交。
6. WHEN 管理员确认删除用户，THE UserManager SHALL 调用后端删除接口并刷新列表。
7. WHEN 管理员在查询面板输入用户名、手机号、状态条件并点击查询，THE UserManager SHALL 按条件过滤并重新加载列表。
8. THE UserManager SHALL 支持分页，每页默认显示 20 条记录，并提供页码切换和每页条数选择。
9. THE UserManager SHALL 支持用户状态管理，状态包括正常（1）、停用（0）、锁定（2），在列表中以彩色标签展示（正常:green、停用:red、锁定:orange）。
10. WHEN 管理员为用户分配多个角色，THE UserManager SHALL 将角色 ID 列表（roleIds）随表单一并提交至后端。
11. THE UserManager SHALL 在表单中提供密码重置入口（编辑时显示"重置密码"按钮），WHEN 管理员点击重置密码，THE UserManager SHALL 弹出确认对话框，确认后调用后端重置密码接口。

---

### 需求 7：菜单管理 — 菜单树维护

**用户故事：** 作为系统管理员，我希望能够维护系统菜单树，包括目录、菜单和按钮三种类型，以便控制前端路由和权限标识。

#### 验收标准

1. THE MenuManager SHALL 以树形结构展示所有菜单，每个节点显示菜单名称、图标、排序、类型（彩色标签：目录:blue、菜单:green、按钮:orange）、权限标识、组件路径、状态字段。
2. WHEN 管理员新增或编辑菜单，THE MenuManager SHALL 提供上级菜单（树形选择）、菜单类型（单选：目录/菜单/按钮）、菜单名称（必填）、排序字段，并根据菜单类型动态显示以下字段：
   - 目录（M）：路由地址、菜单图标、显示状态、菜单状态
   - 菜单（C）：路由地址、组件路径、权限标识、菜单图标、显示状态、菜单状态
   - 按钮（F）：权限标识、菜单状态
3. WHEN 管理员在菜单列表行点击"新增"按钮（仅目录和菜单类型显示），THE MenuManager SHALL 弹出新增表单并预填该行菜单 ID 为上级菜单。
4. WHEN 管理员提交新增或编辑表单且必填字段通过校验，THE MenuManager SHALL 调用后端接口保存数据并刷新列表。
5. IF 管理员提交表单时菜单名称为空，THEN THE MenuManager SHALL 显示校验错误提示，阻止提交。
6. WHEN 管理员确认删除菜单，THE MenuManager SHALL 调用后端删除接口并刷新列表。
7. WHEN 管理员在查询面板输入菜单名称、状态条件并点击查询，THE MenuManager SHALL 按条件过滤并重新加载菜单树。
8. THE MenuManager SHALL 默认展开所有树节点。
9. THE MenuManager SHALL 在权限标识字段提供格式提示（如 "system:user:list"），帮助管理员按规范填写。

---

### 需求 8：通用交互规范

**用户故事：** 作为系统管理员，我希望所有系统管理页面具有一致的交互体验，以便降低学习成本并提高操作效率。

#### 验收标准

1. THE System SHALL 在所有管理页面使用 GovernanceCompactQueryPanel（density="ultra" theme="aurora"）作为查询面板，使用 GovernanceListStage（density="ultra" body-fill）作为列表容器。
2. THE System SHALL 在所有新增/编辑弹窗中使用 BmlModal 组件，支持拖拽、缩放、全屏操作。
3. WHEN 后端接口调用成功，THE System SHALL 通过 Arco Design 的 Message 组件显示成功提示（如"新增成功"、"修改成功"、"删除成功"）。
4. WHEN 管理员点击删除按钮，THE System SHALL 通过 a-popconfirm 组件弹出确认提示，防止误操作。
5. THE System SHALL 为每个页面组件设置唯一的 defineOptions({ name: 'SystemXxx' })，确保 keep-alive 缓存正常工作。
6. THE System SHALL 在所有列表页面提供"重置条件"和"查询"两个操作按钮，重置时清空所有查询参数并重新加载数据。
7. THE System SHALL 在所有表格中使用 size="small"、stripe（分页列表）或 default-expand-all-rows（树形列表）、sticky-header、scrollbar 属性，保持视觉一致性。
8. THE System SHALL 在所有状态字段（status）使用彩色标签展示，正常状态使用绿色（green），停用状态使用红色（red）。
9. WHERE 后续业务模块（采购、销售、库存）需要数据权限控制，THE DataScopeAspect SHALL 通过统一的 AOP 切面机制自动应用，无需各模块单独实现。
10. THE System SHALL 在前端 `bml-frontend/src/api/system.ts` 中统一维护所有系统管理模块的 API 接口定义和 TypeScript 类型，供各页面组件复用。
