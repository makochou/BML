package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.module.system.dto.SysMenuDTO;
import com.bml.module.system.entity.SysMenu;
import com.bml.module.system.service.SysMenuService;
import com.bml.core.framework.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
 * <p>
 * 提供系统菜单（权限）的 CRUD 操作接口。菜单为树形结构，支持目录、菜单、按钮三种类型。
 * 所有接口均需要对应的权限标识。
 * </p>
 *
 * <h3>菜单类型说明：</h3>
 * <ul>
 * <li><b>M</b> — 目录（Directory），用于分组菜单项</li>
 * <li><b>C</b> — 菜单（Menu），对应前端的一个页面</li>
 * <li><b>F</b> — 按钮（Button），对应页面上的操作按钮（纯权限标识，无路由）</li>
 * </ul>
 *
 * <h3>权限标识说明：</h3>
 * <table>
 * <tr>
 * <th>操作</th>
 * <th>权限标识</th>
 * </tr>
 * <tr>
 * <td>查询菜单列表</td>
 * <td>{@code system:menu:list}</td>
 * </tr>
 * <tr>
 * <td>查询菜单详情</td>
 * <td>{@code system:menu:query}</td>
 * </tr>
 * <tr>
 * <td>新增菜单</td>
 * <td>{@code system:menu:add}</td>
 * </tr>
 * <tr>
 * <td>修改菜单</td>
 * <td>{@code system:menu:edit}</td>
 * </tr>
 * <tr>
 * <td>删除菜单</td>
 * <td>{@code system:menu:remove}</td>
 * </tr>
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
     * 获取菜单列表
     * <p>
     * 管理员可查看所有菜单，普通用户只能查看自己有权限的菜单。
     * </p>
     *
     * @param dto 查询条件（菜单名称、状态等）
     * @return 菜单列表
     */
    @Operation(summary = "获取菜单列表")
    @PreAuthorize("@ss.hasPermi('system:menu:list')")
    @GetMapping("/list")
    public Result<List<SysMenu>> list(SysMenuDTO dto) {
        Long userId = SecurityUtils.getUserId();
        return Result.ok(menuService.selectMenuList(dto, userId));
    }

    /**
     * 根据菜单编号获取详细信息
     *
     * @param menuId 菜单ID
     * @return 菜单详细信息
     */
    @Operation(summary = "根据菜单编号获取详细信息")
    @PreAuthorize("@ss.hasPermi('system:menu:query')")
    @GetMapping(value = "/{menuId}")
    public Result<SysMenu> getInfo(@PathVariable Long menuId) {
        return Result.ok(menuService.getById(menuId));
    }

    /**
     * 新增菜单
     *
     * @param dto 菜单信息
     * @return 操作结果
     */
    @Operation(summary = "新增菜单")
    @PreAuthorize("@ss.hasPermi('system:menu:add')")
    @PostMapping
    public Result<Void> add(@Validated @RequestBody SysMenuDTO dto) {
        if (!menuService.checkMenuNameUnique(dto)) {
            return Result.badRequest("新增菜单'" + dto.getMenuName() + "'失败，菜单名称已存在");
        }
        return toAjax(menuService.insertMenu(dto));
    }

    /**
     * 修改菜单
     *
     * @param dto 菜单信息
     * @return 操作结果
     */
    @Operation(summary = "修改菜单")
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody SysMenuDTO dto) {
        if (!menuService.checkMenuNameUnique(dto)) {
            return Result.badRequest("修改菜单'" + dto.getMenuName() + "'失败，菜单名称已存在");
        }
        return toAjax(menuService.updateMenu(dto));
    }

    /**
     * 删除菜单
     *
     * @param menuId 菜单ID
     * @return 操作结果
     */
    @Operation(summary = "删除菜单")
    @PreAuthorize("@ss.hasPermi('system:menu:remove')")
    @DeleteMapping("/{menuId}")
    public Result<Void> remove(@PathVariable Long menuId) {
        return toAjax(menuService.removeById(menuId));
    }
}
