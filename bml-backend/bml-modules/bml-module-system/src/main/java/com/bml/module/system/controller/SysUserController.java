package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.PageResult;
import com.bml.core.common.result.Result;
import com.bml.module.system.dto.SysUserDTO;
import com.bml.module.system.entity.SysUser;
import com.bml.module.system.service.SysUserService;
import com.bml.module.system.vo.SysUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 * <p>
 * 提供系统用户的 CRUD 操作接口，所有接口均需要对应的权限标识。
 * </p>
 *
 * <h3>权限标识说明：</h3>
 * <table>
 * <tr>
 * <th>操作</th>
 * <th>权限标识</th>
 * </tr>
 * <tr>
 * <td>查询用户列表</td>
 * <td>{@code system:user:list}</td>
 * </tr>
 * <tr>
 * <td>查询用户详情</td>
 * <td>{@code system:user:query}</td>
 * </tr>
 * <tr>
 * <td>新增用户</td>
 * <td>{@code system:user:add}</td>
 * </tr>
 * <tr>
 * <td>修改用户</td>
 * <td>{@code system:user:edit}</td>
 * </tr>
 * <tr>
 * <td>删除用户</td>
 * <td>{@code system:user:remove}</td>
 * </tr>
 * </table>
 *
 * @author BML Team
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController {

    @Resource
    private SysUserService userService;

    /**
     * 获取用户列表（不分页，保留兼容）
     *
     * @param dto 查询条件（账号、手机号、状态等）
     * @return 用户列表
     */
    @Operation(summary = "获取用户列表")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list")
    public Result<List<SysUserVO>> list(SysUserDTO dto) {
        return Result.ok(userService.selectUserList(dto));
    }

    /**
     * 分页查询用户列表
     *
     * @param dto      查询条件（账号、手机号、状态等）
     * @param pageNum  当前页码（默认 1）
     * @param pageSize 每页条数（默认 20）
     * @return 分页结果（含 records、total 等字段）
     */
    @Operation(summary = "分页查询用户列表")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/page")
    public Result<PageResult<SysUserVO>> page(SysUserDTO dto,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(userService.selectUserPage(dto, pageNum, pageSize));
    }

    /**
     * 根据用户编号获取详细信息
     *
     * @param userId 用户ID
     * @return 用户详细信息
     */
    @Operation(summary = "根据用户编号获取详细信息")
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping(value = "/{userId}")
    public Result<SysUserVO> getInfo(@PathVariable Long userId) {
        return Result.ok(userService.selectUserById(userId));
    }

    /**
     * 新增用户
     *
     * @param dto 用户信息
     * @return 操作结果
     */
    @Operation(summary = "新增用户")
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @PostMapping
    public Result<Void> add(@Validated @RequestBody SysUserDTO dto) {
        return toAjax(userService.insertUser(dto));
    }

    /**
     * 修改用户
     *
     * @param dto 用户信息
     * @return 操作结果
     */
    @Operation(summary = "修改用户")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody SysUserDTO dto) {
        return toAjax(userService.updateUser(dto));
    }

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @Operation(summary = "删除用户")
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @DeleteMapping("/{userId}")
    public Result<Void> remove(@PathVariable Long userId) {
        // 校验是否允许操作（超级管理员不可删除）
        SysUser user = new SysUser();
        user.setId(userId);
        userService.checkUserAllowed(user);
        return toAjax(userService.removeById(userId));
    }

    @PutMapping("/resetPwd")
    @PreAuthorize("@ss.hasPermi('system:user:resetPwd')")
    @Operation(summary = "重置用户密码")
    public Result<Void> resetPwd(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String password = (String) body.get("password");
        userService.resetUserPassword(userId, password);
        return Result.ok();
    }
}
