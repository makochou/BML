-- =============================================================================
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.3.0
-- 说明: 补全业务系统菜单、按钮权限及字段权限
--
-- 问题背景：
--   1. V1.0.0 初始化时仅创建了用户管理(41)、角色管理(42)、菜单管理(43)、
--      部门管理(44) 四个 C 类型菜单，其中部门管理缺少按钮权限，且完全未创建
--      机构管理和岗位管理菜单。
--   2. V2.2.0 仅为用户管理添加了 3 个 F 类型字段权限（phone/email/realName），
--      其余 5 个模块均无字段权限，导致角色授权面板右侧"表单字段"面板大量留白。
--
-- 变更内容：
--   一、菜单与按钮（B 类型）
--     1. 新增 机构管理（id=45）C 类型菜单 + 4 个按钮（451-454）
--     2. 新增 岗位管理（id=46）C 类型菜单 + 4 个按钮（461-464）
--     3. 补全 部门管理（id=44）缺失的 4 个按钮（441-444）
--   二、字段权限（F 类型）— 为所有业务模块补全敏感/关键字段权限
--     4. 角色管理 — 数据范围、备注（425-426）
--     5. 菜单管理 — 权限标识、组件路径、菜单图标（435-437）
--     6. 部门管理 — 负责人、联系电话、邮箱（445-447）
--     7. 机构管理 — 信用代码、法人、注册资本、电话、邮箱（455-459）
--     8. 岗位管理 — 岗位类别、岗位级别、备注（465-467）
--   三、将所有新菜单关联到超级管理员角色（role_id=1）
--
-- ID 编码规则（通用，后续新增模块统一沿用）：
--   ┌──────────┬───────────────────────────────────────────┐
--   │ 层级     │ 编码规则                                    │
--   ├──────────┼───────────────────────────────────────────┤
--   │ C 菜单   │ 4x（x=1~9，挂在系统管理 id=4 下）           │
--   │ B 按钮   │ 4x1 ~ 4x4（每菜单最多 4 个标准 CRUD 按钮）  │
--   │ B 扩展   │ 4x5 ~ 4x9（菜单专属扩展按钮，如重置密码）     │
--   │ F 字段   │ (4x+1)*10+n，即按钮之后顺延                  │
--   │          │ 例：42→ 按钮 421-424，字段 425-429           │
--   │          │ 例：45→ 按钮 451-454，字段 455-459           │
--   └──────────┴───────────────────────────────────────────┘
--
-- 权限标识规则：
--   C 菜单: system:{resource}:list
--   B 按钮: system:{resource}:{action}
--   F 字段: system:{resource}:field:{fieldName}
--   所有权限标识均与后端 @PreAuthorize 注解一一对应
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════════════════════
-- 一、新增 机构管理 菜单（C 类型）
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- 系统管理 (id=4, M)
-- ├── 用户管理 (id=41, C, sort=1)
-- ├── 角色管理 (id=42, C, sort=2)
-- ├── 菜单管理 (id=43, C, sort=3)
-- ├── 部门管理 (id=44, C, sort=4)
-- ├── 机构管理 (id=45, C, sort=5) ← 新增
-- └── 岗位管理 (id=46, C, sort=6) ← 新增
--

-- 机构管理 — 菜单页面
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, perms, icon, sort, visible, status)
VALUES (45, '机构管理', 4, 'C', 'org', 'system/org/index', 'system:org:list', 'apps', 5, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), perms = VALUES(perms);

-- 机构管理 — 按钮权限
-- 对应 SysOrgController / SysOrgDataShareController 中的 @PreAuthorize 注解
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, perms, sort, visible, status)
VALUES
    (451, '机构查询', 45, 'B', 'system:org:query',  1, 1, 1),
    (452, '机构新增', 45, 'B', 'system:org:add',    2, 1, 1),
    (453, '机构编辑', 45, 'B', 'system:org:edit',   3, 1, 1),
    (454, '机构删除', 45, 'B', 'system:org:remove', 4, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), perms = VALUES(perms);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 二、新增 岗位管理 菜单（C 类型）
-- ═══════════════════════════════════════════════════════════════════════════════

-- 岗位管理 — 菜单页面
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, perms, icon, sort, visible, status)
VALUES (46, '岗位管理', 4, 'C', 'post', 'system/post/index', 'system:post:list', 'bookmark', 6, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), perms = VALUES(perms);

-- 岗位管理 — 按钮权限
-- 对应 SysPostController 中的 @PreAuthorize 注解
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, perms, sort, visible, status)
VALUES
    (461, '岗位查询', 46, 'B', 'system:post:query',  1, 1, 1),
    (462, '岗位新增', 46, 'B', 'system:post:add',    2, 1, 1),
    (463, '岗位编辑', 46, 'B', 'system:post:edit',   3, 1, 1),
    (464, '岗位删除', 46, 'B', 'system:post:remove', 4, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), perms = VALUES(perms);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 三、补全 部门管理 按钮权限（V1.0.0 遗漏）
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- 部门管理 (id=44, C)
-- ├── 部门查询 (id=441, B) ← 新增
-- ├── 部门新增 (id=442, B) ← 新增
-- ├── 部门编辑 (id=443, B) ← 新增
-- └── 部门删除 (id=444, B) ← 新增
--
-- 对应 SysDeptController 中的 @PreAuthorize 注解

INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, perms, sort, visible, status)
VALUES
    (441, '部门查询', 44, 'B', 'system:dept:query',  1, 1, 1),
    (442, '部门新增', 44, 'B', 'system:dept:add',    2, 1, 1),
    (443, '部门编辑', 44, 'B', 'system:dept:edit',   3, 1, 1),
    (444, '部门删除', 44, 'B', 'system:dept:remove', 4, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), perms = VALUES(perms);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 四、角色管理 — 字段权限（F 类型）
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- 角色管理 (id=42, C)
-- ├── ...按钮 421-424（已有）
-- ├── 数据范围 (425, F) ← 新增，控制角色详情中数据范围字段的可见性
-- └── 备注信息 (426, F) ← 新增，控制角色详情中备注字段的可见性
--
-- 字段选取原则：数据范围决定角色数据隔离策略，属敏感配置项

INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, perms, sort, visible, status, remark, create_by, create_time)
VALUES
    (425, '数据范围', 42, 'F', 'system:role:field:dataScope', 10, 1, 1,
     '控制角色列表和详情中数据范围字段的可见性', 1, NOW()),
    (426, '备注信息', 42, 'F', 'system:role:field:remark',    11, 1, 1,
     '控制角色列表和详情中备注字段的可见性', 1, NOW())
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), perms = VALUES(perms);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 五、菜单管理 — 字段权限（F 类型）
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- 菜单管理 (id=43, C)
-- ├── ...按钮 431-434（已有）
-- ├── 权限标识 (435, F) ← 新增，控制菜单详情中权限标识字段的可见性
-- ├── 组件路径 (436, F) ← 新增，控制菜单详情中组件路径字段的可见性
-- └── 菜单图标 (437, F) ← 新增，控制菜单详情中图标字段的可见性
--
-- 字段选取原则：权限标识和组件路径为技术实现细节，非技术角色无需查看

INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, perms, sort, visible, status, remark, create_by, create_time)
VALUES
    (435, '权限标识', 43, 'F', 'system:menu:field:perms',     10, 1, 1,
     '控制菜单列表和详情中权限标识字段的可见性', 1, NOW()),
    (436, '组件路径', 43, 'F', 'system:menu:field:component', 11, 1, 1,
     '控制菜单列表和详情中组件路径字段的可见性', 1, NOW()),
    (437, '菜单图标', 43, 'F', 'system:menu:field:icon',      12, 1, 1,
     '控制菜单列表和详情中图标字段的可见性', 1, NOW())
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), perms = VALUES(perms);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 六、部门管理 — 字段权限（F 类型）
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- 部门管理 (id=44, C)
-- ├── ...按钮 441-444（本次新增）
-- ├── 负责人 (445, F) ← 新增，控制部门详情中负责人字段的可见性
-- ├── 联系电话 (446, F) ← 新增，控制部门详情中联系电话字段的可见性
-- └── 邮箱地址 (447, F) ← 新增，控制部门详情中邮箱字段的可见性
--
-- 字段选取原则：部门负责人及其联系方式属通讯录类敏感信息

INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, perms, sort, visible, status, remark, create_by, create_time)
VALUES
    (445, '负责人',   44, 'F', 'system:dept:field:leader', 10, 1, 1,
     '控制部门列表和详情中负责人字段的可见性', 1, NOW()),
    (446, '联系电话', 44, 'F', 'system:dept:field:phone',  11, 1, 1,
     '控制部门列表和详情中联系电话字段的可见性', 1, NOW()),
    (447, '邮箱地址', 44, 'F', 'system:dept:field:email',  12, 1, 1,
     '控制部门列表和详情中邮箱地址字段的可见性', 1, NOW())
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), perms = VALUES(perms);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 七、机构管理 — 字段权限（F 类型）
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- 机构管理 (id=45, C)
-- ├── ...按钮 451-454（本次新增）
-- ├── 信用代码 (455, F) ← 新增，控制机构详情中统一社会信用代码字段的可见性
-- ├── 法定代表人 (456, F) ← 新增，控制机构详情中法定代表人字段的可见性
-- ├── 注册资本 (457, F) ← 新增，控制机构详情中注册资本字段的可见性
-- ├── 联系电话 (458, F) ← 新增，控制机构详情中联系电话字段的可见性
-- └── 邮箱地址 (459, F) ← 新增，控制机构详情中邮箱字段的可见性
--
-- 字段选取原则：信用代码、法人、注册资本为企业核心工商信息，属高度敏感数据

INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, perms, sort, visible, status, remark, create_by, create_time)
VALUES
    (455, '信用代码',   45, 'F', 'system:org:field:creditCode',        10, 1, 1,
     '控制机构详情中统一社会信用代码字段的可见性', 1, NOW()),
    (456, '法定代表人', 45, 'F', 'system:org:field:legalPerson',       11, 1, 1,
     '控制机构详情中法定代表人字段的可见性', 1, NOW()),
    (457, '注册资本',   45, 'F', 'system:org:field:registeredCapital', 12, 1, 1,
     '控制机构详情中注册资本（万元）字段的可见性', 1, NOW()),
    (458, '联系电话',   45, 'F', 'system:org:field:phone',             13, 1, 1,
     '控制机构详情中联系电话字段的可见性', 1, NOW()),
    (459, '邮箱地址',   45, 'F', 'system:org:field:email',             14, 1, 1,
     '控制机构详情中邮箱地址字段的可见性', 1, NOW())
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), perms = VALUES(perms);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 八、岗位管理 — 字段权限（F 类型）
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- 岗位管理 (id=46, C)
-- ├── ...按钮 461-464（本次新增）
-- ├── 岗位类别 (465, F) ← 新增，控制岗位详情中岗位类别字段的可见性
-- ├── 岗位级别 (466, F) ← 新增，控制岗位详情中岗位级别字段的可见性
-- └── 备注信息 (467, F) ← 新增，控制岗位详情中备注字段的可见性
--
-- 字段选取原则：岗位类别和级别涉及薪酬/晋升序列，属 HR 敏感信息

INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, perms, sort, visible, status, remark, create_by, create_time)
VALUES
    (465, '岗位类别', 46, 'F', 'system:post:field:postCategory', 10, 1, 1,
     '控制岗位列表和详情中岗位类别字段的可见性', 1, NOW()),
    (466, '岗位级别', 46, 'F', 'system:post:field:postLevel',    11, 1, 1,
     '控制岗位列表和详情中岗位级别字段的可见性', 1, NOW()),
    (467, '备注信息', 46, 'F', 'system:post:field:remark',       12, 1, 1,
     '控制岗位列表和详情中备注字段的可见性', 1, NOW())
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), perms = VALUES(perms);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 九、超级管理员赋予所有新增菜单、按钮及字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
-- role_id=1 为超级管理员角色（V1.0.0 初始化）
-- half_check=0 表示完全勾选（权限已授予，非半选状态）

INSERT INTO sys_role_menu (role_id, menu_id, half_check)
SELECT 1, id, 0
FROM sys_menu
WHERE id IN (
    -- 机构管理 菜单 + 按钮 + 字段
    45, 451, 452, 453, 454, 455, 456, 457, 458, 459,
    -- 岗位管理 菜单 + 按钮 + 字段
    46, 461, 462, 463, 464, 465, 466, 467,
    -- 部门管理 按钮 + 字段（菜单 id=44 已在 V1.0.0 关联）
    441, 442, 443, 444, 445, 446, 447,
    -- 角色管理 字段（菜单 id=42 及按钮 421-424 已在 V1.0.0 关联）
    425, 426,
    -- 菜单管理 字段（菜单 id=43 及按钮 431-434 已在 V1.0.0 关联）
    435, 436, 437
)
ON DUPLICATE KEY UPDATE half_check = 0;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 附录：业务系统完整菜单结构（截至 V2.3.0）
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- 系统管理 (id=4, M, path='system', visible=0)
-- │
-- ├── 用户管理 (id=41, C, system:user:list)
-- │   ├── 用户查询   (411, B, system:user:query)
-- │   ├── 用户新增   (412, B, system:user:add)
-- │   ├── 用户编辑   (413, B, system:user:edit)
-- │   ├── 用户删除   (414, B, system:user:remove)
-- │   ├── 重置密码   (415, B, system:user:reset)
-- │   ├── 手机号码   (416, F, system:user:field:phone)         [V2.2.0]
-- │   ├── 邮箱地址   (417, F, system:user:field:email)         [V2.2.0]
-- │   └── 真实姓名   (418, F, system:user:field:realName)      [V2.2.0]
-- │
-- ├── 角色管理 (id=42, C, system:role:list)
-- │   ├── 角色查询   (421, B, system:role:query)
-- │   ├── 角色新增   (422, B, system:role:add)
-- │   ├── 角色编辑   (423, B, system:role:edit)
-- │   ├── 角色删除   (424, B, system:role:remove)
-- │   ├── 数据范围   (425, F, system:role:field:dataScope)     [V2.3.0 新增]
-- │   └── 备注信息   (426, F, system:role:field:remark)        [V2.3.0 新增]
-- │
-- ├── 菜单管理 (id=43, C, system:menu:list)
-- │   ├── 菜单查询   (431, B, system:menu:query)
-- │   ├── 菜单新增   (432, B, system:menu:add)
-- │   ├── 菜单编辑   (433, B, system:menu:edit)
-- │   ├── 菜单删除   (434, B, system:menu:remove)
-- │   ├── 权限标识   (435, F, system:menu:field:perms)         [V2.3.0 新增]
-- │   ├── 组件路径   (436, F, system:menu:field:component)     [V2.3.0 新增]
-- │   └── 菜单图标   (437, F, system:menu:field:icon)          [V2.3.0 新增]
-- │
-- ├── 部门管理 (id=44, C, system:dept:list)
-- │   ├── 部门查询   (441, B, system:dept:query)               [V2.3.0 新增]
-- │   ├── 部门新增   (442, B, system:dept:add)                 [V2.3.0 新增]
-- │   ├── 部门编辑   (443, B, system:dept:edit)                [V2.3.0 新增]
-- │   ├── 部门删除   (444, B, system:dept:remove)              [V2.3.0 新增]
-- │   ├── 负责人     (445, F, system:dept:field:leader)        [V2.3.0 新增]
-- │   ├── 联系电话   (446, F, system:dept:field:phone)         [V2.3.0 新增]
-- │   └── 邮箱地址   (447, F, system:dept:field:email)         [V2.3.0 新增]
-- │
-- ├── 机构管理 (id=45, C, system:org:list)                     [V2.3.0 新增]
-- │   ├── 机构查询   (451, B, system:org:query)
-- │   ├── 机构新增   (452, B, system:org:add)
-- │   ├── 机构编辑   (453, B, system:org:edit)
-- │   ├── 机构删除   (454, B, system:org:remove)
-- │   ├── 信用代码   (455, F, system:org:field:creditCode)
-- │   ├── 法定代表人 (456, F, system:org:field:legalPerson)
-- │   ├── 注册资本   (457, F, system:org:field:registeredCapital)
-- │   ├── 联系电话   (458, F, system:org:field:phone)
-- │   └── 邮箱地址   (459, F, system:org:field:email)
-- │
-- └── 岗位管理 (id=46, C, system:post:list)                    [V2.3.0 新增]
--     ├── 岗位查询   (461, B, system:post:query)
--     ├── 岗位新增   (462, B, system:post:add)
--     ├── 岗位编辑   (463, B, system:post:edit)
--     ├── 岗位删除   (464, B, system:post:remove)
--     ├── 岗位类别   (465, F, system:post:field:postCategory)
--     ├── 岗位级别   (466, F, system:post:field:postLevel)
--     └── 备注信息   (467, F, system:post:field:remark)
--
-- ═══════════════════════════════════════════════════════════════════════════════
