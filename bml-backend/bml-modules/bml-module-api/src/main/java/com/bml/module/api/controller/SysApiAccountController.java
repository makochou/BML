package com.bml.module.api.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.PageResult;
import com.bml.core.common.result.Result;
import com.bml.module.api.dto.CreateSysApiAccountCommand;
import com.bml.module.api.dto.SysApiAccountDTO;
import com.bml.module.api.dto.SysApiAccountPageQuery;
import com.bml.module.api.dto.UpdateSysApiAccountCommand;
import com.bml.module.api.dto.UpdateSysApiAccountStatusCommand;
import com.bml.module.api.service.SysApiAccountService;
import com.bml.module.api.vo.ApiCredentialVO;
import com.bml.module.api.vo.SysApiAccountDetailVO;
import com.bml.module.api.vo.SysApiAccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API账号管理控制器。
 */
@Tag(name = "API账号管理")
@RestController
@RequestMapping("/account")
public class SysApiAccountController extends BaseController {

    private final SysApiAccountService accountService;

    public SysApiAccountController(SysApiAccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(summary = "分页查询API账号")
    @PreAuthorize("@ss.hasPermi('api:account:list')")
    @GetMapping("/page")
    public Result<PageResult<SysApiAccountVO>> page(SysApiAccountPageQuery query) {
        return Result.ok(accountService.pageAccounts(query));
    }

    @Operation(summary = "查询API账号详情")
    @PreAuthorize("@ss.hasPermi('api:account:query')")
    @GetMapping("/{id}")
    public Result<SysApiAccountDetailVO> getInfo(@PathVariable Long id) {
        return Result.ok(accountService.getAccountDetail(id));
    }

    @Operation(summary = "按 ID 复制 API 账号副本")
    @PreAuthorize("@ss.hasPermi('api:account:add')")
    @GetMapping("/{id}/copy")
    public Result<SysApiAccountDetailVO> copy(@PathVariable Long id) {
        return Result.ok(accountService.getAccountCopy(id));
    }

    @Operation(summary = "新增API账号")
    @PreAuthorize("@ss.hasPermi('api:account:add')")
    @PostMapping
    public Result<ApiCredentialVO> create(@Valid @RequestBody CreateSysApiAccountCommand command) {
        return Result.ok(accountService.createAccount(command));
    }

    @Operation(summary = "修改API账号")
    @PreAuthorize("@ss.hasPermi('api:account:edit')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateSysApiAccountCommand command) {
        return toAjax(accountService.updateAccount(id, command));
    }

    @Operation(summary = "修改API账号状态")
    @PreAuthorize("@ss.hasPermi('api:account:edit')")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateSysApiAccountStatusCommand command) {
        return toAjax(accountService.updateAccountStatus(id, command.getStatus()));
    }

    @Operation(summary = "删除API账号")
    @PreAuthorize("@ss.hasPermi('api:account:remove')")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        return toAjax(accountService.removeAccount(id));
    }

    @Operation(summary = "重置API账号密钥")
    @PreAuthorize("@ss.hasPermi('api:account:reset')")
    @PutMapping("/{id}/secret/reset")
    public Result<ApiCredentialVO> resetSecret(@PathVariable Long id) {
        return Result.ok(accountService.resetSecret(id));
    }

    @Operation(summary = "获取API账号列表(兼容旧接口)")
    @PreAuthorize("@ss.hasPermi('api:account:list')")
    @GetMapping("/list")
    public Result<List<SysApiAccountVO>> list(SysApiAccountDTO dto) {
        return Result.ok(accountService.selectAccountList(dto));
    }

    @Operation(summary = "新增API账号(兼容旧接口)")
    @PreAuthorize("@ss.hasPermi('api:account:add')")
    @PostMapping("/add")
    public Result<ApiCredentialVO> add(@Valid @RequestBody SysApiAccountDTO dto) {
        return Result.ok(accountService.insertAccount(dto));
    }

    @Operation(summary = "修改API账号(兼容旧接口)")
    @PreAuthorize("@ss.hasPermi('api:account:edit')")
    @PutMapping("/edit")
    public Result<Void> edit(@Valid @RequestBody SysApiAccountDTO dto) {
        return toAjax(accountService.updateAccount(dto));
    }

    @Operation(summary = "删除API账号(兼容旧接口)")
    @PreAuthorize("@ss.hasPermi('api:account:remove')")
    @DeleteMapping("/remove/{id}")
    public Result<Void> removeLegacy(@PathVariable Long id) {
        return toAjax(accountService.removeAccount(id));
    }

    @Operation(summary = "重置密钥(兼容旧接口)")
    @PreAuthorize("@ss.hasPermi('api:account:reset')")
    @PutMapping("/{id}/reset")
    public Result<ApiCredentialVO> resetSecretLegacy(@PathVariable Long id) {
        return Result.ok(accountService.resetSecret(id));
    }
}
