package com.bml.core.framework.operlog;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 系统操作日志注解。
 * <p>
 * 标注在 Controller 方法上，由系统模块的操作日志切面统一采集请求信息、执行结果、异常信息和耗时，
 * 并写入 sys_operation_log 表。业务代码只需要声明模块标题和业务类型，即可完成标准审计留痕。
 * </p>
 *
 * @author BML Team
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /**
     * 模块标题，例如“用户管理”“API账号管理”。
     */
    String title();

    /**
     * 业务类型，例如新增、修改、删除、重置等。
     */
    BusinessType businessType() default BusinessType.OTHER;

    /**
     * 操作人类别，默认后台用户。
     */
    OperatorType operatorType() default OperatorType.MANAGE;

    /**
     * 是否保存请求参数。
     */
    boolean saveRequestData() default true;

    /**
     * 是否保存响应结果。
     */
    boolean saveResponseData() default true;
}
