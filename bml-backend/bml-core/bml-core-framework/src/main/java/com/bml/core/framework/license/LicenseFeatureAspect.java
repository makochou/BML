package com.bml.core.framework.license;

import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 许可证功能模块授权切面。
 * <p>
 * 拦截标注了 {@link RequireFeature} 的 Controller 类（或方法），
 * 检查当前许可证是否包含所需的功能模块。
 * 未授权时抛出 {@link BusinessException}，前端统一拦截处理。
 * </p>
 * <p>
 * 优先级设为 1（仅次于许可证拦截器），确保在业务逻辑之前执行。
 * </p>
 *
 * @author BML Team
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class LicenseFeatureAspect {

    private final BmlLicenseHolder licenseHolder;

    public LicenseFeatureAspect(BmlLicenseHolder licenseHolder) {
        this.licenseHolder = licenseHolder;
    }

    /**
     * 拦截类级别的 {@link RequireFeature} 注解。
     * <p>
     * 当 Controller 类标注了 {@code @RequireFeature("xxx")} 时，
     * 该类的所有 public 方法在执行前都会检查许可证是否授权了对应模块。
     * </p>
     *
     * @param joinPoint 切点
     * @param requireFeature 注解实例
     */
    @Before("@within(requireFeature)")
    public void checkClassLevelFeature(JoinPoint joinPoint, RequireFeature requireFeature) {
        doCheck(requireFeature.value(), joinPoint);
    }

    /**
     * 拦截方法级别的 {@link RequireFeature} 注解。
     * <p>
     * 支持在单个方法上标注，用于更细粒度的功能模块控制。
     * 方法级别注解优先于类级别。
     * </p>
     *
     * @param joinPoint 切点
     * @param requireFeature 注解实例
     */
    @Before("@annotation(requireFeature)")
    public void checkMethodLevelFeature(JoinPoint joinPoint, RequireFeature requireFeature) {
        doCheck(requireFeature.value(), joinPoint);
    }

    /**
     * 执行功能模块授权检查。
     *
     * @param featureCode 功能模块标识码
     * @param joinPoint 切点（用于日志）
     */
    private void doCheck(String featureCode, JoinPoint joinPoint) {
        // 许可证校验未启用，直接放行
        if (!licenseHolder.isEnabled()) {
            return;
        }

        // 许可证未加载（由拦截器处理，此处不重复拦截）
        BmlLicense license = licenseHolder.getCurrentLicense();
        if (license == null) {
            return;
        }

        // 检查功能模块是否已授权
        if (!license.hasFeature(featureCode)) {
            String label = LicenseFeatureConstants.getFeatureLabel(featureCode);
            log.debug("[License] 功能模块未授权: {} ({}), 请求: {}.{}()",
                    label, featureCode,
                    joinPoint.getSignature().getDeclaringType().getSimpleName(),
                    joinPoint.getSignature().getName());
            throw new BusinessException(
                    GlobalErrorCode.LICENSE_FEATURE_NOT_AUTHORIZED.getCode(),
                    String.format("许可证未授权「%s」功能模块，请联系供应商升级许可证", label));
        }
    }
}
