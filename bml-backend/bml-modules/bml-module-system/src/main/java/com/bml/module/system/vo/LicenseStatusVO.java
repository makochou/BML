package com.bml.module.system.vo;

import com.bml.core.framework.license.BmlLicense;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 许可证状态视图对象。
 * <p>
 * 前端许可证管理页面和路由守卫通过该对象判断系统是否已激活。
 * </p>
 *
 * @author BML Team
 */
@Data
@Builder
public class LicenseStatusVO {

    /** 许可证校验是否启用 */
    private boolean enabled;

    /** 是否已激活（存在有效许可证） */
    private boolean activated;

    /** 是否已过期 */
    private boolean expired;

    /** 许可证 ID */
    private String licenseId;

    /** 客户名称 */
    private String customerName;

    /** 客户编码 */
    private String customerCode;

    /** 授权产品版本 */
    private String productVersion;

    /** 授权功能模块 */
    private List<String> features;

    /** 最大 API 账号数量 */
    private Integer maxApiAccounts;

    /** 允许 API 账号调用新增的用户数 */
    private Integer maxUsersPerAccount;

    /** 前端业务系统用户数 */
    private Integer maxTotalUsers;

    /** 当前已创建的 API 账号数量（含所有状态，用于前端配额进度展示） */
    private Long currentApiAccounts;

    /** 当前处于启用状态（status=1）的 API 账号数量 */
    private Long activeApiAccounts;

    /** 当前已创建的前台业务用户数量（含所有状态，用于前端配额进度展示） */
    private Long currentTotalUsers;

    /** 当前处于启用状态（status=1）的前台业务用户数量 */
    private Long activeTotalUsers;

    /** 所有 API 账号累计创建的用户总数（含所有状态） */
    private Long currentApiUsers;

    /** 所有 API 账号累计创建的、且处于启用状态（status=1）的用户数 */
    private Long activeApiUsers;

    /**
     * 本次许可证更新后被自动冻结（停用）的超额用户数量。
     * <p>
     * 仅在许可证配额降级时有值，前端据此展示警告提示。
     * 正常升级或未超额时为 null 或 0。
     * </p>
     */
    private Integer frozenUserCount;

    /**
     * 本次许可证更新后被自动冻结（停用）的超额 API 账号数量。
     * <p>
     * 仅在许可证 maxApiAccounts 降级时有值。
     * </p>
     */
    private Integer frozenApiAccountCount;

    /**
     * 本次许可证更新后被自动冻结的超额 API 创建的用户数量。
     */
    private Integer frozenApiUserCount;

    /** 签发日期 */
    private LocalDate issueDate;

    /** 到期日期 */
    private LocalDate expireDate;

    /** 附加说明 */
    private String remark;

    /** 许可证文件绝对路径（便于开发测试时定位文件） */
    private String filePath;

    /** 错误消息（未激活或过期时显示） */
    private String errorMessage;

    /**
     * 从许可证模型构建 VO。
     *
     * @param license 许可证模型（可能为 null）
     * @param enabled 是否启用许可证校验
     * @param errorMessage 错误消息
     * @return VO 对象
     */
    public static LicenseStatusVO from(BmlLicense license, boolean enabled, String errorMessage) {
        LicenseStatusVOBuilder builder = LicenseStatusVO.builder()
                .enabled(enabled)
                .errorMessage(errorMessage);

        if (license != null) {
            builder.activated(true)
                    .expired(license.isExpired())
                    .licenseId(license.getLicenseId())
                    .customerName(license.getCustomerName())
                    .customerCode(license.getCustomerCode())
                    .productVersion(license.getProductVersion())
                    .features(license.getFeatures())
                    .maxApiAccounts(license.getMaxApiAccounts())
                    .maxUsersPerAccount(license.getMaxUsersPerAccount())
                    .maxTotalUsers(license.getMaxTotalUsers())
                    .issueDate(license.getIssueDate())
                    .expireDate(license.getExpireDate())
                    .remark(license.getRemark());
        } else {
            builder.activated(false).expired(false);
        }
        return builder.build();
    }
}
