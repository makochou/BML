package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.core.framework.operlog.BusinessType;
import com.bml.core.framework.operlog.OperationLog;
import com.bml.module.system.service.SysOnlineUserService;
import com.bml.module.system.vo.SysOnlineUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "在线用户")
@RestController
@RequestMapping("/system/online")
public class SysOnlineUserController extends BaseController {

    @Resource
    private SysOnlineUserService onlineUserService;

    @Operation(summary = "查询在线用户")
    @PreAuthorize("@ss.hasPermi('system:online:list')")
    @GetMapping("/list")
    public Result<List<SysOnlineUserVO>> list(@RequestParam(required = false) String username) {
        return Result.ok(onlineUserService.listOnlineUsers(username));
    }

    @Operation(summary = "强制用户下线")
    @OperationLog(title = "在线用户", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('system:online:forceLogout')")
    @DeleteMapping("/{userKey}")
    public Result<Void> forceLogout(@PathVariable String userKey) {
        return toAjax(onlineUserService.forceLogout(userKey));
    }
}
