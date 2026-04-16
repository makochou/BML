package com.bml.core.framework.license;

import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 许可证配额校验器。
 * <p>
 * 提供通用的许可证配额检查方法，业务层在执行「创建」类操作前调用以确保不超出许可证授权范围。
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 *   // 在 Service 的 createXxx 方法中
 *   licenseQuotaChecker.checkApiAccountQuota(currentCount);
 *   licenseQuotaChecker.checkUserQuota(currentCount);
 *   licenseQuotaChecker.checkFeatureAuthorized("api_gateway");
 * </pre>
 * </p>
 * <p>
 * 配额值为 0 时表示不限制。许可证未启用或未加载时同样不限制（由 {@link LicenseCheckInterceptor} 负责整体拦截）。
 * </p>
 *
 * @author BML Team
 */
@Slf4j
@Component
public class LicenseQuotaChecker {

    private final BmlLicenseHolder licenseHolder;

    public LicenseQuotaChecker(BmlLicenseHolder licenseHolder) {
        this.licenseHolder = licenseHolder;
    }

    /**
     * 校验 API 账号数量是否超出许可证配额。
     * <p>
     * 在创建新 API 账号之前调用，传入当前已有的账号数量。
     * 如果许可证的 {@code maxApiAccounts} 大于 0 且当前数量已达上限，则抛出异常。
     * </p>
     *
     * @param currentCount 当前已存在的 API 账号数量
     * @throws BusinessException 配额超限时抛出
     */
    public void checkApiAccountQuota(long currentCount) {
        BmlLicense license = getEffectiveLicense();
        if (license == null) {
            return;
        }
        int max = license.getMaxApiAccounts();
        if (max > 0 && currentCount >= max) {
            log.warn("[License] API 账号配额超限: 当前 {}, 上限 {}", currentCount, max);
            throw new BusinessException(GlobalErrorCode.LICENSE_QUOTA_API_ACCOUNT_EXCEEDED.getCode(),
                    String.format("API 账号数量已达许可证授权上限（%d 个），如需更多请联系供应商升级许可证", max));
        }
    }

    /**
     * 校验前端业务系统用户数量是否超出许可证配额。
     * <p>
     * 在创建新的前端业务用户之前调用，传入当前已有的业务用户数量。
     * 如果许可证的 {@code maxTotalUsers} 大于 0 且当前数量已达上限，则抛出异常。
     * </p>
     * <p>
     * 注意：此配额仅针对前台业务系统（{@code /}）的使用用户，
     * 不含中台管理平台（{@code /admin}）的管理员账号。
     * 中台管理平台永远只有一个配置管理员，不受许可证配额限制。
     * </p>
     *
     * @param currentCount 当前已存在的前端业务用户数量
     * @throws BusinessException 配额超限时抛出
     */
    public void checkUserQuota(long currentCount) {
        BmlLicense license = getEffectiveLicense();
        if (license == null) {
            return;
        }
        int max = license.getMaxTotalUsers();
        if (max > 0 && currentCount >= max) {
            log.warn("[License] 用户配额超限: 当前 {}, 上限 {}", currentCount, max);
            throw new BusinessException(GlobalErrorCode.LICENSE_QUOTA_USER_EXCEEDED.getCode(),
                    String.format("用户数量已达许可证授权上限（%d 个），如需更多请联系供应商升级许可证", max));
        }
    }

    /**
     * 校验许可证是否授权了指定功能模块。
     *
     * @param feature 功能模块标识（如 api_gateway, enterprise, system 等）
     * @throws BusinessException 未授权时抛出
     */
    public void checkFeatureAuthorized(String feature) {
        BmlLicense license = getEffectiveLicense();
        if (license == null) {
            return;
        }
        if (!license.hasFeature(feature)) {
            log.warn("[License] 功能模块未授权: {}", feature);
            throw new BusinessException(GlobalErrorCode.LICENSE_FEATURE_NOT_AUTHORIZED.getCode(),
                    String.format("许可证未授权「%s」功能模块，请联系供应商升级许可证", feature));
        }
    }

    /**
     * 获取当前生效的许可证。
     * <p>
     * 如果许可证校验未启用或无有效许可证，返回 {@code null}（表示不做配额限制）。
     * </p>
     *
     * @return 有效许可证，或 {@code null}
     */
    private BmlLicense getEffectiveLicense() {
        if (!licenseHolder.isEnabled()) {
            return null;
        }
        return licenseHolder.getCurrentLicense();
    }
}
