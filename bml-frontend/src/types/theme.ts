/**
 * 主题引擎共享 TypeScript 类型定义。
 *
 * 本文件作为前端主题模块（`useTheme` Composable、`themeEngine`、`themeStore`、
 * `ThemeSettingsPanel` 等）的统一类型出口，与后端 `bml-module-system`
 * `theme` 子包中的枚举与 `ThemeProfileJson` POJO 保持字段一一对应：
 * 后端任一字段或枚举值变更，本文件 SHALL 同步更新。
 *
 * 字段对应关系详见 `.kiro/specs/theme-settings-redesign/design.md`
 * 中 “Data Models / 共享 ThemeProfile 字段” 章节。
 *
 * 关联需求：Requirements 4.1 / 10.2。
 */

/**
 * 主题作用域枚举。
 *
 * - `ADMIN`：中台管理（运维 / 平台管理员侧）。
 * - `BUSINESS`：业务系统（终端业务用户侧）。
 *
 * 两个作用域的运行时状态、本地缓存与服务端记录完全隔离，互不读取、互不覆盖。
 */
export type ThemeScope = 'ADMIN' | 'BUSINESS';

/**
 * 主题明暗模式。
 *
 * - `LIGHT`：强制亮色。
 * - `DARK`：强制暗色。
 * - `AUTO`：跟随浏览器 `prefers-color-scheme` 媒体查询。
 */
export type ThemeMode = 'LIGHT' | 'DARK' | 'AUTO';

/**
 * 圆角风格档位。
 *
 * - `SHARP`：直角（0px）。
 * - `SMALL` / `MEDIUM` / `LARGE`：依次递增的圆角档位，
 *   分别映射到 `--bml-radius-sm` / `--bml-radius-md` / `--bml-radius-lg`。
 */
export type RadiusStyle = 'SHARP' | 'SMALL' | 'MEDIUM' | 'LARGE';

/**
 * 紧凑度档位。
 *
 * 控制全局 `--bml-spacing-*` 与组件高度相关 Token，使表格行高、
 * 表单控件高度、按钮高度按比例缩放。
 */
export type Density = 'COMPACT' | 'DEFAULT' | 'LOOSE';

/**
 * 侧边栏风格枚举。
 *
 * 影响侧边栏背景、文字、激活项、悬停态等一组 Token；
 * 折叠状态下的样式由 {@link ThemeProfile.sidebarCollapsedStyle} 单独控制。
 */
export type SidebarStyle = 'LIGHT' | 'DARK' | 'TRANSPARENT' | 'PRIMARY';

/**
 * 侧边栏折叠态风格。
 *
 * 仅允许 `LIGHT` 或 `DARK`，与 {@link SidebarStyle} 解耦，
 * 便于实现 “展开使用主色 / 折叠使用深色” 等组合。
 */
export type SidebarCollapsedStyle = 'LIGHT' | 'DARK';

/**
 * 顶部标题栏 / 导航栏风格枚举。
 */
export type HeaderStyle = 'LIGHT' | 'DARK' | 'PRIMARY' | 'TRANSPARENT';

/**
 * 字体大小档位。
 *
 * 实际字号通过相对单位（`rem` 或基于 `--bml-font-size-base` 的派生变量）派生。
 */
export type FontScale = 'SMALL' | 'DEFAULT' | 'LARGE' | 'XLARGE';

/**
 * 十六进制颜色值字符串（形如 `#RRGGBB`）。
 *
 * 仅作为类型层面的语义标注；实际格式校验由后端
 * `@HexColor` 自定义校验注解与前端 `themeEngine` 在写入 Token
 * 之前完成。
 */
export type HexColor = string;

/**
 * 主题完整配置对象（`ThemeProfile`）。
 *
 * 字段集合与后端 `ThemeProfileJson` POJO 一一对应，
 * 任意字段缺失或多余都会导致校验失败（`THEME_INVALID_PROFILE`）。
 */
export interface ThemeProfile {
    /** 主色调，如 `#165DFF`。用于按钮、链接、激活态等关键交互。 */
    primaryColor: HexColor;
    /** 辅助色，用于次级强调与点缀。 */
    secondaryColor: HexColor;
    /** 高亮 / 强调色，用于徽标、标签、提醒类组件。 */
    accentColor: HexColor;
    /** 成功状态色（如成功提示、成功标签）。 */
    successColor: HexColor;
    /** 警告状态色。 */
    warningColor: HexColor;
    /** 错误状态色。 */
    errorColor: HexColor;
    /** 信息状态色。 */
    infoColor: HexColor;
    /** 正文主文字色。需满足 WCAG AA 对比度要求。 */
    textColor: HexColor;
    /** 全局背景色。 */
    backgroundColor: HexColor;
    /** 通用边框 / 分割线色。 */
    borderColor: HexColor;

    /** 明暗模式。 */
    mode: ThemeMode;
    /** 圆角风格档位。 */
    radius: RadiusStyle;
    /** 紧凑度档位。 */
    density: Density;
    /** 侧边栏展开态风格。 */
    sidebarStyle: SidebarStyle;
    /** 侧边栏折叠态风格。 */
    sidebarCollapsedStyle: SidebarCollapsedStyle;
    /** 顶部标题栏风格。 */
    headerStyle: HeaderStyle;
    /** 字体大小档位。 */
    fontScale: FontScale;

    /**
     * 引用的预设 ID。
     *
     * - 当前 Profile 由某个预设直接应用而来时取该预设 ID；
     * - 自定义修改后或预设被删除后，应被服务端解引用为 `null`
     *   （详见后端 `ThemeService` 的 `clearPresetRefByPresetId` 行为）。
     */
    presetRef: string | null;
}

/**
 * 主题预设（`ThemePreset`）。
 *
 * 对应数据库表 `bml_theme_preset` 的一行：
 * 同一预设需要同时提供 ADMIN / BUSINESS 两个作用域的变体，
 * 以便在不同作用域下应用同一预设时获得各自的最佳呈现。
 */
export interface ThemePreset {
    /** 预设 ID。内置预设使用语义化 ID（如 `PRESET_BEST`）。 */
    id: string;
    /** 预设展示名。 */
    name: string;
    /** 预设描述（可选）。 */
    description?: string;
    /** 是否为系统内置预设。内置预设不可被修改或删除。 */
    isBuiltIn: boolean;
    /**
     * 是否为系统默认预设。
     *
     * 全局有且仅有一个 `isDefault=true` 的预设（即 `PRESET_BEST`），
     * 用于首次访问、未登录及 “恢复默认” 动作的目标。
     */
    isDefault: boolean;
    /** 排序权重，列表按升序展示。 */
    sortOrder: number;
    /** ADMIN 作用域下的 Profile 变体。 */
    profileAdmin: ThemeProfile;
    /** BUSINESS 作用域下的 Profile 变体。 */
    profileBusiness: ThemeProfile;
    /** 创建时间（ISO 8601 字符串）。 */
    createdAt: string;
    /** 更新时间（ISO 8601 字符串）。 */
    updatedAt: string;
}

/**
 * 单字段校验错误。
 *
 * 与后端 `ThemeProfileValidator` 抛出的
 * `BusinessException(THEME_INVALID_PROFILE, List<FieldError>)` 中的
 * `FieldError` 结构对齐。
 */
export interface ThemeFieldError {
    /** 出错字段路径，如 `primaryColor`、`mode`。 */
    field: string;
    /** 机器可读的错误码，如 `INVALID_HEX_COLOR`、`INVALID_ENUM`。 */
    code: string;
    /** 人类可读的错误说明。 */
    message: string;
}

/**
 * 主题引擎错误对象。
 *
 * 由 `useTheme().error` 暴露，供 UI 高亮非法字段或在右上角显示提示。
 * 错误码与后端 `ThemeErrorCode` 枚举保持一致：
 * - `THEME_INVALID_PROFILE`
 * - `THEME_SCOPE_INVALID`
 * - `THEME_PRESET_NOT_FOUND`
 * - `THEME_BUILTIN_NOT_MUTABLE`
 * - `THEME_PERSIST_FAILED`
 * - `THEME_SCOPE_UNRESOLVED`（前端独有，路由作用域无法识别时抛出）
 */
export interface ThemeError {
    /** 错误码。 */
    code:
    | 'THEME_INVALID_PROFILE'
    | 'THEME_SCOPE_INVALID'
    | 'THEME_PRESET_NOT_FOUND'
    | 'THEME_BUILTIN_NOT_MUTABLE'
    | 'THEME_PERSIST_FAILED'
    | 'THEME_SCOPE_UNRESOLVED'
    | (string & {});
    /** 错误说明。 */
    message: string;
    /**
     * 字段级错误明细。
     *
     * 仅 `code === 'THEME_INVALID_PROFILE'` 时存在，对应后端
     * 错误响应 `data` 字段反序列化结果。
     */
    fieldErrors?: ThemeFieldError[];
}

/**
 * 跨标签同步消息体（`ThemeBroadcastMessage`）。
 *
 * 通过 `BroadcastChannel('bml-theme-sync')` 在多标签之间分发；
 * 接收方根据 `kind` 执行不同的同步动作，并通过 `senderId` 忽略自身消息，
 * 以避免回环触发。
 */
export type ThemeBroadcastMessage =
    | {
        /** 主题完整字段更新。 */
        kind: 'profile-changed';
        /** 触发更新的作用域。 */
        scope: ThemeScope;
        /** 更新后的完整 Profile，接收方据此调用 `applyTokens`。 */
        profile: ThemeProfile;
        /** 发送方标签 ID，用于忽略自身消息。 */
        senderId: string;
    }
    | {
        /** 应用了某个预设。 */
        kind: 'preset-applied';
        scope: ThemeScope;
        /** 被应用的预设 ID。 */
        presetId: string;
        senderId: string;
    }
    | {
        /** 恢复到 PRESET_BEST。 */
        kind: 'restored';
        scope: ThemeScope;
        senderId: string;
    };
