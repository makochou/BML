# 技术规范

## 1. 编码规范

- 文本文件统一 UTF-8
- 换行默认 LF，Windows 脚本保留 CRLF
- 根目录 `.editorconfig` 与 `.gitattributes` 为唯一规范入口

## 2. Java 规范

- DTO / VO / Entity 职责分离
- Controller 不直出敏感实体
- 统一使用 `Result<T>` 返回
- 业务异常统一使用 `BusinessException`

## 3. 前端规范

- 请求层统一在 `src/utils/request.ts`
- 统一读取 `message`
- 统一处理 401 刷新令牌
- 错误场景保留 `traceId`

## 4. SQL 与迁移规范

- 结构变更统一通过 Flyway 管理
- 已执行迁移禁止直接修改历史语义
- 变更必须通过空库启动验收脚本验证

## 5. 构建规范

- 后端版本管理以父 POM 为准
- 前端依赖锁定以 `package-lock.json` 为准
- 合并前至少执行后端测试与前端构建
