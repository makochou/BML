# 空库启动验收说明

## 1. 目标

本说明用于验证后端能否在“全新空库 + 独立端口 + 真实 Flyway 迁移”的条件下正常启动。

验收脚本：

- `bml-backend/scripts/validate-empty-startup.ps1`

## 2. 脚本职责

脚本会按顺序完成以下动作：

1. 打包 `bml-app`
2. 创建一个全新的临时数据库
3. 注入独立的数据库、Redis、端口环境变量
4. 启动后端 JAR
5. 轮询管理端口 `/actuator/health`
6. 校验核心表是否创建成功
7. 校验历史 `bml_api_*` 表是否已经清理

## 3. 运行前准备

- 本机已安装 `mysql`
- 本机已安装 `mvn`
- 本机已安装 `java`
- MariaDB 与 Redis 处于可访问状态
- 数据库账号具备建库权限

## 4. 使用示例

```powershell
cd D:\workspace\BML\bml-backend

.\scripts\validate-empty-startup.ps1 `
  -DbHost 127.0.0.1 `
  -DbPort 3306 `
  -DbName bml_system_empty_check `
  -DbUser root `
  -DbPassword your_password `
  -RedisHost 127.0.0.1 `
  -RedisPort 6379
```

## 5. 成功标准

脚本结束后至少满足以下条件：

- 管理端口健康检查返回 `UP`
- `flyway_schema_history` 存在
- `sys_user`、`sys_role`、`sys_menu`、`sys_api_account`、`sys_api_registry`、`sys_api_permission`、`sys_alert` 存在
- `bml_api_group`、`bml_api_info`、`bml_api_app`、`bml_api_access` 不存在

## 6. 失败排查

常见失败原因如下：

- 数据库密码未配置：检查 `-DbPassword`
- Redis 不可达：检查主机、端口与密码
- 端口冲突：更换 `-ServerPort` 与 `-ManagementPort`
- Flyway 失败：查看脚本输出的 `app.stdout.log` 与 `app.stderr.log`

## 7. 维护要求

- 任何新增 Flyway 迁移，都要确认脚本仍可通过
- 任何新增核心表，都要把校验逻辑补进脚本
- 若启动依赖新增环境变量，必须同步补到脚本和文档中
