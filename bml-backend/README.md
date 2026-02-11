# BML Backend Project

BML (Business Management L...) 是一个基于 **Spring Boot 3.2 + MyBatis-Plus + Vue 3** 的前后端分离后台管理系统。

## 🛠️ 技术栈

- **Core**: Spring Boot 3.2.3, Java 21
- **Database**: MariaDB 10.6+ / MySQL 8.0+
- **ORM**: MyBatis-Plus 3.5.3 + ShardingSphere 5.4.1
- **Cache**: Redis 6+ (Redisson 3.27.0)
- **Security**: Spring Security 6 + JJWT 0.12.5 (Stateless)
- **Tooling**: MapStruct, Hutool, Guava, Lombok
- **API Docs**: SpringDoc OpenApi 3 (Swagger 3)

## 📋 环境准备

在启动项目之前，请确保您的开发环境满足以下要求：

- **JDK**: OpenJDK 21+
- **Maven**: 3.8+
- **Database**: MariaDB 10.6+ 或 MySQL 8.0+ (创建数据库 `bml_system`)
- **Redis**: 6.0+ (默认端口 6379, 无密码或自行配置)
- **IDE**: IntelliJ IDEA (推荐)

## 🚀 快速开始

### 1. 克隆与导入
```bash
git clone <repository-url>
```
使用 IntelliJ IDEA 打开项目根目录 (`bml-backend`)，等待 Maven 依赖下载完成。

### 2. 数据库配置
1. 创建数据库：
   ```sql
   CREATE DATABASE IF NOT EXISTS `bml_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
   ```
2. 修改配置 (Optional)：
   打开 `bml-app/src/main/resources/application-dev.yml`，根据本地环境修改数据库和 Redis 连接信息：
   ```yaml
   spring:
     datasource:
       url: jdbc:mariadb://localhost:3306/bml_system...
       username: root
       password: your_password
     data:
       redis:
         host: localhost
         password: ""
   ```

### 3. 应用启动
项目整合了 **Flyway**，应用启动时会自动检测并执行 `src/main/resources/db/migration` 下的 SQL 脚本，完成表结构和初始化数据的创建。

**启动入口**: `com.bml.app.BmlApplication` (位于 `bml-app` 模块)

### 4. 验证运行
启动成功后，访问 Swagger UI 查看 API 文档：
- **地址**: [http://localhost:8080/api/swagger-ui/index.html](http://localhost:8080/api/swagger-ui/index.html)
- **账号**: `admin`
- **密码**: `admin123`

### 5. 登录测试
1. 在 Swagger 中找到 `/auth/login` 接口。
2. 输入 default credential (`admin` / `admin123`) 获取 Token。
3. 点击 "Authorize" 按钮，输入 `Bearer <your_token>` 进行认证。
4. 测试 `/system/user/list` 等受保护接口。

## 📂 模块说明

- **bml-backend** (Root)
  - `bml-core` (核心基础层)
    - `bml-core-common`: 通用常量、异常、Result 封装
    - `bml-core-base`: 基础实体、Controller、Service 抽象
    - `bml-core-framework`: 全局配置、Security、WebMvc、Jackson、Redis
  - `bml-modules` (业务模块层)
    - `bml-module-system`: 系统管理 (用户、角色、菜单、部门)
    - `bml-module-api`: 业务 API 示例
  - `bml-app` (启动入口)

## 🤝 贡献与规范
- 严格遵守 RESTful API 规范。
- 统一使用 `Result<T>` 响应结构。
- Service 层接口禁止使用 FQN。
- 实体与 DTO/VO 转换使用 MapStruct。

---
**Happy Coding!** 🚀
