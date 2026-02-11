package com.bml.module.api.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.module.api.dto.ApiGroupDTO;
import com.bml.module.api.entity.ApiGroup;
import com.bml.module.api.service.ApiGroupService;
import com.bml.module.api.vo.ApiGroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API分组控制器
 *
 * @author BML Team
 */
@Tag(name = "API分组管理")
@RestController
@RequestMapping("/api/group")
public class ApiGroupController extends BaseController {

    @Resource
    private ApiGroupService apiGroupService;

    @Operation(summary = "获取分组列表")
    @GetMapping("/list")
    public Result<List<ApiGroupVO>> list(ApiGroupDTO dto) {
        return Result.ok(apiGroupService.selectGroupList(dto));
    }

    @Operation(summary = "根据编号获取详细信息")
    @GetMapping(value = "/{groupId}")
    public Result<ApiGroup> getInfo(@PathVariable Long groupId) {
        return Result.ok(apiGroupService.getById(groupId));
    }

    @Operation(summary = "新增分组")
    @PostMapping
    public Result<Void> add(@Validated @RequestBody ApiGroupDTO dto) {
        return toAjax(apiGroupService.insertGroup(dto));
    }

    @Operation(summary = "修改分组")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody ApiGroupDTO dto) {
        return toAjax(apiGroupService.updateGroup(dto));
    }

    @Operation(summary = "删除分组")
    @DeleteMapping("/{groupId}")
    public Result<Void> remove(@PathVariable Long groupId) {
        return toAjax(apiGroupService.removeById(groupId));
    }
}
