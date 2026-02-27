# 当前依赖基线

## 1. 后端

| 类别 | 依赖 | 版本 |
| --- | --- | --- |
| 语言 | Java | 21 |
| 核心框架 | Spring Boot | 3.2.3 |
| 权限 | Spring Security | 6.x |
| ORM | MyBatis-Plus | 3.5.5 |
| 迁移 | Flyway | 10.8.1 |
| 数据库驱动 | MariaDB Connector/J | 3.3.2 |
| 缓存 | Redisson | 3.27.0 |
| 工具 | Hutool | 5.8.26 |
| 转换 | MapStruct | 1.5.5.Final |
| 文档 | SpringDoc | 2.3.0 |

## 2. 前端

| 类别 | 依赖 | 版本 |
| --- | --- | --- |
| 框架 | Vue | 3.5.25 |
| 构建 | Vite | 7.3.1 |
| 语言 | TypeScript | 5.9.3 |
| 状态管理 | Pinia | 3.0.4 |
| 路由 | Vue Router | 5.0.2 |
| 请求 | Axios | 1.13.5 |
| UI | Arco Design Vue | 2.57.0 |
| 图表 | ECharts | 6.0.0 |
| 测试 | Vitest | 3.2.4 |

## 3. 管理规则

- 后端版本统一由父 `pom.xml` 管理
- 前端版本统一由 `package.json` 与 `package-lock.json` 管理
- 版本升级前先确认是否影响统一响应、认证、OpenAPI 链路
