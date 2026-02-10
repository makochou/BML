package com.bml.core.common.constant;

/**
 * 全局常量
 *
 * @author BML Team
 */
public interface GlobalConstants {

    /**
     * 链路追踪ID Key
     */
    String TRACE_ID = "traceId";

    /**
     * 系统用户ID
     */
    Long SYSTEM_USER_ID = 0L;

    /**
     * 根节点ID
     */
    Long ROOT_ID = 0L;

    /**
     * 正常状态
     */
    Integer STATUS_NORMAL = 1;

    /**
     * 禁用状态
     */
    Integer STATUS_DISABLE = 0;

    /**
     * 删除标记: 未删除
     */
    Integer NOT_DELETED = 0;

    /**
     * 删除标记: 已删除
     */
    Integer DELETED = 1;
}
