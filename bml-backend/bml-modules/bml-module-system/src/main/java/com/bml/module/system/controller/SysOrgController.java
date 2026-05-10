package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.core.framework.operlog.BusinessType;
import com.bml.core.framework.operlog.OperationLog;
import com.bml.module.system.converter.OrgConverter;
import com.bml.module.system.dto.SysOrgDTO;
import com.bml.module.system.entity.SysOrg;
import com.bml.module.system.service.SysOrgService;
import com.bml.module.system.vo.SysOrgVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 机构管理控制器
 * <p>
 * 提供系统机构的 CRUD 操作接口。机构为树形结构，支持无限层级嵌套。
 * 所有接口均需要对应的权限标识。
 * </p>
 *
 * <h3>权限标识说明：</h3>
 * <table>
 * <tr><th>操作</th><th>权限标识</th></tr>
 * <tr><td>查询机构列表</td><td>{@code system:org:list}</td></tr>
 * <tr><td>查询机构详情</td><td>{@code system:org:query}</td></tr>
 * <tr><td>新增机构</td><td>{@code system:org:add}</td></tr>
 * <tr><td>修改机构</td><td>{@code system:org:edit}</td></tr>
 * <tr><td>删除机构</td><td>{@code system:org:remove}</td></tr>
 * </table>
 *
 * @author BML Team
 */
@Tag(name = "机构管理")
@RestController
@RequestMapping("/system/org")
public class SysOrgController extends BaseController {

    @Resource
    private SysOrgService orgService;

    /**
     * 获取机构树列表
     */
    @Operation(summary = "获取机构树列表")
    @PreAuthorize("@ss.hasPermi('system:org:list')")
    @GetMapping("/list")
    public Result<List<SysOrgVO>> list(SysOrgDTO dto) {
        List<SysOrg> orgs = orgService.selectOrgList(dto);
        return Result.ok(OrgConverter.INSTANCE.toVOList(orgService.buildOrgTree(orgs)));
    }

    /**
     * 根据机构编号获取详细信息
     */
    @Operation(summary = "根据机构编号获取详细信息")
    @PreAuthorize("@ss.hasPermi('system:org:query')")
    @GetMapping(value = "/{orgId}")
    public Result<SysOrgVO> getInfo(@PathVariable Long orgId) {
        return Result.ok(OrgConverter.INSTANCE.toVO(orgService.getById(orgId)));
    }

    /**
     * 新增机构
     */
    @Operation(summary = "新增机构")
    @OperationLog(title = "机构管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('system:org:add')")
    @PostMapping
    public Result<Void> add(@Validated @RequestBody SysOrgDTO dto) {
        if (orgService.checkOrgNameUnique(dto)) {
            return Result.badRequest("新增机构'" + dto.getOrgName() + "'失败，机构名称已存在");
        }
        if (!orgService.checkOrgCodeUnique(dto)) {
            return Result.badRequest("机构编码已存在");
        }
        return toAjax(orgService.insertOrg(dto));
    }

    /**
     * 修改机构
     */
    @Operation(summary = "修改机构")
    @OperationLog(title = "机构管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('system:org:edit')")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody SysOrgDTO dto) {
        if (orgService.checkOrgNameUnique(dto)) {
            return Result.badRequest("修改机构'" + dto.getOrgName() + "'失败，机构名称已存在");
        }
        if (!orgService.checkOrgCodeUnique(dto)) {
            return Result.badRequest("机构编码已存在");
        }
        return toAjax(orgService.updateOrg(dto));
    }

    /**
     * 删除机构
     */
    @Operation(summary = "删除机构")
    @OperationLog(title = "机构管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('system:org:remove')")
    @DeleteMapping("/{orgId}")
    public Result<Void> remove(@PathVariable Long orgId) {
        if (orgService.checkOrgHasChild(orgId)) {
            return Result.badRequest("存在子机构，不允许删除");
        }
        return toAjax(orgService.removeById(orgId));
    }
}
