package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.core.common.result.PageResult;
import com.bml.module.system.dto.SysLoginLogQuery;
import com.bml.module.system.entity.SysLoginLog;
import com.bml.module.system.vo.SysLoginLogVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 系统登录日志服务接口。
 * <p>
 * 登录日志属于安全审计数据，统一由服务层提供写入、查询、删除和清空能力，避免认证控制器直接感知日志表结构。
 * </p>
 *
 * @author BML Team
 */
public interface SysLoginLogService extends BaseService<SysLoginLog> {

    /**
     * 分页查询登录日志。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<SysLoginLogVO> selectLoginLogPage(SysLoginLogQuery query);

    /**
     * 根据日志ID查询详情。
     *
     * @param id 日志ID
     * @return 登录日志详情
     */
    SysLoginLogVO selectLoginLogById(Long id);

    /**
     * 记录一次认证审计事件。
     *
     * @param username 用户账号
     * @param success  是否成功
     * @param message  审计消息
     * @param request  HTTP请求，用于解析IP、浏览器与操作系统
     */
    void recordLoginEvent(String username, boolean success, String message, HttpServletRequest request);

    /**
     * 批量删除登录日志。
     *
     * @param ids 日志ID集合
     * @return 是否删除成功
     */
    boolean deleteLoginLogs(List<Long> ids);

    /**
     * 清空全部登录日志。
     *
     * @return 删除记录数
     */
    int cleanLoginLogs();
}
