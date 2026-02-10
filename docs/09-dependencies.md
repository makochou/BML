# BML-Backend 依赖版本管理

> **版本**: v2.0 | **项目**: bml-backend | **日期**: 2026-02-09

---

## 父POM统一版本管理

所有依赖版本通过父POM的 `<dependencyManagement>` 统一管理，子模块只声明依赖不指定版本。

---

## 核心依赖版本

### 基础框架

| 依赖 | 版本 | 说明 |
|------|------|------|
| Java | 21 LTS | 虚拟线程支持 |
| Spring Boot | 3.2.3 | 核心框架 |
| Spring Security | 6.2.2 | 安全框架 |
| Spring Validation | 3.2.3 | 参数校验 |

### 持久层

| 依赖 | 版本 | 说明 |
|------|------|------|
| MyBatis-Plus | 3.5.5 | ORM增强 |
| MariaDB Connector/J | 3.3.2 | 数据库驱动 |
| HikariCP | 5.1.0 | 连接池 |
| ShardingSphere-JDBC | 5.4.1 | 分库分表 |
| Flyway | 10.8.1 | 数据库迁移 |

### 缓存

| 依赖 | 版本 | 说明 |
|------|------|------|
| Spring Data Redis | 3.2.3 | Redis集成 |
| Lettuce | 6.3.1 | Redis客户端 |
| Redisson | 3.27.0 | 分布式锁 |

### 工具库

| 依赖 | 版本 | 说明 |
|------|------|------|
| Hutool | 5.8.26 | 工具集合 |
| Guava | 33.0.0-jre | Google工具库 |
| MapStruct | 1.5.5.Final | 对象映射 |
| Lombok | 1.18.30 | 代码简化 |

### 安全

| 依赖 | 版本 | 说明 |
|------|------|------|
| JJWT | 0.12.5 | JWT处理 |
| Bouncy Castle | 1.77 | 加密扩展 |

### API文档

| 依赖 | 版本 | 说明 |
|------|------|------|
| SpringDoc OpenAPI | 2.3.0 | OpenAPI 3.1 |

### 日志

| 依赖 | 版本 | 说明 |
|------|------|------|
| SLF4J | 2.0.12 | 日志门面 |
| Logback | 1.4.14 | 日志实现 |

### JSON

| 依赖 | 版本 | 说明 |
|------|------|------|
| Jackson | 2.16.1 | JSON处理 |

### 测试

| 依赖 | 版本 | 说明 |
|------|------|------|
| JUnit Jupiter | 5.10.2 | 单元测试 |
| Mockito | 5.10.0 | Mock框架 |
| Testcontainers | 1.19.5 | 容器化测试 |

---

## 前端依赖版本 (API管理中台)

| 依赖 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.19 | 前端框架 |
| Vite | 5.1.3 | 构建工具 |
| Element Plus | 2.5.6 | UI组件库 |
| Vue Router | 4.2.5 | 路由管理 |
| Pinia | 2.1.7 | 状态管理 |
| Axios | 1.6.7 | HTTP客户端 |
| Monaco Editor | 0.46.0 | 代码编辑器 |
| ECharts | 5.5.0 | 图表库 |
| TypeScript | 5.3.3 | 类型支持 |

---

## 父POM配置示例

```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

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
    <bouncycastle.version>1.77</bouncycastle.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Spring Boot BOM -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- ShardingSphere -->
        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>shardingsphere-jdbc</artifactId>
            <version>${shardingsphere.version}</version>
        </dependency>

        <!-- Flyway -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
            <version>${flyway.version}</version>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-mysql</artifactId>
            <version>${flyway.version}</version>
        </dependency>

        <!-- MariaDB -->
        <dependency>
            <groupId>org.mariadb.jdbc</groupId>
            <artifactId>mariadb-java-client</artifactId>
            <version>${mariadb.version}</version>
        </dependency>

        <!-- Redisson -->
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
            <version>${redisson.version}</version>
        </dependency>

        <!-- Hutool -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>

        <!-- Guava -->
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>${guava.version}</version>
        </dependency>

        <!-- SpringDoc -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- JJWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Bouncy Castle -->
        <dependency>
            <groupId>org.bouncycastle</groupId>
            <artifactId>bcprov-jdk18on</artifactId>
            <version>${bouncycastle.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```
