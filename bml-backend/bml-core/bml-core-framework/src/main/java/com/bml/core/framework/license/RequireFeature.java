package com.bml.core.framework.license;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 许可证功能模块授权注解。
 * <p>
 * 标注在 Controller 类上，表示该控制器的所有接口均需要许可证授权对应的功能模块。
 * 当许可证中不包含指定功能模块时，接口将返回 {@code LICENSE_FEATURE_NOT_AUTHORIZED(2212)} 错误。
 * </p>
 * <p>
 * 使用示例：
 * <pre>
 *   &#64;RequireFeature(LicenseFeatureConstants.ALERT)
 *   &#64;RestController
 *   &#64;RequestMapping("/system/alert")
 *   public class SysAlertController { ... }
 * </pre>
 * </p>
 *
 * @author BML Team
 * @see LicenseFeatureConstants
 * @see LicenseFeatureAspect
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireFeature {

    /**
     * 功能模块标识码。
     * <p>
     * 推荐使用 {@link LicenseFeatureConstants} 中的常量。
     * </p>
     *
     * @return 功能模块标识码
     */
    String value();
}
