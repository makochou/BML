package com.bml.module.system.datascope;

/**
 * 机构数据隔离模式枚举。
 * <p>
 * 与数据库字段 sys_org.data_isolation 对齐。
 * 每个机构可独立配置隔离策略，控制本机构数据对外部（父级、同级）的可见性。
 * </p>
 * <p>
 * <b>五种模式说明：</b>
 * <pre>
 * ┌──────────────────────────────────────────────────────────────┐
 * │  模式            │ 上级可见 │ 同级可见 │ 备注               │
 * ├──────────────────┼──────────┼──────────┼────────────────────┤
 * │ 0 - 共享         │    ✔     │    ✘     │ 默认，适合统一管控 │
 * │ 1 - 完全隔离     │    ✘     │    ✘     │ 数据完全独立       │
 * │ 2 - 汇总共享     │   ✔(汇总)│    ✘     │ 上级仅看聚合数据   │
 * │ 3 - 同级互通     │    ✔     │    ✔     │ 兄弟机构可互查     │
 * │ 4 - 按模块隔离   │   分模块 │   分模块 │ 需配合模块配置表   │
 * └──────────────────────────────────────────────────────────────┘
 * </pre>
 * </p>
 *
 * @author BML Team
 */
public enum DataIsolationType {

    /**
     * 共享（上级穿透）。
     * <p>
     * 默认模式。上级机构可查看所有下级机构数据，下级不可看上级，同级不可互看。
     * 适用于集团统一管控场景，总部做全域报表和全局查询。
     * </p>
     */
    SHARED(0),

    /**
     * 完全隔离。
     * <p>
     * 各机构数据完全独立，即使存在上下级关系也不可互查。
     * 适用于独立法人子公司、合规要求严格的金融/医疗行业。
     * </p>
     */
    ISOLATED(1),

    /**
     * 汇总共享。
     * <p>
     * 上级机构只能查看下级的汇总/统计数据，不能查看明细记录。
     * 在 SQL 层面与共享模式相同（包含在父级查询范围），
     * 业务层需根据此标记决定是否返回明细还是聚合数据。
     * </p>
     */
    SUMMARY_SHARED(2),

    /**
     * 同级互通。
     * <p>
     * 同一父机构下的兄弟机构可以互相查看数据，同时上级也可查看。
     * 适用于紧密协作的区域公司之间共享客户/项目数据。
     * </p>
     */
    SIBLING_SHARED(3),

    /**
     * 按模块隔离。
     * <p>
     * 部分业务模块（如财务）隔离，部分模块（如人事/公告）共享。
     * 当前版本需配合 sys_org_module_isolation 配置表使用（预留扩展）。
     * 未配置模块映射时，默认行为等同于"共享"模式。
     * </p>
     */
    MODULE_ISOLATED(4);

    private final int code;

    DataIsolationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 根据编码查找枚举，未匹配返回 null。
     */
    public static DataIsolationType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DataIsolationType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断本模式是否允许上级机构查看明细数据。
     * <p>
     * ISOLATED(1) 不允许；SUMMARY_SHARED(2) 在 SQL 层面允许但业务层应限制为汇总。
     * </p>
     */
    public boolean isVisibleToParent() {
        return this != ISOLATED;
    }

    /**
     * 判断本模式是否允许同级兄弟机构互查数据。
     */
    public boolean isVisibleToSibling() {
        return this == SIBLING_SHARED;
    }
}
