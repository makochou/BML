package com.bml.module.enterprise.controller;

import com.bml.core.common.result.PageResult;
import com.bml.core.common.result.Result;
import com.bml.module.enterprise.dto.EnterpriseCompanyPageQuery;
import com.bml.module.enterprise.service.OpenEnterpriseDemoService;
import com.bml.module.enterprise.vo.OpenEnterpriseCompanyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业档案开放接口。
 */
@Tag(name = "企业开放接口-企业档案")
@RestController
@RequestMapping("/open/api/enterprise/company")
public class OpenEnterpriseCompanyController {

    private final OpenEnterpriseDemoService openEnterpriseDemoService;

    public OpenEnterpriseCompanyController(OpenEnterpriseDemoService openEnterpriseDemoService) {
        this.openEnterpriseDemoService = openEnterpriseDemoService;
    }

    @Operation(summary = "分页查询企业档案", description = "后续企业管理前台可通过该接口查询企业列表")
    @GetMapping("/page")
    public Result<PageResult<OpenEnterpriseCompanyVO>> page(EnterpriseCompanyPageQuery query) {
        return Result.ok(openEnterpriseDemoService.pageCompanies(query));
    }

    @Operation(summary = "查询企业档案详情", description = "根据企业ID返回企业档案详情")
    @GetMapping("/{companyId}")
    public Result<OpenEnterpriseCompanyVO> detail(@PathVariable Long companyId) {
        return Result.ok(openEnterpriseDemoService.getCompanyDetail(companyId));
    }
}
