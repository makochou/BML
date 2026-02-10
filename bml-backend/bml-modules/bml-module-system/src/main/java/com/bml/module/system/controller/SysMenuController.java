package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.core.framework.security.utils.SecurityUtils;
import com.bml.module.system.entity.SysMenu;
import com.bml.module.system.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单信息
 *
 * @author BML Team
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/system/menu")
public class SysMenuController extends BaseController {

    @Resource
    private SysMenuService menuService;

    @Operation(summary = "获取菜单列表")
    @PreAuthorize("@ss.hasPermi('system:menu:list')")
    @GetMapping("/list")
    public Result<List<com.bml.module.system.vo.SysMenuVO>> list(com.bml.module.system.dto.SysMenuDTO menu) {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> list = menuService.selectMenuList(menu, userId);
        return Result.ok(com.bml.module.system.converter.MenuConverter.INSTANCE.toVOList(list));
    }

    @Operation(summary = "根据编号获取详细信息")
    @PreAuthorize("@ss.hasPermi('system:menu:query')")
    @GetMapping(value = "/{menuId}")
    public Result<com.bml.module.system.vo.SysMenuVO> getInfo(@PathVariable Long menuId) {
        return Result.ok(com.bml.module.system.converter.MenuConverter.INSTANCE.toVO(menuService.getById(menuId)));
    }

    @Operation(summary = "新增菜单")
    @PreAuthorize("@ss.hasPermi('system:menu:add')")
    @PostMapping
    public Result<Void> add(@Validated @RequestBody com.bml.module.system.dto.SysMenuDTO menu) {
        if (menuService.checkMenuNameUnique(menu)) {
            // return fail("新增菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
            // Allowing duplication for now or need better check logic
        }
        return toAjax(menuService.insertMenu(menu));
    }

    @Operation(summary = "修改菜单")
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody com.bml.module.system.dto.SysMenuDTO menu) {
        if (menuService.checkMenuNameUnique(menu)) {
            // return fail...
        }
        return toAjax(menuService.updateMenu(menu));
    }

    @Operation(summary = "删除菜单")
    @PreAuthorize("@ss.hasPermi('system:menu:remove')")
    @DeleteMapping("/{menuId}")
    public Result<Void> remove(@PathVariable Long menuId) {
        return toAjax(menuService.removeById(menuId));
    }

    protected Result<Void> toAjax(boolean result) {
        return result ? success() : fail("操作失败");
    }
}
