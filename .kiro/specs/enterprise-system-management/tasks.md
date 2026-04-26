# 实现任务

## 任务列表

- [x] 1. 数据库迁移与后端 DTO/VO 补全
  - [x] 1.1 在 `bml-backend/bml-app/src/main/resources/db/migration/` 新建 `V1.9.4__add_role_custom_org_scope.sql`，创建 `sys_role_org` 表（字段：role_id BIGINT NOT NULL、org_id BIGINT NOT NULL、联合主键 PRIMARY KEY(role_id, org_id)、索引 KEY idx_org_id(org_id)），并添加表注释"角色自定义数据权限 — 角色机构关联表"
  - [x] 1.2 在 `bml-backend/bml-modules/bml-module-system/src/main/java/com/bml/module/system/dto/SysRoleDTO.java` 中新增 `@Schema(description = "自定义数据权限时的机构 ID 列表") private List<Long> customOrgIds;` 字段
  - [x] 1.3 在 `bml-backend/bml-modules/bml-module-system/src/main/java/com/bml/module/system/dto/SysUserDTO.java` 中新增 `@Schema(description = "角色 ID 列表") private List<Long> roleIds;` 字段
  - [x] 1.4 在 `bml-backend/bml-modules/bml-module-system/src/main/java/com/bml/module/system/vo/SysUserVO.java` 中新增 `roleIds: List<Long>`、`roleNames: List<String>`、`orgName: String`、`deptName: String`、`postName: String` 字段，并在 `SysUserService.selectUserList()` 查询结果中通过二次查询或 JOIN 填充这些关联名称字段

- [x] 2. 后端 SysOrgService 补全
  - [x] 2.1 在 `SysOrgService` 接口中声明 `boolean checkOrgCodeUnique(SysOrgDTO dto)` 方法；在 `SysOrgServiceImpl` 中实现：使用 `LambdaQueryWrapper` 查询 `orgCode` 相同且 `deleted=0` 的记录，若 `dto.getId()` 不为空则排除自身，存在则返回 false（不唯一）
  - [x] 2.2 在 `SysOrgService` 接口中声明 `boolean checkOrgHasChild(Long orgId)` 方法；在 `SysOrgServiceImpl` 中实现：查询 `parent_id = orgId` 且 `deleted=0` 的记录数，大于 0 则返回 true
  - [x] 2.3 在 `SysOrgController.add()` 和 `SysOrgController.edit()` 中调用 `checkOrgCodeUnique()`，若不唯一则返回 `Result.fail("机构编码已存在")`；在 `SysOrgController.remove()` 中调用 `checkOrgHasChild()`，若有子机构则返回 `Result.fail("存在子机构，不允许删除")`

- [x] 3. 后端 SysRoleService 补全
  - [x] 3.1 在 `SysRoleService` 接口中声明 `List<Long> selectMenuIdsByRoleId(Long roleId)` 方法；在 `SysRoleServiceImpl` 中实现：通过 `SysRoleMenuMapper` 查询 `role_id = roleId` 的所有 `menu_id` 并返回列表
  - [x] 3.2 在 `SysRoleServiceImpl` 中实现带 `@Transactional(rollbackFor = Exception.class)` 的 `insertRole(SysRoleDTO dto)` 方法：先调用 `save(role)` 保存角色，再调用 `SysRoleMenuMapper.batchInsert(roleId, menuIds)` 批量插入菜单关联，若 `dto.getCustomOrgIds()` 不为空则调用 `saveRoleOrgs(roleId, customOrgIds)` 维护机构关联
  - [x] 3.3 在 `SysRoleServiceImpl` 中实现带 `@Transactional(rollbackFor = Exception.class)` 的 `updateRole(SysRoleDTO dto)` 方法：先调用 `updateById(role)` 更新角色，再删除旧菜单关联（`deleteRoleMenusByRoleId(roleId)`），再批量插入新菜单关联，再删除旧机构关联并重新插入
  - [x] 3.4 在 `SysRoleServiceImpl` 中实现 `saveRoleOrgs(Long roleId, List<Long> orgIds)` 方法：先删除 `sys_role_org` 中 `role_id = roleId` 的所有记录，再批量插入新的 `(roleId, orgId)` 记录
  - [x] 3.5 在 `SysRoleController.getInfo()` 中，将 `selectMenuIdsByRoleId(roleId)` 的结果设置到返回的 `SysRoleVO.menuIds` 字段中

- [x] 4. 后端 SysUserService 补全
  - [x] 4.1 在 `SysUserServiceImpl` 中实现带 `@Transactional(rollbackFor = Exception.class)` 的 `insertUser(SysUserDTO dto)` 方法：先调用 `save(user)` 保存用户（密码使用 `BCryptPasswordEncoder.encode()` 加密），再通过 `SysUserRoleMapper.batchInsert(userId, roleIds)` 批量插入用户角色关联
  - [x] 4.2 在 `SysUserServiceImpl` 中实现带 `@Transactional(rollbackFor = Exception.class)` 的 `updateUser(SysUserDTO dto)` 方法：先调用 `updateById(user)` 更新用户基本信息，再删除旧角色关联（`SysUserRoleMapper.deleteByUserId(userId)`），再批量插入新角色关联
  - [x] 4.3 在 `SysUserServiceImpl` 中实现 `resetUserPassword(Long userId, String newPassword)` 方法：使用 `BCryptPasswordEncoder.encode(newPassword)` 加密后，通过 `LambdaUpdateWrapper` 更新 `sys_user.password` 字段
  - [x] 4.4 在 `SysUserController` 中新增 `@PutMapping("/resetPwd")` 接口，接收 `@RequestBody Map<String, Object> body`（含 userId 和 password），调用 `resetUserPassword()` 并返回 `Result.ok()`，权限标识为 `system:user:resetPwd`

- [x] 5. 后端 SysPostService 分页支持
  - [x] 5.1 在 `SysPostService` 接口中声明 `PageResult<SysPostVO> selectPostPage(SysPostDTO dto, int pageNum, int pageSize)` 方法；在 `SysPostServiceImpl` 中实现：构建 `Page<SysPost>` 对象，使用 `LambdaQueryWrapper` 按 `postName/postCode/orgId/postCategory/status` 条件过滤，调用 `page()` 方法，将结果转换为 `PageResult<SysPostVO>` 返回
  - [x] 5.2 修改 `SysPostController.list()` 方法签名，增加 `@RequestParam(defaultValue = "1") int pageNum` 和 `@RequestParam(defaultValue = "20") int pageSize` 参数，调用 `selectPostPage()` 返回 `Result<PageResult<SysPostVO>>`

- [x] 6. 前端 API 类型层补全（`bml-frontend/src/api/system.ts`）
  - [x] 6.1 在文件顶部新增 `PageQuery` 接口（`pageNum?: number; pageSize?: number`）和 `PageResult<T>` 泛型接口（`records: T[]; total: number; current: number; size: number`）
  - [x] 6.2 在 `RoleForm` 接口中新增 `customOrgIds?: number[]` 字段；在 `UserQuery` 接口中新增 `orgId?: number` 字段
  - [x] 6.3 新增 `resetUserPassword(userId: number, newPassword: string)` 函数：`request.put('/system/user/resetPwd', { userId, password: newPassword })`
  - [x] 6.4 新增 `fetchPostPage`、`fetchUserPage`、`fetchRolePage` 三个分页请求函数，分别接收对应的 Query 类型与 `PageQuery` 的交叉类型参数，调用对应的 `/list` 接口

- [x] 7. 机构管理前端增强（`bml-frontend/src/views/business/system/org/index.vue`）
  - [x] 7.1 在 `<script>` 中定义 `ISOLATION_OPTIONS` 常量数组（5 项，每项含 value/label/desc/color）和 `ORG_TYPE_OPTIONS` 常量数组（6 项，每项含 value/label/color），替换模板中所有硬编码的机构类型和隔离模式选项
  - [x] 7.2 将 BmlModal 内的表单改为 `<a-tabs>` 三 Tab 页结构：「基本信息」Tab 包含上级机构 tree-select、机构名称（必填）、机构编码（必填）、机构类型（必填，使用 `ORG_TYPE_OPTIONS`）、数据隔离模式（必填，使用自定义选项模板同时显示 label 和 desc）、负责人、排序、状态、备注；「工商信息」Tab 包含统一社会信用代码、法定代表人、注册资本 input-number（单位：万元）、成立日期 date-picker、经营范围 textarea；「联系与地址」Tab 包含联系电话、邮箱、省份、城市、区县、详细地址
  - [x] 7.3 将 BmlModal 尺寸属性更新为 `width=820, height=640, min-width=640, min-height=480`
  - [x] 7.4 在 `handleDelete(id)` 中，调用后端删除接口前先检查响应，若后端返回"存在子机构"错误则通过 `Message.warning()` 提示用户，不执行删除

- [x] 8. 部门管理前端增强（`bml-frontend/src/views/business/system/dept/index.vue`）
  - [x] 8.1 在查询面板的 `<a-form>` 中新增「所属机构」`a-tree-select` 字段（绑定 `queryParams.orgId`，数据来源 `orgTreeData`，field-names 使用 `{ key: 'id', title: 'orgName', children: 'children' }`），`onMounted` 时调用 `fetchOrgList()` 加载机构树数据
  - [x] 8.2 在表单的「所属机构」`a-tree-select` 上绑定 `@change="handleOrgChange"` 事件；实现 `handleOrgChange(orgId)` 函数：调用 `fetchDeptList({ orgId })` 刷新 `deptTreeOptions`，并将 `formData.parentId` 重置为 `undefined`

- [x] 9. 岗位管理前端增强（`bml-frontend/src/views/business/system/post/index.vue`）
  - [x] 9.1 在查询面板新增「所属机构」`a-tree-select` 字段（绑定 `queryParams.orgId`），`onMounted` 时调用 `fetchOrgList()` 加载机构树；`handleSearch()` 时将 `queryParams.orgId` 传入 `fetchPostPage()` 参数
  - [x] 9.2 在 `<script>` 中新增 `pagination` 响应式对象（`{ current: 1, pageSize: 20, total: 0 }`）；将 `loadData()` 改为调用 `fetchPostPage({ ...queryParams, pageNum: pagination.current, pageSize: pagination.pageSize })`，从响应的 `data.total` 更新 `pagination.total`，从 `data.records` 更新 `tableData`；在 `GovernanceListStage` 的 `body` slot 末尾添加 `<a-pagination>` 组件，绑定 `pagination` 属性，`@change` 时更新 `pagination.current` 并重新调用 `loadData()`

- [x] 10. 角色管理前端增强（`bml-frontend/src/views/business/system/role/index.vue`）
  - [x] 10.1 新增 `pagination` 响应式对象；将 `loadData()` 改为调用 `fetchRolePage()`，更新 `pagination.total` 和 `tableData`；在列表底部添加 `<a-pagination>` 组件
  - [x] 10.2 在 `handleEdit(record)` 中，调用 `fetchRoleDetail(record.id)` 获取详情，将响应数据的 `menuIds` 赋值给 `formData.menuIds`，将 `customOrgIds` 赋值给 `formData.customOrgIds`
  - [x] 10.3 在「数据权限」Tab 中，`formData.dataScope` 的 `<a-select>` 下方添加 `v-if="formData.dataScope === 7"` 的「自定义机构范围」`a-tree-select`（multiple、tree-checkable，绑定 `formData.customOrgIds`，数据来源 `orgTreeData`）；`onMounted` 时调用 `fetchOrgList()` 加载机构树数据到 `orgTreeData`
  - [x] 10.4 在 `handleSubmit()` 中，确保 `formData.customOrgIds` 随其他字段一并传入 `createRole()` 或 `updateRole()` 的请求体

- [x] 11. 用户管理前端增强（`bml-frontend/src/views/business/system/user/index.vue`）
  - [x] 11.1 在查询面板新增「所属机构」`a-tree-select` 字段（绑定 `queryParams.orgId`），`onMounted` 时调用 `fetchOrgList()` 加载机构树
  - [x] 11.2 新增 `pagination` 响应式对象；将 `loadData()` 改为调用 `fetchUserPage()`，更新 `pagination.total` 和 `tableData`；在列表底部添加 `<a-pagination>` 组件
  - [x] 11.3 在 `<script>` 中定义 `USER_STATUS_MAP` 常量（key 为 0/1/2，value 为 `{ label, color }`）；将状态列的模板改为 `<a-tag :color="USER_STATUS_MAP[record.status]?.color">{{ USER_STATUS_MAP[record.status]?.label }}</a-tag>`；查询面板状态下拉框增加 `<a-option :value="2">锁定</a-option>`
  - [x] 11.4 在 `handleEdit(record)` 中，调用 `fetchUserDetail(record.id)` 获取详情，将响应数据的 `roleIds` 赋值给 `formData.roleIds`
  - [x] 11.5 在 BmlModal 的 `<template #footer>` 中，`formData.id` 存在时渲染「重置密码」`<a-button status="warning">`；点击后弹出 `<a-modal>` 确认对话框（含新密码 `<a-input-password>` 输入框），确认后调用 `resetUserPassword(formData.id, newPassword)`，成功后调用 `Message.success('密码重置成功')`

- [x] 12. 菜单管理前端增强（`bml-frontend/src/views/business/system/menu/index.vue`）
  - [x] 12.1 将权限标识（perms）的 `<a-form-item>` 增加 `extra="格式：模块:资源:操作，如 system:user:list"` 属性
  - [x] 12.2 在表单中增加「是否外链」字段：`<a-form-item v-if="formData.menuType !== 'F'" field="isFrame" label="是否外链"><a-radio-group v-model="formData.isFrame"><a-radio :value="0">内嵌框架</a-radio><a-radio :value="1">外部链接</a-radio></a-radio-group></a-form-item>`；在 `defaultForm()` 函数中将 `isFrame` 默认值设为 `0`
