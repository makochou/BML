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

    /** 全部数据 */
    ALL(1),
    /** 本组织及下级 */
    ORG_AND_CHILD(2),
    /** 仅本组织 */
    ORG(3),
    /** 本部门及下级 */
    DEPT_AND_CHILD(4),
    /** 仅本部门 */
    DEPT(5),
    /** 仅本人 */
    SELF(6),
    /** 自定义范围 */
    CUSTOM(7);

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
