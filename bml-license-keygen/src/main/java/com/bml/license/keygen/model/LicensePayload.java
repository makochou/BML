package com.bml.license.keygen.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.List;

/**
 * 许可证载荷模型。
 * <p>
 * 该对象会被序列化为 JSON 并使用 RSA 私钥签名，最终写入 {@code .lic} 文件。
 * 与后端 {@code BmlLicense} 模型字段保持一致。
 * </p>
 *
 * @author BML Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LicensePayload {

    /** 许可证唯一标识 */
    private String licenseId;

    /** 客户名称 */
    private String customerName;

    /** 客户编码 */
    private String customerCode;

    /** 授权产品版本 */
    private String productVersion;

    /** 授权功能模块列表 */
    private List<String> features;

    /** 最大 API 账号数量（0=不限） */
    private int maxApiAccounts;

    /** 单个 API 账号最大用户配额（0=不限） */
    private int maxUsersPerAccount;

    /** 全局最大用户总数（0=不限） */
    private int maxTotalUsers;

    /** 许可证签发日期 */
    private LocalDate issueDate;

    /** 许可证到期日期 */
    private LocalDate expireDate;

    /** 附加说明 */
    private String remark;

    // ── Getter / Setter ──

    public String getLicenseId() { return licenseId; }
    public void setLicenseId(String licenseId) { this.licenseId = licenseId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }

    public String getProductVersion() { return productVersion; }
    public void setProductVersion(String productVersion) { this.productVersion = productVersion; }

    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }

    public int getMaxApiAccounts() { return maxApiAccounts; }
    public void setMaxApiAccounts(int maxApiAccounts) { this.maxApiAccounts = maxApiAccounts; }

    public int getMaxUsersPerAccount() { return maxUsersPerAccount; }
    public void setMaxUsersPerAccount(int maxUsersPerAccount) { this.maxUsersPerAccount = maxUsersPerAccount; }

    public int getMaxTotalUsers() { return maxTotalUsers; }
    public void setMaxTotalUsers(int maxTotalUsers) { this.maxTotalUsers = maxTotalUsers; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getExpireDate() { return expireDate; }
    public void setExpireDate(LocalDate expireDate) { this.expireDate = expireDate; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
