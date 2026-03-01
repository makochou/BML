package com.bml.module.enterprise.controller;

import com.bml.core.common.result.Result;
import com.bml.module.enterprise.service.OpenEnterpriseDemoService;
import com.bml.module.enterprise.vo.OpenEnterpriseDashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业管理业务概览开放接口。
 */
@Tag(name = "企业开放接口-业务概览")
@RestController
@RequestMapping("/open/api/enterprise/dashboard")
public class OpenEnterpriseDashboardController {

    private final OpenEnterpriseDemoService openEnterpriseDemoService;

    public OpenEnterpriseDashboardController(OpenEnterpriseDemoService openEnterpriseDemoService) {
        this.openEnterpriseDemoService = openEnterpriseDemoService;
    }

    @Operation(summary = "查询企业管理业务概览", description = "为企业管理前台提供首页概览数据样板")
    @GetMapping("/summary")
    public Result<OpenEnterpriseDashboardVO> summary() {
        return Result.ok(openEnterpriseDemoService.getDashboardSummary());
    }
}
