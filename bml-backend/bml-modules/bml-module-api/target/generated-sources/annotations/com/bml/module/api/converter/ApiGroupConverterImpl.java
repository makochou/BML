package com.bml.module.api.converter;

import com.bml.module.api.dto.ApiGroupDTO;
import com.bml.module.api.entity.ApiGroup;
import com.bml.module.api.vo.ApiGroupVO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-10T19:34:02+0800",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Oracle Corporation)"
)
public class ApiGroupConverterImpl implements ApiGroupConverter {

    @Override
    public ApiGroup toEntity(ApiGroupDTO dto) {
        if ( dto == null ) {
            return null;
        }

        ApiGroup apiGroup = new ApiGroup();

        apiGroup.setId( dto.getId() );
        apiGroup.setName( dto.getName() );
        apiGroup.setDescription( dto.getDescription() );
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

        apiGroupVO.setId( entity.getId() );
        apiGroupVO.setName( entity.getName() );
        apiGroupVO.setDescription( entity.getDescription() );
        apiGroupVO.setSort( entity.getSort() );
        apiGroupVO.setStatus( entity.getStatus() );
        apiGroupVO.setCreateTime( entity.getCreateTime() );

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
