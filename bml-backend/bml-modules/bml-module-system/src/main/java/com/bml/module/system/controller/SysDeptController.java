package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.core.framework.security.utils.SecurityUtils;
import com.bml.module.system.entity.SysDept;
import com.bml.module.system.service.SysDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门信息
 *
 * @author BML Team
 */
@Tag(name = "部门管理")
@RestController
@RequestMapping("/system/dept")
public class SysDeptController extends BaseController {

    @Resource
    private SysDeptService deptService;

    @Operation(summary = "获取部门列表")
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/list")
    public Result<List<com.bml.module.system.vo.SysDeptVO>> list(com.bml.module.system.dto.SysDeptDTO dept) {
        List<SysDept> depts = deptService.selectDeptList(dept);
        return Result.ok(com.bml.module.system.converter.DeptConverter.INSTANCE.toVOList(depts));
    }

    @Operation(summary = "查询部门列表（排除节点）")
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/list/exclude/{deptId}")
    public Result<List<SysDept>> excludeChild(@PathVariable(value = "deptId", required = false) Long deptId) {
        List<SysDept> depts = deptService.selectDeptList(new com.bml.module.system.dto.SysDeptDTO());
        // TODO: Filter logic
        return Result.ok(depts);
    }

    @Operation(summary = "根据编号获取详细信息")
    @PreAuthorize("@ss.hasPermi('system:dept:query')")
    @GetMapping(value = "/{deptId}")
    public Result<com.bml.module.system.vo.SysDeptVO> getInfo(@PathVariable Long deptId) {
        return Result.ok(com.bml.module.system.converter.DeptConverter.INSTANCE.toVO(deptService.getById(deptId)));
    }

    @Operation(summary = "新增部门")
    @PreAuthorize("@ss.hasPermi('system:dept:add')")
    @PostMapping
    public Result<Void> add(@Validated @RequestBody com.bml.module.system.dto.SysDeptDTO dept) {
        if (deptService.checkDeptNameUnique(dept)) {
            return fail("新增部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        return toAjax(deptService.insertDept(dept));
    }

    @Operation(summary = "修改部门")
    @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody com.bml.module.system.dto.SysDeptDTO dept) {
        if (deptService.checkDeptNameUnique(dept)) {
            return fail("修改部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        return toAjax(deptService.updateDept(dept));
    }

    @Operation(summary = "删除部门")
    @PreAuthorize("@ss.hasPermi('system:dept:remove')")
    @DeleteMapping("/{deptId}")
    public Result<Void> remove(@PathVariable Long deptId) {
        return toAjax(deptService.removeById(deptId));
    }

    protected Result<Void> toAjax(boolean result) {
        return result ? success() : fail("操作失败");
    }
}
