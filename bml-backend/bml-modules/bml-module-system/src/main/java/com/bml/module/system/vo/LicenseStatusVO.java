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

    /** 单账号最大用户数 */
    private Integer maxUsersPerAccount;

    /** 前端业务系统用户数 */
    private Integer maxTotalUsers;

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
