# 阶段 C 实施教程（Entity 出参收口 + 数据权限 + 动态路由）

## 1. 目标与范围

本教程对应 2026-02-28 的阶段 C 落地，覆盖三项核心能力：

1. 后端控制器出参统一从 Entity 收口到 VO。
2. 数据权限以注解 + AOP 方式落地，支持按角色 `data_scope` 自动注入 SQL 过滤。
3. 前端路由改为“静态基线 + 后端动态注入”，菜单与路由树由 `/auth/routers` 驱动。

## 2. 后端出参收口规范

### 2.1 已完成收口的接口

- `SysUserController`：`Result<List<SysUserVO>>`、`Result<SysUserVO>`
- `SysRoleController`：`Result<List<SysRoleVO>>`、`Result<SysRoleVO>`
- `SysDeptController`：`Result<List<SysDeptVO>>`、`Result<SysDeptVO>`
- `SysMenuController`：`Result<List<SysMenuVO>>`、`Result<SysMenuVO>`
- `SysAlertController`：`Result<List<SysAlertVO>>`
- `AuthController#getInfo`：`UserInfoVO.user` 改为 `SysUserVO`
- `AuthController#getRouters`：改为 `Result<List<RouterVO>>`

### 2.2 开发约束

新增接口时请遵循：

1. Controller 不直接返回实体类。
2. DTO 只用于入参，VO 只用于出参。
3. 转换统一放在 Converter（MapStruct 或手写）中，不在 Controller 拼字段。

## 3. 数据权限落地规范

### 3.1 能力结构

数据权限能力位于 `bml-module-system`：

- `@DataScope`：声明查询涉及的字段列。
- `DataScopeAspect`：按当前登录用户角色的 `data_scope` 生成 SQL 条件。
- `DataScopeContext`：在线程上下文传递 SQL 片段。
- `DataScopeType`：数据范围枚举（1~7）。

### 3.2 使用方式

在 Service 查询方法上标注注解：

```java
@DataScope(deptColumn = "dept_id", orgColumn = "org_id", userColumn = "id", creatorColumn = "create_by")
public List<SysUser> selectUserList(SysUserDTO user) {
    LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
    String dataScopeSql = DataScopeContext.getDataScopeSql();
    if (StrUtil.isNotBlank(dataScopeSql)) {
        queryWrapper.apply(dataScopeSql);
    }
    return baseMapper.selectList(queryWrapper);
}
```

### 3.3 已接入方法

- `SysUserServiceImpl#selectUserList`
- `SysDeptServiceImpl#selectDeptList`
- `SysRoleServiceImpl#selectRoleList`

### 3.4 注意事项

1. SQL 列名必须是固定常量，不得拼接用户输入。
2. 如果角色配置为自定义范围（7），当前版本默认回退到 `creatorColumn`，无列可用时拒绝访问。
3. 超级管理员（`userId=1`）不受数据范围限制。

## 4. 动态路由落地规范

### 4.1 后端路由契约

后端新增：

- `RouterVO`
- `RouterMetaVO`

`SysMenuServiceImpl#buildMenus` 负责将菜单树转换为路由树。

### 4.2 前端加载机制

`src/router/index.ts` 实现：

1. 静态基线路由（首页、登录、管理框架）。
2. 登录后访问后台路径时，自动请求 `/auth/routers`。
3. 将后端路由树转换成 Vue Router 路由并动态 `addRoute`。
4. 登出时统一清理动态路由与权限状态。

### 4.3 侧边栏渲染

`src/layout/Layout.vue` 改为读取 `permission store`：

- 菜单数据来源：`usePermissionStore().sidebarMenus`
- 图标映射：后端 `meta.icon` -> 前端 Arco Icon 组件
- 迷你侧栏（收起态）也使用同一份菜单树

### 4.4 新增菜单接入步骤

若后端新增菜单想被前端自动识别：

1. DB 菜单配置 `menu_type` 必须为 `M` 或 `C`。
2. `path` 与 `component` 正确填写（例如 `app/AppList`）。
3. `component` 对应文件路径：`src/views/${component}.vue`。
4. 若组件不存在，前端自动降级到 `FeatureDisabled`。

## 5. 数据迁移与初始化

新增迁移脚本：

- `V1.5.0__bootstrap_dynamic_route_menus.sql`

作用：

1. 补齐工作台/API 管理/在线调试/应用管理菜单。
2. 自动授予超级管理员角色菜单关联（幂等）。

## 6. 测试清单

### 6.1 后端

- `DataScopeAspectTest`：验证数据权限 SQL 生成规则。
- `SysMenuServiceImplRouteBuildTest`：验证菜单到路由转换规则。

执行：

```bash
cd bml-backend
mvn -q test
```

### 6.2 前端

执行：

```bash
cd bml-frontend
npm test
npm run build
npm run build:ci
```

## 7. 版本治理说明

本阶段未新增三方依赖版本。
依赖与插件版本仍统一由父工程 `bml-backend/pom.xml` 管理，子模块不新增独立版本号配置。
