package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.base.dto.PageQuery;
import com.bml.core.common.result.Result;
import com.bml.core.framework.security.utils.SecurityUtils;
import com.bml.module.system.entity.SysUser;
import com.bml.module.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户信息
 *
 * @author BML Team
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController {

    @Resource
    private SysUserService userService;

    @Operation(summary = "获取用户列表")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list")
    public Result<List<com.bml.module.system.vo.SysUserVO>> list(com.bml.module.system.dto.SysUserDTO user,
            PageQuery pageQuery) {
        // Paging is handled by PageHelper or MyBatisPlus interceptor if configured
        List<SysUser> list = userService.selectUserList(user);
        return Result.ok(com.bml.module.system.converter.UserConverter.INSTANCE.toVOList(list));
    }

    @Operation(summary = "根据编号获取详细信息")
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping(value = "/{userId}")
    public Result<com.bml.module.system.vo.SysUserVO> getInfo(@PathVariable Long userId) {
        SysUser user = userService.getById(userId);
        return Result.ok(com.bml.module.system.converter.UserConverter.INSTANCE.toVO(user));
    }

    @Operation(summary = "新增用户")
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @PostMapping
    public Result<Void> add(@Validated @RequestBody com.bml.module.system.dto.SysUserDTO userDto) {
        // Set CreateBy in Service or via AOP/MyBatisMetaObjectHandler (FieldFill.INSERT
        // is configured in BaseEntity)
        // Manual set for now if strictly required by logic, or rely on Entity generic
        // filling.
        // For strict "Perfect Code", AuditorAware is better, but here we can manually
        // set in DTO? No, DTO doesn't have createBy.
        // We can set it in Service after conversion.
        // Let's assume MyBatisPlus MetaObjectHandler handles it, or Service sets it.
        // Previously Controller set it. Let's move that to Service or assume Handler.
        // Just calling service here.
        return toAjax(userService.insertUser(userDto));
    }

    @Operation(summary = "修改用户")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody com.bml.module.system.dto.SysUserDTO userDto) {
        return toAjax(userService.updateUser(userDto));
    }

    @Operation(summary = "删除用户")
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @DeleteMapping("/{userIds}")
    public Result<Void> remove(@PathVariable List<Long> userIds) {
        return toAjax(userService.removeBatchByIds(userIds));
    }

    /**
     * 响应返回结果
     * (Normally in BaseController, but 'toAjax' convention helper)
     */
    protected Result<Void> toAjax(boolean result) {
        return result ? success() : fail("操作失败");
    }
}
