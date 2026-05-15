-- =============================================================================
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.16.0
-- 说明: 主题设置重设计 — 初始化主题预设表与用户主题设置表，并写入内置预设种子数据。
--
-- 本迁移对应 spec: theme-settings-redesign（任务 1.1）
-- 设计文档: .kiro/specs/theme-settings-redesign/design.md（Data Models 章节）
-- 需求覆盖: 7.5（迁移版本号 > V2.15.1）, 7.6（PRESET_BEST + 至少 1 条备选预设种子）,
--          2.1（完整 ThemeProfile 字段）, 2.2（is_built_in=1 不可改）,
--          2.6（PRESET_BEST 满足 WCAG 2.1 AA 对比度）
--
-- 设计要点：
--   1. bml_theme_preset：存储系统内置 + 平台级自定义主题预设；
--      - profile_admin / profile_business 为合法 JSON，分别对应 ADMIN / BUSINESS 作用域变体；
--      - is_built_in=1 表示内置预设（不可被修改、删除）；
--      - is_default=1 表示系统默认预设（仅 PRESET_BEST 该列为 1）。
--   2. bml_theme_user_setting：每用户每作用域唯一一条 ThemeProfile；
--      - 通过唯一键 uk_user_scope (user_id, scope) 强约束「一用户 + 一作用域 = 一记录」；
--      - preset_ref 引用 bml_theme_preset.id；当被引用预设删除时，由业务层将其置 NULL（保留 profile）。
--   3. 字符集与排序规则与全库保持一致（utf8mb4 / utf8mb4_unicode_ci）。
--
-- WCAG AA 对比度（已离线验证，记录于 spec 任务 1.1 校验过程）：
--   - PRESET_BEST.admin    文字 #1D2129 / 背景 #FFFFFF  → 16.13:1（≥ 4.5:1）
--   - PRESET_BEST.business 文字 #1D2129 / 背景 #F7F8FA  → 15.18:1（≥ 4.5:1）
--   - PRESET_OCEAN.admin   文字 #0B2A3E / 背景 #F4F9FC  → 13.99:1（≥ 4.5:1）
--   - PRESET_OCEAN.business文字 #0B2A3E / 背景 #FFFFFF  → 14.84:1（≥ 4.5:1）
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════════════════════
-- 一、主题预设表 bml_theme_preset
-- ═══════════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS bml_theme_preset (
    id                  VARCHAR(64)   NOT NULL                                COMMENT '预设 ID（内置使用语义 ID，如 PRESET_BEST、PRESET_OCEAN）',
    name                VARCHAR(64)   NOT NULL                                COMMENT '预设名称',
    description         VARCHAR(255)           DEFAULT NULL                   COMMENT '预设描述',
    is_built_in         TINYINT(1)    NOT NULL DEFAULT 0                      COMMENT '是否内置（1=内置不可修改/删除，0=平台级自定义）',
    is_default          TINYINT(1)    NOT NULL DEFAULT 0                      COMMENT '是否系统默认预设（仅 PRESET_BEST=1）',
    sort_order          INT           NOT NULL DEFAULT 0                      COMMENT '排序权重，数字越小越靠前',
    profile_admin       JSON          NOT NULL                                COMMENT 'ADMIN 作用域变体的完整 ThemeProfile（合法 JSON）',
    profile_business    JSON          NOT NULL                                COMMENT 'BUSINESS 作用域变体的完整 ThemeProfile（合法 JSON）',
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_default (is_default),
    KEY idx_built_in (is_built_in)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '主题预设（内置 + 平台级自定义）';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 二、用户主题设置表 bml_theme_user_setting
-- ═══════════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS bml_theme_user_setting (
    id          BIGINT         NOT NULL AUTO_INCREMENT                        COMMENT '主键 ID（自增）',
    user_id     BIGINT         NOT NULL                                       COMMENT '用户 ID（关联 sys_user.id）',
    scope       VARCHAR(16)    NOT NULL                                       COMMENT '作用域：ADMIN | BUSINESS',
    preset_ref  VARCHAR(64)             DEFAULT NULL                          COMMENT '引用的预设 ID（指向 bml_theme_preset.id；解引用后置 NULL）',
    profile     JSON           NOT NULL                                       COMMENT 'ThemeProfile 完整字段（合法 JSON）',
    updated_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_scope (user_id, scope),
    KEY idx_preset_ref (preset_ref)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户主题设置（每用户每作用域唯一一条）';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 三、内置预设种子数据
-- ───────────────────────────────────────────────────────────────────────────────
-- 1) PRESET_BEST  ：系统默认预设；现代毛玻璃 + 柔和渐变 + 高对比可读性；
--                   mode=AUTO 跟随系统亮/暗模式；is_default=1。
-- 2) PRESET_OCEAN ：海洋蓝备选预设；清新明亮的海蓝 + 大圆角；mode=LIGHT。
--
-- 注：JSON 中字段与设计文档 ThemeProfile 字段表保持一一对应；
--     枚举字符串均使用大写形式（与后端 enums 一致）；
--     #RRGGBB 颜色已在线下校验通过 WCAG 2.1 AA 对比度阈值（详见文件头注释）。
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT IGNORE INTO bml_theme_preset
    (id, name, description, is_built_in, is_default, sort_order, profile_admin, profile_business)
VALUES
    (
        'PRESET_BEST',
        '极致默认',
        '系统内置最佳默认方案：现代毛玻璃质感 + 柔和渐变 + 高对比可读性，适配亮/暗双模式。',
        1, 1, 0,
        -- profile_admin（ADMIN 作用域变体）
        JSON_OBJECT(
            'primaryColor',           '#165DFF',
            'secondaryColor',         '#4080FF',
            'accentColor',            '#722ED1',
            'successColor',           '#00B42A',
            'warningColor',           '#FF7D00',
            'errorColor',             '#F53F3F',
            'infoColor',              '#86909C',
            'textColor',              '#1D2129',
            'backgroundColor',        '#FFFFFF',
            'borderColor',            '#E5E6EB',
            'mode',                   'AUTO',
            'radius',                 'MEDIUM',
            'density',                'DEFAULT',
            'sidebarStyle',           'DARK',
            'sidebarCollapsedStyle',  'DARK',
            'headerStyle',            'LIGHT',
            'fontScale',              'DEFAULT',
            'presetRef',              NULL
        ),
        -- profile_business（BUSINESS 作用域变体）
        JSON_OBJECT(
            'primaryColor',           '#165DFF',
            'secondaryColor',         '#4080FF',
            'accentColor',            '#722ED1',
            'successColor',           '#00B42A',
            'warningColor',           '#FF7D00',
            'errorColor',             '#F53F3F',
            'infoColor',              '#86909C',
            'textColor',              '#1D2129',
            'backgroundColor',        '#F7F8FA',
            'borderColor',            '#E5E6EB',
            'mode',                   'AUTO',
            'radius',                 'MEDIUM',
            'density',                'DEFAULT',
            'sidebarStyle',           'LIGHT',
            'sidebarCollapsedStyle',  'LIGHT',
            'headerStyle',            'LIGHT',
            'fontScale',              'DEFAULT',
            'presetRef',              NULL
        )
    ),
    (
        'PRESET_OCEAN',
        '海洋蓝',
        '清新明亮的海蓝色调 + 大圆角，适合长时间阅读与数据展示场景。',
        1, 0, 10,
        -- profile_admin（ADMIN 作用域变体）
        JSON_OBJECT(
            'primaryColor',           '#0E72B5',
            'secondaryColor',         '#14B8A6',
            'accentColor',            '#06B6D4',
            'successColor',           '#10B981',
            'warningColor',           '#F59E0B',
            'errorColor',             '#EF4444',
            'infoColor',              '#6B7280',
            'textColor',              '#0B2A3E',
            'backgroundColor',        '#F4F9FC',
            'borderColor',            '#DBE9F2',
            'mode',                   'LIGHT',
            'radius',                 'LARGE',
            'density',                'DEFAULT',
            'sidebarStyle',           'PRIMARY',
            'sidebarCollapsedStyle',  'DARK',
            'headerStyle',            'PRIMARY',
            'fontScale',              'DEFAULT',
            'presetRef',              NULL
        ),
        -- profile_business（BUSINESS 作用域变体）
        JSON_OBJECT(
            'primaryColor',           '#0E72B5',
            'secondaryColor',         '#14B8A6',
            'accentColor',            '#06B6D4',
            'successColor',           '#10B981',
            'warningColor',           '#F59E0B',
            'errorColor',             '#EF4444',
            'infoColor',              '#6B7280',
            'textColor',              '#0B2A3E',
            'backgroundColor',        '#FFFFFF',
            'borderColor',            '#DBE9F2',
            'mode',                   'LIGHT',
            'radius',                 'LARGE',
            'density',                'DEFAULT',
            'sidebarStyle',           'LIGHT',
            'sidebarCollapsedStyle',  'LIGHT',
            'headerStyle',            'LIGHT',
            'fontScale',              'DEFAULT',
            'presetRef',              NULL
        )
    );
