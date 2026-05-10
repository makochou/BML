package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.PageResult;
import com.bml.core.common.result.Result;
import com.bml.core.framework.operlog.BusinessType;
import com.bml.core.framework.operlog.OperationLog;
import com.bml.module.system.dto.SysNoticeDTO;
import com.bml.module.system.service.SysNoticeService;
import com.bml.module.system.vo.SysNoticeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "通知公告")
@RestController
@RequestMapping("/system/notice")
public class SysNoticeController extends BaseController {

    @Resource
    private SysNoticeService noticeService;

    @Operation(summary = "分页查询公告")
    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @GetMapping("/page")
    public Result<PageResult<SysNoticeVO>> page(SysNoticeDTO dto) {
        return Result.ok(noticeService.selectNoticePage(dto));
    }

    @Operation(summary = "查询公告详情")
    @PreAuthorize("@ss.hasPermi('system:notice:query')")
    @GetMapping("/{id}")
    public Result<SysNoticeVO> getInfo(@PathVariable Long id) {
        return Result.ok(noticeService.selectNoticeById(id));
    }

    @Operation(summary = "新增公告")
    @OperationLog(title = "通知公告", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('system:notice:add')")
    @PostMapping
    public Result<Void> add(@Validated @RequestBody SysNoticeDTO dto) {
        return toAjax(noticeService.insertNotice(dto));
    }

    @Operation(summary = "修改公告")
    @OperationLog(title = "通知公告", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('system:notice:edit')")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody SysNoticeDTO dto) {
        return toAjax(noticeService.updateNotice(dto));
    }

    @Operation(summary = "删除公告")
    @OperationLog(title = "通知公告", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('system:notice:remove')")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        return toAjax(noticeService.removeById(id));
    }

    @Operation(summary = "发布公告")
    @OperationLog(title = "通知公告", businessType = BusinessType.STATUS)
    @PreAuthorize("@ss.hasPermi('system:notice:publish')")
    @PutMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        return toAjax(noticeService.publishNotice(id));
    }

    @Operation(summary = "撤回公告")
    @OperationLog(title = "通知公告", businessType = BusinessType.STATUS)
    @PreAuthorize("@ss.hasPermi('system:notice:publish')")
    @PutMapping("/{id}/revoke")
    public Result<Void> revoke(@PathVariable Long id) {
        return toAjax(noticeService.revokeNotice(id));
    }
}
