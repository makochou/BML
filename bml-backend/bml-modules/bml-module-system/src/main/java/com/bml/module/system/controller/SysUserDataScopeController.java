package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.module.system.entity.SysUserDataScope;
import com.bml.module.system.service.SysUserDataScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户个人数据权限控制器
 * <p>
 * 提供用户个人数据权限的查询和配置接口。
 * 在用户管理页面中嵌入使用，为特定用户配置独立的数据权限。
 * </p>
 *
 * @author BML Team
 */
@Tag(name = "用户个人数据权限")
@RestController
@RequestMapping("/system/user/datascope")
public class SysUserDataScopeController extends BaseController {

    @Resource
    private SysUserDataScopeService userDataScopeService;

    /**
     * 查询用户个人数据权限配置
     *
     * @param userId 用户ID
     * @return 数据权限配置，不存在则返回 null
     */
    @Operation(summary = "查询用户个人数据权限配置")
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/{userId}")
    public Result<SysUserDataScope> getByUserId(@PathVariable Long userId) {
        return Result.ok(userDataScopeService.selectActiveByUserId(userId));
    }

    /**
     * 保存或更新用户个人数据权限配置
     *
     * @param dataScope 数据权限配置
     * @return 操作结果
     */
    @Operation(summary = "保存用户个人数据权限配置")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PostMapping
    public Result<Void> save(@RequestBody SysUserDataScope dataScope) {
        return toAjax(userDataScopeService.saveOrUpdateByUserId(dataScope));
    }

    /**
     * 删除用户个人数据权限配置
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @Operation(summary = "删除用户个人数据权限配置")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @DeleteMapping("/{userId}")
    public Result<Void> remove(@PathVariable Long userId) {
        return toAjax(userDataScopeService.removeByUserId(userId));
    }
}
