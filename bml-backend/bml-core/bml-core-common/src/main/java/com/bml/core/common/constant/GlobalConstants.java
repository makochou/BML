package com.bml.core.common.constant;

/**
 * 全局常量定义
 * <p>
 * 集中管理项目中所有通用常量，避免魔法值散落在业务代码中。
 * 新增常量时请按类别分组，并附上中文注释说明用途。
 * </p>
 *
 * @author BML Team
 */
public interface GlobalConstants {

    // ======================== 链路追踪 ========================

    /**
     * MDC 链路追踪ID的 Key
     */
    String TRACE_ID = "traceId";

    // ======================== 系统用户 ========================

    /**
     * 前台业务超级管理员用户ID（bml 用户）
     * <p>
     * 数据库中前台业务超级管理员（bml）的固定 ID。
     * V1.2.0 迁移脚本将原 ID=1 的中台管理员从 sys_user 表中移除，
     * 改为配置文件管理，并创建了 ID=2 的前台业务管理员 bml。
     * 此常量用于：
     * <ul>
     *   <li>菜单查询时判断是否显示全部菜单（{@code SysMenuServiceImpl}）</li>
     *   <li>数据权限切面中跳过超级管理员的数据范围限制（{@code DataScopeAspect}）</li>
     *   <li>许可证配额保护，防止超级管理员被冻结（{@code LicenseQuotaEnforcer}）</li>
     * </ul>
     * </p>
     *
     * <p><b>注意：</b>若后续通过 Flyway 迁移脚本修改了超级管理员的 ID，
     * 请同步更新此常量，或改用角色编码（{@link #SUPER_ADMIN_ROLE_CODE}）进行判断。</p>
     */
    Long SYSTEM_USER_ID = 2L;

    /**
     * 中台配置管理员的虚拟用户ID
     * <p>
     * 中台管理平台的管理员不存储在 sys_user 表中，而是配置在 application.yml。
     * 登录时使用此虚拟 ID 构建 {@link com.bml.core.framework.security.model.LoginUser}，
     * 用于在 {@code AuthController.getInfo()} 中区分配置管理员和数据库用户。
     * </p>
     *
     * <p><b>注意：</b>此 ID 不对应任何数据库记录，仅作为标识符使用。</p>
     */
    Long ADMIN_USER_ID = -1L;

    /**
     * 超级管理员角色编码
     * <p>
     * 数据库 sys_role 表中超级管理员角色的 role_key 字段值。
     * 拥有此角色的用户在登录时会被赋予 {@code *:*:*} 全量权限标识，
     * 从而绕过所有 {@code @PreAuthorize} 权限校验。
     * </p>
     */
    String SUPER_ADMIN_ROLE_CODE = "admin";

    // ======================== 业务系统菜单 ========================

    /**
     * 业务系统菜单根目录路径标识
     * <p>
     * 业务系统（ip:port/）的所有功能菜单均挂载在此顶级目录下。
     * 该常量对应 sys_menu 表中 path='system' 的顶级 M 类型目录。
     * </p>
     * <p>
     * <b>架构背景：</b>
     * <ul>
     *   <li>中台管理（ip:port/admin）的菜单为顶级非 system 路径的菜单
     *       （如工作台、资产目录、授权治理等），与业务系统完全解耦</li>
     *   <li>业务系统角色授权时，仅授权此目录下的菜单（机构管理、部门管理、
     *       用户管理、角色管理等），不涉及中台管理菜单</li>
     * </ul>
     * </p>
     *
     * @see com.bml.module.system.service.impl.SysMenuServiceImpl#selectPermissionMenuList()
     */
    String BUSINESS_SYSTEM_MENU_PATH = "system";

    // ======================== 树形结构 ========================

    /**
     * 树形结构根节点ID
     * <p>
     * 菜单树、部门树等树形结构中，顶级节点的 parentId 约定为 0。
     * </p>
     */
    Long ROOT_NODE_ID = 0L;

    // ======================== 状态标识 ========================

    /**
     * 正常状态（状态值: 1）
     * <p>
     * 适用于：用户状态、角色状态、菜单状态、部门状态等
     * </p>
     */
    Integer STATUS_NORMAL = 1;

    /**
     * 停用状态（状态值: 0）
     * <p>
     * 适用于：用户状态、角色状态、菜单状态、部门状态等
     * </p>
     */
    Integer STATUS_DISABLE = 0;

    // ======================== 逻辑删除 ========================

    /**
     * 删除标记: 未删除（值: 0）
     */
    Integer NOT_DELETED = 0;

    /**
     * 删除标记: 已删除（值: 1）
     */
    Integer DELETED = 1;
}
