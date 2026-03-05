package com.bml.module.api.constant;

/**
 * API 账号类型常量。
 * <p>
 * 将账号类型的魔数值统一收口到此类，消除各层散落的硬编码，
 * 后续如需扩展新类型（如"合作伙伴"），只需在此处新增常量并更新校验范围即可。
 * </p>
 *
 * <ul>
 * <li>{@link #INTERNAL} — 内部账号，一般由本企业自有业务系统使用</li>
 * <li>{@link #EXTERNAL} — 外部账号，一般由第三方合作系统或外部集成方使用</li>
 * </ul>
 */
public final class ApiAccountTypeConstants {

    /** 内部账号 */
    public static final int INTERNAL = 1;

    /** 外部账号 */
    public static final int EXTERNAL = 2;

    /** 默认账号类型（内部账号） */
    public static final int DEFAULT = INTERNAL;

    /** 当前支持的最小账号类型值 */
    public static final int MIN_VALUE = INTERNAL;

    /** 当前支持的最大账号类型值 */
    public static final int MAX_VALUE = EXTERNAL;

    private ApiAccountTypeConstants() {
    }

    /**
     * 校验账号类型值是否合法。
     *
     * @param accountType 待校验的账号类型值
     * @return 合法返回 true，否则返回 false
     */
    public static boolean isValid(int accountType) {
        return accountType >= MIN_VALUE && accountType <= MAX_VALUE;
    }

    /**
     * 返回账号类型的中文描述。
     *
     * @param accountType 账号类型值
     * @return 中文描述字符串
     */
    public static String getLabel(int accountType) {
        return switch (accountType) {
            case INTERNAL -> "内部账号";
            case EXTERNAL -> "外部账号";
            default -> "未知类型";
        };
    }
}
