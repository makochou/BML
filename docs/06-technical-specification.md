# BML-Backend 技术规范 V2.0

> **版本**: v2.0 | **项目**: bml-backend | **日期**: 2026-02-09

---

## 1. 项目结构规范

### 1.1 Maven模块命名

```
bml-{层级}-{模块名}

示例:
bml-core-framework     # 核心-框架
bml-core-common        # 核心-通用
bml-module-system      # 模块-系统
bml-ext-wechat         # 扩展-微信
```

### 1.2 包命名规范

```
com.bml.{层级}.{模块}.{分层}

示例:
com.bml.core.framework.config      # 核心框架-配置
com.bml.module.system.controller   # 系统模块-控制器
com.bml.module.system.service.impl # 系统模块-服务实现
```

### 1.3 模块内部结构

```
src/main/java/com/bml/module/xxx/
├── controller/              # 控制器 - 接收请求,调用服务
├── service/                 # 服务接口
│   └── impl/               # 服务实现
├── mapper/                  # MyBatis Mapper接口
├── entity/                  # 数据库实体
├── dto/                     # 数据传输对象
│   ├── request/            # 请求DTO
│   └── response/           # 响应VO
├── convert/                 # MapStruct转换器
├── enums/                   # 枚举定义
└── constants/               # 常量定义
```

---

## 2. 命名规范

### 2.1 类命名

| 类型 | 规则 | 示例 |
|------|------|------|
| Controller | XxxController | UserController |
| Service | XxxService / XxxServiceImpl | UserService |
| Mapper | XxxMapper | UserMapper |
| Entity | Xxx (无后缀) | User, SysUser |
| 请求DTO | XxxRequest 或 XxxReq | UserCreateRequest |
| 响应VO | XxxVO 或 XxxResponse | UserVO |
| 转换器 | XxxConvert | UserConvert |
| 枚举 | XxxEnum | StatusEnum |
| 常量 | XxxConstants | SystemConstants |
| 工具类 | XxxUtils | DateUtils |
| 配置类 | XxxConfig | SecurityConfig |
| 切面 | XxxAspect | OperationLogAspect |

### 2.2 方法命名

| 场景 | 命名规则 | 示例 |
|------|---------|------|
| 查询单个 | getXxx / findXxx | getUserById |
| 查询列表 | listXxx / findXxxList | listUsers |
| 分页查询 | pageXxx | pageUsers |
| 新增 | create / add / save | createUser |
| 更新 | update / modify | updateUser |
| 删除 | delete / remove | deleteUser |
| 批量删除 | batchDelete | batchDeleteUsers |
| 导出 | export | exportUsers |
| 导入 | import | importUsers |

### 2.3 变量命名

```java
// 集合类型加复数或List/Map后缀
List<User> users;
List<Long> userIds;
Map<Long, User> userMap;

// boolean类型加is/has/can前缀
boolean isAdmin;
boolean hasPermission;
boolean canDelete;

// 常量全大写下划线分隔
public static final int MAX_PAGE_SIZE = 100;
public static final String DEFAULT_PASSWORD = "123456";
```

---

## 3. 统一响应规范

### 3.1 响应结构

```java
@Data
public class Result<T> {
    /** 状态码 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 响应数据 */
    private T data;
    /** 响应时间戳 */
    private long timestamp;
    /** 链路ID */
    private String traceId;

    // 成功
    public static <T> Result<T> ok() { ... }
    public static <T> Result<T> ok(T data) { ... }
    public static <T> Result<T> ok(String message, T data) { ... }

    // 失败
    public static <T> Result<T> fail(int code, String message) { ... }
    public static <T> Result<T> fail(ErrorCode errorCode) { ... }
}
```

### 3.2 分页响应

```java
@Data
public class PageResult<T> {
    private List<T> records;   // 数据列表
    private long total;        // 总记录数
    private int pageNum;       // 当前页
    private int pageSize;      // 每页条数
    private int pages;         // 总页数
}
```

### 3.3 响应示例

```json
// 成功响应
{
  "code": 200,
  "message": "操作成功",
  "data": { "id": 1, "username": "admin" },
  "timestamp": 1707474000000,
  "traceId": "a1b2c3d4e5f6"
}

// 分页响应
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [...],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 10
  },
  "timestamp": 1707474000000,
  "traceId": "a1b2c3d4e5f6"
}

// 错误响应
{
  "code": 1001,
  "message": "用户名已存在",
  "data": null,
  "timestamp": 1707474000000,
  "traceId": "a1b2c3d4e5f6"
}
```

---

## 4. 错误码规范

### 4.1 错误码分段

| 范围 | 类别 | 说明 |
|------|------|------|
| 200 | 成功 | 操作成功 |
| 400 | 参数错误 | 请求参数校验失败 |
| 401 | 未认证 | 未登录或Token无效 |
| 403 | 无权限 | 权限不足 |
| 404 | 不存在 | 资源不存在 |
| 500 | 系统错误 | 服务器内部错误 |
| 1000-1099 | 用户模块 | 用户相关错误 |
| 1100-1199 | 角色模块 | 角色相关错误 |
| 1200-1299 | 菜单模块 | 菜单相关错误 |
| 1300-1399 | 部门模块 | 部门相关错误 |
| 2000-2099 | 认证模块 | 登录相关错误 |
| 2100-2199 | 授权模块 | License相关错误 |
| 3000-3999 | 业务模块 | 业务逻辑错误 |

### 4.2 错误码枚举示例

```java
public enum ErrorCode {
    // 成功
    SUCCESS(200, "操作成功"),

    // 通用错误
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统繁忙,请稍后重试"),

    // 用户模块 1000-1099
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "用户已禁用"),
    USERNAME_EXISTS(1003, "用户名已存在"),
    PASSWORD_ERROR(1004, "密码错误"),
    PASSWORD_EXPIRED(1005, "密码已过期"),
    ACCOUNT_LOCKED(1006, "账号已锁定"),

    // 授权模块 2100-2199
    LICENSE_INVALID(2101, "授权无效"),
    LICENSE_EXPIRED(2102, "授权已过期"),
    LICENSE_MAX_USERS(2103, "已达最大用户数"),
    LICENSE_MACHINE_ERROR(2104, "机器码不匹配");
}
```

---

## 5. 接口规范

### 5.1 URL设计

```
/{version}/{module}/{resource}[/{action}]

规则:
- version: v1, v2 等版本号
- module: 模块名 (system, hr, oa等)
- resource: 资源名 (复数形式)
- action: 可选,非CRUD操作
```

### 5.2 HTTP方法

| 方法 | 用途 | 示例 |
|------|------|------|
| GET | 查询 | GET /v1/system/users |
| POST | 新增 | POST /v1/system/users |
| PUT | 更新 | PUT /v1/system/users/{id} |
| DELETE | 删除 | DELETE /v1/system/users/{id} |
| PATCH | 部分更新 | PATCH /v1/system/users/{id} |

### 5.3 Controller示例

```java
@Tag(name = "用户管理")
@RestController
@RequestMapping("/v1/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户分页列表")
    @GetMapping
    @PreAuthorize("@perm.has('system:user:query')")
    public Result<PageResult<UserVO>> page(UserQueryRequest request) {
        return Result.ok(userService.page(request));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('system:user:query')")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("@perm.has('system:user:add')")
    @OperationLog(module = "用户管理", operation = "创建用户")
    public Result<Long> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.ok(userService.create(request));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @PreAuthorize("@perm.has('system:user:edit')")
    @OperationLog(module = "用户管理", operation = "更新用户")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody UserUpdateRequest request) {
        userService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.has('system:user:delete')")
    @OperationLog(module = "用户管理", operation = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}
```

---

## 6. 日志规范

### 6.1 操作日志注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    /** 模块名称 */
    String module() default "";
    /** 操作名称 */
    String operation() default "";
    /** 是否保存请求参数 */
    boolean saveRequest() default true;
    /** 是否保存响应结果 */
    boolean saveResponse() default false;
    /** 敏感字段(脱敏处理) */
    String[] sensitiveFields() default {};
}
```

### 6.2 日志级别使用

| 级别 | 使用场景 |
|------|---------|
| ERROR | 系统异常、业务错误 |
| WARN | 潜在问题、降级处理 |
| INFO | 关键业务流程、状态变更 |
| DEBUG | 调试信息、方法入参出参 |
| TRACE | 详细追踪信息 |

### 6.3 日志输出格式

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n
```

---

## 7. 数据库规范

### 7.1 表命名

| 前缀 | 用途 | 示例 |
|------|------|------|
| sys_ | 系统表 | sys_user |
| biz_ | 业务表 | biz_order |
| log_ | 日志表 | log_operation |
| tmp_ | 临时表 | tmp_import |

### 7.2 通用字段

```sql
id           BIGINT        -- 主键
create_by    BIGINT        -- 创建人
create_time  DATETIME      -- 创建时间
update_by    BIGINT        -- 更新人
update_time  DATETIME      -- 更新时间
deleted      TINYINT       -- 逻辑删除(0否1是)
version      INT           -- 乐观锁版本
```

---

## 8. 版本依赖清单

```xml
<properties>
    <!-- Java -->
    <java.version>21</java.version>

    <!-- Spring -->
    <spring-boot.version>3.2.3</spring-boot.version>

    <!-- 持久层 -->
    <mybatis-plus.version>3.5.5</mybatis-plus.version>
    <shardingsphere.version>5.4.1</shardingsphere.version>
    <flyway.version>10.8.1</flyway.version>
    <mariadb.version>3.3.2</mariadb.version>

    <!-- 缓存 -->
    <redisson.version>3.27.0</redisson.version>

    <!-- 工具 -->
    <hutool.version>5.8.26</hutool.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
    <guava.version>33.0.0-jre</guava.version>

    <!-- 文档 -->
    <springdoc.version>2.3.0</springdoc.version>

    <!-- 安全 -->
    <jjwt.version>0.12.5</jjwt.version>
</properties>
```
