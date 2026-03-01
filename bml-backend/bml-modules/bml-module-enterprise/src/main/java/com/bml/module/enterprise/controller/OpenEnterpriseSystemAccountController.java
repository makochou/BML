package com.bml.module.enterprise.controller;

import com.bml.core.common.result.PageResult;
import com.bml.core.common.result.Result;
import com.bml.module.enterprise.dto.EnterpriseSystemAccountPageQuery;
import com.bml.module.enterprise.service.OpenEnterpriseDemoService;
import com.bml.module.enterprise.vo.OpenEnterpriseSystemAccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业系统账号开放接口。
 */
@Tag(name = "企业开放接口-系统账号")
@RestController
@RequestMapping("/open/api/enterprise/system-account")
public class OpenEnterpriseSystemAccountController {

    private final OpenEnterpriseDemoService openEnterpriseDemoService;

    public OpenEnterpriseSystemAccountController(OpenEnterpriseDemoService openEnterpriseDemoService) {
        this.openEnterpriseDemoService = openEnterpriseDemoService;
    }

    @Operation(summary = "分页查询企业系统账号", description = "为企业管理前台提供系统账号列表查询样板")
    @GetMapping("/page")
    public Result<PageResult<OpenEnterpriseSystemAccountVO>> page(EnterpriseSystemAccountPageQuery query) {
        return Result.ok(openEnterpriseDemoService.pageSystemAccounts(query));
    }

    @Operation(summary = "查询企业系统账号详情", description = "根据系统账号ID返回企业账号详情")
    @GetMapping("/{accountId}")
    public Result<OpenEnterpriseSystemAccountVO> detail(@PathVariable Long accountId) {
        return Result.ok(openEnterpriseDemoService.getSystemAccountDetail(accountId));
    }
}
