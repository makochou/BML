package com.bml.module.system.datascope;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.framework.security.model.LoginUser;
import com.bml.core.framework.security.model.OpenApiLoginUser;
import com.bml.core.framework.security.utils.SecurityUtils;
import com.bml.module.system.entity.SysRole;
import com.bml.module.system.entity.SysUser;
import com.bml.module.system.service.SysRoleService;
import com.bml.module.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据权限 AOP 切面。
 * <p>
 * 设计目标：
 * 1) 查询方法只关心业务条件，不重复写权限 SQL；
 * 2) 权限规则统一由切面组装，并通过 {@link DataScopeContext} 注入；
 * 3) 规则缺失时默认拒绝（fail-close），避免越权；
 * 4) 尊重机构 data_isolation 配置，隔离机构的数据不会泄露到父级/同级查询。
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
        if (GlobalConstants.SYSTEM_USER_ID.equals(userId)) {
            // 超级管理员不做数据范围限制
            return StrUtil.EMPTY;
        }

        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        if (CollUtil.isEmpty(roles)) {
            return DENY_ALL_SQL;
        }

        Long deptId = loginUser.getDeptId();
        Long orgId = null;
        SysUser currentUser = userService.getById(userId);
        if (currentUser != null) {
            orgId = currentUser.getOrgId();
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
     * 当前版本未引入角色-部门自定义关联表，暂以创建人范围兜底。
     */
    private String buildCustomSql(String creatorColumn, Long userId, Long roleId) {
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
