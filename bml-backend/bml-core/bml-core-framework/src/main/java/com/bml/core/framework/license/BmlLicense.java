package com.bml.core.framework.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * BML 许可证数据模型。
 * <p>
 * 该模型对应许可证文件 (.lic) 中 JSON payload 部分的结构。
 * 许可证文件整体格式为：{@code Base64(JSON payload) + "\n---BML-SIG---\n" + Base64(RSA signature)}。
 * </p>
 * <p>
 * 字段说明：
 * <ul>
 *     <li>{@code licenseId}：许可证唯一标识，由离线工具生成</li>
 *     <li>{@code customerName}：客户名称，展示用</li>
 *     <li>{@code customerCode}：客户编码，唯一标识客户</li>
 *     <li>{@code productVersion}：授权的产品版本号</li>
 *     <li>{@code features}：授权的功能模块列表</li>
 *     <li>{@code maxApiAccounts}：最大 API 账号数量</li>
 *     <li>{@code maxUsersPerAccount}：单个 API 账号最大用户数</li>
 *     <li>{@code maxTotalUsers}：前端业务系统最大用户数（不含中台管理员）</li>
 *     <li>{@code issueDate}：签发日期</li>
 *     <li>{@code expireDate}：到期日期</li>
 * </ul>
 * </p>
 *
 * @author BML Team
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BmlLicense implements Serializable {

    private static final long serialVersionUID = 1L;

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

    /** 最大 API 账号数量（0 表示不限） */
    private int maxApiAccounts;

    /** 单个 API 账号最大用户配额（0 表示不限） */
    private int maxUsersPerAccount;

    /**
     * 前端业务系统最大用户总数（0 表示不限）。
     * <p>
     * 该配额控制的是前台业务系统（{@code /} 路径）的使用用户数量，
     * 不含中台管理平台（{@code /admin}）的管理员账号。
     * 中台管理平台永远只有一个配置管理员，不受此配额限制。
     * </p>
     */
    private int maxTotalUsers;

    /** 许可证签发日期 */
    private LocalDate issueDate;

    /** 许可证到期日期 */
    private LocalDate expireDate;

    /** 附加说明信息 */
    private String remark;

    /**
     * 判断许可证是否已过期。
     *
     * @return 已过期返回 {@code true}
     */
    public boolean isExpired() {
        return expireDate != null && LocalDate.now().isAfter(expireDate);
    }

    /**
     * 判断许可证是否包含指定功能模块。
     *
     * @param feature 功能模块标识
     * @return 包含则返回 {@code true}
     */
    public boolean hasFeature(String feature) {
        return features != null && features.contains(feature);
    }
}
