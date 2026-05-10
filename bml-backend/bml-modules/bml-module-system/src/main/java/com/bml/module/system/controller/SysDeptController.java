package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.Result;
import com.bml.core.framework.operlog.BusinessType;
import com.bml.core.framework.operlog.OperationLog;
import com.bml.module.system.converter.DeptConverter;
import com.bml.module.system.dto.SysDeptDTO;
import com.bml.module.system.entity.SysDept;
import com.bml.module.system.service.SysDeptService;
import com.bml.module.system.vo.SysDeptVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 * <p>
 * 提供系统部门的 CRUD 操作接口。部门为树形结构，支持无限层级嵌套。
 * 所有接口均需要对应的权限标识。
 * </p>
 *
 * <h3>权限标识说明：</h3>
 * <table>
 * <tr>
 * <th>操作</th>
 * <th>权限标识</th>
 * </tr>
 * <tr>
 * <td>查询部门列表</td>
 * <td>{@code system:dept:list}</td>
 * </tr>
 * <tr>
 * <td>查询部门详情</td>
 * <td>{@code system:dept:query}</td>
 * </tr>
 * <tr>
 * <td>新增部门</td>
 * <td>{@code system:dept:add}</td>
 * </tr>
 * <tr>
 * <td>修改部门</td>
 * <td>{@code system:dept:edit}</td>
 * </tr>
 * <tr>
 * <td>删除部门</td>
 * <td>{@code system:dept:remove}</td>
 * </tr>
 * </table>
 *
 * @author BML Team
 */
@Tag(name = "部门管理")
@RestController
@RequestMapping("/system/dept")
public class SysDeptController extends BaseController {

    @Resource
    private SysDeptService deptService;

    /**
     * 获取部门树列表
     * <p>
     * 返回的部门列表已构建为树形结构，前端可直接渲染。
     * </p>
     *
     * @param dto 查询条件（部门名称、状态等）
     * @return 部门树列表
     */
    @Operation(summary = "获取部门树列表")
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/list")
    public Result<List<SysDeptVO>> list(SysDeptDTO dto) {
        List<SysDept> depts = deptService.selectDeptList(dto);
        return Result.ok(DeptConverter.INSTANCE.toVOList(deptService.buildDeptTree(depts)));
    }

    /**
     * 根据部门编号获取详细信息
     *
     * @param deptId 部门ID
     * @return 部门详细信息
     */
    @Operation(summary = "根据部门编号获取详细信息")
    @PreAuthorize("@ss.hasPermi('system:dept:query')")
    @GetMapping(value = "/{deptId}")
    public Result<SysDeptVO> getInfo(@PathVariable Long deptId) {
        return Result.ok(DeptConverter.INSTANCE.toVO(deptService.getById(deptId)));
    }

    /**
     * 新增部门
     *
     * @param dto 部门信息（需包含 parentId）
     * @return 操作结果
     */
    @Operation(summary = "新增部门")
    @OperationLog(title = "部门管理", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('system:dept:add')")
    @PostMapping
    public Result<Void> add(@Validated @RequestBody SysDeptDTO dto) {
        if (!deptService.checkDeptNameUnique(dto)) {
            return Result.badRequest("新增部门'" + dto.getDeptName() + "'失败，部门名称已存在");
        }
        return toAjax(deptService.insertDept(dto));
    }

    /**
     * 修改部门
     *
     * @param dto 部门信息
     * @return 操作结果
     */
    @Operation(summary = "修改部门")
    @OperationLog(title = "部门管理", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @PutMapping
    public Result<Void> edit(@Validated @RequestBody SysDeptDTO dto) {
        if (!deptService.checkDeptNameUnique(dto)) {
            return Result.badRequest("修改部门'" + dto.getDeptName() + "'失败，部门名称已存在");
        }
        return toAjax(deptService.updateDept(dto));
    }

    /**
     * 删除部门
     * <p>
     * 如果部门下存在子部门（正常状态），将拒绝删除。
     * </p>
     *
     * @param deptId 部门ID
     * @return 操作结果
     */
    @Operation(summary = "删除部门")
    @OperationLog(title = "部门管理", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('system:dept:remove')")
    @DeleteMapping("/{deptId}")
    public Result<Void> remove(@PathVariable Long deptId) {
        List<SysDept> children = deptService.selectChildrenDeptById(deptId);
        if (children != null && !children.isEmpty()) {
            return Result.badRequest("存在下级部门，不允许删除");
        }
        return toAjax(deptService.removeById(deptId));
    }
}
