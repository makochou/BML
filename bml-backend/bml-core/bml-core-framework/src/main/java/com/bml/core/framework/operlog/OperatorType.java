package com.bml.core.framework.operlog;

/**
 * 操作人类别枚举。
 * <p>
 * 与 sys_operation_log.operator_type 字段对应，用于区分日志来源，方便审计人员按后台用户、移动端用户、
 * API 账号等维度过滤日志。
 * </p>
 *
 * @author BML Team
 */
public enum OperatorType {

    /** 其他来源 */
    OTHER(0, "其他"),

    /** 后台或业务系统登录用户 */
    MANAGE(1, "后台用户"),

    /** 移动端用户 */
    MOBILE(2, "移动端用户"),

    /** 外部 API 账号 */
    OPEN_API(3, "API账号");

    private final int code;

    private final String label;

    OperatorType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
