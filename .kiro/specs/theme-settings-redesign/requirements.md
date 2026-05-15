# Requirements Document

## Introduction

本特性对 BML 平台中"中台管理（Admin）"与"业务系统（Business）"两套主题设置进行整体重新设计。
重新设计后的主题系统需要满足以下核心目标：

1. **全量覆盖**：主题切换需联动全部可视组件（按钮、侧边栏、标题栏、面包屑、卡片、表格、表单、弹窗、消息、滚动条、图标、链接、标签、菜单等）。
2. **顶级默认方案**：内置一套追求"最佳视觉效果"的默认主题预设（`PRESET_BEST`）；任意自定义状态下，用户均可一键"恢复默认"回到该方案。
3. **多维主题模型**：支持颜色方案、明/暗/跟随系统模式、圆角风格、紧凑度、侧边栏风格、字体大小档位等多个 Design Token 维度。
4. **作用域隔离 + 引擎复用**：中台管理与业务系统的主题作用域相互隔离，但底层主题引擎、组件、Hook、数据模型完全复用。
5. **通用化与可维护**：后端遵循统一响应/统一异常机制；前端基于 CSS 变量 + Design Token + 通用 `useTheme` Composable 实现；提供完整中文注释与开发者教程文档。
6. **极致体验**：切换无刷新即时生效、无 FOUC（白屏闪烁），本地预渲染 + 服务端跨设备同步。
7. **统一版本管理**：所有依赖（前后端）版本号统一在父 `pom.xml` 管理；选用最新、最常用、最适配的版本。

本特性涉及前端 `bml-frontend`、后端 `bml-backend`（多 Maven 模块），并通过 Flyway 引入新表与种子数据。

## Glossary

- **Theme_Engine**：主题引擎模块。负责读取 Design Token 并将其应用为 CSS 变量、`<body>` 属性、组件样式开关。前后端共享同一份 Token 数据模型。
- **Theme_Profile**：单个完整主题配置对象，包含全部主题维度（颜色、模式、圆角、紧凑度、侧边栏风格、字体档位等）。
- **Theme_Preset**：系统内置、不可删除的预设主题方案。包含 `PRESET_BEST` 等若干预设。
- **PRESET_BEST**：内置"最佳默认方案"。视觉方向：现代毛玻璃质感 + 柔和渐变 + 高对比可读性，包含完整亮色与暗色变体；为系统首次访问、未登录、未配置以及"恢复默认"动作的目标主题。
- **Theme_Scope**：主题作用域，枚举值为 `ADMIN`（中台管理）、`BUSINESS`（业务系统）。两个作用域的主题状态与持久化数据相互独立。
- **Design_Token**：Token 化的样式变量集合，包括语义色、状态色、尺寸、圆角、字号、间距等。最终被映射为 CSS 自定义属性（`--bml-*`、`--arcoblue-*` 等）。
- **Theme_Mode**：主题明暗模式，枚举值 `LIGHT`、`DARK`、`AUTO`（跟随系统 `prefers-color-scheme`）。
- **Sidebar_Style**：侧边栏风格，枚举值 `LIGHT`、`DARK`、`TRANSPARENT`、`PRIMARY`，并附带"折叠样式"开关。
- **Header_Style**：顶部标题栏/导航栏风格，枚举值 `LIGHT`、`DARK`、`PRIMARY`、`TRANSPARENT`。
- **Radius_Style**：圆角风格档位，枚举值 `SHARP`（直角）、`SMALL`、`MEDIUM`、`LARGE`。
- **Density**：紧凑度档位，枚举值 `COMPACT`、`DEFAULT`、`LOOSE`。
- **Font_Scale**：字体大小档位，枚举值 `SMALL`、`DEFAULT`、`LARGE`、`XLARGE`。
- **Theme_API**：后端主题相关 RESTful 接口集合，统一返回 `Result<T>`。
- **Theme_Repository**：主题数据持久化层，对应数据库表 `bml_theme_preset`、`bml_theme_user_setting`。
- **Theme_Composable**：前端通用主题组合式函数 `useTheme`，封装读取/应用/切换/恢复/订阅主题状态的能力。
- **Theme_Restore_Action**："恢复默认"动作。无条件将当前作用域的 `Theme_Profile` 重置为 `PRESET_BEST`。
- **FOUC**：Flash Of Unstyled Content，页面加载阶段出现的样式闪烁。本特性需消除主题相关的 FOUC。
- **Result_Wrapper**：后端统一响应体类型，对应 `com.bml.core.common.result.Result<T>`。
- **Unified_Exception_Handler**：后端统一异常处理器，将异常转换为统一的 `Result_Wrapper` 错误响应。
- **Tutorial_Document**：开发者教程文档，要求中文撰写，覆盖主题引擎使用方法、Token 扩展、组件接入示例与常见问题。

## Requirements

### Requirement 1: 主题作用范围与全量样式联动

**User Story:** 作为平台用户，我希望切换主题后，所有可视组件（按钮、侧边栏、标题栏、菜单、表格、表单、弹窗等）均同步变化，以获得统一一致的视觉体验。

#### Acceptance Criteria

1. WHEN 用户在中台管理或业务系统中切换 `Theme_Profile` 的任一维度，THE Theme_Engine SHALL 在不刷新页面的前提下同步更新以下组件类别的呈现：按钮（含 hover、active、disabled、focus 四态）、侧边栏（背景、文字、激活项、折叠态）、顶部标题栏与导航栏、面包屑、卡片、表格（表头、行、斑马纹、悬停、选中）、表单控件（输入框、选择器、单选、复选、开关、日期）、模态框、抽屉、消息提示（Message/Notification）、滚动条、图标、链接、标签、徽标、菜单。
2. THE Theme_Engine SHALL 通过 CSS 自定义属性（Design_Token）驱动所有组件样式，禁止任何业务组件使用主题相关的硬编码颜色、圆角或字号。
3. WHEN 用户在 Admin 作用域内切换 `Theme_Profile`，THE Theme_Engine SHALL 仅更新 Admin 作用域下页面的样式，并保持 Business 作用域的当前 `Theme_Profile` 不变。
4. WHEN 用户在 Business 作用域内切换 `Theme_Profile`，THE Theme_Engine SHALL 仅更新 Business 作用域下页面的样式，并保持 Admin 作用域的当前 `Theme_Profile` 不变。
5. THE Theme_Engine SHALL 对中台管理与业务系统提供完全相同的主题维度集合与 API 接口。
6. IF 业务页面引用了未在 Design_Token 中定义的颜色变量，THEN THE Theme_Engine SHALL 在开发模式下通过控制台输出告警信息，并在运行期回退到 `PRESET_BEST` 中对应的 Token 值。

### Requirement 2: 内置最佳默认方案 PRESET_BEST

**User Story:** 作为平台用户，我希望系统内置一套极致美观的默认主题方案，以便我开箱即用即可获得最优视觉体验。

#### Acceptance Criteria

1. THE Theme_Engine SHALL 内置一个名为 `PRESET_BEST` 的 `Theme_Preset`，覆盖颜色方案、`Theme_Mode`（同时提供亮色变体与暗色变体）、`Radius_Style`、`Density`、`Sidebar_Style`、`Header_Style` 与 `Font_Scale` 全部维度。
2. THE Theme_Engine SHALL 将 `PRESET_BEST` 标记为"系统预设"且不可被删除、不可被重命名、不可被用户编辑。
3. WHEN 用户首次访问平台或当前作用域无任何已保存 `Theme_Profile`，THE Theme_Engine SHALL 自动加载并应用 `PRESET_BEST` 对应作用域的变体。
4. WHEN 用户在登录前访问任意页面，THE Theme_Engine SHALL 应用 `PRESET_BEST`（亮色变体或按 `AUTO` 跟随系统决定）。
5. THE Theme_Preset 列表接口 SHALL 返回 `PRESET_BEST` 以及任意其它内置预设的完整 Token 数据，并以 `isDefault=true` 标识 `PRESET_BEST`。
6. THE Theme_Engine SHALL 保证 `PRESET_BEST` 在 `LIGHT` 与 `DARK` 模式下均通过 WCAG 2.1 AA 级别的文字与背景对比度要求（正文 ≥ 4.5:1，大号文本 ≥ 3:1）。

### Requirement 3: 恢复默认主题

**User Story:** 作为平台用户，我希望在做了任意自定义后能够一键恢复到最美观的默认方案，以便快速回到推荐外观。

#### Acceptance Criteria

1. THE 主题设置面板 SHALL 在 Admin 作用域与 Business 作用域中均提供一个名为"恢复默认"的按钮组件 `ThemeRestoreButton`。
2. WHEN 用户点击"恢复默认"按钮并完成确认，THE Theme_Engine SHALL 将当前作用域的 `Theme_Profile` 全部维度重置为 `PRESET_BEST` 对应作用域的变体。
3. WHEN `Theme_Restore_Action` 执行，THE Theme_API SHALL 调用 `POST /api/theme/restore?scope={ADMIN|BUSINESS}` 并返回更新后的 `Theme_Profile`，包装在 `Result_Wrapper` 中。
4. WHEN `Theme_Restore_Action` 执行成功，THE Theme_Engine SHALL 在 500 毫秒以内完成所有可视组件的样式重渲染，并向当前作用域所有打开的页面广播变更事件。
5. IF `Theme_Restore_Action` 执行过程中后端调用失败，THEN THE Theme_Engine SHALL 在前端先以本地缓存的 `PRESET_BEST` 应用主题，并提示用户"已在本地恢复默认主题，云端同步失败，请稍后重试"。
6. THE Theme_Engine SHALL 不需要用户额外提供任何参数即可执行 `Theme_Restore_Action`。

### Requirement 4: 多维度主题配置

**User Story:** 作为平台用户，我希望可以从颜色、明暗模式、圆角、紧凑度、侧边栏、字体大小等多个维度自定义主题，以便适配自己的使用习惯。

#### Acceptance Criteria

1. THE Theme_Profile SHALL 至少包含以下字段：`primaryColor`、`secondaryColor`、`accentColor`、`successColor`、`warningColor`、`errorColor`、`infoColor`、`textColor`、`backgroundColor`、`borderColor`、`mode`（`Theme_Mode`）、`radius`（`Radius_Style`）、`density`（`Density`）、`sidebarStyle`（`Sidebar_Style`）、`sidebarCollapsedStyle`、`headerStyle`（`Header_Style`）、`fontScale`（`Font_Scale`）。
2. WHEN 用户切换 `Theme_Mode` 为 `AUTO`，THE Theme_Engine SHALL 订阅浏览器 `prefers-color-scheme` 媒体查询，并在系统明暗模式变更时自动切换 `LIGHT` 或 `DARK` 变体。
3. WHEN 用户切换 `Radius_Style`，THE Theme_Engine SHALL 更新全局 `--bml-radius-sm`、`--bml-radius-md`、`--bml-radius-lg` 三个 Token 并使所有组件圆角同步变化。
4. WHEN 用户切换 `Density`，THE Theme_Engine SHALL 更新全局 `--bml-spacing-*` 与组件高度相关 Token，使表格行高、表单控件高度、按钮高度按比例缩放。
5. WHEN 用户切换 `Sidebar_Style`，THE Theme_Engine SHALL 同步更新侧边栏背景、文字、激活项、分组标题、悬停态、折叠态对应的 Token，且不影响其它区域。
6. WHEN 用户切换 `Header_Style`，THE Theme_Engine SHALL 同步更新顶部标题栏背景、文字、图标、用户菜单的 Token。
7. WHEN 用户切换 `Font_Scale`，THE Theme_Engine SHALL 更新 `--bml-font-size-base` Token，所有字号均通过相对单位（rem 或基于该 Token 的派生变量）派生。
8. IF 用户提交的 `Theme_Profile` 中任一字段非法（包含但不限于非 #RRGGBB 颜色值、非合法枚举、超出区间数值），THEN THE Theme_API SHALL 返回错误码 `THEME_INVALID_PROFILE`，并在错误响应中列出全部非法字段及原因。

### Requirement 5: 主题持久化与作用域隔离

**User Story:** 作为平台用户，我希望我的主题选择在本地以及不同设备登录后均能保留并保持作用域独立，以获得稳定个性化体验。

#### Acceptance Criteria

1. WHEN 已登录用户更新当前作用域的 `Theme_Profile`，THE Theme_API SHALL 通过 `PUT /api/theme/me?scope={ADMIN|BUSINESS}` 将该 `Theme_Profile` 持久化到 `bml_theme_user_setting` 表。
2. WHEN 用户切换 `Theme_Profile` 后，THE Theme_Engine SHALL 同步将该 `Theme_Profile` 写入浏览器本地存储（`localStorage`），键名按作用域区分：`bml-theme-admin`、`bml-theme-business`。
3. WHEN 已登录用户在新设备首次登录，THE Theme_Engine SHALL 通过 `GET /api/theme/me?scope={ADMIN|BUSINESS}` 拉取服务端保存的 `Theme_Profile` 并应用。
4. WHEN 服务端 `Theme_Profile` 与本地缓存冲突，THE Theme_Engine SHALL 以服务端版本为准，并刷新本地缓存。
5. THE Theme_Repository SHALL 为每个用户与每个作用域的组合保存且仅保存一条 `Theme_Profile` 记录，记录字段包括 `userId`、`scope`、Theme_Profile 全字段、`updatedAt`。
6. WHEN 未登录用户切换 `Theme_Profile`，THE Theme_Engine SHALL 仅写入本地存储，且 SHALL 在登录后将本地 `Theme_Profile` 上传至服务端覆盖同作用域的服务端记录。
7. THE Theme_Engine SHALL 保证 Admin 作用域的本地缓存与服务端记录与 Business 作用域完全隔离，互不读取、互不覆盖。

### Requirement 6: 无刷新即时生效与防闪烁

**User Story:** 作为平台用户，我希望切换主题与刷新页面时不会出现样式闪烁或白屏，以保证视觉流畅。

#### Acceptance Criteria

1. WHEN 用户在主题设置面板切换任一维度，THE Theme_Engine SHALL 在 100 毫秒以内完成 CSS 变量更新，且 SHALL 不触发任何整页路由重载。
2. WHEN 同一用户在多个浏览器标签页打开本平台，且其中一个标签页执行了主题切换或 `Theme_Restore_Action`，THE Theme_Engine SHALL 通过 `BroadcastChannel`（或等效跨标签通讯机制）使其它标签页在 1 秒以内同步应用新 `Theme_Profile`。
3. WHEN 浏览器加载平台首屏 HTML，THE 平台 SHALL 在 `<head>` 中通过同步内联脚本读取本地存储中的 `Theme_Profile` 并设置初始 CSS 变量与 `<body>` 主题属性，以避免 FOUC。
4. THE Theme_Engine SHALL 保证主题切换过渡动画的总时长不超过 300 毫秒，且 SHALL 允许用户在过渡动画进行期间继续与界面交互（不阻塞点击、输入或路由跳转）。
5. IF 本地存储中无可用 `Theme_Profile`，THEN THE 平台 SHALL 内联应用 `PRESET_BEST` 的初始 Token，避免首屏出现未上色状态。

### Requirement 7: 后端主题接口与统一响应

**User Story:** 作为后端开发者，我希望主题相关接口遵循统一响应、统一异常机制，以保持与平台其它模块一致。

#### Acceptance Criteria

1. THE Theme_API SHALL 提供以下接口，全部使用 `Result_Wrapper` 包装返回值：
   - `GET /api/theme/presets`：列出所有 `Theme_Preset`。
   - `GET /api/theme/me?scope={ADMIN|BUSINESS}`：查询当前用户当前作用域的 `Theme_Profile`。
   - `PUT /api/theme/me?scope={ADMIN|BUSINESS}`：更新当前用户当前作用域的 `Theme_Profile`。
   - `POST /api/theme/restore?scope={ADMIN|BUSINESS}`：恢复至 `PRESET_BEST`。
   - `GET /api/theme/default?scope={ADMIN|BUSINESS}`：返回 `PRESET_BEST` 对应作用域的 `Theme_Profile` 数据。
2. THE Theme_API SHALL 使用平台已存在的 `com.bml.core.common.result.Result<T>` 作为响应体类型，禁止裸返回业务对象。
3. WHEN Theme_API 处理过程中出现业务异常，THE Unified_Exception_Handler SHALL 将异常映射为 `Result_Wrapper` 错误响应，错误码与消息使用平台统一异常体系。
4. THE Theme_API SHALL 通过参数校验（JSR-380 注解）拒绝非法 `scope`、非法颜色格式与非法枚举值，并返回错误码 `THEME_INVALID_PROFILE`。
5. THE Theme_Repository SHALL 通过 Flyway 迁移脚本创建表结构，迁移文件版本号 SHALL 大于现有最高版本号 `V2.15.1`。
6. THE Flyway 迁移脚本 SHALL 在 `bml_theme_preset` 表中插入 `PRESET_BEST` 与至少 1 条其它备选预设作为初始种子数据。
7. THE Theme_API 控制器与服务实现 SHALL 放置在合适的 `bml-modules` 子模块（建议复用既有 `bml-module-system`），并 SHALL 注册到统一权限框架（已登录用户即可访问 `me`、`restore`、`presets`）。

### Requirement 8: 前端通用主题引擎与组件

**User Story:** 作为前端开发者，我希望主题逻辑被封装为通用 Hook 与通用组件，以便在 Admin 与 Business 任意页面中直接复用。

#### Acceptance Criteria

1. THE Theme_Composable `useTheme(scope)` SHALL 暴露以下能力：当前 `Theme_Profile`（响应式只读）、`updateProfile(partial)`、`applyPreset(presetId)`、`restoreDefault()`、`presets`（响应式列表）、`isLoading`、`error`。
2. THE 前端 SHALL 提供通用组件 `ThemeSettingsPanel`，包含颜色选择、模式选择、圆角档位、紧凑度、侧边栏风格、顶部栏风格、字体档位、预设方案、恢复默认按钮等子区块；通过 prop `scope` 控制作用域。
3. THE 前端 SHALL 提供通用组件 `ThemePresetCard` 用于展示单个 `Theme_Preset` 的预览缩略图与名称。
4. THE 前端 SHALL 提供通用组件 `ThemeRestoreButton`，封装确认弹窗、API 调用、错误提示与状态指示。
5. THE 前端 SHALL 提供通用工具 `applyTokens(tokens, root?)`，将 Design_Token 对象批量写入指定根元素的 CSS 自定义属性，并兼容 `body[arco-theme]` 属性切换。
6. THE 前端 SHALL 在 Admin 与 Business 两套布局中分别挂载 `ThemeSettingsPanel`，且共用同一份 Composable 与组件实现，不允许出现两份重复实现。
7. THE 前端 SHALL 将所有主题变量定义在统一的 Token 文件（`src/styles/tokens.scss` 或等效）并由主题引擎在运行时覆写。
8. IF 业务页面在不传入 `scope` 的情况下调用 `useTheme()`，THEN THE Theme_Composable SHALL 根据当前路由所处的布局自动推断 `Theme_Scope`；当页面同时处于 Admin 与 Business 嵌套上下文（例如 Admin 中嵌入 Business iframe）时 SHALL 以外层 Admin 作用域优先；当当前路由不属于 Admin 或 Business 任一被识别布局时 SHALL 立即抛出错误码 `THEME_SCOPE_UNRESOLVED`。

### Requirement 9: 版本管理与依赖规范

**User Story:** 作为研发负责人，我希望本特性新增的依赖统一在父 pom 管理且为最新最常用版本，以便长期维护。

#### Acceptance Criteria

1. THE 后端 SHALL 将本特性新增或升级的全部依赖（Spring Boot Starter、Validation、Jackson、Flyway、MyBatis-Plus 等）的版本号声明在父 `pom.xml` 的 `<properties>` 与 `<dependencyManagement>` 中，子模块 SHALL 不显式声明版本号。
2. THE 前端 SHALL 在 `package.json` 中选用截至开发期最新且广泛使用的稳定版本（Vue 3.x、TypeScript 5.x、Vite 5.x 或 6.x、Pinia 2.x、Arco Design Vue 当前稳定版），且不引入处于 alpha/beta/rc 阶段的版本。
3. WHERE 项目使用 Maven 前端构建插件（如 `frontend-maven-plugin`）或同类插件，THE 父 `pom.xml` SHALL 统一管理其版本号。
4. IF 出现需要新增的依赖在父 pom 中未声明版本，THEN 实施任务 SHALL 先在父 `pom.xml` 添加版本声明再在子模块引用。
5. THE 实施任务 SHALL 不允许向子模块或前端 `package.json` 引入与父 `pom.xml` 或现有锁文件冲突的版本。

### Requirement 10: 中文注释与开发者教程

**User Story:** 作为研发团队成员，我希望本特性的代码与文档使用规范中文注释并配套教程文档，以降低后续接入与维护成本。

#### Acceptance Criteria

1. THE 本特性新增或修改的全部 Java 类、方法、关键逻辑 SHALL 使用规范中文 Javadoc/注释，覆盖类用途、参数、返回值、异常、关键算法说明。
2. THE 本特性新增或修改的全部 TypeScript / Vue 文件 SHALL 使用规范中文 JSDoc/注释，覆盖模块用途、导出 API、参数、返回值与关键逻辑说明。
3. THE 实施任务 SHALL 在 `docs/`（或同等约定路径）下产出一份中文教程文档 `theme-engine-guide.md`，内容至少包含：架构概览、Token 体系、`useTheme` 使用、组件接入示例、扩展 Preset 的步骤、扩展新维度的步骤、常见问题。
4. THE 教程文档 SHALL 提供至少 3 个可运行的代码示例（应用预设、恢复默认、自定义新颜色 Token），并标注每段代码所在文件路径。
5. THE 教程文档 SHALL 包含一份"主题维度对照表"，列出每个 Design_Token 的语义、默认值、影响的组件清单。
6. THE 教程文档 SHALL 使用规范中文，避免出现机翻或不通顺表达。

### Requirement 11: 视觉质量与可访问性

**User Story:** 作为平台用户，我希望默认主题在视觉上达到顶级水准且对可访问性友好，以获得高级且舒适的使用感。

#### Acceptance Criteria

1. THE `PRESET_BEST` 亮色变体 SHALL 满足以下视觉特征：主色与渐变协调、卡片具有柔和阴影、关键组件使用毛玻璃或半透明叠层、页面整体留白比例不低于设计稿规定值（设计稿在设计阶段确定）。
2. THE `PRESET_BEST` 暗色变体 SHALL 提供与亮色变体在层级与对比度上等价的视觉表现，背景使用低饱和深色（接近 `#0E1116` 区间），主色亮度做对应提升。
3. THE Theme_Engine SHALL 保证任意主题方案下，正文文本与其背景的对比度不低于 WCAG 2.1 AA 级阈值（正文 4.5:1，大号文本 3:1）。
4. WHEN 用户切换 `Theme_Mode`、`Radius_Style`、`Sidebar_Style`、`Header_Style`，THE Theme_Engine SHALL 在过渡阶段使用 `transition` 而不是页面重新渲染，过渡时长不超过 300 毫秒。
5. THE 主题设置面板 SHALL 提供每个维度的实时预览，使用户在确认前即可看到效果。
6. THE 主题设置面板 SHALL 使用 `PRESET_BEST` 作为面板自身的样式基底，以确保设置入口本身即体现"最美观默认方案"。

### Requirement 12: 主题预设管理（CRUD）

**User Story:** 作为平台管理员，我希望能管理（增/删/改/查）平台级自定义预设方案，以便为团队提供统一的可选模板。

#### Acceptance Criteria

1. THE Theme_API SHALL 提供受保护接口（仅 Admin 作用域且具备 `system:theme:manage` 权限的用户可访问）：
   - `POST /api/theme/presets`：新增自定义 `Theme_Preset`。
   - `PUT /api/theme/presets/{id}`：修改自定义 `Theme_Preset`。
   - `DELETE /api/theme/presets/{id}`：删除自定义 `Theme_Preset`。
2. IF 调用方尝试修改或删除 `isBuiltIn=true` 的预设（包含 `PRESET_BEST`），THEN THE Theme_API SHALL 拒绝请求并返回错误码 `THEME_BUILTIN_NOT_MUTABLE`。
3. THE 自定义预设 SHALL 在 `GET /api/theme/presets` 返回结果中与内置预设一同返回，并通过 `isBuiltIn` 字段区分。
4. WHEN 自定义预设被删除，THE Theme_Engine SHALL 自动将所有当前 `Theme_Profile` 仍引用该预设的用户的 `presetRef` 字段置空，但保留其当前 `Theme_Profile` 的具体 Token 值。
5. THE Theme_Preset 字段集合 SHALL 与 `Theme_Profile` 一致，并附加 `id`、`name`、`description`、`isBuiltIn`、`isDefault`、`createdAt`、`updatedAt`。

### Requirement 13: 错误处理与降级

**User Story:** 作为平台用户，我希望即使后端或网络出现问题，主题功能也能优雅降级，不影响其它业务。

#### Acceptance Criteria

1. IF Theme_API 任一接口返回 5xx 错误或网络中断，THEN THE Theme_Engine SHALL 沿用本地缓存中的 `Theme_Profile`（若本地缓存损坏或缺失则使用 `PRESET_BEST` 兜底），并 SHALL 在右上角显示一次性提示"主题云端同步失败，已使用本地配置"，无论本地缓存是否可用均显示该提示。
2. IF 本地缓存中的 `Theme_Profile` 解析失败（例如版本不兼容），THEN THE Theme_Engine SHALL 丢弃该缓存并应用 `PRESET_BEST`。
3. IF Flyway 迁移脚本在启动阶段失败，THEN THE 后端 SHALL 完整终止启动（不允许进入只读模式或部分启动），并 SHALL 输出包含迁移版本号、失败 SQL 与异常堆栈的错误日志，避免使用不一致的 Theme_Repository 数据。
4. WHEN Theme_Composable 内部抛出未捕获错误，THE Theme_Engine SHALL 捕获并将错误信息写入控制台与平台前端错误上报通道，且 SHALL 不导致页面崩溃。
