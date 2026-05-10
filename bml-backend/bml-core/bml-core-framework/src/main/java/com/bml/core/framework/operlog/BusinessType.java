package com.bml.core.framework.operlog;

/**
 * 操作日志业务类型枚举。
 * <p>
 * 与 sys_operation_log.business_type 字段保持一致，用于从注解层统一描述一次操作的业务性质。
 * 后续新增导入、导出、授权等动作时，只需要扩展本枚举即可复用统一日志切面。
 * </p>
 *
 * @author BML Team
 */
public enum BusinessType {

    /** 其他操作 */
    OTHER(0, "其他"),

    /** 新增数据 */
    INSERT(1, "新增"),

    /** 修改数据 */
    UPDATE(2, "修改"),

    /** 删除数据 */
    DELETE(3, "删除"),

    /** 查询数据 */
    QUERY(4, "查询"),

    /** 授权或分配权限 */
    GRANT(5, "授权"),

    /** 重置密码、密钥等敏感凭证 */
    RESET(6, "重置"),

    /** 清理历史数据 */
    CLEAN(7, "清空"),

    /** 同步外部或内部目录数据 */
    SYNC(8, "同步"),

    /** 业务状态变更 */
    STATUS(9, "状态变更"),

    /** 导出数据 */
    EXPORT(10, "导出");

    private final int code;

    private final String label;

    BusinessType(int code, String label) {
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
