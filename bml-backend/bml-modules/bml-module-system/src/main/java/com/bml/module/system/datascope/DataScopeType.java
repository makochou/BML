package com.bml.module.system.datascope;

/**
 * 数据权限范围枚举。
 * <p>
 * 与数据库字段 sys_role.data_scope 对齐。
 * </p>
 *
 * @author BML Team
 */
public enum DataScopeType {

    /** 全部数据 — 不受任何限制，可查看系统所有数据 */
    ALL(1),
    /** 本组织及下级 — 可查看本机构及所有下级机构数据（受机构隔离模式影响） */
    ORG_AND_CHILD(2),
    /** 仅本组织 — 只能查看当前所属机构的数据 */
    ORG(3),
    /** 本部门及下级 — 可查看本部门及所有下级部门数据 */
    DEPT_AND_CHILD(4),
    /** 仅本部门 — 只能查看当前所属部门的数据 */
    DEPT(5),
    /** 仅本人 — 只能查看自己创建的数据 */
    SELF(6),
    /** 自定义范围 — 管理员手动指定可访问的机构/部门范围 */
    CUSTOM(7),
    /** 本人及所有下属员工 — 可查看自己及汇报链所有下属员工创建的数据 */
    SELF_AND_SUBORDINATES(8);

    private final int code;

    DataScopeType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 根据编码查找枚举。
     */
    public static DataScopeType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DataScopeType scopeType : values()) {
            if (scopeType.code == code) {
                return scopeType;
            }
        }
        return null;
    }
}
