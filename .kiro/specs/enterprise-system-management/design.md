# 设计文档

## 概述

本文档描述企业管理系统（BML）系统管理模块的技术设计方案。该模块为整个企业管理平台的基础支撑层，提供机构、部门、岗位、角色、用户、菜单六个子模块的完整 CRUD 能力，并通过后端 AOP 数据权限切面为后续采购、销售、库存等业务模块提供统一的数据隔离保障。

**现状分析：** 六个子模块的前端视图（`bml-frontend/src/views/business/system/`）和后端控制器均已存在骨架代码，但部分功能尚未完整实现（如机构表单的工商信息 Tab、角色的数据权限说明、用户的密码重置、分页等）。本次设计以**补全和增强**为主，不推倒重来。

---

## 架构

### 整体分层

```
前端（Vue 3 + TypeScript + Arco Design）
├── bml-frontend/src/api/system.ts          ← 统一 API 类型 + 请求函数
├── bml-frontend/src/views/business/system/
│   ├── org/index.vue                       ← 机构管理（树形 + 工商信息 Tab）
│   ├── dept/index.vue                      ← 部门管理（树形 + 机构关联）
│   ├── post/index.vue                      ← 岗位管理（分页列表）
│   ├── role/index.vue                      ← 角色管理（菜单权限 + 数据权限）
│   ├── user/index.vue                      ← 用户管理（分页 + 密码重置）
│   └── menu/index.vue                      ← 菜单管理（树形 + 动态字段）
└── bml-frontend/src/router/index.ts        ← 静态路由（已配置，无需修改）

后端（Spring Boot 3.3.6 + MyBatis-Plus 3.5.9）
├── bml-module-system/controller/           ← REST 控制器（已存在，需补全接口）
├── bml-module-system/service/              ← 业务逻辑（已存在，需补全方法）
├── bml-module-system/entity/               ← 实体（已存在，字段完整）
├── bml-module-system/dto/                  ← 请求 DTO（已存在，需补全字段）
├── bml-module-system/vo/                   ← 响应 VO（已存在，需补全字段）
├── bml-module-system/mapper/               ← MyBatis-Plus Mapper（已存在）
└── bml-app/src/main/resources/db/migration/ ← Flyway 迁移脚本
```

### 数据权限架构

```
用户请求
  └─ Spring Security 认证（JWT）
       └─ DataScopeAspect（AOP 切面）
            ├─ 读取当前用户角色的 dataScope 字段
            ├─ 读取用户所在机构的 dataIsolation 字段
            └─ 动态注入 SQL WHERE 条件到 DataScopeContext（ThreadLocal）
                 └─ Mapper 查询时自动应用数据范围过滤
```

---

## 组件与接口设计

### 1. 前端通用规范

所有系统管理页面遵循统一的三层布局：

```
<div class="page-wrapper">
  <!-- 层 1：查询面板 -->
  <GovernanceCompactQueryPanel density="ultra" theme="aurora">
    <template #footerActions>重置 + 查询按钮</template>
    <a-form layout="inline">查询字段</a-form>
  </GovernanceCompactQueryPanel>

  <!-- 层 2：列表舞台 -->
  <GovernanceListStage density="ultra" body-fill>
    <template #actions>新增按钮</template>
    <a-table ...>表格列</a-table>
  </GovernanceListStage>

  <!-- 层 3：新增/编辑弹窗 -->
  <BmlModal v-model:visible="dialogVisible" ...>
    <a-form layout="vertical">
      <a-tabs>分 Tab 页的表单字段</a-tabs>
    </a-form>
    <template #footer>取消 + 确定按钮</template>
  </BmlModal>
</div>
```

**页面级 CSS 规范：**
```css
.page-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  overflow: hidden;
  box-sizing: border-box;
}
```

---

### 2. 机构管理（OrgManager）

#### 2.1 前端视图：`org/index.vue`

**现状：** 已有树形表格、查询面板、新增/编辑弹窗骨架，但表单缺少工商信息 Tab 和联系与地址 Tab。

**需要补全：**

表单分三个 Tab 页：

| Tab | 字段 |
|-----|------|
| 基本信息 | 上级机构（tree-select）、机构名称*、机构编码*、机构类型*、数据隔离模式*、负责人、排序、状态、备注 |
| 工商信息 | 统一社会信用代码（18位）、法定代表人、注册资本（万元）、成立日期、经营范围（textarea） |
| 联系与地址 | 联系电话、邮箱、省份、城市、区县、详细地址 |

**数据隔离模式选项（带说明文字）：**

```typescript
const ISOLATION_OPTIONS = [
  { value: 0, label: '共享', desc: '上级机构可查看所有下级机构数据', color: 'arcoblue' },
  { value: 1, label: '完全隔离', desc: '各机构数据完全独立，上下级均不可互查', color: 'orangered' },
  { value: 2, label: '汇总共享', desc: '上级仅可查看下级的汇总统计，不可查看明细', color: 'gold' },
  { value: 3, label: '同级互通', desc: '同一父机构下的兄弟机构可互查数据', color: 'green' },
  { value: 4, label: '按模块隔离', desc: '部分业务模块隔离，部分共享（需配合模块配置）', color: 'purple' },
];
```

**机构类型选项：**

```typescript
const ORG_TYPE_OPTIONS = [
  { value: 1, label: '集团', color: 'red' },
  { value: 2, label: '公司', color: 'arcoblue' },
  { value: 3, label: '分公司', color: 'cyan' },
  { value: 4, label: '子公司', color: 'purple' },
  { value: 5, label: '办事处', color: 'green' },
  { value: 6, label: '事业部', color: 'gold' },
];
```

**BmlModal 尺寸：** `width=820, height=640, min-width=640, min-height=480`

#### 2.2 后端接口

| 方法 | 路径 | 权限标识 | 说明 |
|------|------|----------|------|
| GET | `/system/org/list` | `system:org:list` | 获取机构树（支持 orgName/orgCode/orgType/status 过滤） |
| GET | `/system/org/{id}` | `system:org:query` | 获取机构详情 |
| POST | `/system/org` | `system:org:add` | 新增机构 |
| PUT | `/system/org` | `system:org:edit` | 修改机构 |
| DELETE | `/system/org/{id}` | `system:org:remove` | 删除机构 |

**SysOrgDTO 完整字段（已存在，确认包含）：**
`id, parentId, orgName*, orgCode, orgType, creditCode, legalPerson, registeredCapital, establishDate, sort, leader, phone, email, province, city, district, address, businessScope, status, remark, dataIsolation`

**SysOrgVO 完整字段（已存在，确认包含）：**
以上所有字段 + `createTime, children: List<SysOrgVO>`

**SysOrgService 需补全方法：**
- `selectOrgList(SysOrgDTO dto)` — 按条件查询机构列表（已存在）
- `buildOrgTree(List<SysOrg> orgs)` — 构建树形结构（已存在）
- `checkOrgCodeUnique(SysOrgDTO dto)` — 校验机构编码唯一性（需补全）
- `checkOrgHasChild(Long orgId)` — 校验是否有子机构（删除前校验，需补全）

---

### 3. 部门管理（DeptManager）

#### 3.1 前端视图：`dept/index.vue`

**现状：** 已有完整实现，包含树形表格、查询面板、新增/编辑弹窗（含所属机构、上级部门、部门类型、职能分类等字段）。

**需要补全：**
- 查询面板增加"所属机构"筛选条件（tree-select）
- 表单中"所属机构"变更时，联动刷新"上级部门"的树形选项（仅显示该机构下的部门）

**职能分类常量：**
```typescript
const FUNC_TYPES = ['管理', '研发', '销售', '财务', '人事', '行政', '生产', '采购', '仓储'];
```

**部门类型颜色映射：**
```typescript
const DEPT_TYPE_COLOR: Record<number, string> = { 1: 'purple', 2: 'arcoblue', 3: 'green', 4: 'cyan' };
const DEPT_TYPE_LABEL: Record<number, string> = { 1: '事业部', 2: '中心', 3: '部门', 4: '小组' };
```

#### 3.2 后端接口

| 方法 | 路径 | 权限标识 | 说明 |
|------|------|----------|------|
| GET | `/system/dept/list` | `system:dept:list` | 获取部门树（支持 deptName/orgId/status 过滤） |
| GET | `/system/dept/{id}` | `system:dept:query` | 获取部门详情 |
| POST | `/system/dept` | `system:dept:add` | 新增部门 |
| PUT | `/system/dept` | `system:dept:edit` | 修改部门 |
| DELETE | `/system/dept/{id}` | `system:dept:remove` | 删除部门 |

**SysDeptDTO 需补全字段：** `orgId`（所属机构 ID，用于联动过滤）

---

### 4. 岗位管理（PostManager）

#### 4.1 前端视图：`post/index.vue`

**现状：** 已有完整实现，包含分页列表、查询面板（岗位名称/编码/类别/状态）、新增/编辑弹窗。

**需要补全：**
- 列表增加分页组件（`a-pagination`），当前无分页
- 查询面板增加"所属机构"筛选条件

**分页状态：**
```typescript
const pagination = reactive({ current: 1, pageSize: 20, total: 0 });
```

#### 4.2 后端接口

| 方法 | 路径 | 权限标识 | 说明 |
|------|------|----------|------|
| GET | `/system/post/list` | `system:post:list` | 获取岗位列表（分页，支持 postName/postCode/orgId/postCategory/status 过滤） |
| GET | `/system/post/{id}` | `system:post:query` | 获取岗位详情 |
| POST | `/system/post` | `system:post:add` | 新增岗位 |
| PUT | `/system/post` | `system:post:edit` | 修改岗位 |
| DELETE | `/system/post/{id}` | `system:post:remove` | 删除岗位 |

**后端分页支持：** `SysPostController.list()` 需接收 `PageQuery`（pageNum, pageSize）并返回 `PageResult<SysPostVO>`。

---

### 5. 角色管理（RoleManager）

#### 5.1 前端视图：`role/index.vue`

**现状：** 已有完整实现，包含列表、查询面板、三 Tab 弹窗（基本信息/菜单权限/数据权限）。

**需要补全：**
- 数据权限 Tab 中，当选择"自定义（7）"时，显示机构/部门多选树，允许管理员手动指定可访问范围
- 列表增加分页组件

**自定义数据权限 UI：**
```vue
<!-- 仅当 formData.dataScope === 7 时显示 -->
<a-form-item v-if="formData.dataScope === 7" label="自定义机构范围">
  <a-tree-select
    v-model="formData.customOrgIds"
    :data="orgTreeData"
    :field-names="{ key: 'id', title: 'orgName', children: 'children' }"
    multiple
    tree-checkable
    placeholder="请选择可访问的机构范围"
  />
</a-form-item>
```

**数据权限颜色映射（已存在，确认）：**
```typescript
const DS_COLOR: Record<number, string> = {
  1: 'red', 2: 'purple', 3: 'arcoblue', 4: 'cyan', 5: 'green', 6: 'orangered', 7: 'gold'
};
```

#### 5.2 后端接口

| 方法 | 路径 | 权限标识 | 说明 |
|------|------|----------|------|
| GET | `/system/role/list` | `system:role:list` | 获取角色列表（分页，支持 roleName/roleCode/status 过滤） |
| GET | `/system/role/{id}` | `system:role:query` | 获取角色详情（含 menuIds） |
| POST | `/system/role` | `system:role:add` | 新增角色（含 menuIds 批量保存） |
| PUT | `/system/role` | `system:role:edit` | 修改角色（含 menuIds 批量更新） |
| DELETE | `/system/role/{id}` | `system:role:remove` | 删除角色 |

**SysRoleDTO 需补全字段：** `customOrgIds: List<Long>`（自定义数据权限时的机构 ID 列表）

**SysRoleService 需补全方法：**
- `insertRoleMenus(Long roleId, List<Long> menuIds)` — 批量保存角色菜单关联（已存在）
- `deleteRoleMenusByRoleId(Long roleId)` — 删除角色菜单关联（已存在）
- `selectMenuIdsByRoleId(Long roleId)` — 查询角色已分配的菜单 ID 列表（需补全，用于编辑回填）

---

### 6. 用户管理（UserManager）

#### 6.1 前端视图：`user/index.vue`

**现状：** 已有完整实现，包含列表、查询面板、两 Tab 弹窗（账号信息/组织与岗位）。

**需要补全：**

1. **分页组件：** 列表底部增加 `a-pagination`
2. **用户状态：** 增加"锁定（2）"状态的展示（橙色标签）
3. **密码重置：** 编辑弹窗底部增加"重置密码"按钮，点击弹出确认对话框
4. **查询面板：** 增加"所属机构"筛选条件

**用户状态标签：**
```typescript
const USER_STATUS_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '正常', color: 'green' },
  0: { label: '停用', color: 'red' },
  2: { label: '锁定', color: 'orange' },
};
```

**密码重置弹窗：**
```vue
<template #footer>
  <a-button v-if="formData.id" status="warning" @click="handleResetPassword">重置密码</a-button>
  <a-button @click="dialogVisible = false">取消</a-button>
  <a-button type="primary" :loading="submitting" @click="handleSubmit">确定</a-button>
</template>
```

#### 6.2 后端接口

| 方法 | 路径 | 权限标识 | 说明 |
|------|------|----------|------|
| GET | `/system/user/list` | `system:user:list` | 获取用户列表（分页，支持 username/phone/orgId/status 过滤） |
| GET | `/system/user/{id}` | `system:user:query` | 获取用户详情（含 roleIds） |
| POST | `/system/user` | `system:user:add` | 新增用户（含 roleIds 批量保存） |
| PUT | `/system/user` | `system:user:edit` | 修改用户（含 roleIds 批量更新） |
| DELETE | `/system/user/{id}` | `system:user:remove` | 删除用户 |
| PUT | `/system/user/resetPwd` | `system:user:resetPwd` | 重置用户密码 |

**SysUserDTO 需补全字段：** `roleIds: List<Long>`（角色 ID 列表）

**SysUserVO 需补全字段：** `roleIds: List<Long>, roleNames: List<String>, orgName: String, deptName: String, postName: String`

**SysUserService 需补全方法：**
- `insertUserRoles(Long userId, List<Long> roleIds)` — 批量保存用户角色关联（需补全）
- `deleteUserRolesByUserId(Long userId)` — 删除用户角色关联（需补全）
- `resetUserPassword(Long userId, String newPassword)` — 重置密码（需补全）

---

### 7. 菜单管理（MenuManager）

#### 7.1 前端视图：`menu/index.vue`

**现状：** 已有完整实现，包含树形表格、查询面板、新增/编辑弹窗（含动态字段显示逻辑）。

**需要补全：**
- 表单中权限标识字段增加格式提示文字（`extra` 属性）
- 表单中增加"是否外链（isFrame）"字段（目录和菜单类型显示）

**权限标识格式提示：**
```vue
<a-form-item field="perms" label="权限标识" extra="格式：模块:资源:操作，如 system:user:list">
  <a-input v-model="formData.perms" placeholder="如：system:user:list" />
</a-form-item>
```

#### 7.2 后端接口

| 方法 | 路径 | 权限标识 | 说明 |
|------|------|----------|------|
| GET | `/system/menu/list` | `system:menu:list` | 获取菜单树（支持 menuName/status 过滤） |
| GET | `/system/menu/{id}` | `system:menu:query` | 获取菜单详情 |
| POST | `/system/menu` | `system:menu:add` | 新增菜单 |
| PUT | `/system/menu` | `system:menu:edit` | 修改菜单 |
| DELETE | `/system/menu/{id}` | `system:menu:remove` | 删除菜单 |

---

### 8. API 类型层（`system.ts`）

**现状：** 已有完整的类型定义和请求函数，覆盖所有六个模块。

**需要补全：**

```typescript
// 补全 RoleForm 中的自定义数据权限字段
export interface RoleForm {
  // ... 已有字段 ...
  customOrgIds?: number[];  // 自定义数据权限时的机构 ID 列表
}

// 补全 UserQuery 中的机构筛选
export interface UserQuery {
  // ... 已有字段 ...
  orgId?: number;
}

// 新增密码重置接口
export const resetUserPassword = (userId: number, newPassword: string) =>
  request.put('/system/user/resetPwd', { userId, password: newPassword });

// 补全角色详情接口（需返回 menuIds）
// fetchRoleDetail 已存在，确认后端返回 menuIds 字段

// 新增分页参数类型
export interface PageQuery {
  pageNum?: number;
  pageSize?: number;
}

// 补全岗位列表为分页接口
export const fetchPostPage = (params?: PostQuery & PageQuery) =>
  request.get('/system/post/list', { params });

// 补全用户列表为分页接口
export const fetchUserPage = (params?: UserQuery & PageQuery) =>
  request.get('/system/user/list', { params });

// 补全角色列表为分页接口
export const fetchRolePage = (params?: RoleQuery & PageQuery) =>
  request.get('/system/role/list', { params });
```

---

### 9. 数据库迁移

**现状：** `V1.9.0` 已完成 sys_org/sys_dept/sys_post 的字段增强，`V1.9.1` 已增强数据隔离模式。

**需要新增 `V1.9.4__add_role_custom_org_scope.sql`：**

```sql
-- 角色自定义数据权限：角色与机构的多对多关联表
-- 当 sys_role.data_scope = 7（自定义）时，通过此表指定可访问的机构范围
CREATE TABLE IF NOT EXISTS sys_role_org (
    role_id   BIGINT NOT NULL COMMENT '角色 ID',
    org_id    BIGINT NOT NULL COMMENT '机构 ID',
    PRIMARY KEY (role_id, org_id),
    KEY idx_org_id (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='角色自定义数据权限 — 角色机构关联表';
```

---

### 10. 后端服务层补全设计

#### 10.1 SysUserService 补全

```java
/**
 * 新增用户（含角色关联）
 * 事务内：先保存用户，再批量插入 sys_user_role 关联记录
 */
@Transactional(rollbackFor = Exception.class)
public void insertUser(SysUserDTO dto);

/**
 * 修改用户（含角色关联更新）
 * 事务内：先更新用户基本信息，再删除旧角色关联，再批量插入新角色关联
 */
@Transactional(rollbackFor = Exception.class)
public void updateUser(SysUserDTO dto);

/**
 * 重置用户密码
 * 使用 BCryptPasswordEncoder 对新密码加密后更新
 */
public void resetUserPassword(Long userId, String newPassword);
```

#### 10.2 SysRoleService 补全

```java
/**
 * 新增角色（含菜单关联）
 * 事务内：先保存角色，再批量插入 sys_role_menu 关联记录
 */
@Transactional(rollbackFor = Exception.class)
public void insertRole(SysRoleDTO dto);

/**
 * 修改角色（含菜单关联更新）
 * 事务内：先更新角色基本信息，再删除旧菜单关联，再批量插入新菜单关联
 */
@Transactional(rollbackFor = Exception.class)
public void updateRole(SysRoleDTO dto);

/**
 * 查询角色已分配的菜单 ID 列表（用于编辑回填）
 */
public List<Long> selectMenuIdsByRoleId(Long roleId);
```

#### 10.3 SysPostService 分页支持

```java
/**
 * 分页查询岗位列表
 * 使用 MyBatis-Plus Page 对象实现分页，返回 PageResult<SysPostVO>
 */
public PageResult<SysPostVO> selectPostPage(SysPostDTO dto, int pageNum, int pageSize);
```

---

### 11. 正确性属性（Correctness Properties）

以下属性描述系统必须满足的不变量，可通过属性测试验证：

**P1 — 机构树完整性：** 对于任意机构节点 N，其 `ancestors` 字段必须包含从根节点到 N 的父节点的完整 ID 链，且链中每个 ID 对应的机构必须存在于数据库中。

**P2 — 数据隔离一致性：** 当机构 A 的 `dataIsolation = 1`（完全隔离）时，对于任意拥有"所在机构及下级（dataScope=2）"权限的用户 U（U 不属于机构 A），`DataScopeAspect` 生成的 SQL 条件中不得包含机构 A 的 ID。

**P3 — 角色菜单关联完整性：** 对于任意角色 R，`sys_role_menu` 表中 R 的菜单 ID 集合必须是 `sys_menu` 表中所有有效菜单 ID 的子集（不存在悬空引用）。

**P4 — 用户角色关联完整性：** 对于任意用户 U，`sys_user_role` 表中 U 的角色 ID 集合必须是 `sys_role` 表中所有有效角色 ID 的子集（不存在悬空引用）。

**P5 — 密码安全性：** 数据库 `sys_user.password` 字段中存储的值必须是 BCrypt 哈希值（以 `$2a$` 开头），不得存储明文密码。

**P6 — 机构编码唯一性：** 对于任意两个未被逻辑删除的机构 A 和 B，若 A ≠ B，则 A.orgCode ≠ B.orgCode。

**P7 — 岗位编码唯一性：** 对于任意两个未被逻辑删除的岗位 A 和 B，若 A ≠ B，则 A.postCode ≠ B.postCode。

---

## 实现计划

### 任务分组

实现分为以下 7 个任务组，每组对应一个子模块，可独立开发和测试：

1. **机构管理增强** — 补全工商信息 Tab、联系与地址 Tab、机构编码唯一性校验
2. **部门管理增强** — 补全机构筛选联动、部门树按机构过滤
3. **岗位管理增强** — 补全分页、机构筛选
4. **角色管理增强** — 补全自定义数据权限机构选择、菜单 ID 回填、分页
5. **用户管理增强** — 补全分页、锁定状态、密码重置、机构筛选
6. **菜单管理增强** — 补全权限标识格式提示、isFrame 字段
7. **后端服务补全** — 补全 UserService/RoleService 事务方法、PostService 分页、数据库迁移脚本
