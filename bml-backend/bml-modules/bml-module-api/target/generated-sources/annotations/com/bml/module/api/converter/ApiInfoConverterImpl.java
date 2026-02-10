package com.bml.module.api.converter;

import com.bml.module.api.dto.ApiInfoDTO;
import com.bml.module.api.entity.ApiInfo;
import com.bml.module.api.vo.ApiInfoVO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-11T00:11:17+0800",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
public class ApiInfoConverterImpl implements ApiInfoConverter {

    @Override
    public ApiInfo toEntity(ApiInfoDTO dto) {
        if ( dto == null ) {
            return null;
        }

        ApiInfo apiInfo = new ApiInfo();

        apiInfo.setId( dto.getId() );
        apiInfo.setAuthType( dto.getAuthType() );
        apiInfo.setExample( dto.getExample() );
        apiInfo.setGroupId( dto.getGroupId() );
        apiInfo.setMethod( dto.getMethod() );
        apiInfo.setName( dto.getName() );
        apiInfo.setPath( dto.getPath() );
        apiInfo.setRemark( dto.getRemark() );
        apiInfo.setRequestParams( dto.getRequestParams() );
        apiInfo.setResponseParams( dto.getResponseParams() );
        apiInfo.setStatus( dto.getStatus() );

        return apiInfo;
    }

    @Override
    public ApiInfoVO toVO(ApiInfo entity) {
        if ( entity == null ) {
            return null;
        }

        ApiInfoVO apiInfoVO = new ApiInfoVO();

        apiInfoVO.setAuthType( entity.getAuthType() );
        apiInfoVO.setCreateTime( entity.getCreateTime() );
        apiInfoVO.setExample( entity.getExample() );
        apiInfoVO.setGroupId( entity.getGroupId() );
        apiInfoVO.setId( entity.getId() );
        apiInfoVO.setMethod( entity.getMethod() );
        apiInfoVO.setName( entity.getName() );
        apiInfoVO.setPath( entity.getPath() );
        apiInfoVO.setRemark( entity.getRemark() );
        apiInfoVO.setRequestParams( entity.getRequestParams() );
        apiInfoVO.setResponseParams( entity.getResponseParams() );
        apiInfoVO.setStatus( entity.getStatus() );

        return apiInfoVO;
    }

    @Override
    public List<ApiInfoVO> toVOList(List<ApiInfo> list) {
        if ( list == null ) {
            return null;
        }

        List<ApiInfoVO> list1 = new ArrayList<ApiInfoVO>( list.size() );
        for ( ApiInfo apiInfo : list ) {
            list1.add( toVO( apiInfo ) );
        }

        return list1;
    }
}
