package com.bml.module.api.controller;

import com.bml.core.common.result.Result;
import com.bml.core.framework.license.LicenseFeatureConstants;
import com.bml.core.framework.license.RequireFeature;
import com.bml.module.api.dto.OpenApiRegistryTreeQuery;
import com.bml.module.api.service.SysOpenApiRegistryService;
import com.bml.module.api.vo.ApiCatalogTreeNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * API 接口列表控制器。
 * <p>
 * 为「API 接口列表」功能菜单提供数据接口，以树形结构展示全量纳管 API：
 * 模块（如系统管理）→ 业务资源（如用户管理）→ 具体接口（如用户列表、新增用户）。
 * 数据来源于 {@code sys_api_registry}，与开放接口目录同步结果一致。
 * </p>
 *
 * <h3>权限说明：</h3>
 * <ul>
 * <li>查询接口目录树：{@code system:apiList:list}</li>
 * </ul>
 *
 * @author BML Team
 */
@Tag(name = "API 接口列表", description = "全量纳管 API 的树形展示，用于 API 接口列表页")
@RequireFeature(LicenseFeatureConstants.API_GATEWAY)
@RestController
@RequestMapping("/api-list")
public class SysApiListController {

    @Resource
    private SysOpenApiRegistryService openApiRegistryService;

    /**
     * 查询 API 接口目录树。
     * <p>
     * 返回三层树形结构，便于前端按「模块 > 业务 > 接口」展示；
     * 支持按关键词、HTTP 方法、状态、模块名筛选（与开放接口目录树查询参数一致）。
     * </p>
     *
     * @param query 可选查询条件：keyword、method、status、moduleName
     * @return 统一响应，data 为树节点列表 {@link List}{@code <}{@link ApiCatalogTreeNodeVO}{@code >}
     */
    @Operation(summary = "查询 API 接口目录树", description = "用于 API 接口列表页的树形展示，支持关键词等方法筛选")
    @PreAuthorize("@ss.hasPermi('system:apiList:list')")
    @GetMapping("/tree")
    public Result<List<ApiCatalogTreeNodeVO>> tree(OpenApiRegistryTreeQuery query) {
        return Result.ok(openApiRegistryService.listApiCatalogTree(query));
    }
}
