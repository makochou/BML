package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.PageResult;
import com.bml.core.common.result.Result;
import com.bml.module.system.converter.PostConverter;
import com.bml.module.system.dto.SysPostDTO;
import com.bml.module.system.service.SysPostService;
import com.bml.module.system.vo.SysPostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 岗位管理控制器
 * <p>
 * 提供系统岗位的 CRUD 操作接口。岗位为扁平列表结构。
 * 所有接口均需要对应的权限标识。
 * </p>
 *
 * <h3>权限标识说明：</h3>
 * <table>
 * <tr><th>操作</th><th>权限标识</th></tr>
 * <tr><td>查询岗位列表</td><td>{@code system:post:list}</td></tr>
 * <tr><td>查询岗位详情</td><td>{@code system:post:query}</td></tr>
 * <tr><td>新增岗位</td><td>{@code system:post:add}</td></tr>
 * <tr><td>修改岗位</td><td>{@code system:post:edit}</td></tr>
 * <tr><td>删除岗位</td><td>{@code system:post:remove}</td></tr>
 * </table>
 *
 * @author BML Team
 */
@Tag(name = "岗位管理")
@RestController
@RequestMapping("/system/post")
public class SysPostController extends BaseController {

    @Resource
    private SysPostService postService;

    /**
     * 获取岗位列表
     */
    @Operation(summary = "获取岗位列表")
    @PreAuthorize("@ss.hasPermi('system:post:list')")
    @GetMapping("/list")
    public Result<PageResult<SysPostVO>> list(SysPostDTO dto,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(postService.selectPostPage(dto, pageNum, pageSize));
    }

    /**
     * 根据岗位编号获取详细信息
     */
    @Operation(summary = "根据岗位编号获取详细信息")
    @PreAuthorize("@ss.hasPermi('system:post:query')")
    @GetMapping(value = "/{postId}")
    public Result<SysPostVO> getInfo(@PathVariable Long postId) {
        return Result.ok(PostConverter.INSTANCE.toVO(postService.getById(postId)));
    }

    /**
     * 新增岗位
     */
    @Operation(summary = "新增岗位")
    @PreAuthorize("@ss.hasPermi('system:post:add')")
    @PostMapping
    public Result<Void> add(@Validated @RequestBody SysPostDTO dto) {
        if (postService.checkPostCodeUnique(dto)) {
            return Result.badRequest("新增岗位'" + dto.getPostName() + "'失败，岗位编码已存在");
        }
        return toAjax(postService.insertPost(dto));
    }

    /**
     * 修改岗位
     */
    @Operation(summary = "修改岗位")
    @PreAuthorize("@ss.hasPermi('system:post:edit')")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody SysPostDTO dto) {
        if (postService.checkPostCodeUnique(dto)) {
            return Result.badRequest("修改岗位'" + dto.getPostName() + "'失败，岗位编码已存在");
        }
        return toAjax(postService.updatePost(dto));
    }

    /**
     * 删除岗位
     */
    @Operation(summary = "删除岗位")
    @PreAuthorize("@ss.hasPermi('system:post:remove')")
    @DeleteMapping("/{postId}")
    public Result<Void> remove(@PathVariable Long postId) {
        return toAjax(postService.removeById(postId));
    }
}
