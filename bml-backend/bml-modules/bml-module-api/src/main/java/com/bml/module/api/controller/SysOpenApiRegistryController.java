package com.bml.module.api.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.module.api.dto.OpenApiRegistryTreeQuery;
import com.bml.module.api.service.SysOpenApiRegistryService;
import com.bml.module.api.vo.OpenApiGroupVO;
import com.bml.module.api.vo.OpenApiRegistrySyncResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 开放接口目录控制器。
 */
@Tag(name = "开放接口目录管理")
@RestController
@RequestMapping("/openapi/registry")
public class SysOpenApiRegistryController extends BaseController {

    private final SysOpenApiRegistryService openApiRegistryService;

    public SysOpenApiRegistryController(SysOpenApiRegistryService openApiRegistryService) {
        this.openApiRegistryService = openApiRegistryService;
    }

    @Operation(summary = "查询开放接口目录树")
    @PreAuthorize("@ss.hasPermi('api:account:authorize')")
    @GetMapping("/tree")
    public Result<List<OpenApiGroupVO>> tree(OpenApiRegistryTreeQuery query) {
        return Result.ok(openApiRegistryService.listRegistryTree(query));
    }

    @Operation(summary = "同步开放接口目录")
    @PreAuthorize("@ss.hasPermi('api:account:sync')")
    @PostMapping("/sync")
    public Result<OpenApiRegistrySyncResultVO> sync() {
        return Result.ok(openApiRegistryService.syncRegistry());
    }
}
