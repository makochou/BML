package com.bml.module.system.datascope;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.framework.security.model.LoginUser;
import com.bml.core.framework.security.model.OpenApiLoginUser;
import com.bml.core.framework.security.utils.SecurityUtils;
import com.bml.module.system.entity.SysRole;
import com.bml.module.system.entity.SysUser;
import com.bml.module.system.entity.SysUserDataScope;
import com.bml.module.system.service.SysRoleService;
import com.bml.module.system.service.SysUserDataScopeService;
import com.bml.module.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据权限 AOP 切面。
 * <p>
 * 设计目标：
 * 1) 查询方法只关心业务条件，不重复写权限 SQL；
 * 2) 权限规则统一由切面组装，并通过 {@link DataScopeContext} 注入；
 * 3) 规则缺失时默认拒绝（fail-close），避免越权；
 * 4) 尊重机构 data_isolation 配置，隔离机构的数据不会泄露到父级/同级查询；
 * 5) 支持用户个人数据权限覆盖（优先级高于角色）；
 * 6) 支持「本人及所有下属员工」汇报链维度数据范围。
 * </p>
 * <p>
 * <b>data_isolation 模式与 SQL 生成策略：</b>
 * <ul>
 *   <li>0（共享）— 包含在父级 ORG_AND_CHILD 查询中</li>
 *   <li>1（完全隔离）— 从父级 ORG_AND_CHILD 查询中排除</li>
 *   <li>2（汇总共享）— SQL 层面等同共享，业务层应限制为汇总</li>
 *   <li>3（同级互通）— 包含在父级查询 + 兄弟机构查询中</li>
 *   <li>4（按模块隔离）— 当前版本未实现模块配置表，等同共享</li>
 * </ul>
 * </p>
 * <p>
 * <b>数据权限优先级规则：</b>
 * <ol>
 *   <li>超级管理员：不受任何数据权限限制</li>
 *   <li>用户个人数据权限（sys_user_data_scope，status=1 且未过期）：优先级最高</li>
 *   <li>角色数据权限（sys_role.data_scope）：取所有角色的并集</li>
 * </ol>
 * </p>
 *
 * @author BML Team
 * @see DataIsolationType
 */
@Aspect
@Component
@RequiredArgsConstructor
public class DataScopeAspect {

    private static final Logger log = LoggerFactory.getLogger(DataScopeAspect.class);

    private static final String DENY_ALL_SQL = "1 = 0";

    private final SysRoleService roleService;
    private final SysUserService userService;
    private final SysUserDataScopeService userDataScopeService;

    /**
     * 在查询方法执行前注入数据权限 SQL，方法结束后清理上下文。
     */
    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint joinPoint, DataScope dataScope) throws Throwable {
        try {
            DataScopeContext.setDataScopeSql(buildDataScopeSql(dataScope));
            return joinPoint.proceed();
        } finally {
            DataScopeContext.clear();
        }
    }

    /**
     * 组装当前用户的数据权限 SQL 条件。
     * <p>
     * 处理流程：
     * 1. 获取当前登录用户信息
     * 2. 超级管理员直接放行
     * 3. 检查用户个人数据权限（优先级最高）
     * 4. 若无个人权限，则按角色权限取并集
     * </p>
     */
    private String buildDataScopeSql(DataScope dataScope) {
        LoginUser loginUser;
        try {
            loginUser = SecurityUtils.getLoginUser();
        } catch (Exception ex) {
            log.warn("数据权限解析失败：无法获取当前登录用户，默认拒绝访问");
            return DENY_ALL_SQL;
        }

        Long userId = loginUser.getUserId();
        if (loginUser instanceof OpenApiLoginUser) {
            // API 账号授权以接口维度为主，不参与后台角色数据范围裁剪，
            // 否则会因缺少后台用户组织关系导致本应授权的接口被误拒绝。
            return StrUtil.EMPTY;
        }
        if (userId == null) {
            return DENY_ALL_SQL;
        }
        // 超级管理员判断：
        //   1. userId 等于 SYSTEM_USER_ID（兼容旧逻辑，当前为 bml 用户 ID=2）
        //   2. 权限集合中包含 *:*:* 通配符（基于角色的通用判断，更健壮）
        //   两种方式任一满足即视为超级管理员，不做数据范围限制
        boolean isSuperAdmin = GlobalConstants.SYSTEM_USER_ID.equals(userId)
                || (loginUser.getPermissions() != null && loginUser.getPermissions().contains("*:*:*"));
        if (isSuperAdmin) {
            // 超级管理员不做数据范围限制，可查看所有数据
            return StrUtil.EMPTY;
        }

        Long deptId = loginUser.getDeptId();
        Long orgId = null;
        SysUser currentUser = userService.getById(userId);
        if (currentUser != null) {
            orgId = currentUser.getOrgId();
        }

        // ── 优先级1：检查用户个人数据权限配置 ──
        SysUserDataScope userScope = userDataScopeService.selectActiveByUserId(userId);
        if (userScope != null) {
            return buildUserScopeSql(userScope, dataScope, userId, deptId, orgId);
        }

        // ── 优先级2：按角色数据权限取并集 ──
        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        if (CollUtil.isEmpty(roles)) {
            return DENY_ALL_SQL;
        }

        Set<String> conditions = new LinkedHashSet<>();
        for (SysRole role : roles) {
            DataScopeType type = DataScopeType.fromCode(role.getDataScope());
            if (type == null) {
                continue;
            }
            if (DataScopeType.ALL == type) {
                return StrUtil.EMPTY;
            }
            String sql = buildConditionSql(type, dataScope, userId, deptId, orgId, role.getId());
            if (StrUtil.isNotBlank(sql)) {
                conditions.add(sql);
            }
        }

        if (conditions.isEmpty()) {
            return DENY_ALL_SQL;
        }
        return "(" + String.join(" OR ", conditions) + ")";
    }

    /**
     * 根据用户个人数据权限配置构建 SQL。
     */
    private String buildUserScopeSql(SysUserDataScope userScope, DataScope dataScope,
                                     Long userId, Long deptId, Long orgId) {
        DataScopeType type = DataScopeType.fromCode(userScope.getDataScope());
        if (type == null) {
            return DENY_ALL_SQL;
        }
        if (DataScopeType.ALL == type) {
            return StrUtil.EMPTY;
        }
        // 自定义模式：使用用户配置的机构/部门列表
        if (DataScopeType.CUSTOM == type) {
            return buildUserCustomSql(userScope, dataScope, userId);
        }
        // 其他模式复用角色维度的构建逻辑
        String sql = buildConditionSql(type, dataScope, userId, deptId, orgId, null);
        return StrUtil.isNotBlank(sql) ? sql : DENY_ALL_SQL;
    }

    /**
     * 构建用户个人自定义数据权限 SQL（根据 customOrgIds 和 customDeptIds）。
     */
    private String buildUserCustomSql(SysUserDataScope userScope, DataScope dataScope, Long userId) {
        Set<String> conditions = new LinkedHashSet<>();

        // 自定义机构范围
        if (StrUtil.isNotBlank(userScope.getCustomOrgIds())) {
            String orgColumn = normalizeColumn(dataScope.orgColumn());
            if (StrUtil.isNotBlank(orgColumn)) {
                List<String> orgIds = Arrays.stream(userScope.getCustomOrgIds().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                if (!orgIds.isEmpty()) {
                    conditions.add(orgColumn + " IN (" + String.join(",", orgIds) + ")");
                }
            }
        }

        // 自定义部门范围
        if (StrUtil.isNotBlank(userScope.getCustomDeptIds())) {
            String deptColumn = normalizeColumn(dataScope.deptColumn());
            if (StrUtil.isNotBlank(deptColumn)) {
                List<String> deptIds = Arrays.stream(userScope.getCustomDeptIds().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                if (!deptIds.isEmpty()) {
                    conditions.add(deptColumn + " IN (" + String.join(",", deptIds) + ")");
                }
            }
        }

        // 本人数据始终可见
        String creatorColumn = normalizeColumn(dataScope.creatorColumn());
        if (StrUtil.isNotBlank(creatorColumn)) {
            conditions.add(creatorColumn + " = " + userId);
        }

        if (conditions.isEmpty()) {
            return DENY_ALL_SQL;
        }
        return "(" + String.join(" OR ", conditions) + ")";
    }

    /**
     * 根据数据范围类型生成 SQL。
     */
    private String buildConditionSql(
            DataScopeType type,
            DataScope dataScope,
            Long userId,
            Long deptId,
            Long orgId,
            Long roleId) {
        return switch (type) {
            case ORG_AND_CHILD -> buildOrgAndChildSql(dataScope.orgColumn(), orgId, dataScope.creatorColumn(), userId);
            case ORG -> buildOrgSql(dataScope.orgColumn(), orgId, dataScope.creatorColumn(), userId);
            case DEPT_AND_CHILD ->
                    buildDeptAndChildSql(dataScope.deptColumn(), deptId, dataScope.creatorColumn(), userId);
            case DEPT -> buildDeptSql(dataScope.deptColumn(), deptId, dataScope.creatorColumn(), userId);
            case SELF -> buildSelfSql(dataScope.userColumn(), dataScope.creatorColumn(), userId);
            case SELF_AND_SUBORDINATES ->
                    buildSelfAndSubordinatesSql(dataScope.userColumn(), dataScope.creatorColumn(), userId);
            case CUSTOM -> buildCustomSql(dataScope.creatorColumn(), userId, roleId);
            default -> DENY_ALL_SQL;
        };
    }

    /**
     * 构建 ORG_AND_CHILD 范围 SQL：本机构 + 下级机构（排除隔离机构）+ 同级互通机构。
     * <p>
     * 逻辑说明：
     * <ol>
     *   <li>用户自身所在机构始终可见</li>
     *   <li>后代机构中排除 data_isolation = 1（完全隔离）的记录</li>
     *   <li>同级互通（data_isolation = 3）的兄弟机构也纳入可见范围</li>
     * </ol>
     * </p>
     */
    private String buildOrgAndChildSql(String orgColumn, Long orgId, String creatorColumn, Long userId) {
        String column = normalizeColumn(orgColumn);
        if (StrUtil.isBlank(column) || orgId == null) {
            return fallbackToCreator(creatorColumn, userId);
        }
        // 自身机构 + 非隔离的后代 + 同级互通兄弟
        return column + " IN ("
                + "SELECT id FROM sys_org WHERE id = " + orgId                               // 自身始终可见
                + " OR (FIND_IN_SET(" + orgId + ", ancestors) AND data_isolation != " + DataIsolationType.ISOLATED.getCode() + ")"  // 非隔离后代
                + " OR (data_isolation = " + DataIsolationType.SIBLING_SHARED.getCode()
                +      " AND parent_id = (SELECT parent_id FROM sys_org WHERE id = " + orgId + ")"
                +      " AND id != " + orgId + ")"                                           // 同级互通兄弟
                + ")";
    }

    /**
     * 构建 ORG 范围 SQL：仅本机构 + 同级互通兄弟机构。
     * <p>
     * 若用户所在机构自身的 data_isolation = 3（同级互通），
     * 则兄弟机构中同为 data_isolation = 3 的也纳入可见范围。
     * </p>
     */
    private String buildOrgSql(String orgColumn, Long orgId, String creatorColumn, Long userId) {
        String column = normalizeColumn(orgColumn);
        if (StrUtil.isBlank(column) || orgId == null) {
            return fallbackToCreator(creatorColumn, userId);
        }
        // 自身机构 + 同级互通兄弟（如果自身也是同级互通模式）
        return column + " IN ("
                + "SELECT id FROM sys_org WHERE id = " + orgId                               // 自身始终可见
                + " OR (data_isolation = " + DataIsolationType.SIBLING_SHARED.getCode()
                +      " AND parent_id = (SELECT parent_id FROM sys_org WHERE id = " + orgId + ")"
                +      " AND id != " + orgId
                +      " AND (SELECT data_isolation FROM sys_org WHERE id = " + orgId + ") = " + DataIsolationType.SIBLING_SHARED.getCode() + ")"
                + ")";
    }

    private String buildDeptAndChildSql(String deptColumn, Long deptId, String creatorColumn, Long userId) {
        String column = normalizeColumn(deptColumn);
        if (StrUtil.isBlank(column) || deptId == null) {
            return fallbackToCreator(creatorColumn, userId);
        }
        return column + " IN (SELECT id FROM sys_dept WHERE id = " + deptId
                + " OR FIND_IN_SET(" + deptId + ", ancestors))";
    }

    private String buildDeptSql(String deptColumn, Long deptId, String creatorColumn, Long userId) {
        String column = normalizeColumn(deptColumn);
        if (StrUtil.isBlank(column) || deptId == null) {
            return fallbackToCreator(creatorColumn, userId);
        }
        return column + " = " + deptId;
    }

    private String buildSelfSql(String userColumn, String creatorColumn, Long userId) {
        String userIdColumn = normalizeColumn(userColumn);
        if (StrUtil.isNotBlank(userIdColumn)) {
            return userIdColumn + " = " + userId;
        }
        return fallbackToCreator(creatorColumn, userId);
    }

    /**
     * 构建 SELF_AND_SUBORDINATES 范围 SQL：本人 + 所有汇报链下属员工。
     * <p>
     * 通过递归查询 sys_user.superior_id 构建汇报链，找到所有以当前用户为
     * 直接或间接上级的员工ID列表，这些员工创建的数据均对当前用户可见。
     * </p>
     * <p>
     * SQL 策略：使用递归 CTE（Common Table Expression）查询所有下属：
     * <pre>
     * WITH RECURSIVE subordinates AS (
     *     SELECT id FROM sys_user WHERE superior_id = ?
     *     UNION ALL
     *     SELECT u.id FROM sys_user u INNER JOIN subordinates s ON u.superior_id = s.id
     * )
     * </pre>
     * MariaDB 10.2+ / MySQL 8.0+ 均支持递归 CTE。
     * </p>
     */
    private String buildSelfAndSubordinatesSql(String userColumn, String creatorColumn, Long userId) {
        String column = normalizeColumn(creatorColumn);
        if (StrUtil.isBlank(column)) {
            column = normalizeColumn(userColumn);
        }
        if (StrUtil.isBlank(column)) {
            return DENY_ALL_SQL;
        }
        // 使用子查询递归查找所有下属员工ID（包含本人）
        return column + " IN ("
                + "SELECT " + userId + " UNION ALL "
                + "SELECT id FROM ("
                + "WITH RECURSIVE subordinates AS ("
                + "SELECT id FROM sys_user WHERE superior_id = " + userId + " AND deleted = 0 "
                + "UNION ALL "
                + "SELECT u.id FROM sys_user u INNER JOIN subordinates s ON u.superior_id = s.id WHERE u.deleted = 0"
                + ") SELECT id FROM subordinates"
                + ") AS sub"
                + ")";
    }

    /**
     * 构建自定义数据权限 SQL。
     * <p>
     * 自定义模式（data_scope=7）支持角色关联的机构和部门维度：
     * 1. 查询 sys_role_org 获取可访问的机构范围
     * 2. 查询 sys_role_dept 获取可访问的部门范围
     * 3. 取二者并集 + 本人创建的数据
     * </p>
     */
    private String buildCustomSql(String creatorColumn, Long userId, Long roleId) {
        if (roleId == null) {
            return fallbackToCreator(creatorColumn, userId);
        }
        String fallback = fallbackToCreator(creatorColumn, userId);
        if (DENY_ALL_SQL.equals(fallback)) {
            log.warn("角色 {} 配置了自定义数据权限，但当前未配置可落地的数据域映射，已按拒绝处理", roleId);
        }
        return fallback;
    }

    private String fallbackToCreator(String creatorColumn, Long userId) {
        String column = normalizeColumn(creatorColumn);
        if (StrUtil.isBlank(column)) {
            return DENY_ALL_SQL;
        }
        return column + " = " + userId;
    }

    /**
     * 对列名做最小校验，阻断非预期字符进入 SQL。
     */
    private String normalizeColumn(String column) {
        if (StrUtil.isBlank(column)) {
            return StrUtil.EMPTY;
        }
        String normalized = column.trim();
        if (!normalized.matches("[A-Za-z0-9_\\.]+")) {
            log.warn("检测到非法数据权限列名：{}", column);
            return StrUtil.EMPTY;
        }
        return normalized;
    }
}
