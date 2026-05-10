/**
 * 系统管理 API 接口层
 * <p>
 * 统一封装前台业务系统的组织与权限模块接口，包括机构、部门、岗位、用户、角色管理。
 * 所有接口均需要 JWT 认证，通过 request 拦截器自动附加 Authorization 头。
 * </p>
 *
 * @module api/system
 */
import axios from 'axios';
import request, { apiBaseURL } from '../utils/request';
import { getAccessToken } from '../utils/auth';

export interface PageQuery {
  pageNum?: number;
  pageSize?: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
  pageNum?: number;
  pageSize?: number;
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
  superiorId?: number;
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
  halfCheckMenuIds?: number[];
  customOrgIds?: number[];
  customDeptIds?: number[];
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
  menuIds?: number[];
  halfCheckMenuIds?: number[];
  customOrgIds?: number[];
  customDeptIds?: number[];
}

/** 菜单查询参数 */
export interface MenuQuery {
  menuName?: string;
  menuType?: string;
  status?: number;
  visible?: number;
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
  path?: string;
  component?: string;
  perms?: string;
  icon?: string;
  sort: number;
  visible: number;
  status: number;
  isFrame?: number;
  remark?: string;
  createTime?: string;
  children?: MenuVO[];
}

/** 字典类型查询参数 */
export interface DictTypeQuery extends PageQuery {
  dictName?: string;
  dictType?: string;
  status?: number;
}

/** 字典类型表单 */
export interface DictTypeForm {
  id?: number;
  dictName?: string;
  dictType?: string;
  status?: number;
  remark?: string;
}

/** 字典类型 VO */
export interface DictTypeVO {
  id: number;
  dictName: string;
  dictType: string;
  status: number;
  remark?: string;
  createTime?: string;
}

/** 字典数据查询参数 */
export interface DictDataQuery extends PageQuery {
  dictType?: string;
  dictLabel?: string;
  status?: number;
}

/** 字典数据表单 */
export interface DictDataForm {
  id?: number;
  dictType?: string;
  dictLabel?: string;
  dictValue?: string;
  cssClass?: string;
  sort?: number;
  status?: number;
  remark?: string;
}

/** 字典数据 VO */
export interface DictDataVO {
  id: number;
  dictType: string;
  dictLabel: string;
  dictValue: string;
  cssClass?: string;
  sort: number;
  status: number;
  remark?: string;
  createTime?: string;
}

/** 参数配置查询参数 */
export interface ConfigQuery extends PageQuery {
  configName?: string;
  configKey?: string;
  configType?: number;
}

/** 参数配置表单 */
export interface ConfigForm {
  id?: number;
  configName?: string;
  configKey?: string;
  configValue?: string;
  configType?: number;
  remark?: string;
}

/** 参数配置 VO */
export interface ConfigVO {
  id: number;
  configName: string;
  configKey: string;
  configValue: string;
  configType: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

/** 文件查询参数 */
export interface FileQuery extends PageQuery {
  originalName?: string;
  fileExt?: string;
  status?: number;
}

/** 文件 VO */
export interface FileVO {
  id: number;
  originalName: string;
  fileName: string;
  fileUrl: string;
  fileExt?: string;
  fileSize: number;
  mimeType?: string;
  storageType: number;
  status: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

/** 通知公告查询参数 */
export interface NoticeQuery extends PageQuery {
  noticeTitle?: string;
  noticeType?: number;
  status?: number;
}

/** 通知公告表单 */
export interface NoticeForm {
  id?: number;
  noticeTitle?: string;
  noticeType?: number;
  noticeContent?: string;
  status?: number;
  remark?: string;
}

/** 通知公告 VO */
export interface NoticeVO {
  id: number;
  noticeTitle: string;
  noticeType: number;
  noticeContent: string;
  status: number;
  publishTime?: string;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

/** 在线用户 VO */
export interface OnlineUserVO {
  userKey: string;
  userId: number;
  username: string;
  deptId?: number;
  status: number;
  loginTime?: string;
  expireTime?: string;
  ttlSeconds?: number;
}

/** 缓存概览 VO */
export interface CacheVO {
  dbSize: number;
  redisVersion?: string;
  usedMemoryHuman?: string;
  connectedClients?: string;
  uptimeInDays?: string;
  keys: string[];
  info: Record<string, unknown>;
}

/** 定时任务查询参数 */
export interface JobQuery extends PageQuery {
  jobName?: string;
  jobGroup?: string;
  status?: number;
}

/** 定时任务表单 */
export interface JobForm {
  id?: number;
  jobName?: string;
  jobGroup?: string;
  invokeTarget?: string;
  cronExpression?: string;
  misfirePolicy?: number;
  concurrent?: number;
  status?: number;
  remark?: string;
}

/** 定时任务 VO */
export interface JobVO {
  id: number;
  jobName: string;
  jobGroup: string;
  invokeTarget: string;
  cronExpression: string;
  misfirePolicy: number;
  concurrent: number;
  status: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

/** 任务日志查询参数 */
export interface JobLogQuery extends PageQuery {
  jobName?: string;
  jobGroup?: string;
  status?: number;
}

/** 任务日志 VO */
export interface JobLogVO {
  id: number;
  jobName: string;
  jobGroup: string;
  invokeTarget: string;
  jobMessage?: string;
  status: number;
  exceptionInfo?: string;
  startTime?: string;
  endTime?: string;
  costTime?: number;
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
  dataIsolation?: number;
  leader?: string;
  phone?: string;
  creditCode?: string;
  legalPerson?: string;
  province?: string;
  city?: string;
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

/** 操作日志查询参数 */
export interface OperationLogQuery extends PageQuery {
  title?: string;
  businessType?: number;
  requestMethod?: string;
  operName?: string;
  operIp?: string;
  status?: number;
  beginTime?: string;
  endTime?: string;
}

/** 操作日志 VO */
export interface OperationLogVO {
  id: number;
  title: string;
  businessType: number;
  method: string;
  requestMethod: string;
  operatorType: number;
  operName: string;
  deptName: string;
  operUrl: string;
  operIp: string;
  operParam: string;
  jsonResult: string;
  status: number;
  errorMsg: string;
  operTime: string;
  costTime: number;
}

/** 登录日志查询参数 */
export interface LoginLogQuery extends PageQuery {
  username?: string;
  ipaddr?: string;
  status?: number;
  msg?: string;
  beginTime?: string;
  endTime?: string;
}

/** 登录日志 VO */
export interface LoginLogVO {
  id: number;
  username: string;
  ipaddr: string;
  loginLocation: string;
  browser: string;
  os: string;
  status: number;
  msg: string;
  loginTime: string;
}

/** 审计中心概览 */
export interface AuditOverviewVO {
  operationTotal: number;
  operationErrorTotal: number;
  loginTotal: number;
  loginFailureTotal: number;
  alertTotal: number;
  unreadAlertTotal: number;
  onlineRetentionDays: number;
  archiveEnabled: boolean;
}

/** 审计归档策略 */
export interface AuditArchiveSettingVO {
  onlineRetentionDays: number;
  archiveEnabled: boolean;
  archiveStorage: string;
  autoCleanEnabled: boolean;
}

/** 安全告警 VO */
export interface SecurityAlertVO {
  id: number;
  alertType: string;
  alertLevel: string;
  alertTitle: string;
  alertContent: string;
  readStatus: number;
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

/* ─── 角色绑定用户 ────────────────────────────────────────── */

/** 角色绑定用户查询参数 */
export interface RoleUserQuery {
  username?: string;
  phone?: string;
  deptId?: number;
}

/** 分页查询已绑定指定角色的用户列表 */
export const fetchAssignedUsers = (roleId: number, params?: RoleUserQuery & PageQuery) =>
  request.get<PageResult<UserVO>>(`/system/role/${roleId}/assignedUsers`, { params });

/** 分页查询未绑定指定角色的用户列表（用于新增绑定时选择） */
export const fetchUnassignedUsers = (roleId: number, params?: RoleUserQuery & PageQuery) =>
  request.get<PageResult<UserVO>>(`/system/role/${roleId}/unassignedUsers`, { params });

/** 批量绑定用户到角色 */
export const assignUsersToRole = (roleId: number, userIds: number[]) =>
  request.post(`/system/role/${roleId}/assignUsers`, userIds);

/** 批量解绑角色下的用户 */
export const unassignUsersFromRole = (roleId: number, userIds: number[]) =>
  request.post(`/system/role/${roleId}/unassignUsers`, userIds);

/* ═══════════════════════════════════════════════════════════
   菜单管理与菜单权限 API
   ═══════════════════════════════════════════════════════════ */

/** 获取菜单管理树 */
export const fetchMenuList = (params?: MenuQuery) =>
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

/** 获取菜单授权树（角色权限分配用，含 M/C/B/F 全部类型） */
export const fetchMenuAuthTree = () =>
  request.get('/system/menu/authTree');

/** 获取权限分配面板数据（扁平列表，角色授权三面板专用） */
export const fetchPermissionData = () =>
  request.get('/system/menu/permissionData');

/* ═══════════════════════════════════════════════════════════
   字典管理 API
   ═══════════════════════════════════════════════════════════ */

/** 分页查询字典类型 */
export const fetchDictTypePage = (params?: DictTypeQuery) =>
  request.get<PageResult<DictTypeVO>>('/system/dict/type/page', { params });

/** 查询字典类型详情 */
export const fetchDictTypeDetail = (id: number) =>
  request.get(`/system/dict/type/${id}`);

/** 新增字典类型 */
export const createDictType = (data: DictTypeForm) =>
  request.post('/system/dict/type', data);

/** 修改字典类型 */
export const updateDictType = (data: DictTypeForm) =>
  request.put('/system/dict/type', data);

/** 删除字典类型 */
export const deleteDictType = (id: number) =>
  request.delete(`/system/dict/type/${id}`);

/** 分页查询字典数据 */
export const fetchDictDataPage = (params?: DictDataQuery) =>
  request.get<PageResult<DictDataVO>>('/system/dict/data/page', { params });

/** 查询字典数据详情 */
export const fetchDictDataDetail = (id: number) =>
  request.get(`/system/dict/data/${id}`);

/** 按字典类型查询启用字典数据 */
export const fetchDictDataByType = (dictType: string) =>
  request.get<DictDataVO[]>(`/system/dict/data/type/${dictType}`);

/** 新增字典数据 */
export const createDictData = (data: DictDataForm) =>
  request.post('/system/dict/data', data);

/** 修改字典数据 */
export const updateDictData = (data: DictDataForm) =>
  request.put('/system/dict/data', data);

/** 删除字典数据 */
export const deleteDictData = (id: number) =>
  request.delete(`/system/dict/data/${id}`);

/* ═══════════════════════════════════════════════════════════
   参数配置 API
   ═══════════════════════════════════════════════════════════ */

/** 分页查询参数配置 */
export const fetchConfigPage = (params?: ConfigQuery) =>
  request.get<PageResult<ConfigVO>>('/system/config/page', { params });

/** 查询参数配置详情 */
export const fetchConfigDetail = (id: number) =>
  request.get(`/system/config/${id}`);

/** 新增参数配置 */
export const createConfig = (data: ConfigForm) =>
  request.post('/system/config', data);

/** 修改参数配置 */
export const updateConfig = (data: ConfigForm) =>
  request.put('/system/config', data);

/** 删除参数配置 */
export const deleteConfig = (id: number) =>
  request.delete(`/system/config/${id}`);

/* ═══════════════════════════════════════════════════════════
   系统运维配置 API
   ═══════════════════════════════════════════════════════════ */

/** 分页查询文件 */
export const fetchFilePage = (params?: FileQuery) =>
  request.get<PageResult<FileVO>>('/system/file/page', { params });

/** 上传文件 */
export const uploadSystemFile = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return request.post<FileVO>('/system/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

/** 下载文件 */
export const downloadSystemFile = (id: number) =>
  axios.get(`${apiBaseURL}/system/file/download/${id}`, {
    responseType: 'blob',
    headers: { Authorization: `Bearer ${getAccessToken()}` },
  });

/** 删除文件 */
export const deleteSystemFile = (id: number) =>
  request.delete(`/system/file/${id}`);

/** 分页查询通知公告 */
export const fetchNoticePage = (params?: NoticeQuery) =>
  request.get<PageResult<NoticeVO>>('/system/notice/page', { params });

/** 查询通知公告详情 */
export const fetchNoticeDetail = (id: number) =>
  request.get<NoticeVO>(`/system/notice/${id}`);

/** 新增通知公告 */
export const createNotice = (data: NoticeForm) =>
  request.post('/system/notice', data);

/** 修改通知公告 */
export const updateNotice = (data: NoticeForm) =>
  request.put('/system/notice', data);

/** 删除通知公告 */
export const deleteNotice = (id: number) =>
  request.delete(`/system/notice/${id}`);

/** 发布通知公告 */
export const publishNotice = (id: number) =>
  request.put(`/system/notice/${id}/publish`);

/** 撤回通知公告 */
export const revokeNotice = (id: number) =>
  request.put(`/system/notice/${id}/revoke`);

/** 查询在线用户 */
export const fetchOnlineUsers = (params?: { username?: string }) =>
  request.get<OnlineUserVO[]>('/system/online/list', { params });

/** 强制用户下线 */
export const forceLogoutOnlineUser = (userKey: string) =>
  request.delete(`/system/online/${userKey}`);

/** 查询缓存概览 */
export const fetchCacheOverview = (params?: { pattern?: string }) =>
  request.get<CacheVO>('/system/cache/overview', { params });

/** 删除缓存键 */
export const deleteCacheKey = (key: string) =>
  request.delete('/system/cache/key', { params: { key } });

/** 按前缀清理缓存 */
export const clearCachePrefix = (prefix: string) =>
  request.delete('/system/cache/prefix', { params: { prefix } });

/** 分页查询定时任务 */
export const fetchJobPage = (params?: JobQuery) =>
  request.get<PageResult<JobVO>>('/system/job/page', { params });

/** 查询定时任务详情 */
export const fetchJobDetail = (id: number) =>
  request.get<JobVO>(`/system/job/${id}`);

/** 新增定时任务 */
export const createJob = (data: JobForm) =>
  request.post('/system/job', data);

/** 修改定时任务 */
export const updateJob = (data: JobForm) =>
  request.put('/system/job', data);

/** 删除定时任务 */
export const deleteJob = (id: number) =>
  request.delete(`/system/job/${id}`);

/** 立即运行定时任务 */
export const runJob = (id: number) =>
  request.post(`/system/job/${id}/run`);

/** 修改定时任务状态 */
export const changeJobStatus = (id: number, status: number) =>
  request.put(`/system/job/${id}/status`, null, { params: { status } });

/** 查询已注册调用目标 */
export const fetchJobTargets = () =>
  request.get<string[]>('/system/job/targets');

/** 分页查询任务日志 */
export const fetchJobLogPage = (params?: JobLogQuery) =>
  request.get<PageResult<JobLogVO>>('/system/job/log/page', { params });

/** 清空任务日志 */
export const cleanJobLogs = () =>
  request.delete('/system/job/log/clean');

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
  request.get<PageResult<UserVO>>('/system/user/page', { params });

/** 分页查询角色 */
export const fetchRolePage = (params?: RoleQuery & PageQuery) =>
  request.get<PageResult<RoleVO>>('/system/role/page', { params });

/* ═══════════════════════════════════════════════════════════
   系统操作日志 API
   ═══════════════════════════════════════════════════════════ */

/** 分页查询操作日志 */
export const fetchOperationLogPage = (params?: OperationLogQuery) =>
  request.get<PageResult<OperationLogVO>>('/system/operlog/page', { params });

/** 获取操作日志详情 */
export const fetchOperationLogDetail = (id: number) =>
  request.get<OperationLogVO>(`/system/operlog/${id}`);

/** 批量删除操作日志 */
export const deleteOperationLogs = (ids: number[]) =>
  request.delete('/system/operlog', { data: ids });

/** 清空操作日志 */
export const cleanOperationLogs = () =>
  request.delete('/system/operlog/clean');

/** 导出操作日志 */
export const exportOperationLogs = (params?: OperationLogQuery) =>
  downloadAuditFile('/system/operlog/export', params);

/* ═══════════════════════════════════════════════════════════
   系统登录日志 API
   ═══════════════════════════════════════════════════════════ */

/** 分页查询登录日志 */
export const fetchLoginLogPage = (params?: LoginLogQuery) =>
  request.get<PageResult<LoginLogVO>>('/system/loginlog/page', { params });

/** 获取登录日志详情 */
export const fetchLoginLogDetail = (id: number) =>
  request.get<LoginLogVO>(`/system/loginlog/${id}`);

/** 批量删除登录日志 */
export const deleteLoginLogs = (ids: number[]) =>
  request.delete('/system/loginlog', { data: ids });

/** 清空登录日志 */
export const cleanLoginLogs = () =>
  request.delete('/system/loginlog/clean');

/** 导出登录日志 */
export const exportLoginLogs = (params?: LoginLogQuery) =>
  downloadAuditFile('/system/loginlog/export', params);

/** 分页查询异常日志（复用操作日志异常记录） */
export const fetchExceptionLogPage = (params?: OperationLogQuery) =>
  request.get<PageResult<OperationLogVO>>('/system/audit/exception-logs/page', { params });

/** 导出异常日志 */
export const exportExceptionLogs = (params?: OperationLogQuery) =>
  downloadAuditFile('/system/audit/exception-logs/export', params);

/* ═══════════════════════════════════════════════════════════
   日志审计中心 API
   ═══════════════════════════════════════════════════════════ */

/** 查询审计中心概览 */
export const fetchAuditOverview = () =>
  request.get<AuditOverviewVO>('/system/audit/overview');

/** 查询风险告警列表 */
export const fetchSecurityAlerts = (params?: { limit?: number }) =>
  request.get<SecurityAlertVO[]>('/system/audit/security-alerts', { params });

/** 将指定风险告警标记为已读 */
export const markSecurityAlertRead = (id: number) =>
  request.put<boolean>(`/system/audit/security-alerts/${id}/read`);

/** 查询审计归档策略 */
export const fetchAuditArchiveSetting = () =>
  request.get<AuditArchiveSettingVO>('/system/audit/archive-setting');

/** 保存审计归档策略 */
export const saveAuditArchiveSetting = (data: AuditArchiveSettingVO) =>
  request.put('/system/audit/archive-setting', data);

/** 下载审计文件 */
async function downloadAuditFile(url: string, params?: object) {
  const response = await axios.get(`${apiBaseURL}${url}`, {
    params,
    responseType: 'blob',
    headers: getAccessToken() ? { Authorization: `Bearer ${getAccessToken()}` } : undefined,
  });
  const disposition = response.headers['content-disposition'] || '';
  const filenameMatch = /filename\\*=UTF-8''([^;]+)/i.exec(disposition);
  const filename = filenameMatch ? decodeURIComponent(filenameMatch[1]) : `audit-${Date.now()}.csv`;
  const blobUrl = window.URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = blobUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(blobUrl);
}

/* ═════════════════════════════════════════════════════════
   机构数据共享规则 API
   ═════════════════════════════════════════════════════════ */

/** 机构数据共享规则 VO */
export interface OrgDataShareVO {
  id: number;
  sourceOrgId: number;
  targetOrgId: number;
  targetOrgName?: string;
  shareType: number;
  moduleCode: string;
  permission: number;
  status: number;
  expireTime: string;
  remark: string;
  createTime: string;
}

/** 机构数据共享规则表单 */
export interface OrgDataShareForm {
  id?: number;
  sourceOrgId?: number;
  targetOrgId?: number;
  shareType?: number;
  moduleCode?: string;
  permission?: number;
  status?: number;
  expireTime?: string;
  remark?: string;
}

/** 查询机构共享规则列表 */
export const fetchOrgShareList = (sourceOrgId: number) =>
  request.get<OrgDataShareVO[]>(`/system/org/share/list/${sourceOrgId}`);

/** 新增机构共享规则 */
export const createOrgShare = (data: OrgDataShareForm) =>
  request.post('/system/org/share', data);

/** 修改机构共享规则 */
export const updateOrgShare = (data: OrgDataShareForm) =>
  request.put('/system/org/share', data);

/** 删除机构共享规则 */
export const deleteOrgShare = (id: number) =>
  request.delete(`/system/org/share/${id}`);

/* ═════════════════════════════════════════════════════════
   用户个人数据权限 API
   ═════════════════════════════════════════════════════════ */

/** 用户个人数据权限 VO */
export interface UserDataScopeVO {
  id: number;
  userId: number;
  dataScope: number;
  customOrgIds: string;
  customDeptIds: string;
  status: number;
  expireTime: string;
  remark: string;
  createTime: string;
}

/** 用户个人数据权限表单 */
export interface UserDataScopeForm {
  id?: number;
  userId?: number;
  dataScope?: number;
  customOrgIds?: string;
  customDeptIds?: string;
  status?: number;
  expireTime?: string;
  remark?: string;
}

/** 查询用户个人数据权限 */
export const fetchUserDataScope = (userId: number) =>
  request.get<UserDataScopeVO>(`/system/user/datascope/${userId}`);

/** 保存用户个人数据权限 */
export const saveUserDataScope = (data: UserDataScopeForm) =>
  request.post('/system/user/datascope', data);

/** 删除用户个人数据权限 */
export const deleteUserDataScope = (userId: number) =>
  request.delete(`/system/user/datascope/${userId}`);

/* ═════════════════════════════════════════════════════════
   个人中心 API（当前登录用户操作自己的信息）
   ═════════════════════════════════════════════════════════ */

/** 个人信息修改参数 */
export interface UpdateProfileForm {
  /** 新账号（5~30 字符） */
  username: string;
  /** 新用户名（2~30 字符） */
  nickname: string;
}

/** 修改密码参数 */
export interface ChangePasswordForm {
  /** 旧密码（当前密码） */
  oldPassword: string;
  /** 新密码（6~30 字符） */
  newPassword: string;
}

/** 获取当前登录用户详细信息（含机构、部门、岗位、角色等） */
export const fetchProfile = () =>
  request.get<UserVO>('/auth/profile');

/** 修改当前登录用户个人信息（账号、用户名） */
export const updateProfile = (data: UpdateProfileForm) =>
  request.put('/auth/profile', data);

/** 修改当前登录用户密码 */
export const changePassword = (data: ChangePasswordForm) =>
  request.put('/auth/password', data);
