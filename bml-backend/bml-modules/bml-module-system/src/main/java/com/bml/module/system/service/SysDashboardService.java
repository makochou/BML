package com.bml.module.system.service;

import com.bml.module.system.vo.DashboardSummaryVO;

/**
 * 工作台监控展板服务接口
 */
public interface SysDashboardService {

    /**
     * 获取管理工作台聚合数据
     * @return 聚合数据项
     */
    DashboardSummaryVO getDashboardSummary();
}
