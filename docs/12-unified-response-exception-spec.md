# 统一响应、统一异常与统一接口返回规范

## 1. 目标

本规范用于统一 BML 后端所有 HTTP 接口的返回协议，覆盖以下场景：

- Controller 正常返回
- Controller 抛出业务异常
- 参数校验失败
- Spring Security 认证失败与权限不足
- Filter / Interceptor 主动拦截
- OpenAPI 签名校验失败

## 2. 统一返回结构

所有接口都必须返回以下 JSON 结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1700000000000,
  "traceId": "f2f0f8b3b6f54d0ea3d41b3a3f4e10b4"
}
```

字段说明：

- `code`：业务状态码
- `message`：面向调用方的可读消息
- `data`：业务返回数据
- `timestamp`：后端生成响应的时间戳
- `traceId`：链路追踪标识，用于排查问题

## 3. HTTP 状态码与业务状态码

必须区分两层语义：

- HTTP 状态码：表达协议层处理结果
- `Result.code`：表达业务层处理结果

当前约定如下：

- `200`：业务成功
- `400`：参数错误、业务规则不满足
- `401`：未认证、登录失效、令牌无效
- `403`：已认证但无权限
- `404`：资源不存在
- `405`：请求方法不支持
- `415`：媒体类型不支持
- `500`：系统内部异常

OpenAPI 相关错误统一映射到 `2101-2107` 业务码区间。

## 4. 后端落地规则

### 4.1 Controller 统一返回 `Result`

推荐写法：

```java
return Result.ok(data);
```

```java
return Result.badRequest("参数不合法");
```

```java
return toAjax(service.updateById(entity));
```

禁止以下做法：

- 手工 `new Result(...)`
- Controller 内部自行写 `response.getWriter()`
- 同一业务在不同入口返回不同字段名

### 4.2 业务异常统一使用 `BusinessException`

`BusinessException` 只用于可预期的业务错误。

推荐写法：

```java
throw new BusinessException("用户名或密码错误");
```

```java
throw new BusinessException(GlobalErrorCode.TOKEN_EXPIRED);
```

### 4.3 全局异常统一交给 `GlobalExceptionHandler`

当前已经统一处理：

- `BusinessException`
- `AccessDeniedException`
- `MethodArgumentNotValidException`
- `BindException`
- `ConstraintViolationException`
- `MissingServletRequestParameterException`
- `HttpMessageNotReadableException`
- `NoHandlerFoundException`
- `HttpRequestMethodNotSupportedException`
- `HttpMediaTypeNotSupportedException`
- `NoResourceFoundException`
- 兜底 `Exception`

### 4.4 非 Controller 入口统一通过工具类写回

以下入口不能依赖 `@RestControllerAdvice`：

- `AuthenticationEntryPoint`
- `AccessDeniedHandler`
- `HandlerInterceptor`
- `Filter`

因此统一通过 `ServletResponseUtils` 写出响应，确保：

- 状态码一致
- JSON 结构一致
- 编码一致
- `traceId` 一致

## 5. OpenAPI 业务码

当前规范如下：

- `2101`：缺少必要签名头
- `2102`：时间戳格式错误
- `2103`：请求已过期
- `2104`：应用凭证无效
- `2105`：当前应用无权访问该接口
- `2106`：重复请求被拒绝
- `2107`：签名校验失败

## 6. 前端对接规则

前端请求层统一识别：

- `code`
- `message`
- `data`
- `timestamp`
- `traceId`

当前前端已完成：

- 统一读取 `message`
- 统一以 `code === 200` 判断成功
- 错误时记录 `traceId`
- 401 时尝试刷新 token

## 7. 验证要求

新增接口或异常处理后，至少验证以下场景：

- 200 成功响应
- 400 参数或业务失败
- 401 未登录 / 登录失效
- 403 权限不足
- 404 路由不存在
- OpenAPI 签名失败

## 8. 维护要求

- 任何新增错误码都必须先进入 `GlobalErrorCode`
- 不允许重新引入 `msg` / `errorMsg` / `errMessage` 等并行字段
- 前后端变更统一返回协议时，必须同步更新本规范
