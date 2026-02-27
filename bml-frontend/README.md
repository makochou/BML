# BML Frontend

## 项目概览

`bml-frontend` 是当前 BML 的管理端前端，基于 `Vue 3 + TypeScript + Vite + Pinia + Arco Design` 构建。

当前可用页面主要包括：

- 登录页
- 首页仪表盘
- 应用管理页
- 通知与监控展示

尚未闭环的 API 管理 / 在线调试入口，当前以占位页形式下线，避免误用。

## 环境要求

- Node.js 20+
- npm 10+

## 本地开发

```bash
npm ci
npm run dev
```

默认后端代理地址：

- `VITE_API_BASE_URL=/api`

## 生产构建

```bash
npm run build
```

## 测试命令

```bash
npm run test
```

## 当前前端收口点

- 登录后统一保存 `accessToken + refreshToken`
- 请求层已接入 401 自动刷新一次
- 后端统一读取 `message` 字段
- 错误链路支持打印 `traceId`，便于联调排查
- 应用管理页不再常驻展示 Secret，只在创建或重置时一次性展示
- 未闭环的 API 管理 / 在线调试入口当前为占位页
