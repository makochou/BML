package com.bml.module.system.converter;

import com.bml.module.system.dto.SysDeptDTO;
import com.bml.module.system.entity.SysDept;
import com.bml.module.system.vo.SysDeptVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 部门对象转换器
 *
 * @author BML Team
 */
@Mapper
public interface DeptConverter {

    DeptConverter INSTANCE = Mappers.getMapper(DeptConverter.class);

    SysDept toEntity(SysDeptDTO dto);

    SysDeptVO toVO(SysDept entity);

    List<SysDeptVO> toVOList(List<SysDept> list);
}
