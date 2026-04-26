/**
 * 系统管理 API 接口层
 * <p>
 * 统一封装前台业务系统的组织与权限模块接口，包括机构、部门、岗位、用户、角色管理。
 * 所有接口均需要 JWT 认证，通过 request 拦截器自动附加 Authorization 头。
 * </p>
 *
 * @module api/system
 */
import request from '../utils/request';

export interface PageQuery {
  pageNum?: number;
  pageSize?: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

/* ═══════════════════════════════════════════════════════════
   类型定义
   ═══════════════════════════════════════════════════════════ */

/** 用户查询参数 */
export interface UserQuery {
  username?: string;
  phone?: string;
  status?: number;
  orgId?: number;
}

/** 用户表单数据（新增/编辑） */
export interface UserForm {
  id?: number;
  username?: string;
  nickname?: string;
  password?: string;
  email?: string;
  phone?: string;
  gender?: number;
  status?: number;
  orgId?: number;
  deptId?: number;
  postId?: number;
  employeeNo?: string;
  entryDate?: string;
  roleIds?: number[];
  remark?: string;
}

/** 用户 VO */
export interface UserVO {
  id: number;
  username: string;
  nickname: string;
  email: string;
  phone: string;
  gender: number;
  avatar: string;
  status: number;
  orgId: number;
  orgName: string;
  deptId: number;
  deptName: string;
  postId: number;
  postName: string;
  employeeNo: string;
  entryDate: string;
  roleIds: number[];
  roleNames: string[];
  createTime: string;
  loginIp: string;
  loginDate: string;
  remark: string;
}

/** 角色查询参数 */
export interface RoleQuery {
  roleName?: string;
  roleCode?: string;
  status?: number;
}

/** 角色表单数据 */
export interface RoleForm {
  id?: number;
  roleName?: string;
  roleCode?: string;
  sort?: number;
  dataScope?: number;
  status?: number;
  menuIds?: number[];
  customOrgIds?: number[];
  remark?: string;
}

/** 角色 VO */
export interface RoleVO {
  id: number;
  roleName: string;
  roleCode: string;
  sort: number;
  dataScope: number;
  status: number;
  remark: string;
  createTime: string;
}

/** 菜单表单数据 */
export interface MenuForm {
  id?: number;
  parentId?: number;
  menuName?: string;
  menuType?: string;
  path?: string;
  component?: string;
  perms?: string;
  icon?: string;
  sort?: number;
  visible?: number;
  status?: number;
  isFrame?: number;
  remark?: string;
}

/** 菜单 VO */
export interface MenuVO {
  id: number;
  parentId: number;
  menuName: string;
  menuType: string;
  path: string;
  component: string;
  perms: string;
  icon: string;
  sort: number;
  visible: number;
  status: number;
  isFrame: number;
  remark: string;
  createTime: string;
  children: MenuVO[];
}

/** 部门表单数据 */
export interface DeptForm {
  id?: number;
  parentId?: number;
  orgId?: number;
  deptName?: string;
  deptCode?: string;
  deptType?: number;
  funcType?: string;
  sort?: number;
  leader?: string;
  phone?: string;
  email?: string;
  status?: number;
}

/** 部门 VO */
export interface DeptVO {
  id: number;
  parentId: number;
  orgId: number;
  orgName: string;
  deptName: string;
  deptCode: string;
  deptType: number;
  funcType: string;
  sort: number;
  leader: string;
  phone: string;
  email: string;
  status: number;
  createTime: string;
  children: DeptVO[];
}

/** 机构查询参数 */
export interface OrgQuery {
  orgName?: string;
  orgCode?: string;
  orgType?: number;
  status?: number;
}

/** 机构表单数据（新增/编辑） */
export interface OrgForm {
  id?: number;
  parentId?: number;
  orgName?: string;
  orgCode?: string;
  orgType?: number;
  creditCode?: string;
  legalPerson?: string;
  registeredCapital?: number;
  establishDate?: string;
  sort?: number;
  leader?: string;
  phone?: string;
  email?: string;
  province?: string;
  city?: string;
  district?: string;
  address?: string;
  businessScope?: string;
  status?: number;
  remark?: string;
  dataIsolation?: number;
}

/** 机构 VO */
export interface OrgVO {
  id: number;
  parentId: number;
  orgName: string;
  orgCode: string;
  orgType: number;
  creditCode: string;
  legalPerson: string;
  registeredCapital: number;
  establishDate: string;
  sort: number;
  leader: string;
  phone: string;
  email: string;
  province: string;
  city: string;
  district: string;
  address: string;
  businessScope: string;
  status: number;
  remark: string;
  dataIsolation: number;
  createTime: string;
  children: OrgVO[];
}

/** 岗位查询参数 */
export interface PostQuery {
  postName?: string;
  postCode?: string;
  orgId?: number;
  postCategory?: string;
  status?: number;
}

/** 岗位表单数据（新增/编辑） */
export interface PostForm {
  id?: number;
  postCode?: string;
  postName?: string;
  orgId?: number;
  postCategory?: string;
  postLevel?: string;
  sort?: number;
  status?: number;
  remark?: string;
}

/** 岗位 VO */
export interface PostVO {
  id: number;
  postCode: string;
  postName: string;
  orgId: number;
  orgName: string;
  postCategory: string;
  postLevel: string;
  sort: number;
  status: number;
  remark: string;
  createTime: string;
}

/* ═══════════════════════════════════════════════════════════
   用户管理 API
   ═══════════════════════════════════════════════════════════ */

/** 获取用户列表 */
export const fetchUserList = (params?: UserQuery) =>
  request.get('/system/user/list', { params });

/** 获取用户详情 */
export const fetchUserDetail = (userId: number) =>
  request.get(`/system/user/${userId}`);

/** 新增用户 */
export const createUser = (data: UserForm) =>
  request.post('/system/user', data);

/** 修改用户 */
export const updateUser = (data: UserForm) =>
  request.put('/system/user', data);

/** 删除用户 */
export const deleteUser = (userId: number) =>
  request.delete(`/system/user/${userId}`);

/** 重置用户密码 */
export const resetUserPassword = (userId: number, newPassword: string) =>
  request.put('/system/user/resetPwd', { userId, password: newPassword });

/* ═══════════════════════════════════════════════════════════
   角色管理 API
   ═══════════════════════════════════════════════════════════ */

/** 获取角色列表 */
export const fetchRoleList = (params?: RoleQuery) =>
  request.get('/system/role/list', { params });

/** 获取角色详情 */
export const fetchRoleDetail = (roleId: number) =>
  request.get(`/system/role/${roleId}`);

/** 新增角色 */
export const createRole = (data: RoleForm) =>
  request.post('/system/role', data);

/** 修改角色 */
export const updateRole = (data: RoleForm) =>
  request.put('/system/role', data);

/** 删除角色 */
export const deleteRole = (roleId: number) =>
  request.delete(`/system/role/${roleId}`);

/* ═══════════════════════════════════════════════════════════
   菜单管理 API
   ═══════════════════════════════════════════════════════════ */

/** 获取菜单列表 */
export const fetchMenuList = (params?: { menuName?: string; status?: number }) =>
  request.get('/system/menu/list', { params });

/** 获取菜单详情 */
export const fetchMenuDetail = (menuId: number) =>
  request.get(`/system/menu/${menuId}`);

/** 新增菜单 */
export const createMenu = (data: MenuForm) =>
  request.post('/system/menu', data);

/** 修改菜单 */
export const updateMenu = (data: MenuForm) =>
  request.put('/system/menu', data);

/** 删除菜单 */
export const deleteMenu = (menuId: number) =>
  request.delete(`/system/menu/${menuId}`);

/* ═══════════════════════════════════════════════════════════
   部门管理 API
   ═══════════════════════════════════════════════════════════ */

/** 获取部门树列表 */
export const fetchDeptList = (params?: { deptName?: string; status?: number; orgId?: number }) =>
  request.get('/system/dept/list', { params });

/** 获取部门详情 */
export const fetchDeptDetail = (deptId: number) =>
  request.get(`/system/dept/${deptId}`);

/** 新增部门 */
export const createDept = (data: DeptForm) =>
  request.post('/system/dept', data);

/** 修改部门 */
export const updateDept = (data: DeptForm) =>
  request.put('/system/dept', data);

/** 删除部门 */
export const deleteDept = (deptId: number) =>
  request.delete(`/system/dept/${deptId}`);

/* ═══════════════════════════════════════════════════════════
   机构管理 API
   ═══════════════════════════════════════════════════════════ */

/** 获取机构树列表 */
export const fetchOrgList = (params?: OrgQuery) =>
  request.get('/system/org/list', { params });

/** 获取机构详情 */
export const fetchOrgDetail = (orgId: number) =>
  request.get(`/system/org/${orgId}`);

/** 新增机构 */
export const createOrg = (data: OrgForm) =>
  request.post('/system/org', data);

/** 修改机构 */
export const updateOrg = (data: OrgForm) =>
  request.put('/system/org', data);

/** 删除机构 */
export const deleteOrg = (orgId: number) =>
  request.delete(`/system/org/${orgId}`);

/* ═══════════════════════════════════════════════════════════
   岗位管理 API
   ═══════════════════════════════════════════════════════════ */

/** 获取岗位列表 */
export const fetchPostList = (params?: PostQuery) =>
  request.get('/system/post/list', { params });

/** 获取岗位详情 */
export const fetchPostDetail = (postId: number) =>
  request.get(`/system/post/${postId}`);

/** 新增岗位 */
export const createPost = (data: PostForm) =>
  request.post('/system/post', data);

/** 修改岗位 */
export const updatePost = (data: PostForm) =>
  request.put('/system/post', data);

/** 删除岗位 */
export const deletePost = (postId: number) =>
  request.delete(`/system/post/${postId}`);

/* ═══════════════════════════════════════════════════════════
   分页查询 API
   ═══════════════════════════════════════════════════════════ */

/** 分页查询岗位 */
export const fetchPostPage = (params?: PostQuery & PageQuery) =>
  request.get<PageResult<PostVO>>('/system/post/list', { params });

/** 分页查询用户 */
export const fetchUserPage = (params?: UserQuery & PageQuery) =>
  request.get<PageResult<UserVO>>('/system/user/list', { params });

/** 分页查询角色 */
export const fetchRolePage = (params?: RoleQuery & PageQuery) =>
  request.get<PageResult<RoleVO>>('/system/role/list', { params });
