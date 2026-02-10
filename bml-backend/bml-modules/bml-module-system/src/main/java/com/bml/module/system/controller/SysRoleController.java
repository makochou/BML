package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.core.framework.security.utils.SecurityUtils;
import com.bml.module.system.entity.SysRole;
import com.bml.module.system.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色信息
 *
 * @author BML Team
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/system/role")
public class SysRoleController extends BaseController {

    @Resource
    private SysRoleService roleService;

    @Operation(summary = "获取角色列表")
    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/list")
    public Result<List<com.bml.module.system.vo.SysRoleVO>> list(com.bml.module.system.dto.SysRoleDTO role) {
        List<SysRole> list = roleService.selectRoleList(role);
        return Result.ok(com.bml.module.system.converter.RoleConverter.INSTANCE.toVOList(list));
    }

    @Operation(summary = "根据编号获取详细信息")
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping(value = "/{roleId}")
    public Result<com.bml.module.system.vo.SysRoleVO> getInfo(@PathVariable Long roleId) {
        return Result.ok(com.bml.module.system.converter.RoleConverter.INSTANCE.toVO(roleService.getById(roleId)));
    }

    @Operation(summary = "新增角色")
    @PreAuthorize("@ss.hasPermi('system:role:add')")
    @PostMapping
    public Result<Void> add(@Validated @RequestBody com.bml.module.system.dto.SysRoleDTO role) {
        return toAjax(roleService.insertRole(role));
    }

    @Operation(summary = "修改角色")
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody com.bml.module.system.dto.SysRoleDTO role) {
        return toAjax(roleService.updateRole(role));
    }

    @Operation(summary = "删除角色")
    @PreAuthorize("@ss.hasPermi('system:role:remove')")
    @DeleteMapping("/{roleIds}")
    public Result<Void> remove(@PathVariable List<Long> roleIds) {
        return toAjax(roleService.removeBatchByIds(roleIds));
    }

    protected Result<Void> toAjax(boolean result) {
        return result ? success() : fail("操作失败");
    }
}
