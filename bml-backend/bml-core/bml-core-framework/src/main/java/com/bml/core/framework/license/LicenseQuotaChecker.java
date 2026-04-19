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
     * 校验 API 账号累计新增用户数是否超出许可证总配额。
     * <p>
     * 这是全局配额：所有 API 账号创建的累计活跃用户数不能超过该上限。
     * 在新增属于 API 创建的用户（createBy < 0）前调用。
     * </p>
     *
     * @param currentActiveCount 当前由 API 账号创建的活跃用户总数
     * @throws BusinessException 超出限制时抛出
     */
    public void checkApiUserQuota(long currentActiveCount) {
        BmlLicense license = getEffectiveLicense();
        if (license == null) {
            return;
        }
        int max = license.getMaxUsersPerAccount();
        if (max > 0 && currentActiveCount >= max) {
            log.warn("[License] API 累计新增用户数超限: 当前 {}, 上限 {}", currentActiveCount, max);
            throw new BusinessException(GlobalErrorCode.LICENSE_QUOTA_USER_EXCEEDED.getCode(),
                    String.format("操作失败：API 账号累计可创建活跃用户数已达许可证上限（%d 个）", max));
        }
    }

    /**
     * 校验是否允许启用（恢复）由 API 账号创建的用户。
     *
     * @param currentActiveCount 当前由 API 账号创建的活跃用户总数
     * @throws BusinessException 超出限制时抛出
     */
    public void checkEnableApiUserQuota(long currentActiveCount) {
        BmlLicense license = getEffectiveLicense();
        if (license == null) {
            return;
        }
        int max = license.getMaxUsersPerAccount();
        if (max > 0 && currentActiveCount >= max) {
            log.warn("[License] API 创建的用户启用被拒绝: 当前活跃用户 {}, 配额上限 {}", currentActiveCount, max);
            throw new BusinessException(GlobalErrorCode.LICENSE_QUOTA_USER_EXCEEDED.getCode(),
                    String.format("启用失败：API 账号累计可创建活跃用户数已达许可证上限（%d 个）", max));
        }
    }

    /**
     * 校验是否允许启用（恢复）用户。
     * <p>
     * 当管理员将已停用的用户重新启用时调用。
     * 如果当前活跃用户数已达配额上限，则拒绝启用操作。
     * </p>
     * <p>
     * 此方法与 {@link #checkUserQuota(long)} 配合，构成完整的用户配额防线：
     * <ul>
     *     <li>{@code checkUserQuota} — 阻止「新增」用户超额</li>
     *     <li>{@code checkEnableUserQuota} — 阻止「启用」用户超额</li>
     * </ul>
     * 避免通过手动启用被冻结的用户来绕过配额限制。
     * </p>
     *
     * @param currentActiveCount 当前处于正常状态（status=1）的用户数量（不含待启用的这个）
     * @throws BusinessException 启用后将超额时抛出
     */
    public void checkEnableUserQuota(long currentActiveCount) {
        BmlLicense license = getEffectiveLicense();
        if (license == null) {
            return;
        }
        int max = license.getMaxTotalUsers();
        if (max > 0 && currentActiveCount >= max) {
            log.warn("[License] 用户启用被拒绝: 当前活跃用户 {}, 配额上限 {}", currentActiveCount, max);
            throw new BusinessException(GlobalErrorCode.LICENSE_QUOTA_USER_EXCEEDED.getCode(),
                    String.format("启用失败：当前活跃用户数已达许可证上限（%d 个），请先停用其他用户或联系供应商升级许可证", max));
        }
    }

    /**
     * 校验是否允许启用（恢复）API 账号。
     * <p>
     * 当管理员将已停用的 API 账号重新启用时调用。
     * 如果当前活跃 API 账号数已达配额上限，则拒绝启用操作。
     * </p>
     * <p>
     * 此方法与 {@link #checkApiAccountQuota(long)} 配合，构成完整的 API 账号配额防线：
     * <ul>
     *     <li>{@code checkApiAccountQuota} — 阻止「新增」API 账号超额</li>
     *     <li>{@code checkEnableApiAccountQuota} — 阻止「启用」API 账号超额</li>
     * </ul>
     * </p>
     *
     * @param currentActiveCount 当前处于启用状态（status=1）的 API 账号数量（不含待启用的这个）
     * @throws BusinessException 启用后将超额时抛出
     */
    public void checkEnableApiAccountQuota(long currentActiveCount) {
        BmlLicense license = getEffectiveLicense();
        if (license == null) {
            return;
        }
        int max = license.getMaxApiAccounts();
        if (max > 0 && currentActiveCount >= max) {
            log.warn("[License] API 账号启用被拒绝: 当前活跃账号 {}, 配额上限 {}", currentActiveCount, max);
            throw new BusinessException(GlobalErrorCode.LICENSE_QUOTA_API_ACCOUNT_EXCEEDED.getCode(),
                    String.format("启用失败：当前活跃 API 账号数已达许可证上限（%d 个），请先停用其他账号或联系供应商升级许可证", max));
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
