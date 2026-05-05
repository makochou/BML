package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.module.system.entity.SysOrgDataShare;
import com.bml.module.system.service.SysOrgDataShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 机构数据共享规则控制器
 * <p>
 * 提供机构间数据共享规则的 CRUD 操作接口。
 * 在机构管理页面中嵌入使用，配置某个机构的数据对外共享策略。
 * </p>
 *
 * @author BML Team
 */
@Tag(name = "机构数据共享规则")
@RestController
@RequestMapping("/system/org/share")
public class SysOrgDataShareController extends BaseController {

    @Resource
    private SysOrgDataShareService orgDataShareService;

    /**
     * 根据源机构ID查询共享规则列表
     *
     * @param sourceOrgId 源机构ID
     * @return 共享规则列表
     */
    @Operation(summary = "查询机构数据共享规则列表")
    @PreAuthorize("@ss.hasPermi('system:org:list')")
    @GetMapping("/list/{sourceOrgId}")
    public Result<List<SysOrgDataShare>> list(@PathVariable Long sourceOrgId) {
        return Result.ok(orgDataShareService.selectBySourceOrg(sourceOrgId));
    }

    /**
     * 获取共享规则详情
     *
     * @param id 规则ID
     * @return 共享规则详情
     */
    @Operation(summary = "获取共享规则详情")
    @PreAuthorize("@ss.hasPermi('system:org:query')")
    @GetMapping("/{id}")
    public Result<SysOrgDataShare> getInfo(@PathVariable Long id) {
        return Result.ok(orgDataShareService.getById(id));
    }

    /**
     * 新增共享规则
     *
     * @param share 共享规则信息
     * @return 操作结果
     */
    @Operation(summary = "新增机构数据共享规则")
    @PreAuthorize("@ss.hasPermi('system:org:add')")
    @PostMapping
    public Result<Void> add(@RequestBody SysOrgDataShare share) {
        return toAjax(orgDataShareService.insertShare(share));
    }

    /**
     * 修改共享规则
     *
     * @param share 共享规则信息
     * @return 操作结果
     */
    @Operation(summary = "修改机构数据共享规则")
    @PreAuthorize("@ss.hasPermi('system:org:edit')")
    @PutMapping
    public Result<Void> edit(@RequestBody SysOrgDataShare share) {
        return toAjax(orgDataShareService.updateShare(share));
    }

    /**
     * 删除共享规则
     *
     * @param id 规则ID
     * @return 操作结果
     */
    @Operation(summary = "删除机构数据共享规则")
    @PreAuthorize("@ss.hasPermi('system:org:remove')")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        return toAjax(orgDataShareService.removeById(id));
    }
}
