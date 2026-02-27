# 当前系统架构

## 1. 总览

BML 当前采用模块化单体架构：后端由一个 Spring Boot 应用装配启动，前端为独立 Vite 应用。

## 2. 仓库结构

### 2.1 后端

- `bml-core`
  - `bml-core-common`：错误码、结果封装、公共异常
  - `bml-core-base`：基础控制器、基础实体、通用抽象
  - `bml-core-framework`：安全、Web、Redis、OpenAPI、全局异常
- `bml-modules`
  - `bml-module-system`：系统管理、认证、监控告警
  - `bml-module-api`：API 账号、OpenAPI 授权
- `bml-app`
  - 启动入口、Flyway 迁移、运行配置

### 2.2 前端

- `src/views/login`：登录页
- `src/views/dashboard`：首页与监控展示
- `src/views/app`：应用管理页
- `src/views/common/FeatureDisabled.vue`：未开放能力占位页
- `src/utils/request.ts`：统一请求与刷新令牌逻辑

## 3. 关键链路

### 3.1 登录链路

1. 前端调用 `/api/auth/login`
2. 后端认证成功后返回 `accessToken + refreshToken`
3. 前端后续请求统一携带 `Authorization: Bearer <accessToken>`
4. 令牌过期后由前端自动调用 `/api/auth/refresh`

### 3.2 OpenAPI 链路

1. 请求进入 `/open/api/**`
2. 过滤器缓存原始 body
3. 拦截器校验签名头、时间戳、应用身份、授权范围和 `nonce`
4. 校验通过后进入业务处理

## 4. 当前边界

- 前端后台目前仍以静态路由为主
- API 管理 / 在线调试页面当前为占位页
- 数据权限表仍保留，但不代表当前已经完整启用运行时规则
