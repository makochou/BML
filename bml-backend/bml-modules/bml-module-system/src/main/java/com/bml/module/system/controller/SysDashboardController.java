package com.bml.module.system.controller;

import com.bml.core.common.result.Result;
import com.bml.module.system.service.SysDashboardService;
import com.bml.module.system.vo.DashboardSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "首页管理", description = "首页管理平台大盘统计聚合接口")
@RestController
@RequestMapping("/system/dashboard")
@RequiredArgsConstructor
public class SysDashboardController {

    private final SysDashboardService sysDashboardService;

    @Operation(summary = "获取控制台聚合数据")
    @GetMapping("/summary")
    public Result<DashboardSummaryVO> getSummary() {
        DashboardSummaryVO summary = sysDashboardService.getDashboardSummary();
        return Result.ok(summary);
    }
}
