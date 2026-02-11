package com.bml.module.api.service;

/**
 * API 同步服务接口
 *
 * @author BML Team
 */
public interface ApiSyncService {

    /**
     * 同步所有API接口
     * <p>
     * 扫描系统中的 Controller 和 RequestMapping，
     * 自动创建分组和接口信息，并更新到数据库。
     * </p>
     *
     * @return 同步数量信息
     */
    String syncAll();
}
