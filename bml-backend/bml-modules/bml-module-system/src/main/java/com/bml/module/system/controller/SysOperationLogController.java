package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.PageResult;
import com.bml.core.common.result.Result;
import com.bml.core.framework.operlog.BusinessType;
import com.bml.core.framework.operlog.OperationLog;
import com.bml.module.system.dto.SysOperationLogQuery;
import com.bml.module.system.service.SysOperationLogService;
import com.bml.module.system.vo.SysOperationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统操作日志控制器。
 * <p>
 * 提供操作日志分页查询、详情查看、批量删除和清空能力。该模块面向安全审计与问题追踪，
 * 仅授权用户可访问，不对普通业务用户开放。
 * </p>
 *
 * @author BML Team
 */
@Tag(name = "系统操作日志", description = "系统关键操作审计日志")
@RestController
@RequestMapping("/system/operlog")
@RequiredArgsConstructor
public class SysOperationLogController extends BaseController {

    private final SysOperationLogService operationLogService;

    /**
     * 分页查询系统操作日志。
     *
     * @param query 查询条件
     * @return 分页日志数据
     */
    @Operation(summary = "分页查询系统操作日志")
    @PreAuthorize("@ss.hasPermi('system:operlog:list')")
    @GetMapping("/page")
    public Result<PageResult<SysOperationLogVO>> page(SysOperationLogQuery query) {
        return Result.ok(operationLogService.selectOperationLogPage(query));
    }

    /**
     * 根据日志ID查询详情。
     *
     * @param id 日志ID
     * @return 日志详情
     */
    @Operation(summary = "查询系统操作日志详情")
    @PreAuthorize("@ss.hasPermi('system:operlog:query')")
    @GetMapping("/{id}")
    public Result<SysOperationLogVO> getInfo(@PathVariable Long id) {
        return Result.ok(operationLogService.selectOperationLogById(id));
    }

    /**
     * 批量删除系统操作日志。
     *
     * @param ids 日志ID列表
     * @return 操作结果
     */
    @Operation(summary = "批量删除系统操作日志")
    @OperationLog(title = "操作日志", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('system:operlog:remove')")
    @DeleteMapping
    public Result<Void> remove(@RequestBody List<Long> ids) {
        return toAjax(operationLogService.deleteOperationLogs(ids));
    }

    /**
     * 清空全部系统操作日志。
     *
     * @return 操作结果
     */
    @Operation(summary = "清空系统操作日志")
    @OperationLog(title = "操作日志", businessType = BusinessType.CLEAN)
    @PreAuthorize("@ss.hasPermi('system:operlog:clean')")
    @DeleteMapping("/clean")
    public Result<Void> clean() {
        return toAjax(operationLogService.cleanOperationLogs() >= 0);
    }
}
