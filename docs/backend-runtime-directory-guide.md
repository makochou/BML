# BML 后端运行目录与 License/logs 存储规范

## 1. 背景

BML 后端是 Maven 多模块工程，后端根目录为 `bml-backend`，实际 Spring Boot 启动模块为 `bml-app`。

历史配置中，日志目录 `./logs` 和许可证目录 `License` 都是相对路径。当开发人员从 `bml-backend` 根目录启动后端时，运行期文件会生成到：

```text
bml-backend/logs
bml-backend/License
```

这会造成运行期文件散落在后端根目录，不利于部署、备份、清理和后续维护。

当前规范统一要求：**后端运行期文件默认全部生成并存储到 `bml-app` 目录下**。

```text
bml-backend/bml-app/logs
bml-backend/bml-app/License
```

## 2. 统一运行目录工具

运行目录解析由核心框架工具类统一负责：

```text
bml-backend/bml-core/bml-core-framework/src/main/java/com/bml/core/framework/runtime/BmlRuntimePaths.java
```

核心规则如下：

1. 如果 JVM 参数显式指定 `-Dbml.runtime.dir=/path/to/bml-app`，优先使用该目录。
2. 如果当前启动目录名是 `bml-app`，使用当前目录作为运行目录。
3. 如果当前启动目录下存在 `bml-app` 子目录，使用该子目录作为运行目录。
4. 其他场景使用当前工作目录，兼容 Jar 包独立部署。

## 3. 日志目录规范

日志配置文件：

```text
bml-backend/bml-app/src/main/resources/logback-spring.xml
```

日志目录通过系统属性 `bml.runtime.dir` 解析：

```xml
<property name="LOG_HOME" value="${bml.runtime.dir:-.}/logs"/>
```

默认输出：

```text
bml-backend/bml-app/logs/bml-backend.log
bml-backend/bml-app/logs/bml-backend-error.log
bml-backend/bml-app/logs/archive/
```

## 4. License 目录规范

许可证配置文件：

```text
bml-backend/bml-app/src/main/resources/application.yml
```

默认配置：

```yaml
bml:
  license:
    storage-dir: ${BML_LICENSE_STORAGE_DIR:License}
    file-name: bml-license.lic
```

`storage-dir` 如果是相对路径，会通过 `BmlRuntimePaths.resolveRuntimePath` 统一解析到运行目录下，因此默认落盘位置为：

```text
bml-backend/bml-app/License/bml-license.lic
```

如果生产环境确实需要把许可证放到外部挂载目录，可以使用绝对路径覆盖：

```bash
-Dbml.runtime.dir=/opt/bml/bml-app
```

或：

```bash
BML_LICENSE_STORAGE_DIR=/data/bml/license
```

注意：`BML_LICENSE_STORAGE_DIR` 配置为绝对路径时，会直接使用该绝对路径，不再拼接 `bml-app`。

## 5. 启动方式建议

### 5.1 Maven 本地启动

推荐从后端根目录启动：

```bash
mvn -pl bml-app spring-boot:run
```

运行目录会自动识别为：

```text
bml-backend/bml-app
```

### 5.2 IDE 启动

IDE 直接运行 `com.bml.app.BmlApplication` 即可。

如果 IDE 工作目录是 `bml-backend`，系统会自动识别 `bml-app` 子目录。

如果 IDE 工作目录是 `bml-app`，系统会直接使用当前目录。

### 5.3 Jar 包部署

Jar 包独立部署时，建议显式指定运行目录：

```bash
java -Dbml.runtime.dir=/opt/bml/bml-app -jar bml-app.jar
```

此时日志和许可证默认目录为：

```text
/opt/bml/bml-app/logs
/opt/bml/bml-app/License
```

## 6. 旧目录迁移说明

如果历史环境已生成以下目录：

```text
bml-backend/logs
bml-backend/License
```

处理方式：

1. 将 `bml-backend/License` 中的许可证文件迁移到 `bml-backend/bml-app/License`。
2. 旧日志如需保留，可手动备份到运维归档目录；如无需保留，可直接删除 `bml-backend/logs`。
3. 确认后端启动后只在 `bml-app/logs` 和 `bml-app/License` 下生成运行期文件。

## 7. 后续开发约定

1. 新增本地文件存储功能时，不允许直接使用 `Path.of("相对路径")` 或 `Paths.get("相对路径")` 作为最终落盘目录。
2. 通用运行期文件应优先使用 `BmlRuntimePaths.resolveRuntimePath(path)`。
3. 如果配置支持绝对路径，绝对路径必须原样生效，方便生产环境挂载外部磁盘。
4. 子模块新增依赖时不得写版本号，版本必须统一放到父 `pom.xml` 的 `dependencyManagement` 或 `properties` 中。
5. 接口返回继续使用项目统一响应对象 `Result<T>` 和分页对象 `PageResult<T>`，禁止新增不一致的响应结构。

## 8. 验证清单

完成路径调整后建议执行：

```bash
mvn -f bml-backend/pom.xml -DskipTests compile
mvn -f bml-backend/pom.xml -pl bml-core/bml-core-framework test
```

并检查：

```text
bml-backend/License     不再生成
bml-backend/logs        不再生成
bml-backend/bml-app/License  正常生成
bml-backend/bml-app/logs     正常生成
```
