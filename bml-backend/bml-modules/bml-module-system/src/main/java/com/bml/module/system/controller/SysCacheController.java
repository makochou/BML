package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.core.framework.operlog.BusinessType;
import com.bml.core.framework.operlog.OperationLog;
import com.bml.module.system.service.SysCacheService;
import com.bml.module.system.vo.SysCacheVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "缓存管理")
@RestController
@RequestMapping("/system/cache")
public class SysCacheController extends BaseController {

    @Resource
    private SysCacheService cacheService;

    @Operation(summary = "查询缓存概览")
    @PreAuthorize("@ss.hasPermi('system:cache:list')")
    @GetMapping("/overview")
    public Result<SysCacheVO> overview(@RequestParam(required = false) String pattern) {
        return Result.ok(cacheService.overview(pattern));
    }

    @Operation(summary = "查询缓存键")
    @PreAuthorize("@ss.hasPermi('system:cache:list')")
    @GetMapping("/keys")
    public Result<List<String>> keys(@RequestParam(required = false) String pattern) {
        return Result.ok(cacheService.keys(pattern));
    }

    @Operation(summary = "删除缓存键")
    @OperationLog(title = "缓存管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('system:cache:remove')")
    @DeleteMapping("/key")
    public Result<Void> deleteKey(@RequestParam String key) {
        return toAjax(cacheService.deleteKey(key));
    }

    @Operation(summary = "按前缀清理缓存")
    @OperationLog(title = "缓存管理", businessType = BusinessType.CLEAN)
    @PreAuthorize("@ss.hasPermi('system:cache:clear')")
    @DeleteMapping("/prefix")
    public Result<Void> clearPrefix(@RequestParam String prefix) {
        return toAjax(cacheService.clearPrefix(prefix));
    }
}
