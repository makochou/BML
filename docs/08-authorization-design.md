# 认证与安全设计

## 1. 登录认证

当前采用：

- 用户名密码登录
- `accessToken + refreshToken`
- Redis 会话缓存

## 2. 令牌规则

- Access Token 用于访问业务接口
- Refresh Token 用于续期登录态
- 前端只在收到 401 时尝试刷新一次

## 3. OpenAPI 安全规则

- 必须携带 `appKey`、`timestamp`、`nonce`、`sign`
- 签名串包含 `method`、`path`、`query`、`bodySha256`
- `nonce` 使用 Redis 做限时防重放
- 账号状态、授权范围、签名结果全部必须通过后端校验

## 4. 当前收口要求

- Secret 加密存储
- Secret 不常驻返回
- CORS 通过白名单控制
- Actuator 仅暴露必要端点
