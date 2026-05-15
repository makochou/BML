# Implementation Plan: 主题设置重设计（Theme Settings Redesign）

## Overview

本实施计划基于 `requirements.md` 与 `design.md`，将"中台管理 / 业务系统"双作用域主题引擎拆分为可增量交付的代码任务。
后端基于 `bml-module-system` 子包 `theme/`，前端基于 `bml-frontend` 的 `composables` / `components/theme` / `store` / `utils` / `styles`。
任务编排原则：

- 先底层数据与 Token，再服务与组件，最后整合与文档；
- 测试任务作为子任务（标记 `*` 表示可选），属性测试覆盖设计文档第 10 条 Correctness Properties；
- 不引入 alpha/beta/rc 版本，所有依赖在父 `pom.xml` / `package.json` 顶层声明。

## Tasks

- [x] 1. 后端 - 数据库迁移与种子数据
  - [x] 1.1 创建 Flyway 迁移脚本 `V2.16.0__init_theme.sql`
    - 在 `bml-app/src/main/resources/db/migration/` 下创建 `V2.16.0__init_theme.sql`
    - 编写 `bml_theme_preset` 与 `bml_theme_user_setting` 两张表的 DDL，字段、索引、唯一键 (`uk_user_scope`) 与注释严格遵循设计文档 Data Models 章节
    - 插入 `PRESET_BEST` 与至少 1 条备选内置预设（如 `PRESET_OCEAN`）的种子数据，`is_built_in=1`、`PRESET_BEST.is_default=1`
    - `profile_admin` / `profile_business` 列必须为合法 JSON 且通过 WCAG AA 对比度
    - _Requirements: 7.5, 7.6, 2.1, 2.2, 2.6_

  - [x] 1.2 编写 Flyway 迁移集成测试（Testcontainers MariaDB）
    - 在 `bml-module-system` 测试包下新增 `ThemeMigrationIT`，使用 Testcontainers 启动干净 MariaDB
    - 断言迁移成功、`PRESET_BEST` 行存在且 `is_default=1`、`uk_user_scope` 唯一约束生效
    - _Requirements: 7.5, 7.6, 13.3_

- [x] 2. 后端 - 共享枚举与错误码
  - [x] 2.1 创建主题枚举类
    - 新增 `com.bml.module.system.theme.enums` 包
    - 创建枚举：`ThemeScope`、`ThemeMode`、`SidebarStyle`、`HeaderStyle`、`RadiusStyle`、`Density`、`FontScale`
    - 每个枚举类与方法补充中文 Javadoc
    - _Requirements: 4.1, 10.1_

  - [x] 2.2 创建 `ThemeErrorCode` 实现 `ErrorCode`
    - 在 `enums` 包下创建 `ThemeErrorCode`，定义 `THEME_INVALID_PROFILE`、`THEME_SCOPE_INVALID`、`THEME_PRESET_NOT_FOUND`、`THEME_BUILTIN_NOT_MUTABLE`、`THEME_PERSIST_FAILED` 五个枚举项
    - 实现 `com.bml.core.common.result.ErrorCode` 接口（`getCode` / `getMessage`），编码遵循设计文档约定
    - _Requirements: 7.3, 7.4, 12.2_

  - [x] 2.3 编写 `ThemeErrorCodeTest` 单元测试
    - 断言每个错误码的 `code` 与 `message` 与设计文档一致，防止后续无意修改
    - _Requirements: 7.3, 7.4_

- [x] 3. 后端 - 实体、DTO、VO、常量
  - [x] 3.1 创建实体与 JSON POJO
    - 创建 `theme/entity/ThemePreset.java`、`theme/entity/ThemeUserSetting.java`，使用 `@TableName(autoResultMap=true)` 与 `JacksonTypeHandler`
    - 创建 `ThemeProfileJson` POJO（共享字段载体），字段与设计文档 ThemeProfile 表对齐
    - _Requirements: 4.1, 5.5, 12.5_

  - [x] 3.2 创建 DTO / VO
    - 创建 `theme/dto/ThemeProfileDTO.java`、`theme/dto/ThemePresetUpsertDTO.java`：使用 JSR-380 注解（`@NotNull`、`@Pattern`、自定义 `@HexColor` 后续步骤补全）
    - 创建 `theme/vo/ThemeProfileVO.java`、`theme/vo/ThemePresetVO.java`
    - _Requirements: 4.1, 7.4, 12.5_

  - [x] 3.3 创建 `ThemeConstants` 与自定义校验注解
    - 创建 `theme/constant/ThemeConstants.java`，定义 `PRESET_BEST_ID`、本地存储键名等常量
    - 创建 `theme/validator/HexColor.java` 注解 + 校验器，校验 `^#[0-9A-Fa-f]{6}$`
    - _Requirements: 4.8, 7.4_

- [x] 4. 后端 - Profile 校验器与 MapStruct 转换器
  - [x] 4.1 实现 `ThemeProfileValidator`
    - 在 `theme/validator/ThemeProfileValidator.java` 中实现"全量字段收集"语义：遍历所有字段后才决定是否抛出，确保非法字段全量返回
    - 校验失败时抛出 `BusinessException(THEME_INVALID_PROFILE, List<FieldError>)`
    - _Requirements: 4.8, 7.4_

  - [x] 4.2 编写 `ThemeProfileValidator` 属性测试
    - **Property 6: 非法字段全量返回 (P_VALIDATE_ALL)**
    - **Validates: Requirements 4.8, 7.4**
    - 使用 jqwik 随机生成包含 1..N 个非法字段的 Profile，断言错误响应 `data` 字段长度 = 注入的非法字段数量
    - _Requirements: 4.8, 7.4_

  - [x] 4.3 实现 `ThemeProfileConverter`（MapStruct）
    - 创建 `theme/converter/ThemeProfileConverter.java`，提供 Entity↔DTO↔VO 互转
    - 处理 JSON 嵌套字段与枚举映射
    - _Requirements: 7.1, 7.2_

- [x] 5. 后端 - Mapper 层
  - [x] 5.1 创建 `ThemePresetMapper` 与 XML 映射
    - 继承 `BaseMapper<ThemePreset>`，提供 `selectByDefault()`、`selectAllOrderBySort()` 等方法
    - 必要时补充 XML（位于 `mapper/theme/`）
    - _Requirements: 7.1, 7.5_

  - [x] 5.2 创建 `ThemeUserSettingMapper` 与 XML 映射
    - 继承 `BaseMapper<ThemeUserSetting>`
    - 提供 `selectByUserAndScope(Long userId, String scope)` 与 `clearPresetRefByPresetId(String id)` 自定义 SQL
    - _Requirements: 5.1, 5.5, 12.4_

- [ ] 6. 后端 - Service 层
  - [x] 6.1 创建 `ThemeService` 接口与 `ThemeServiceImpl`
    - 实现 `getMyProfile(scope)`、`upsertMyProfile(scope, dto)`、`restoreToBest(scope)`、`listPresets()`、`getDefault(scope)`、`createPreset(dto)`、`updatePreset(id, dto)`、`deletePreset(id)`
    - `restoreToBest` 必须读取 `PRESET_BEST` 对应作用域变体后 upsert 到 `bml_theme_user_setting`
    - 删除自定义预设时调用 `clearPresetRefByPresetId` 解引用
    - 修改 / 删除 `is_built_in=true` 的预设抛出 `THEME_BUILTIN_NOT_MUTABLE`
    - 全部公开方法补充中文 Javadoc
    - _Requirements: 3.2, 3.3, 5.1, 5.3, 5.4, 7.1, 12.1, 12.2, 12.4_

  - [x] 6.2 编写 `ThemeServiceImplTest` 单元测试
    - 使用 Mockito 覆盖 `getMyProfile`（命中 / 未命中默认回退）、`upsertMyProfile`、`restoreToBest`、CRUD presets
    - _Requirements: 3.2, 5.1, 7.1, 12.1_

  - [-] 6.3 编写属性测试 - Restore 幂等
    - **Property 2: Restore 幂等 (P_RESTORE_IDEMPOTENT)**
    - **Validates: Requirements 3.2, 3.6**
    - 使用 jqwik 生成任意初始 Profile，连续调用 `restoreToBest(scope)` N (1..20) 次，断言最终 Profile 与单次调用结果逐字段相等且等于 `PRESET_BEST` 对应变体
    - _Requirements: 3.2, 3.6_

  - [x] 6.4 编写属性测试 - 内置预设不可变
    - **Property 4: 内置预设不可变 (P_BUILTIN_IMMUTABLE)**
    - **Validates: Requirements 2.2, 12.2**
    - 使用 jqwik 随机生成对内置预设的 PUT / DELETE 调用，断言全部抛出 `THEME_BUILTIN_NOT_MUTABLE` 且数据库行未变
    - _Requirements: 2.2, 12.2_

  - [-] 6.5 编写属性测试 - 预设删除解引用
    - **Property 5: 预设删除解引用 (P_PRESET_DEREF)**
    - **Validates: Requirements 12.4**
    - 使用 jqwik 生成 N 条引用同一自定义预设的 user setting，删除该预设后断言所有 `preset_ref=NULL` 且 `profile` 字段保持原值
    - _Requirements: 12.4_

- [ ] 7. 后端 - Controller 层
  - [ ] 7.1 实现 `ThemeController`
    - 创建 `theme/controller/ThemeController.java`，按设计文档接口表注册全部端点
    - `me`、`restore`、`presets` GET 仅要求已登录；`presets` 写入接口添加 `@PreAuthorize("@ss.hasPermi('system:theme:manage')")`
    - `default` 接口需放行未登录访问（按平台白名单机制配置）
    - 全部返回 `Result<T>`，请求体使用 `@Validated`
    - _Requirements: 7.1, 7.2, 7.7, 12.1, 2.4_

  - [~] 7.2 编写 `@WebMvcTest` 控制器测试
    - 验证全部接口返回 `Result<T>`、`@PreAuthorize` 注解存在、JSR-380 校验生效（非法 scope 返回 `THEME_SCOPE_INVALID`）
    - 模仿 `MonitorPermissionAnnotationTest` 写法
    - _Requirements: 7.1, 7.2, 7.4, 12.1_

- [ ] 8. 后端 - 默认接口白名单与权限菜单
  - [~] 8.1 配置 `/api/theme/default` 白名单
    - 在平台 Security 白名单（如 `application.yml` 或 SecurityConfig）中添加 `/api/theme/default`
    - 不影响其它鉴权接口
    - _Requirements: 2.4, 7.7_

  - [~] 8.2 新增菜单与权限迁移脚本 `V2.16.1__add_theme_manage_menu.sql`
    - 在该迁移中插入 `system:theme:manage` 权限及对应菜单项（位于"系统管理 → 主题管理"）
    - 默认仅授予超级管理员
    - _Requirements: 7.7, 12.1_

- [~] 9. 后端检查点 - 通过所有后端测试
  - 运行 `mvn -pl bml-modules/bml-module-system -am test`
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. 前端 - 类型与 Token 文件
  - [x] 10.1 创建 TypeScript 类型 `src/types/theme.ts`
    - 定义 `ThemeScope`、`ThemeMode`、`RadiusStyle`、`Density`、`SidebarStyle`、`HeaderStyle`、`FontScale`、`ThemeProfile`、`ThemePreset`、`ThemeError`、`ThemeBroadcastMessage`
    - 字段必须与后端 ThemeProfile 一一对应
    - 中文 JSDoc
    - _Requirements: 4.1, 10.2_

  - [x] 10.2 创建 `src/styles/tokens.scss` 与 `tokens.preset-best.scss`
    - `tokens.scss` 定义全局 CSS 自定义属性默认值（颜色、`--bml-radius-{sm,md,lg}`、`--bml-spacing-*`、`--bml-font-size-base`、`--bml-line-height-base`、`--bml-shadow-*`、`--arcoblue-{1..10}`）
    - `tokens.preset-best.scss` 导出 `PRESET_BEST` 亮 / 暗变体常量供 bootstrap 使用
    - 在主入口 `main.ts` 引入 `tokens.scss`
    - _Requirements: 1.2, 4.3, 4.7, 6.5, 8.7_

- [x] 11. 前端 - 主题引擎工具
  - [x] 11.1 实现 `src/utils/themeEngine.ts`
    - 迁移并扩展原 `utils/theme.ts` 的 `generatePalette` / `hexToRgb`
    - 实现 `applyTokens(profile, scope, root?)`：写入全部 `--bml-*` / `--arcoblue-*`、设置 `<body>` 属性 (`arco-theme`、`data-bml-density` 等)、过渡 class 加移、未知 Token 开发态告警
    - 实现 `subscribeAutoMode(callback)` 处理 `prefers-color-scheme`
    - 中文 JSDoc
    - _Requirements: 1.1, 1.2, 1.6, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 8.5, 11.4_

  - [x] 11.2 编写 `themeEngine.test.ts` 单元测试
    - 覆盖色阶生成、Token 写入、过渡 class 加移、AUTO 订阅 / 取消订阅
    - _Requirements: 1.1, 4.2, 4.3, 11.4_

  - [x] 11.3 编写属性测试 - AUTO 模式跟随
    - **Property 8: AUTO 模式跟随 (P_AUTO_FOLLOW)**
    - **Validates: Requirements 4.2**
    - 使用 fast-check 在 jsdom 中模拟 `matchMedia` 切换，断言 `mode=AUTO` 时 `body[arco-theme]` 同步切换；其它模式下系统切换不产生副作用
    - _Requirements: 4.2_

  - [x] 11.4 编写属性测试 - WCAG AA 对比度
    - **Property 10: WCAG AA 对比度 (P_CONTRAST)**
    - **Validates: Requirements 2.6, 11.3**
    - 使用 fast-check 对 `PRESET_BEST` 亮 / 暗变体计算正文与大号文本对比度，断言 ≥ 4.5:1 / 3:1
    - _Requirements: 2.6, 11.3_

- [x] 12. 前端 - Broadcast 与 Bootstrap
  - [x] 12.1 实现 `src/utils/themeBroadcast.ts`
    - 包装 `BroadcastChannel('bml-theme-sync')`，定义 `senderId` 与消息类型
    - 暴露 `publish(msg)` / `subscribe(handler)` / `dispose()`，自动忽略自身消息
    - _Requirements: 6.2_

  - [x] 12.2 实现 `src/utils/themeBootstrap.ts` 与 Vite 注入
    - 编写编译为 IIFE 的引导脚本：读取 `localStorage`，命中则注入 Token，否则回退到 `__BML_PRESET_BEST__`
    - 配置 Vite 插件在 `index.html` `<head>` 内联注入；构建期通过 `tokens.preset-best.scss` 派生 `__BML_PRESET_BEST__`
    - _Requirements: 6.3, 6.5, 13.2_

  - [x] 12.3 编写属性测试 - 跨标签同步收敛
    - **Property 7: 跨标签同步收敛 (P_BROADCAST_CONVERGE)**
    - **Validates: Requirements 6.2**
    - 使用 fast-check 在测试中模拟 N 个 `themeBroadcast` 通道，任一发起 `updateProfile`，1 秒内其它通道的 store 与 DOM Token 与发起方一致
    - _Requirements: 6.2_

  - [x] 12.4 编写属性测试 - 首屏无 FOUC
    - **Property 9: 首屏无 FOUC (P_NO_FOUC)**
    - **Validates: Requirements 6.3, 6.5**
    - 在 jsdom 中执行 bootstrap 脚本：分别测试 `localStorage` 命中 / 缺失 / 损坏三种场景，断言 `getComputedStyle(body).getPropertyValue('--bml-color-primary')` 在 `DOMContentLoaded` 之前已为合法颜色，且缺失场景下等于 `PRESET_BEST` 对应主色
    - _Requirements: 6.3, 6.5, 13.2_

- [ ] 13. 前端 - Pinia 主题 Store
  - [x] 13.1 实现 `src/store/theme.ts`
    - 定义 `ThemeStoreState`、`actions`：`hydrate(scope)`、`patch(scope, partial)`、`applyPreset(scope, id)`、`restore(scope)`、`fetchPresets()`、`onBroadcast(msg)`
    - 所有写操作内部调用 `themeEngine.applyTokens` + 写 `localStorage` + `themeBroadcast.publish`
    - Admin / Business 状态独立，命名空间互不读取
    - _Requirements: 1.3, 1.4, 5.2, 5.7, 6.1, 8.6_

  - [x] 13.2 迁移 `store/app.ts` 中的主题字段
    - 保留 `app.ts` 的非主题字段（`menuCollapse`、`navbar` 等）
    - 删除主题相关字段，业务页面改为 `useThemeStore()`
    - _Requirements: 8.6, 8.7_

  - [-] 13.3 编写 `themeStore.test.ts` 单元测试
    - 覆盖 `patch` / `applyPreset` / `restore` 行为，验证三方写入
    - _Requirements: 5.2, 8.6_

  - [x] 13.4 编写属性测试 - 作用域隔离
    - **Property 1: 作用域隔离 (P_ISOLATION)**
    - **Validates: Requirements 1.3, 1.4, 5.7**
    - 使用 fast-check 生成 Profile 序列 `A_admin → A_business`，先后应用，断言 `themeStore.admin` 与 `localStorage['bml-theme-admin']` 在 business 写入后完全不变；反向同
    - _Requirements: 1.3, 1.4, 5.7_

- [ ] 14. 前端 - API 与 useTheme Composable
  - [x] 14.1 实现 `src/api/theme.ts`
    - 使用项目既有 axios 实例，封装 `getPresets`、`getMyProfile(scope)`、`updateMyProfile(scope, dto)`、`restore(scope)`、`getDefault(scope)`、`createPreset(dto)`、`updatePreset(id, dto)`、`deletePreset(id)`
    - 错误响应中 `data` 字段为 `FieldError[]` 时正确反序列化
    - _Requirements: 7.1, 7.2, 7.3_

  - [x] 14.2 实现 `src/composables/useTheme.ts`
    - 暴露 `profile` / `presets` / `isLoading` / `error` / `updateProfile` / `applyPreset` / `restoreDefault` / `scope`
    - `updateProfile`：本地 patch → 写 `localStorage` → broadcast → 后台 `PUT` → 服务端结果回填覆盖本地
    - 后台失败时仍保留本地更改并通过 `Message.warning` 与 `error` 暴露
    - 不传入 `scope` 时按当前路由布局推断（Admin 嵌 Business 时取外层 Admin）；无法识别时抛 `THEME_SCOPE_UNRESOLVED`
    - 全局 `try/catch` + 错误上报通道
    - 中文 JSDoc
    - _Requirements: 3.5, 5.6, 8.1, 8.8, 13.1, 13.4_

  - [-] 14.3 编写 `useTheme.test.ts` 单元测试
    - Mock axios + localStorage + BroadcastChannel；覆盖 `updateProfile` / `applyPreset` / `restoreDefault` 流程及失败降级
    - _Requirements: 5.6, 8.1, 13.1_

  - [ ] 14.4 编写属性测试 - 持久化三方一致
    - **Property 3: 持久化三方一致 (P_DUAL_WRITE)**
    - **Validates: Requirements 5.1, 5.2, 5.4**
    - 使用 fast-check 生成任意合法 partial Profile，调用 `updateProfile(p)` 后断言 `themeStore[scope]` / `localStorage['bml-theme-{scope}']` 反序列化对象 / 服务端返回三者结构等价
    - _Requirements: 5.1, 5.2, 5.4_

- [~] 15. 前端检查点 - 通过引擎与 store 测试
  - 运行 `npm run test --run` （仅运行已实现测试）
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 16. 前端 - 通用主题组件
  - [x] 16.1 实现 `ThemePresetCard.vue`
    - 渲染预设缩略（侧栏 + 顶栏 + 内容区四色块），点击触发 `applyPreset`
    - Props：`preset: ThemePreset`、`active?: boolean`
    - _Requirements: 8.3_

  - [x] 16.2 实现 `ThemeRestoreButton.vue`
    - 内嵌 `a-popconfirm` 二次确认；调用 `useTheme().restoreDefault()`
    - 捕获错误并显示 `Message.warning`
    - _Requirements: 3.1, 3.5, 8.4_

  - [x] 16.3 实现 7 个 Section 子组件
    - 在 `components/theme/` 下创建 `ThemeColorSection.vue`、`ThemeModeSection.vue`、`ThemeRadiusSection.vue`、`ThemeDensitySection.vue`、`ThemeSidebarSection.vue`、`ThemeHeaderSection.vue`、`ThemeFontSection.vue`
    - 每个 Section 以 `useTheme(scope)` 读取 / 更新对应字段，提供实时预览
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 8.2, 11.5_

  - [x] 16.4 实现 `ThemeSettingsPanel.vue`
    - 保留原抽屉布局，组合上述 7 个 Section + 预设网格 (`ThemePresetCard`) + `ThemeRestoreButton`
    - Props：`scope: ThemeScope`、`compact?: boolean`
    - 面板自身样式以 `PRESET_BEST` 为基底，便于即时预览
    - 字段非法时高亮 `FieldError` 提示
    - _Requirements: 4.8, 8.2, 11.5, 11.6_

  - [~] 16.5 删除旧 `components/ThemeSettings.vue`
    - 确认无引用后移除
    - _Requirements: 8.6_

  - [~] 16.6 编写 `ThemeSettingsPanel.test.ts`
    - 验证 7 个 Section 渲染、字段更新触发 `updateProfile`、字段错误高亮
    - _Requirements: 4.8, 8.2_

- [ ] 17. 前端 - Admin / Business 布局集成
  - [~] 17.1 在 Admin 布局挂载 `ThemeSettingsPanel`
    - 在 Admin 顶部栏入口处挂载 `<ThemeSettingsPanel scope="ADMIN" />`
    - 启动时调用 `useTheme('ADMIN')` 完成 hydrate
    - _Requirements: 1.3, 8.2, 8.6_

  - [~] 17.2 在 Business 布局挂载 `ThemeSettingsPanel`
    - 在 Business 顶部栏入口处挂载 `<ThemeSettingsPanel scope="BUSINESS" />`
    - 启动时调用 `useTheme('BUSINESS')` 完成 hydrate
    - _Requirements: 1.4, 8.2, 8.6_

  - [~] 17.3 应用启动时初始化主题
    - 在 `main.ts` 中（Vue 实例挂载前）调用 `themeStore.fetchPresets()` 与按当前路由作用域执行 `hydrate(scope)`
    - 注册 `themeBroadcast.subscribe(themeStore.onBroadcast)`
    - _Requirements: 5.3, 5.4, 5.6, 6.2_

  - [~] 17.4 业务组件硬编码主题样式扫描与替换
    - 全局搜索 `ThemeSettings.vue` 之外的硬编码颜色、圆角，替换为 `var(--bml-*)`
    - 对未定义 Token 引用补充默认值或退回 `PRESET_BEST` Token
    - _Requirements: 1.1, 1.2, 1.6_

- [ ] 18. 文档 - 中文教程
  - [~] 18.1 编写 `docs/theme-engine-guide.md`
    - 章节：架构概览（含设计 Mermaid 图）、Token 体系与命名规范、`useTheme` 使用（双作用域示例）、组件接入（Panel / PresetCard / RestoreButton）、扩展新 Preset 步骤、扩展新维度步骤、主题维度对照表、常见问题（FOUC、跨标签、对比度）
    - 至少 3 个可运行示例：应用预设、恢复默认、自定义新颜色 Token，每段附文件路径
    - _Requirements: 10.3, 10.4, 10.5, 10.6_

- [~] 19. 最终检查点 - 全量测试与构建
  - 运行 `mvn -pl bml-modules/bml-module-system -am test`
  - 运行 `npm run test --run` 与 `npm run build`
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- 标记 `*` 的子任务为可选测试任务，可在 MVP 阶段跳过；非测试任务（包括迁移、API、组件、集成）必须实现。
- 每条任务在 `_Requirements:` 中精确引用具体子需求条款；属性测试任务额外通过 `**Validates: Requirements X.Y**` 锚定设计文档第 10 章中的 Property。
- 所有新增依赖必须先在父 `pom.xml` 或前端 `package.json` 顶层声明再被子模块引用；禁止 alpha/beta/rc 版本（参见 R9）。
- 检查点用于在阶段交付前确认现有测试通过，便于增量验证。

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "10.1", "10.2"] },
    { "id": 1, "tasks": ["1.2", "2.2", "3.1", "11.1", "12.1"] },
    { "id": 2, "tasks": ["2.3", "3.2", "3.3", "5.1", "5.2", "11.2", "11.3", "11.4", "12.2"] },
    { "id": 3, "tasks": ["4.1", "4.3", "13.1", "14.1", "16.1", "16.2"] },
    { "id": 4, "tasks": ["4.2", "6.1", "13.2", "14.2", "16.3", "12.3", "12.4"] },
    { "id": 5, "tasks": ["6.2", "6.3", "6.4", "6.5", "7.1", "13.3", "13.4", "14.3", "14.4", "16.4"] },
    { "id": 6, "tasks": ["7.2", "8.1", "8.2", "16.5", "16.6", "17.1", "17.2"] },
    { "id": 7, "tasks": ["17.3", "17.4", "18.1"] }
  ]
}
```
