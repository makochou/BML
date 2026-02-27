# BML Backend

## 项目概览

`bml-backend` 是当前 BML 项目的后端主仓库，基于 `Spring Boot 3 + MyBatis-Plus + MariaDB + Redis` 构建，当前已经落地的核心能力包括：

- 认证中心：用户名密码登录、`accessToken + refreshToken`、Redis 会话管理
- 系统管理：用户、角色、菜单、部门、监控告警
- OpenAPI 安全链路：签名校验、`nonce` 防重放、接口级授权
- 工程基线：Flyway 迁移、统一响应、统一异常、父 POM 版本治理

## 环境要求

- JDK 21
- Maven 3.9+
- MariaDB 10.6+
- Redis 6+

## 配置方式

后端当前只保留一份配置文件：

- `bml-app/src/main/resources/application.yml`

环境差异全部通过环境变量覆盖，不再拆分 `application-local.yml` 或 `application-prod.yml`。敏感配置不允许直接提交到仓库，请优先参考：

```bash
copy .env.example .env
```

关键环境变量如下：

- `BML_DB_URL`
- `BML_DB_USERNAME`
- `BML_DB_PASSWORD`
- `BML_REDIS_HOST`
- `BML_REDIS_PORT`
- `BML_REDIS_PASSWORD`
- `BML_JWT_SECRET`
- `BML_OPENAPI_SECRET_ENCRYPTION_KEY`

## 本地启动

1. 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS `bml_system`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;
```

2. 配置数据库与 Redis 环境变量
3. 启动应用

```bash
mvn -pl bml-app spring-boot:run
```

也可以直接运行启动类：

- `com.bml.app.BmlApplication`

## 数据迁移

应用启动时会自动执行 Flyway：

- 系统基础表
- API 账号相关表
- 告警表
- 安全收口迁移 `V1.3.0__security_hardening.sql`

如需验证空库启动，请使用：

- `bml-backend/scripts/validate-empty-startup.ps1`

详细说明见 [10-empty-db-validation.md](../docs/10-empty-db-validation.md)。

## 接口说明

- 业务接口根路径：`/api`
- 管理端口默认：`18080`
- Actuator 默认仅暴露 `health`、`info`

Swagger 默认关闭，如需在本地临时打开：

```bash
set BML_SWAGGER_ENABLED=true
```

访问地址：

- `http://localhost:8080/api/swagger-ui/index.html`

## 当前安全约束

- OpenAPI 签名串固定为 `appKey + timestamp + nonce + method + path + canonicalQuery + bodySha256`
- `nonce` 通过 Redis 做 5 分钟防重放
- API Secret 不通过列表接口和详情接口常驻返回
- `sys_api_*` 是当前唯一保留的 API 数据模型

## 验证命令

```bash
mvn -q clean test
```
