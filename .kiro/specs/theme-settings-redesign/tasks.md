# Implementation Plan: 主题设置重设计（Theme Settings Redesign）

## Overview

本实施计划基于 `requirements.md` 与 `design.md`，将"中台管理 / 业务系统"双作用域主题引擎拆分为可增量交付的代码任务。
后端基于 `bml-module-system` 子包 `theme/`，前端基于 `bml-frontend` 的 `composables` / `components/theme` / `store` / `utils` / `styles`。
任务编排原则：

- 先底层数据与 Token，再服务与组件，最后整合与文档；
- 测试任务作为子任务（标记 `*` 表示可选），属性测试覆盖设计文档第 10 条 Correctness Properties；
- 不引入 alpha/beta/rc 版本，所有依赖在父 `pom.xml` / `package.json` 顶层声明。

---

## Tasks

- [x] 1. 后端 - 数据库迁移与种子数据
  - [x] 1.1 创建 Flyway 迁移脚本 `V2.16.0__init_theme.sql`
    - 创建 `bml_theme_preset` 与 `bml_theme_user_setting` 表
    - 插入 `PRESET_BEST` 与备选预设种子数据
    - _Requirements: 7.5, 7.6_
  - [x] 1.2 编写 Flyway 迁移集成测试（Testcontainers MariaDB）
    - 验证迁移脚本在干净库上成功执行
    - 验证 PRESET_BEST 种子存在、唯一约束生效
    - _Requirements: 7.5, 7.6_

- [x] 2. 后端 - 共享枚举与错误码
  - [x] 2.1 创建主题枚举类
    - 创建 `ThemeScope`、`ThemeMode`、`SidebarStyle`、`HeaderStyle`、`RadiusStyle`、`Density`、`FontScale` 枚举
    - _Requirements: 4.1_
  - [x] 2.2 创建 `ThemeErrorCode` 实现 `ErrorCode`
    - 实现 `THEME_INVALID_PROFILE`、`THEME_SCOPE_INVALID`、`THEME_PRESET_NOT_FOUND`、`THEME_BUILTIN_NOT_MUTABLE`、`THEME_PERSIST_FAILED`
    - _Requirements: 7.3_
  - [x] 2.3 编写 `ThemeErrorCodeTest` 单元测试
    - 验证错误码与消息稳定性
    - _Requirements: 7.3_

- [x] 3. 后端 - 实体、DTO、VO、常量
  - [x] 3.1 创建实体与 JSON POJO
    - 创建 `ThemePreset`、`ThemeUserSetting`、`ThemeProfileJson` 实体
    - _Requirements: 5.5, 12.5_
  - [x] 3.2 创建 DTO / VO
    - 创建 `ThemeProfileDTO`、`ThemePresetUpsertDTO`、`ThemeProfileVO`、`ThemePresetVO`
    - _Requirements: 7.1, 7.2_
  - [x] 3.3 创建 `ThemeConstants` 与自定义校验注解
    - 创建 `ThemeConstants`（含 `PRESET_BEST_ID`）与 `@HexColor` 注解
    - _Requirements: 4.8, 7.4_

- [x] 4. 后端 - Profile 校验器与 MapStruct 转换器
  - [x] 4.1 实现 `ThemeProfileValidator`
    - 校验颜色格式、枚举合法性、全量字段错误收集
    - _Requirements: 4.8, 7.4_
  - [x]* 4.2 编写 `ThemeProfileValidator` 属性测试
    - **Property 6: 非法字段全量返回（P_VALIDATE_ALL）**
    - **Validates: Requirements 4.8, 7.4**
  - [x] 4.3 实现 `ThemeProfileConverter`（MapStruct）
    - DTO ↔ Entity ↔ VO 转换
    - _Requirements: 7.1_

- [x] 5. 后端 - Mapper 层
  - [x] 5.1 创建 `ThemePresetMapper` 与 XML 映射
    - 实现预设 CRUD 与排序查询
    - _Requirements: 7.1, 12.1_
  - [x] 5.2 创建 `ThemeUserSettingMapper` 与 XML 映射
    - 实现用户设置 upsert 与按 userId+scope 查询
    - _Requirements: 5.1, 5.5_

- [x] 6. 后端 - Service 层
  - [x] 6.1 创建 `ThemeService` 接口与 `ThemeServiceImpl`
    - 实现 getMyProfile、upsertMyProfile、restoreToBest、CRUD presets、内置预设保护、删除解引用
    - _Requirements: 3.2, 5.1, 7.1, 12.1, 12.4_
  - [x]* 6.2 编写 `ThemeServiceImplTest` 单元测试
    - 覆盖全部 service 方法的正常与异常路径
    - _Requirements: 3.2, 5.1, 7.1, 12.1_
  - [x]* 6.3 编写属性测试 - Restore 幂等
    - **Property 2: Restore 幂等（P_RESTORE_IDEMPOTENT）**
    - **Validates: Requirements 3.2, 3.6**
  - [x]* 6.4 编写属性测试 - 内置预设不可变
    - **Property 4: 内置预设不可变（P_BUILTIN_IMMUTABLE）**
    - **Validates: Requirements 2.2, 12.2**
  - [x]* 6.5 编写属性测试 - 预设删除解引用
    - **Property 5: 预设删除解引用（P_PRESET_DEREF）**
    - **Validates: Requirements 12.4**

- [x] 7. 后端 - Controller 层
  - [x] 7.1 实现 `ThemeController`
    - 实现全部 REST 接口，使用 `Result<T>` 包装
    - _Requirements: 7.1, 7.2, 7.7_
  - [x]* 7.2 编写 `@WebMvcTest` 控制器测试
    - 验证接口返回格式、异常处理、权限注解
    - _Requirements: 7.1, 7.3, 7.4_

- [x] 8. 后端 - 默认接口白名单与权限菜单
  - [x] 8.1 配置 `/api/theme/default` 白名单
    - 将公开接口加入安全白名单
    - _Requirements: 7.1_
  - [x] 8.2 新增菜单与权限迁移脚本 `V2.16.1__add_theme_manage_menu.sql`
    - 添加 `system:theme:manage` 权限与菜单项
    - _Requirements: 7.7, 12.1_

- [x] 9. 后端检查点 - 通过所有后端测试
  - ✅ `mvn -pl bml-modules/bml-module-system -am test` 通过（含 jqwik 属性测试 340 tries）
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. 前端 - 类型与 Token 文件
  - [x] 10.1 创建 TypeScript 类型 `src/types/theme.ts`
    - 定义 `ThemeProfile`、`ThemePreset`、`ThemeScope` 等类型
    - _Requirements: 4.1, 8.1_
  - [x] 10.2 创建 `src/styles/tokens.scss` 与 `tokens.preset-best.scss`
    - 定义全局 Token CSS 变量默认值与 PRESET_BEST 常量
    - _Requirements: 2.1, 8.7_

- [x] 11. 前端 - 主题引擎工具
  - [x] 11.1 实现 `src/utils/themeEngine.ts`
    - 实现 `applyTokens`、色阶生成、AUTO 模式订阅、过渡动画
    - _Requirements: 1.1, 1.2, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 6.1, 8.5_
  - [x]* 11.2 编写 `themeEngine.test.ts` 单元测试
    - 测试色阶生成、Token 写入、过渡 class、AUTO 模式切换
    - _Requirements: 4.2, 4.3_
  - [x]* 11.3 编写属性测试 - AUTO 模式跟随
    - **Property 8: AUTO 模式跟随（P_AUTO_FOLLOW）**
    - **Validates: Requirements 4.2**
  - [x]* 11.4 编写属性测试 - WCAG AA 对比度
    - **Property 10: WCAG AA 对比度（P_CONTRAST）**
    - **Validates: Requirements 2.6, 11.3**

- [x] 12. 前端 - Broadcast 与 Bootstrap
  - [x] 12.1 实现 `src/utils/themeBroadcast.ts`
    - 封装 `BroadcastChannel('bml-theme-sync')` 与消息类型
    - _Requirements: 6.2_
  - [x] 12.2 实现 `src/utils/themeBootstrap.ts` 与 Vite 注入
    - 编写同步 IIFE 脚本，构建期内联到 `<head>`
    - _Requirements: 6.3, 6.5_
  - [x]* 12.3 编写属性测试 - 跨标签同步收敛
    - **Property 7: 跨标签同步收敛（P_BROADCAST_CONVERGE）**
    - **Validates: Requirements 6.2**
  - [x]* 12.4 编写属性测试 - 首屏无 FOUC
    - **Property 9: 首屏无 FOUC（P_NO_FOUC）**
    - **Validates: Requirements 6.3, 6.5**

- [x] 13. 前端 - Pinia 主题 Store
  - [x] 13.1 实现 `src/store/theme.ts`
    - 创建 `themeStore`，包含 admin/business 双 namespace 状态与 actions
    - _Requirements: 5.2, 5.7, 8.1_
  - [x] 13.2 迁移 `store/app.ts` 中的主题字段
    - 将主题相关字段移至 `themeStore`，保留非主题字段
    - _Requirements: 8.1_
  - [x]* 13.3 编写 `themeStore.test.ts` 单元测试
    - 测试作用域隔离、状态独立
    - _Requirements: 1.3, 1.4, 5.7_
  - [x]* 13.4 编写属性测试 - 作用域隔离
    - **Property 1: 作用域隔离（P_ISOLATION）**
    - **Validates: Requirements 1.3, 1.4, 5.7**

- [x] 14. 前端 - API 与 useTheme Composable
  - [x] 14.1 实现 `src/api/theme.ts`
    - 封装全部主题 API 调用（axios）
    - _Requirements: 7.1_
  - [x] 14.2 实现 `src/composables/useTheme.ts`
    - 实现 `UseThemeReturn` 接口，含 updateProfile、applyPreset、restoreDefault
    - _Requirements: 8.1, 8.8_
  - [x]* 14.3 编写 `useTheme.test.ts` 单元测试
    - Mock axios/localStorage/BroadcastChannel，测试完整流程
    - _Requirements: 8.1, 13.1_
  - [x]* 14.4 编写属性测试 - 持久化三方一致
    - **Property 3: 持久化三方一致（P_DUAL_WRITE）**
    - **Validates: Requirements 5.1, 5.2, 5.4**

- [x] 15. 前端检查点 - 通过引擎与 store 测试
  - 运行 `npm run test --run` （仅运行已实现测试）
  - ✅ 13 test files, 118 tests passed (2026-05-15)
  - Ensure all tests pass, ask the user if questions arise.

- [x] 16. 前端 - 通用主题组件
  - [x] 16.1 实现 `ThemePresetCard.vue`
    - 渲染预设缩略图与名称，点击触发 applyPreset
    - _Requirements: 8.3_
  - [x] 16.2 实现 `ThemeRestoreButton.vue`
    - 封装确认弹窗、API 调用、错误提示
    - _Requirements: 3.1, 8.4_
  - [x] 16.3 实现 7 个 Section 子组件
    - ThemeColorSection / ThemeModeSection / ThemeRadiusSection / ThemeDensitySection / ThemeSidebarSection / ThemeHeaderSection / ThemeFontSection
    - _Requirements: 4.1, 8.2, 11.5_
  - [x] 16.4 实现 `ThemeSettingsPanel.vue`
    - 组合所有 section + 预设网格 + 恢复默认按钮
    - _Requirements: 8.2, 8.6_
  - [x] 16.5 删除旧 `components/ThemeSettings.vue`
    - 移除旧实现，确保无残留引用
    - _Requirements: 8.2_
  - [x]* 16.6 编写 `ThemeSettingsPanel.test.ts`
    - 测试渲染各 section、回调 dispatch
    - _Requirements: 8.2_

- [x] 17. 前端 - Admin / Business 布局集成
  - [x] 17.1 在 Admin 布局挂载 `ThemeSettingsPanel`
    - 集成到 Admin 布局侧边抽屉
    - _Requirements: 8.6_
  - [x] 17.2 在 Business 布局挂载 `ThemeSettingsPanel`
    - 集成到 Business 布局侧边抽屉
    - _Requirements: 8.6_
  - [x] 17.3 应用启动时初始化主题
    - 在 main.ts 中调用 themeStore.hydrate
    - _Requirements: 2.3, 5.3, 6.3_
  - [x] 17.4 业务组件硬编码主题样式扫描与替换
    - ✅ `Layout.vue`：背景色、文本色、边框色、分割线等替换为 `var(--bml-*)`
    - ✅ `BusinessLayout.vue`：同上
    - ✅ `TagsView.vue`：标签文本色、关闭按钮色、暗色模式适配
    - ✅ `Workplace.vue`（中台工作台）：统计卡片、面板标题、监控指标、活动日志
    - ✅ `Home.vue`（业务工作台）：标题色、描述色、步骤卡片
    - 注：登录页（`Login.vue`/`BusinessLogin.vue`）和许可证页（`license/index.vue`）为独立装饰性设计，不跟随主题变化，保留原值
    - _Requirements: 1.1, 1.2, 1.6_

- [x] 18. 文档 - 中文教程
  - [x] 18.1 编写 `docs/theme-engine-guide.md`
    - 包含架构概览、Token 体系、useTheme 使用、组件接入示例、扩展步骤、维度对照表、常见问题
    - 至少 3 个可运行代码示例
    - _Requirements: 10.3, 10.4, 10.5, 10.6_

- [x] 19. 最终检查点 - 全量测试与构建
  - ✅ `mvn -pl bml-modules/bml-module-system -am test` — 通过
  - ✅ `npm run test` — 13 文件 118 测试通过
  - ✅ `npx vite build` — 构建成功
  - 注：`vue-tsc` 有 2 个预先存在的类型警告（非本特性引入），不影响运行时
  - Ensure all tests pass, ask the user if questions arise.

---

## Notes

- 标记 `*` 的子任务为可选测试任务，可在 MVP 阶段跳过；非测试任务（包括迁移、API、组件、集成）必须实现。
- 每条任务在 `_Requirements:` 中精确引用具体子需求条款；属性测试任务额外通过 `**Validates: Requirements X.Y**` 锚定设计文档第 10 章中的 Property。
- 所有新增依赖必须先在父 `pom.xml` 或前端 `package.json` 顶层声明再被子模块引用；禁止 alpha/beta/rc 版本（参见 R9）。
- 检查点用于在阶段交付前确认现有测试通过，便于增量验证。

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "2.2"] },
    { "id": 1, "tasks": ["1.2", "2.3", "3.1", "3.2", "3.3"] },
    { "id": 2, "tasks": ["4.1", "4.3", "5.1", "5.2"] },
    { "id": 3, "tasks": ["4.2", "6.1"] },
    { "id": 4, "tasks": ["6.2", "6.3", "6.4", "6.5", "7.1"] },
    { "id": 5, "tasks": ["7.2", "8.1", "8.2"] },
    { "id": 6, "tasks": ["10.1", "10.2"] },
    { "id": 7, "tasks": ["11.1", "12.1", "12.2"] },
    { "id": 8, "tasks": ["11.2", "11.3", "11.4", "12.3", "12.4", "13.1"] },
    { "id": 9, "tasks": ["13.2", "13.3", "13.4", "14.1"] },
    { "id": 10, "tasks": ["14.2"] },
    { "id": 11, "tasks": ["14.3", "14.4"] },
    { "id": 12, "tasks": ["16.1", "16.2", "16.3"] },
    { "id": 13, "tasks": ["16.4", "16.5"] },
    { "id": 14, "tasks": ["16.6", "17.1", "17.2", "17.3"] },
    { "id": 15, "tasks": ["17.4", "18.1"] }
  ]
}
```
