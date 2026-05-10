package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.core.common.result.PageResult;
import com.bml.module.system.dto.SysOperationLogQuery;
import com.bml.module.system.entity.SysOperationLog;
import com.bml.module.system.vo.SysOperationLogVO;

import java.util.List;

/**
 * 系统操作日志服务接口。
 *
 * @author BML Team
 */
public interface SysOperationLogService extends BaseService<SysOperationLog> {

    /**
     * 分页查询操作日志。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<SysOperationLogVO> selectOperationLogPage(SysOperationLogQuery query);

    /**
     * 统计操作日志数量。
     * <p>
     * 操作日志只统计业务操作行为；登录、登出等认证类事件已由登录日志模块独立承载，避免在审计看板中重复计算。
     * </p>
     *
     * @param status 操作状态，传入 {@code null} 时统计全部状态
     * @return 操作日志数量
     */
    long countOperationLogs(Integer status);

    /**
     * 根据日志ID查询详情。
     *
     * @param id 日志ID
     * @return 日志详情
     */
    SysOperationLogVO selectOperationLogById(Long id);

    /**
     * 批量删除操作日志。
     *
     * @param ids 日志ID集合
     * @return 是否删除成功
     */
    boolean deleteOperationLogs(List<Long> ids);

    /**
     * 清空全部操作日志。
     *
     * @return 删除的记录数
     */
    int cleanOperationLogs();
}
