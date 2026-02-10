package com.bml.module.api.converter;

import com.bml.module.api.dto.ApiGroupDTO;
import com.bml.module.api.entity.ApiGroup;
import com.bml.module.api.vo.ApiGroupVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * API分组转换器
 *
 * @author BML Team
 */
@Mapper
public interface ApiGroupConverter {

    ApiGroupConverter INSTANCE = Mappers.getMapper(ApiGroupConverter.class);

    ApiGroup toEntity(ApiGroupDTO dto);

    ApiGroupVO toVO(ApiGroup entity);

    List<ApiGroupVO> toVOList(List<ApiGroup> list);
}
