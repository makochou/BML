package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.PageResult;
import com.bml.core.common.result.Result;
import com.bml.core.framework.operlog.BusinessType;
import com.bml.core.framework.operlog.OperationLog;
import com.bml.module.system.dto.SysJobDTO;
import com.bml.module.system.dto.SysJobLogQuery;
import com.bml.module.system.service.SysJobService;
import com.bml.module.system.vo.SysJobLogVO;
import com.bml.module.system.vo.SysJobVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "定时任务")
@RestController
@RequestMapping("/system/job")
public class SysJobController extends BaseController {

    @Resource
    private SysJobService jobService;

    @Operation(summary = "分页查询任务")
    @PreAuthorize("@ss.hasPermi('system:job:list')")
    @GetMapping("/page")
    public Result<PageResult<SysJobVO>> page(SysJobDTO dto) {
        return Result.ok(jobService.selectJobPage(dto));
    }

    @Operation(summary = "查询任务详情")
    @PreAuthorize("@ss.hasPermi('system:job:query')")
    @GetMapping("/{id}")
    public Result<SysJobVO> getInfo(@PathVariable Long id) {
        return Result.ok(jobService.selectJobById(id));
    }

    @Operation(summary = "新增任务")
    @OperationLog(title = "定时任务", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('system:job:add')")
    @PostMapping
    public Result<Void> add(@Validated @RequestBody SysJobDTO dto) {
        return toAjax(jobService.insertJob(dto));
    }

    @Operation(summary = "修改任务")
    @OperationLog(title = "定时任务", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('system:job:edit')")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody SysJobDTO dto) {
        return toAjax(jobService.updateJob(dto));
    }

    @Operation(summary = "删除任务")
    @OperationLog(title = "定时任务", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('system:job:remove')")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        return toAjax(jobService.deleteJob(id));
    }

    @Operation(summary = "立即运行任务")
    @OperationLog(title = "定时任务", businessType = BusinessType.OTHER)
    @PreAuthorize("@ss.hasPermi('system:job:run')")
    @PostMapping("/{id}/run")
    public Result<Void> run(@PathVariable Long id) {
        return toAjax(jobService.runOnce(id));
    }

    @Operation(summary = "修改任务状态")
    @OperationLog(title = "定时任务", businessType = BusinessType.STATUS)
    @PreAuthorize("@ss.hasPermi('system:job:changeStatus')")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        return toAjax(jobService.changeStatus(id, status));
    }

    @Operation(summary = "分页查询任务日志")
    @PreAuthorize("@ss.hasPermi('system:joblog:list')")
    @GetMapping("/log/page")
    public Result<PageResult<SysJobLogVO>> logPage(SysJobLogQuery query) {
        return Result.ok(jobService.selectLogPage(query));
    }

    @Operation(summary = "清空任务日志")
    @OperationLog(title = "任务日志", businessType = BusinessType.CLEAN)
    @PreAuthorize("@ss.hasPermi('system:joblog:clean')")
    @DeleteMapping("/log/clean")
    public Result<Void> cleanLogs() {
        return toAjax(jobService.cleanLogs());
    }

    @Operation(summary = "查询已注册调用目标")
    @PreAuthorize("@ss.hasPermi('system:job:list')")
    @GetMapping("/targets")
    public Result<List<String>> targets() {
        return Result.ok(jobService.registeredTargets());
    }
}
