package com.bml.module.api.controller;

import com.bml.core.common.result.Result;
import com.bml.module.api.dto.ApiCallbackLogPageQuery;
import com.bml.module.api.service.SysApiCallbackLogService;
import com.bml.module.api.vo.ApiCallbackLogPageVO;
import com.bml.module.api.vo.ApiCallbackLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API 账号回调日志与重试控制器。
 */
@Tag(name = "API账号回调日志管理")
@RestController
@RequestMapping("/account")
public class SysApiAccountCallbackController {

    private final SysApiCallbackLogService callbackLogService;

    public SysApiAccountCallbackController(SysApiCallbackLogService callbackLogService) {
        this.callbackLogService = callbackLogService;
    }

    @Operation(summary = "分页查询指定账号的回调日志")
    @PreAuthorize("@ss.hasPermi('api:account:query')")
    @GetMapping("/{id}/callback-log/page")
    public Result<ApiCallbackLogPageVO> pageLogs(@PathVariable Long id, ApiCallbackLogPageQuery query) {
        return Result.ok(callbackLogService.pageLogs(id, query));
    }

    @Operation(summary = "发送测试回调")
    @PreAuthorize("@ss.hasPermi('api:account:edit')")
    @PostMapping("/{id}/callback/test")
    public Result<ApiCallbackLogVO> triggerTestCallback(@PathVariable Long id) {
        return Result.ok(callbackLogService.triggerTestCallback(id));
    }

    @Operation(summary = "手动重试回调日志")
    @PreAuthorize("@ss.hasPermi('api:account:edit')")
    @PostMapping("/callback-log/{logId}/retry")
    public Result<ApiCallbackLogVO> retryCallback(@PathVariable Long logId) {
        return Result.ok(callbackLogService.retryNow(logId));
    }
}
