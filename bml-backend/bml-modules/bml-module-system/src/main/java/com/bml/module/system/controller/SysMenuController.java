package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.common.result.Result;
import com.bml.core.framework.operlog.BusinessType;
import com.bml.core.framework.operlog.OperationLog;
import com.bml.module.system.dto.SysMenuDTO;
import com.bml.module.system.service.SysMenuService;
import com.bml.module.system.converter.MenuConverter;
import com.bml.module.system.vo.SysMenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单权限控制器。
 * <p>
 * 提供角色授权面板所需的权限数据查询能力，同时提供菜单、按钮、字段权限的标准 CRUD 能力。
 * 菜单管理是 RBAC 权限体系的元数据入口，所有新增菜单都应同步维护权限标识、组件路径和按钮权限。
 * </p>
 *
 * <h3>接口说明：</h3>
 * <table>
 * <tr><th>接口</th><th>用途</th><th>权限要求</th></tr>
 * <tr><td>GET /authTree</td><td>获取完整菜单授权树</td><td>角色管理相关权限</td></tr>
 * <tr><td>GET /permissionData</td><td>获取业务系统权限面板数据</td><td>角色管理相关权限</td></tr>
 * </table>
 *
 * @author BML Team
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/system/menu")
public class SysMenuController extends BaseController {

    @Resource
    private SysMenuService menuService;

    /**
     * 获取菜单授权树
     * <p>
     * 用于角色权限分配，返回完整的菜单树结构（含目录 M、菜单 C、按钮 B、字段 F 四种类型）。
     * 该接口无需 {@code system:menu:list} 权限，拥有角色管理权限即可调用。
     * </p>
     *
     * @return 完整菜单授权树
     */
    @Operation(summary = "获取菜单授权树（角色权限分配用）")
    @PreAuthorize("@ss.hasPermi('system:role:list') or @ss.hasPermi('system:role:edit') or @ss.hasPermi('system:role:add')")
    @GetMapping("/authTree")
    public Result<List<SysMenuVO>> authTree() {
        // 使用 SYSTEM_USER_ID 以超管身份获取全部菜单（含 M/C/B/F 全部类型）
        return Result.ok(MenuConverter.INSTANCE.toVOList(
                menuService.selectMenuList(new SysMenuDTO(), GlobalConstants.SYSTEM_USER_ID)));
    }

    /**
     * 获取权限分配面板数据（业务系统角色授权专用）
     * <p>
     * 专为角色权限分配三面板 UI 设计，返回扁平列表格式的<b>业务系统菜单</b>。
     * 仅包含"系统管理"（path='system'）目录及其所有子孙菜单，
     * 不包含中台管理的菜单（工作台、资产目录、授权治理、系统监控等）。
     * </p>
     * <p>
     * 前端根据 menuType 和 parentId 分组为：
     * <ul>
     *   <li>左面板：模块菜单（M 目录 + C 菜单），以树形展示</li>
     *   <li>中面板：功能按钮（B），按所属菜单分组展示</li>
     *   <li>右面板：表单字段（F），按所属菜单分组展示</li>
     * </ul>
     * </p>
     *
     * @return 扁平菜单列表（仅业务系统菜单）
     */
    @Operation(summary = "获取权限分配面板数据（角色授权三面板专用）")
    @PreAuthorize("@ss.hasPermi('system:role:list') or @ss.hasPermi('system:role:edit') or @ss.hasPermi('system:role:add')")
    @GetMapping("/permissionData")
    public Result<List<SysMenuVO>> permissionData() {
        return Result.ok(MenuConverter.INSTANCE.toVOList(
                menuService.selectPermissionMenuList()));
    }

    /**
     * 查询菜单管理树。
     *
     * @param dto 查询条件
     * @return 菜单树
     */
    @Operation(summary = "查询菜单管理树")
    @PreAuthorize("@ss.hasPermi('system:menu:list')")
    @GetMapping("/list")
    public Result<List<SysMenuVO>> list(SysMenuDTO dto) {
        return Result.ok(MenuConverter.INSTANCE.toVOList(menuService.selectMenuTree(dto)));
    }

    /**
     * 查询菜单详情。
     *
     * @param menuId 菜单ID
     * @return 菜单详情
     */
    @Operation(summary = "查询菜单详情")
    @PreAuthorize("@ss.hasPermi('system:menu:query')")
    @GetMapping("/{menuId}")
    public Result<SysMenuVO> getInfo(@PathVariable Long menuId) {
        return Result.ok(MenuConverter.INSTANCE.toVO(menuService.getById(menuId)));
    }

    /**
     * 新增菜单、按钮或字段权限。
     *
     * @param dto 菜单表单
     * @return 操作结果
     */
    @Operation(summary = "新增菜单")
    @OperationLog(title = "菜单管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('system:menu:add')")
    @PostMapping
    public Result<Void> add(@RequestBody SysMenuDTO dto) {
        return toAjax(menuService.insertMenu(dto));
    }

    /**
     * 修改菜单、按钮或字段权限。
     *
     * @param dto 菜单表单
     * @return 操作结果
     */
    @Operation(summary = "修改菜单")
    @OperationLog(title = "菜单管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @PutMapping
    public Result<Void> edit(@RequestBody SysMenuDTO dto) {
        return toAjax(menuService.updateMenu(dto));
    }

    /**
     * 删除菜单。
     *
     * @param menuId 菜单ID
     * @return 操作结果
     */
    @Operation(summary = "删除菜单")
    @OperationLog(title = "菜单管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('system:menu:remove')")
    @DeleteMapping("/{menuId}")
    public Result<Void> remove(@PathVariable Long menuId) {
        return toAjax(menuService.deleteMenu(menuId));
    }
}
