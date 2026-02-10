package com.bml.module.api.converter;

import com.bml.module.api.dto.ApiInfoDTO;
import com.bml.module.api.entity.ApiInfo;
import com.bml.module.api.vo.ApiInfoVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * API详情转换器
 *
 * @author BML Team
 */
@Mapper
public interface ApiInfoConverter {

    ApiInfoConverter INSTANCE = Mappers.getMapper(ApiInfoConverter.class);

    ApiInfo toEntity(ApiInfoDTO dto);

    ApiInfoVO toVO(ApiInfo entity);

    List<ApiInfoVO> toVOList(List<ApiInfo> list);
}
