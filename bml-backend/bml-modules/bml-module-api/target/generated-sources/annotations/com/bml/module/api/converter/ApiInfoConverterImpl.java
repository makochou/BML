package com.bml.module.api.converter;

import com.bml.module.api.dto.ApiInfoDTO;
import com.bml.module.api.entity.ApiInfo;
import com.bml.module.api.vo.ApiInfoVO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-10T19:34:02+0800",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Oracle Corporation)"
)
public class ApiInfoConverterImpl implements ApiInfoConverter {

    @Override
    public ApiInfo toEntity(ApiInfoDTO dto) {
        if ( dto == null ) {
            return null;
        }

        ApiInfo apiInfo = new ApiInfo();

        apiInfo.setId( dto.getId() );
        apiInfo.setGroupId( dto.getGroupId() );
        apiInfo.setName( dto.getName() );
        apiInfo.setPath( dto.getPath() );
        apiInfo.setMethod( dto.getMethod() );
        apiInfo.setStatus( dto.getStatus() );
        apiInfo.setAuthType( dto.getAuthType() );
        apiInfo.setRequestParams( dto.getRequestParams() );
        apiInfo.setResponseParams( dto.getResponseParams() );
        apiInfo.setExample( dto.getExample() );
        apiInfo.setRemark( dto.getRemark() );

        return apiInfo;
    }

    @Override
    public ApiInfoVO toVO(ApiInfo entity) {
        if ( entity == null ) {
            return null;
        }

        ApiInfoVO apiInfoVO = new ApiInfoVO();

        apiInfoVO.setId( entity.getId() );
        apiInfoVO.setGroupId( entity.getGroupId() );
        apiInfoVO.setName( entity.getName() );
        apiInfoVO.setPath( entity.getPath() );
        apiInfoVO.setMethod( entity.getMethod() );
        apiInfoVO.setStatus( entity.getStatus() );
        apiInfoVO.setAuthType( entity.getAuthType() );
        apiInfoVO.setRequestParams( entity.getRequestParams() );
        apiInfoVO.setResponseParams( entity.getResponseParams() );
        apiInfoVO.setExample( entity.getExample() );
        apiInfoVO.setRemark( entity.getRemark() );
        apiInfoVO.setCreateTime( entity.getCreateTime() );

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
