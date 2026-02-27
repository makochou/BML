# Maven 构建治理规范

## 1. 目标

本规范用于统一 BML 后端的 Maven 治理方式，解决以下问题：

- 依赖版本分散在多个子模块，升级成本高
- 插件版本和编译参数缺少统一入口
- 子模块重复声明内部模块版本，增加维护噪声
- 构建规范、测试参数和基线校验无法统一执行

## 2. 当前原则

### 2.1 父 POM 是唯一版本入口

所有框架、依赖、构建插件版本都必须在父 POM 统一管理：

- `bml-backend/pom.xml`

子模块只表达“依赖什么”，不再表达“依赖哪个版本”。

### 2.2 依赖版本统一放在 `properties`

当前父 POM 已集中管理：

- Spring Boot
- MyBatis-Plus
- Flyway
- MariaDB Driver
- Redisson
- Hutool
- MapStruct
- Guava
- SpringDoc
- JJWT
- Bouncy Castle
- OSHI
- Maven 插件版本

### 2.3 依赖统一放在 `dependencyManagement`

适合进入 `dependencyManagement` 的依赖：

- 多个模块共用的三方依赖
- 所有内部模块依赖
- 明确需要锁定版本的基础设施依赖

### 2.4 插件统一放在 `pluginManagement`

适合进入 `pluginManagement` 的内容：

- 插件版本号
- 通用默认配置
- 未来可能被多个模块复用的构建插件

## 3. 当前已落地的治理项

- 编译参数统一：Java 21、UTF-8、`parameters=true`
- 测试参数统一：`surefire.argLine`
- 版本治理统一：父 POM `properties`
- 依赖治理统一：父 POM `dependencyManagement`
- 插件治理统一：父 POM `pluginManagement`
- 基线校验统一：`maven-enforcer-plugin`

## 4. 子模块编写规范

子模块 `pom.xml` 必须遵守：

- 不重复声明内部模块版本号
- 不重复声明三方依赖版本号
- 不重复声明公共插件版本号
- 不重复声明公共编译参数
- 只保留当前模块真实需要的依赖和插件

## 5. 新增依赖流程

新增依赖时严格按以下顺序执行：

1. 先确认仓库内是否已有等价能力
2. 在父 POM `properties` 增加版本号
3. 在父 POM `dependencyManagement` 增加依赖
4. 在具体子模块中只写 `groupId` 和 `artifactId`
5. 执行全量构建验证

## 6. 新增插件流程

新增构建插件时严格按以下顺序执行：

1. 在父 POM `properties` 增加插件版本
2. 在父 POM `pluginManagement` 增加插件定义
3. 判断是否需要全局执行
4. 若全局执行，放入父 POM `build/plugins`
5. 若仅单模块执行，只在对应模块声明坐标，不写版本号

## 7. 常用命令

### 7.1 全量构建

```powershell
cd D:\workspace\BML\bml-backend
mvn clean install
```

### 7.2 构建单模块并自动带上依赖模块

```powershell
mvn -pl bml-app -am clean install
```

### 7.3 仅执行测试

```powershell
mvn test
```

### 7.4 检查子模块是否私自声明版本

```powershell
rg -n "<version>\\$\\{project.version\\}</version>" D:\workspace\BML\bml-backend -g pom.xml
```

## 8. 维护要求

- 版本升级优先从父 POM 入手
- 不允许在子模块临时“补一个版本”解决问题
- 构建治理文档与父 POM 注释必须同步维护
