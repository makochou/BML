# BML-Backend 项目文档中心

> **企业管理系统后端** | Java 21 | Spring Boot 3.2.3 | MariaDB

---

## 📚 文档清单

| 序号 | 文档 | 说明 |
|------|------|------|
| 01 | [需求规格说明书](./01-requirements.md) | 功能需求、非功能需求、技术约束 |
| 02 | [系统架构设计](./02-system-architecture.md) | 整体架构、模块设计、技术决策 |
| 03 | [权限系统设计](./03-permission-design.md) | RBAC+ABAC混合模型、四层权限 |
| 04 | [数据库设计](./04-database-design.md) | ER图、表结构、索引策略 |
| 05 | [API管理中台设计](./05-api-portal-design.md) | 功能设计、UI规范、技术实现 |
| 06 | [技术规范](./06-technical-specification.md) | 编码规范、接口规范、错误码 |
| 07 | [开发阶段计划](./07-development-phases.md) | 6阶段开发计划、里程碑 |
| 08 | [授权与安全设计](./08-authorization-design.md) | License、JWT、加密方案 |
| 09 | [依赖版本管理](./09-dependencies.md) | 全部依赖版本清单 |

---

## 📁 SQL脚本

| 脚本 | 说明 |
|------|------|
| [V1.0.0__init_system_tables.sql](./sql/V1.0.0__init_system_tables.sql) | 系统库初始化脚本 |

---

## 🏗️ 项目架构

```
bml-backend/
├── bml-core/                    # 核心框架
│   ├── bml-core-framework/      # 框架代码
│   ├── bml-core-common/         # 通用工具
│   └── bml-core-base/           # 基础抽象
├── bml-modules/                 # 业务模块
│   └── bml-module-system/       # 系统管理
├── bml-extensions/              # 扩展插件
├── bml-customization/           # 客户定制
├── bml-app/                     # 启动应用
└── bml-api-portal/              # API管理中台
```

---

## ✨ 核心特性

| 特性 | 说明 |
|------|------|
| 🔐 四层权限 | 菜单 → 按钮 → 字段 → 数据 |
| 📋 License授权 | RSA签名 + 机器绑定 |
| 🗄️ 分库分表 | ShardingSphere透明分片 |
| 📝 操作日志 | @OperationLog注解 + 异步写入 |
| 🔑 JWT认证 | 双Token机制 |
| 🌐 API中台 | 目录 + 调试 + 账号管理 |

---

## 🚀 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- MariaDB 11+
- Redis 7+
- Node.js 20+ (API中台)

### 初始化步骤

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE bml_system DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p -e "CREATE DATABASE bml_business DEFAULT CHARACTER SET utf8mb4;"

# 2. 执行初始化脚本
mysql -u root -p bml_system < docs/sql/V1.0.0__init_system_tables.sql

# 3. 配置环境变量
export BML_DB_HOST=localhost
export BML_DB_PASSWORD=your_password
export BML_REDIS_HOST=localhost

# 4. 构建项目
mvn clean install -DskipTests

# 5. 启动应用
java -jar bml-app/target/bml-app.jar
```

### 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 超级管理员 |

---

## 📅 开发里程碑

| 里程碑 | 时间 | 交付物 |
|--------|------|--------|
| M1 | 第3周 | 基础框架 |
| M2 | 第7周 | 系统模块 |
| M3 | 第10周 | API中台 |
| M4 | 第11周 | 测试完成 |
| M5 | 第12周 | 正式上线 |

---

## 📞 技术支持

如有问题，请联系项目负责人。
