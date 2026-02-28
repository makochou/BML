package com.bml.module.system.datascope;

import java.lang.annotation.*;

/**
 * 数据权限注解。
 * <p>
 * 标注在 Service 查询方法上，用于声明当前查询涉及的数据列。
 * AOP 会根据当前登录用户的角色 dataScope 自动拼接 SQL 条件。
 * </p>
 *
 * @author BML Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 部门字段列名，例如：dept_id 或 u.dept_id。
     */
    String deptColumn() default "";

    /**
     * 组织字段列名，例如：org_id 或 u.org_id。
     */
    String orgColumn() default "";

    /**
     * 用户主键字段列名，例如：id 或 u.id。
     */
    String userColumn() default "";

    /**
     * 创建人字段列名，例如：create_by。
     * 当业务表不包含组织/部门字段时，可回退按创建人限制。
     */
    String creatorColumn() default "";
}
