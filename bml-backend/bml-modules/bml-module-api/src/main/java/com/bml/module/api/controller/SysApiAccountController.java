package com.bml.module.api.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.module.api.dto.SysApiAccountDTO;
import com.bml.module.api.entity.SysApiAccount;
import com.bml.module.api.service.SysApiAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API账号管理
 *
 * @author BML Team
 */
@Tag(name = "API账号管理")
@RestController
@RequestMapping("/api/account")
public class SysApiAccountController extends BaseController {

    @Resource
    private SysApiAccountService accountService;

    @Operation(summary = "获取API账号列表")
    @PreAuthorize("@ss.hasPermi('api:account:list')")
    @GetMapping("/list")
    public Result<List<SysApiAccount>> list(SysApiAccountDTO dto) {
        return Result.ok(accountService.selectAccountList(dto));
    }

    @Operation(summary = "根据ID获取详细信息")
    @PreAuthorize("@ss.hasPermi('api:account:query')")
    @GetMapping(value = "/{id}")
    public Result<SysApiAccount> getInfo(@PathVariable Long id) {
        return Result.ok(accountService.getById(id));
    }

    @Operation(summary = "新增API账号")
    @PreAuthorize("@ss.hasPermi('api:account:add')")
    @PostMapping("/add")
    public Result<Void> add(@Validated @RequestBody SysApiAccountDTO dto) {
        return toAjax(accountService.insertAccount(dto));
    }

    @Operation(summary = "修改API账号")
    @PreAuthorize("@ss.hasPermi('api:account:edit')")
    @PutMapping("/edit")
    public Result<Void> edit(@Validated @RequestBody SysApiAccountDTO dto) {
        return toAjax(accountService.updateAccount(dto));
    }

    @Operation(summary = "删除API账号")
    @PreAuthorize("@ss.hasPermi('api:account:remove')")
    @DeleteMapping("/remove/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        return toAjax(accountService.removeById(id));
    }

    @Operation(summary = "重置密钥")
    @PreAuthorize("@ss.hasPermi('api:account:reset')")
    @PutMapping("/{id}/reset")
    public Result<String> resetSecret(@PathVariable Long id) {
        return Result.ok(accountService.resetSecret(id));
    }
}
