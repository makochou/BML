package com.bml.module.api.converter;

import com.bml.module.api.dto.ApiGroupDTO;
import com.bml.module.api.entity.ApiGroup;
import com.bml.module.api.vo.ApiGroupVO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-11T00:11:17+0800",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
public class ApiGroupConverterImpl implements ApiGroupConverter {

    @Override
    public ApiGroup toEntity(ApiGroupDTO dto) {
        if ( dto == null ) {
            return null;
        }

        ApiGroup apiGroup = new ApiGroup();

        apiGroup.setId( dto.getId() );
        apiGroup.setDescription( dto.getDescription() );
        apiGroup.setName( dto.getName() );
        apiGroup.setSort( dto.getSort() );
        apiGroup.setStatus( dto.getStatus() );

        return apiGroup;
    }

    @Override
    public ApiGroupVO toVO(ApiGroup entity) {
        if ( entity == null ) {
            return null;
        }

        ApiGroupVO apiGroupVO = new ApiGroupVO();

        apiGroupVO.setCreateTime( entity.getCreateTime() );
        apiGroupVO.setDescription( entity.getDescription() );
        apiGroupVO.setId( entity.getId() );
        apiGroupVO.setName( entity.getName() );
        apiGroupVO.setSort( entity.getSort() );
        apiGroupVO.setStatus( entity.getStatus() );

        return apiGroupVO;
    }

    @Override
    public List<ApiGroupVO> toVOList(List<ApiGroup> list) {
        if ( list == null ) {
            return null;
        }

        List<ApiGroupVO> list1 = new ArrayList<ApiGroupVO>( list.size() );
        for ( ApiGroup apiGroup : list ) {
            list1.add( toVO( apiGroup ) );
        }

        return list1;
    }
}
