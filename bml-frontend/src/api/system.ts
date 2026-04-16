/**
 * 系统管理 API 接口层
 * <p>
 * 统一封装前台业务系统的系统管理模块接口，包括用户、角色、菜单、部门管理。
 * 所有接口均需要 JWT 认证，通过 request 拦截器自动附加 Authorization 头。
 * </p>
 *
 * @module api/system
 */
import request from '../utils/request';

/* ═══════════════════════════════════════════════════════════
   类型定义
   ═══════════════════════════════════════════════════════════ */

/** 用户查询参数 */
export interface UserQuery {
  username?: string;
  phone?: string;
  status?: number;
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
  deptId?: number;
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
  deptId: number;
  deptName: string;
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
  deptName?: string;
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
  deptName: string;
  sort: number;
  leader: string;
  phone: string;
  email: string;
  status: number;
  createTime: string;
  children: DeptVO[];
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
export const fetchDeptList = (params?: { deptName?: string; status?: number }) =>
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
