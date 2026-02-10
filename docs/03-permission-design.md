# BML-Backend 权限系统设计 V2.0

> **版本**: v2.0 | **项目**: bml-backend | **日期**: 2026-02-09

---

## 1. 权限模型概述

采用 **RBAC + ABAC 混合模型**，兼顾管理便捷性和访问控制灵活性。

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         权限模型架构                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ┌──────────────────────────────────────────────────────────────────┐ │
│   │                    RBAC 基础模型                                  │ │
│   │                                                                   │ │
│   │    ┌──────┐   N:M   ┌──────┐   N:M   ┌────────────┐              │ │
│   │    │ 用户 │ ───────▶│ 角色 │ ───────▶│   权限     │              │ │
│   │    │ User │         │ Role │         │ Permission │              │ │
│   │    └──────┘         └──────┘         └────────────┘              │ │
│   │         │                                  │                      │ │
│   │         │                                  ▼                      │ │
│   │         │           ┌────────────────────────────────────────┐   │ │
│   │         │           │            权限类型分类               │   │ │
│   │         │           ├──────────┬──────────┬────────┬────────┤   │ │
│   │         │           │  菜单权限 │  按钮权限 │ 字段权限│ 数据权限│   │ │
│   │         │           │   L1     │    L2    │   L3   │   L4   │   │ │
│   │         │           └──────────┴──────────┴────────┴────────┘   │ │
│   │         │                                                        │ │
│   │         └──────────────────────┐                                 │ │
│   │                                ▼                                 │ │
│   │    ┌──────┐             ┌──────────┐                            │ │
│   │    │ 部门 │◀────────────│ 用户部门 │                            │ │
│   │    │ Dept │             │   归属   │                            │ │
│   │    └──────┘             └──────────┘                            │ │
│   │         │                                                        │ │
│   │         ▼                                                        │ │
│   │    ┌──────┐                                                      │ │
│   │    │ 组织 │                                                      │ │
│   │    │ Org  │                                                      │ │
│   │    └──────┘                                                      │ │
│   └──────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│   ┌──────────────────────────────────────────────────────────────────┐ │
│   │                    ABAC 扩展规则                                  │ │
│   │                                                                   │ │
│   │   动态属性条件:                                                   │ │
│   │   ┌─────────────┬─────────────┬─────────────┬─────────────┐      │ │
│   │   │  时间属性   │  位置属性   │  设备属性   │  环境属性   │      │ │
│   │   │ 工作时间   │  IP白名单   │  设备类型   │  生产/测试  │      │ │
│   │   │ 节假日     │  地理位置   │  可信设备   │  租户隔离   │      │ │
│   │   └─────────────┴─────────────┴─────────────┴─────────────┘      │ │
│   └──────────────────────────────────────────────────────────────────┘ │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 四层权限控制

### 2.1 L1 菜单权限

**作用**: 控制用户可以看到哪些菜单项

```java
// 前端路由守卫
const hasPermission = (route) => {
  return userMenuCodes.includes(route.meta.permission)
}

// 后端返回用户菜单树
@GetMapping("/user/menus")
public Result<List<MenuVO>> getUserMenus() {
  return Result.ok(menuService.getUserMenuTree(SecurityUtils.getUserId()));
}
```

### 2.2 L2 按钮权限

**作用**: 控制用户可以操作哪些功能按钮

```java
// 后端接口权限校验
@PostMapping
@PreAuthorize("@perm.has('system:user:add')")
public Result<Long> createUser(@RequestBody UserRequest request) {
  return Result.ok(userService.create(request));
}

// 前端按钮显示控制
<el-button v-if="hasPermission('system:user:add')">新增</el-button>
```

**权限标识规范**: `{模块}:{资源}:{操作}`
- system:user:add (用户新增)
- system:user:edit (用户编辑)
- system:user:delete (用户删除)
- system:user:export (用户导出)

### 2.3 L3 字段权限

**作用**: 控制表单字段的可见性、可编辑性

| 控制项 | 说明 |
|--------|------|
| visible | 字段是否可见 |
| editable | 字段是否可编辑 |
| required | 字段是否必填 |

```java
// 字段权限注解
@FieldPermission(menu = "system:user")
public class UserVO {
    private Long id;

    @FieldMask(type = MaskType.PHONE)  // 脱敏
    private String phone;

    @FieldControl  // 受控字段
    private BigDecimal salary;
}

// 返回时根据权限过滤字段
@GetMapping("/{id}")
public Result<UserVO> getUser(@PathVariable Long id) {
  UserVO vo = userService.getById(id);
  return Result.ok(fieldPermissionService.filterFields(vo, "system:user"));
}
```

**字段权限表设计**:
```sql
CREATE TABLE sys_field_permission (
    role_id BIGINT,           -- 角色ID
    menu_code VARCHAR(100),   -- 菜单编码
    field_name VARCHAR(50),   -- 字段名
    visible TINYINT,          -- 是否可见
    editable TINYINT,         -- 是否可编辑
    required TINYINT          -- 是否必填
);
```

### 2.4 L4 数据权限

**作用**: 控制用户可以访问的数据范围

| 范围类型 | 编码 | SQL条件示例 |
|---------|------|------------|
| 全部数据 | 1 | 无条件 |
| 本组织及下级 | 2 | `org_id IN (本组织+下级ID列表)` |
| 仅本组织 | 3 | `org_id = #{userOrgId}` |
| 本部门及下级 | 4 | `dept_id IN (本部门+下级ID列表)` |
| 仅本部门 | 5 | `dept_id = #{userDeptId}` |
| 仅本人 | 6 | `create_by = #{userId}` |
| 自定义 | 7 | 根据规则表达式 |

```java
// 数据权限注解
@DataScope(deptAlias = "d", orgAlias = "o", userAlias = "u")
public List<User> selectUserList(UserQuery query) {
  return userMapper.selectList(query);
}

// AOP切面自动拼接SQL
@Aspect
public class DataScopeAspect {
    @Before("@annotation(dataScope)")
    public void before(DataScope dataScope) {
        LoginUser user = SecurityUtils.getLoginUser();
        // 根据用户角色的数据权限,拼接SQL条件
        String sqlCondition = buildDataScopeCondition(user, dataScope);
        DataScopeContextHolder.set(sqlCondition);
    }
}
```

---

## 3. ABAC动态规则

### 3.1 规则表达式

支持SpEL表达式定义动态规则:

```java
// 时间条件: 仅工作时间可访问
"T(java.time.LocalTime).now().isAfter(T(java.time.LocalTime).of(9,0)) && 
 T(java.time.LocalTime).now().isBefore(T(java.time.LocalTime).of(18,0))"

// IP条件: 仅内网可访问
"#request.remoteAddr.startsWith('192.168.')"

// 组合条件: 工作时间+内网
"#isWorkTime && #isIntranet"
```

### 3.2 规则表设计

```sql
CREATE TABLE sys_access_rule (
    id BIGINT PRIMARY KEY,
    rule_name VARCHAR(100),       -- 规则名称
    rule_type VARCHAR(20),        -- 规则类型: TIME/IP/DEVICE/CUSTOM
    rule_expression TEXT,         -- SpEL表达式
    enabled TINYINT,              -- 是否启用
    description VARCHAR(255)      -- 描述
);

CREATE TABLE sys_role_access_rule (
    role_id BIGINT,               -- 角色ID
    rule_id BIGINT,               -- 规则ID
    menu_code VARCHAR(100)        -- 菜单编码(为空表示全局)
);
```

---

## 4. 权限校验流程

```
┌─────────────┐
│   请求进入   │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│  Token有效性    │──── 无效 ────▶ 401 未认证
└──────┬──────────┘
       │ 有效
       ▼
┌─────────────────┐
│  License有效性  │──── 无效 ────▶ 403 未授权
└──────┬──────────┘
       │ 有效
       ▼
┌─────────────────┐
│  接口权限检查   │──── 无权限 ──▶ 403 无权限
│ @PreAuthorize  │
└──────┬──────────┘
       │ 有权限
       ▼
┌─────────────────┐
│  ABAC规则检查   │──── 不满足 ──▶ 403 条件不满足
└──────┬──────────┘
       │ 满足
       ▼
┌─────────────────┐
│  执行业务逻辑   │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│  数据权限过滤   │ ← @DataScope AOP处理
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│  字段权限过滤   │ ← 响应时过滤字段
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│   返回结果     │
└─────────────────┘
```

---

## 5. 核心代码示例

### 5.1 权限校验服务

```java
@Service("perm")
public class PermissionService {

    /**
     * 检查是否有权限
     */
    public boolean has(String permission) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null) return false;
        if (user.isAdmin()) return true;
        return user.getPermissions().contains(permission);
    }

    /**
     * 检查是否有任一权限
     */
    public boolean hasAny(String... permissions) {
        for (String permission : permissions) {
            if (has(permission)) return true;
        }
        return false;
    }
}
```

### 5.2 数据权限切面

```java
@Aspect
@Component
public class DataScopeAspect {

    @Before("@annotation(dataScope)")
    public void doBefore(JoinPoint point, DataScope dataScope) {
        LoginUser user = SecurityUtils.getLoginUser();
        StringBuilder sql = new StringBuilder();

        for (SysRole role : user.getRoles()) {
            int scope = role.getDataScope();
            switch (scope) {
                case 1: // 全部
                    return;
                case 2: // 本组织及下级
                    sql.append(String.format(" OR %s.org_id IN (%s)",
                        dataScope.orgAlias(),
                        user.getOrgIdWithChildren()));
                    break;
                // ... 其他情况
            }
        }

        DataScopeHolder.set(sql.toString());
    }
}
```
