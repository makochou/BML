package com.bml.module.api.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.core.framework.operlog.BusinessType;
import com.bml.core.framework.operlog.OperationLog;
import com.bml.module.api.dto.SaveApiAccountAuthorizationCommand;
import com.bml.module.api.service.SysApiAccountAuthorizationService;
import com.bml.module.api.vo.ApiAccountAuthorizationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API账号授权控制器。
 */
@Tag(name = "API账号授权管理")
@RestController
@RequestMapping("/account")
public class SysApiAccountAuthorizationController extends BaseController {

    private final SysApiAccountAuthorizationService authorizationService;

    public SysApiAccountAuthorizationController(SysApiAccountAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Operation(summary = "获取账号授权快照")
    @PreAuthorize("@ss.hasPermi('api:account:authorize')")
    @GetMapping("/{id}/authorization")
    public Result<ApiAccountAuthorizationVO> getAuthorization(@PathVariable Long id) {
        return Result.ok(authorizationService.getAuthorizationSnapshot(id));
    }

    @Operation(summary = "保存账号授权")
    @OperationLog(title = "API账号授权", businessType = BusinessType.GRANT)
    @PreAuthorize("@ss.hasPermi('api:account:authorize')")
    @PutMapping("/{id}/authorization")
    public Result<Void> saveAuthorization(@PathVariable Long id,
            @Valid @RequestBody SaveApiAccountAuthorizationCommand command) {
        return toAjax(authorizationService.saveAuthorization(id, command));
    }
}
