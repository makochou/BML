package com.bml.module.system.controller;

import com.bml.core.base.controller.BaseController;
import com.bml.core.common.result.PageResult;
import com.bml.core.common.result.Result;
import com.bml.core.framework.operlog.BusinessType;
import com.bml.core.framework.operlog.OperationLog;
import com.bml.module.system.dto.SysDictDataDTO;
import com.bml.module.system.dto.SysDictTypeDTO;
import com.bml.module.system.service.SysDictService;
import com.bml.module.system.vo.SysDictDataVO;
import com.bml.module.system.vo.SysDictTypeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
 * 字典管理控制器。
 * <p>
 * 提供字典类型与字典数据的统一维护接口。字典属于基础配置能力，建议所有业务枚举值优先通过本模块维护，
 * 避免在前端页面或后端业务代码中硬编码可选项。
 * </p>
 *
 * @author BML Team
 */
@Tag(name = "字典管理", description = "字典类型与字典数据维护")
@RestController
@RequestMapping("/system/dict")
@RequiredArgsConstructor
public class SysDictController extends BaseController {

    private final SysDictService dictService;

    /** 分页查询字典类型。 */
    @Operation(summary = "分页查询字典类型")
    @PreAuthorize("@ss.hasPermi('system:dict:list')")
    @GetMapping("/type/page")
    public Result<PageResult<SysDictTypeVO>> typePage(SysDictTypeDTO dto) {
        return Result.ok(dictService.selectDictTypePage(dto));
    }

    /** 查询字典类型详情。 */
    @Operation(summary = "查询字典类型详情")
    @PreAuthorize("@ss.hasPermi('system:dict:query')")
    @GetMapping("/type/{id}")
    public Result<SysDictTypeVO> typeInfo(@PathVariable Long id) {
        return Result.ok(dictService.selectDictTypeById(id));
    }

    /** 新增字典类型。 */
    @Operation(summary = "新增字典类型")
    @OperationLog(title = "字典类型", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('system:dict:add')")
    @PostMapping("/type")
    public Result<Void> addType(@Validated @RequestBody SysDictTypeDTO dto) {
        return toAjax(dictService.insertDictType(dto));
    }

    /** 修改字典类型。 */
    @Operation(summary = "修改字典类型")
    @OperationLog(title = "字典类型", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('system:dict:edit')")
    @PutMapping("/type")
    public Result<Void> editType(@Validated @RequestBody SysDictTypeDTO dto) {
        return toAjax(dictService.updateDictType(dto));
    }

    /** 删除字典类型。 */
    @Operation(summary = "删除字典类型")
    @OperationLog(title = "字典类型", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('system:dict:remove')")
    @DeleteMapping("/type/{id}")
    public Result<Void> removeType(@PathVariable Long id) {
        return toAjax(dictService.deleteDictType(id));
    }

    /** 分页查询字典数据。 */
    @Operation(summary = "分页查询字典数据")
    @PreAuthorize("@ss.hasPermi('system:dict:list')")
    @GetMapping("/data/page")
    public Result<PageResult<SysDictDataVO>> dataPage(SysDictDataDTO dto) {
        return Result.ok(dictService.selectDictDataPage(dto));
    }

    /** 查询字典数据详情。 */
    @Operation(summary = "查询字典数据详情")
    @PreAuthorize("@ss.hasPermi('system:dict:query')")
    @GetMapping("/data/{id}")
    public Result<SysDictDataVO> dataInfo(@PathVariable Long id) {
        return Result.ok(dictService.selectDictDataById(id));
    }

    /** 按类型查询启用字典数据。 */
    @Operation(summary = "按类型查询启用字典数据")
    @GetMapping("/data/type/{dictType}")
    public Result<List<SysDictDataVO>> dataByType(@PathVariable String dictType) {
        return Result.ok(dictService.selectDictDataByType(dictType));
    }

    /** 新增字典数据。 */
    @Operation(summary = "新增字典数据")
    @OperationLog(title = "字典数据", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('system:dict:add')")
    @PostMapping("/data")
    public Result<Void> addData(@Validated @RequestBody SysDictDataDTO dto) {
        return toAjax(dictService.insertDictData(dto));
    }

    /** 修改字典数据。 */
    @Operation(summary = "修改字典数据")
    @OperationLog(title = "字典数据", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('system:dict:edit')")
    @PutMapping("/data")
    public Result<Void> editData(@Validated @RequestBody SysDictDataDTO dto) {
        return toAjax(dictService.updateDictData(dto));
    }

    /** 删除字典数据。 */
    @Operation(summary = "删除字典数据")
    @OperationLog(title = "字典数据", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('system:dict:remove')")
    @DeleteMapping("/data/{id}")
    public Result<Void> removeData(@PathVariable Long id) {
        return toAjax(dictService.deleteDictData(id));
    }
}
