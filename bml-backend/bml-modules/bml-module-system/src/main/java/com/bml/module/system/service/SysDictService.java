package com.bml.module.system.service;

import com.bml.core.common.result.PageResult;
import com.bml.module.system.dto.SysDictDataDTO;
import com.bml.module.system.dto.SysDictTypeDTO;
import com.bml.module.system.vo.SysDictDataVO;
import com.bml.module.system.vo.SysDictTypeVO;

import java.util.List;

/**
 * 字典管理服务接口。
 * <p>
 * 统一封装字典类型与字典数据的业务操作，为后续所有模块提供稳定的基础枚举能力。
 * </p>
 *
 * @author BML Team
 */
public interface SysDictService {

    /** 分页查询字典类型。 */
    PageResult<SysDictTypeVO> selectDictTypePage(SysDictTypeDTO dto);

    /** 查询字典类型详情。 */
    SysDictTypeVO selectDictTypeById(Long id);

    /** 新增字典类型。 */
    boolean insertDictType(SysDictTypeDTO dto);

    /** 修改字典类型。 */
    boolean updateDictType(SysDictTypeDTO dto);

    /** 删除字典类型。 */
    boolean deleteDictType(Long id);

    /** 分页查询字典数据。 */
    PageResult<SysDictDataVO> selectDictDataPage(SysDictDataDTO dto);

    /** 查询字典数据详情。 */
    SysDictDataVO selectDictDataById(Long id);

    /** 按字典类型查询启用的字典数据。 */
    List<SysDictDataVO> selectDictDataByType(String dictType);

    /** 新增字典数据。 */
    boolean insertDictData(SysDictDataDTO dto);

    /** 修改字典数据。 */
    boolean updateDictData(SysDictDataDTO dto);

    /** 删除字典数据。 */
    boolean deleteDictData(Long id);
}
