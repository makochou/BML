# 当前权限设计

## 1. 目标

当前权限体系以 RBAC 为主，先保证接口级授权稳定可用，再逐步演进到更细粒度的动态权限。

## 2. 当前已落地能力

- Spring Security + `@PreAuthorize` 接口级权限校验
- 菜单 / 按钮权限码规范
- 删除动作统一使用 `remove`
- OpenAPI 独立接口授权

## 3. 权限码规范

统一格式：

```text
module:resource:action
```

示例：

- `system:user:list`
- `system:user:add`
- `system:user:edit`
- `system:user:remove`

## 4. 当前边界

- 前端动态路由未正式接入
- 数据权限规则未完整落地
- ABAC 仍处于规划阶段

## 5. 开发要求

- 新增受保护接口前必须先定义权限码
- 控制器、初始化菜单、文档三处权限码必须一致
- 删除类动作禁止再使用 `delete` 命名
