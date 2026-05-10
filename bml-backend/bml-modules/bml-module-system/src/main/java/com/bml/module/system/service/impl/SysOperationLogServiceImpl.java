package com.bml.module.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.result.PageResult;
import com.bml.module.system.dto.SysOperationLogQuery;
import com.bml.module.system.entity.SysOperationLog;
import com.bml.module.system.mapper.SysOperationLogMapper;
import com.bml.module.system.service.SysOperationLogService;
import com.bml.module.system.vo.SysOperationLogVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统操作日志服务实现。
 * <p>
 * 统一封装操作日志的分页查询、详情查询、批量删除和清空能力。日志属于审计数据，查询默认按操作时间倒序，
 * 让审计人员优先看到最近发生的关键操作。
 * </p>
 *
 * @author BML Team
 */
@Service
public class SysOperationLogServiceImpl extends BaseServiceImpl<SysOperationLogMapper, SysOperationLog>
        implements SysOperationLogService {

    private static final int DEFAULT_PAGE_NUM = 1;

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAX_PAGE_SIZE = 200;

    private static final String LOGIN_LOG_TITLE = "登录日志";

    @Override
    public PageResult<SysOperationLogVO> selectOperationLogPage(SysOperationLogQuery query) {
        SysOperationLogQuery safeQuery = query == null ? new SysOperationLogQuery() : query;
        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        Page<SysOperationLog> page = this.page(new Page<>(pageNum, pageSize), buildQueryWrapper(safeQuery));
        List<SysOperationLogVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public long countOperationLogs(Integer status) {
        return this.count(buildVisibleOperationLogWrapper()
                .eq(status != null, SysOperationLog::getStatus, status));
    }

    @Override
    public SysOperationLogVO selectOperationLogById(Long id) {
        SysOperationLog operationLog = id == null ? null : this.getById(id);
        return operationLog == null ? null : toVO(operationLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteOperationLogs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return this.removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanOperationLogs() {
        return this.baseMapper.delete(new QueryWrapper<SysOperationLog>().isNotNull("id"));
    }

    private LambdaQueryWrapper<SysOperationLog> buildQueryWrapper(SysOperationLogQuery query) {
        LambdaQueryWrapper<SysOperationLog> wrapper = buildVisibleOperationLogWrapper();
        wrapper.like(StrUtil.isNotBlank(query.getTitle()), SysOperationLog::getTitle, query.getTitle())
                .eq(query.getBusinessType() != null, SysOperationLog::getBusinessType, query.getBusinessType())
                .eq(StrUtil.isNotBlank(query.getRequestMethod()), SysOperationLog::getRequestMethod,
                        normalizeRequestMethod(query.getRequestMethod()))
                .like(StrUtil.isNotBlank(query.getOperName()), SysOperationLog::getOperName, query.getOperName())
                .like(StrUtil.isNotBlank(query.getOperIp()), SysOperationLog::getOperIp, query.getOperIp())
                .eq(query.getStatus() != null, SysOperationLog::getStatus, query.getStatus())
                .ge(query.getBeginTime() != null, SysOperationLog::getOperTime, query.getBeginTime())
                .le(query.getEndTime() != null, SysOperationLog::getOperTime, query.getEndTime())
                .orderByDesc(SysOperationLog::getOperTime, SysOperationLog::getId);
        return wrapper;
    }

    private LambdaQueryWrapper<SysOperationLog> buildVisibleOperationLogWrapper() {
        return new LambdaQueryWrapper<SysOperationLog>().ne(SysOperationLog::getTitle, LOGIN_LOG_TITLE);
    }

    private SysOperationLogVO toVO(SysOperationLog operationLog) {
        SysOperationLogVO vo = new SysOperationLogVO();
        BeanUtils.copyProperties(operationLog, vo);
        return vo;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String normalizeRequestMethod(String requestMethod) {
        return StrUtil.blankToDefault(requestMethod, "").trim().toUpperCase();
    }
}
