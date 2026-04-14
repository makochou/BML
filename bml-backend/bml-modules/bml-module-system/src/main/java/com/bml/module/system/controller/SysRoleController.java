package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.core.framework.license.LicenseFeatureConstants;
import com.bml.core.framework.license.RequireFeature;
import com.bml.module.system.converter.RoleConverter;
import com.bml.module.system.dto.SysRoleDTO;
import com.bml.module.system.entity.SysRole;
import com.bml.module.system.service.SysRoleService;
import com.bml.module.system.vo.SysRoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 * <p>
 * 提供系统角色的 CRUD 操作接口，所有接口均需要对应的权限标识。
 * 角色与菜单权限通过 {@code sys_role_menu} 中间表关联。
 * </p>
 *
 * <h3>权限标识说明：</h3>
 * <table>
 * <tr>
 * <th>操作</th>
 * <th>权限标识</th>
 * </tr>
 * <tr>
 * <td>查询角色列表</td>
 * <td>{@code system:role:list}</td>
 * </tr>
 * <tr>
 * <td>查询角色详情</td>
 * <td>{@code system:role:query}</td>
 * </tr>
 * <tr>
 * <td>新增角色</td>
 * <td>{@code system:role:add}</td>
 * </tr>
 * <tr>
 * <td>修改角色</td>
 * <td>{@code system:role:edit}</td>
 * </tr>
 * <tr>
 * <td>删除角色</td>
 * <td>{@code system:role:remove}</td>
 * </tr>
 * </table>
 *
 * @author BML Team
 */
@Tag(name = "角色管理")
@RequireFeature(LicenseFeatureConstants.SYSTEM)
@RestController
@RequestMapping("/system/role")
public class SysRoleController extends BaseController {

    @Resource
    private SysRoleService roleService;

    /**
     * 获取角色列表
     *
     * @param dto 查询条件（角色名称、角色编码、状态等）
     * @return 角色列表
     */
    @Operation(summary = "获取角色列表")
    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/list")
    public Result<List<SysRoleVO>> list(SysRoleDTO dto) {
        return Result.ok(RoleConverter.INSTANCE.toVOList(roleService.selectRoleList(dto)));
    }

    /**
     * 根据角色编号获取详细信息
     *
     * @param roleId 角色ID
     * @return 角色详细信息
     */
    @Operation(summary = "根据角色编号获取详细信息")
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping(value = "/{roleId}")
    public Result<SysRoleVO> getInfo(@PathVariable Long roleId) {
        return Result.ok(RoleConverter.INSTANCE.toVO(roleService.getById(roleId)));
    }

    /**
     * 新增角色
     * <p>
     * 新增角色时，可同时关联菜单权限（通过 {@code menuIds} 字段）。
     * </p>
     *
     * @param dto 角色信息（含 menuIds）
     * @return 操作结果
     */
    @Operation(summary = "新增角色")
    @PreAuthorize("@ss.hasPermi('system:role:add')")
    @PostMapping
    public Result<Void> add(@Validated @RequestBody SysRoleDTO dto) {
        return toAjax(roleService.insertRole(dto));
    }

    /**
     * 修改角色
     * <p>
     * 修改角色时，会重新关联菜单权限（先删除旧关联，再插入新关联）。
     * </p>
     *
     * @param dto 角色信息（含 menuIds）
     * @return 操作结果
     */
    @Operation(summary = "修改角色")
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody SysRoleDTO dto) {
        return toAjax(roleService.updateRole(dto));
    }

    /**
     * 删除角色
     *
     * @param roleId 角色ID
     * @return 操作结果
     */
    @Operation(summary = "删除角色")
    @PreAuthorize("@ss.hasPermi('system:role:remove')")
    @DeleteMapping("/{roleId}")
    public Result<Void> remove(@PathVariable Long roleId) {
        return toAjax(roleService.removeById(roleId));
    }
}
