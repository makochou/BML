# BML Extensions 模块

## 模块说明

`bml-extensions` 是 BML 平台的扩展模块，用于存放不属于核心业务但对系统有增强作用的功能组件。

## 用途

- 第三方服务集成（如短信、邮件、OSS 存储等）
- 通用工具类扩展
- 中间件集成适配器

## 状态

当前为空模块，保留结构以备后续扩展使用。

## 目录结构（规划）

```
bml-extensions/
├── pom.xml
├── README.md
└── src/
    └── main/
        └── java/
            └── com/bml/extensions/
                ├── sms/        # 短信服务
                ├── email/      # 邮件服务
                └── oss/        # 对象存储
```
