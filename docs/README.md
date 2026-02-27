# BML Docs

本目录只保留当前仓库真实落地、并且能够指导后续开发的文档。若文档与代码冲突，以代码、Flyway 迁移和测试结果为准。

## 当前发布范围

- 后端采用 `Spring Boot 3 + MyBatis-Plus` 的模块化单体架构
- 当前已交付模块为 `system` 与 `api`
- 认证链路采用 `JWT access token + refresh token + Redis 会话`
- OpenAPI 采用 `appKey + timestamp + nonce + method + path + canonicalQuery + bodySha256` 签名
- API Secret 只在创建或重置时一次性返回，数据库内加密存储
- 前端当前开放首页、应用管理页和通知监控能力

## 文档目录

| 编号 | 文件 | 说明 |
| --- | --- | --- |
| 01 | [01-requirements.md](./01-requirements.md) | 当前交付范围、验收边界与非目标 |
| 02 | [02-system-architecture.md](./02-system-architecture.md) | 当前系统架构、模块边界与主链路 |
| 03 | [03-permission-design.md](./03-permission-design.md) | 当前权限模型、权限码规范与边界 |
| 04 | [04-database-design.md](./04-database-design.md) | 当前数据库模型、核心表与迁移规范 |
| 05 | [05-api-portal-design.md](./05-api-portal-design.md) | API 账号与 OpenAPI 现状说明 |
| 06 | [06-technical-specification.md](./06-technical-specification.md) | 开发规范、命名规范、接口约定 |
| 07 | [07-development-phases.md](./07-development-phases.md) | 当前阶段状态与后续里程碑 |
| 08 | [08-authorization-design.md](./08-authorization-design.md) | 认证、安全与 OpenAPI 授权设计 |
| 09 | [09-dependencies.md](./09-dependencies.md) | 后端和前端依赖基线 |
| 10 | [10-empty-db-validation.md](./10-empty-db-validation.md) | 空库启动验收脚本与使用说明 |
| 11 | [11-maven-build-governance.md](./11-maven-build-governance.md) | 父 POM、版本治理与插件治理 |
| 12 | [12-unified-response-exception-spec.md](./12-unified-response-exception-spec.md) | 统一响应、统一异常、统一返回规范 |
| 13 | [13-encoding-and-comment-guidelines.md](./13-encoding-and-comment-guidelines.md) | 编码规范、中文注释规范与排查指南 |

## 文档使用原则

- 设计口径必须服务当前代码，而不是服务历史方案
- 任何新增功能都要先更新对应规范文档，再进入开发
- 任何涉及编码、乱码、注释、统一响应的变更，都必须同步更新本目录
