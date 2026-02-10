package com.bml.module.api.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.module.api.dto.ApiInfoDTO;
import com.bml.module.api.entity.ApiInfo;
import com.bml.module.api.service.ApiInfoService;
import com.bml.module.api.vo.ApiInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API接口管理控制器
 *
 * @author BML Team
 */
@Tag(name = "API接口管理")
@RestController
@RequestMapping("/api/info")
public class ApiInfoController extends BaseController {

    @Resource
    private ApiInfoService apiInfoService;

    @Operation(summary = "获取API列表")
    @GetMapping("/list")
    public Result<List<ApiInfoVO>> list(ApiInfoDTO dto) {
        return Result.ok(apiInfoService.selectApiList(dto));
    }

    @Operation(summary = "根据编号获取详细信息")
    @GetMapping(value = "/{apiId}")
    public Result<ApiInfo> getInfo(@PathVariable Long apiId) {
        return Result.ok(apiInfoService.getById(apiId));
    }

    @Operation(summary = "新增API")
    @PostMapping
    public Result<Void> add(@Validated @RequestBody ApiInfoDTO dto) {
        return toAjax(apiInfoService.insertApi(dto));
    }

    @Operation(summary = "修改API")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody ApiInfoDTO dto) {
        return toAjax(apiInfoService.updateApi(dto));
    }

    @Operation(summary = "删除API")
    @DeleteMapping("/{apiId}")
    public Result<Void> remove(@PathVariable Long apiId) {
        return toAjax(apiInfoService.removeById(apiId));
    }
    
    protected Result<Void> toAjax(boolean result) {
        return result ? success() : fail("操作失败");
    }
}
