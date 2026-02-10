package com.bml.module.system.converter;

import com.bml.module.system.dto.SysDeptDTO;
import com.bml.module.system.entity.SysDept;
import com.bml.module.system.vo.SysDeptVO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-11T00:11:15+0800",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
public class DeptConverterImpl implements DeptConverter {

    @Override
    public SysDept toEntity(SysDeptDTO dto) {
        if ( dto == null ) {
            return null;
        }

        SysDept sysDept = new SysDept();

        sysDept.setId( dto.getId() );
        sysDept.setDeptName( dto.getDeptName() );
        sysDept.setEmail( dto.getEmail() );
        sysDept.setLeader( dto.getLeader() );
        sysDept.setParentId( dto.getParentId() );
        sysDept.setPhone( dto.getPhone() );
        sysDept.setSort( dto.getSort() );
        sysDept.setStatus( dto.getStatus() );

        return sysDept;
    }

    @Override
    public SysDeptVO toVO(SysDept entity) {
        if ( entity == null ) {
            return null;
        }

        SysDeptVO sysDeptVO = new SysDeptVO();

        sysDeptVO.setChildren( toVOList( entity.getChildren() ) );
        sysDeptVO.setCreateTime( entity.getCreateTime() );
        sysDeptVO.setDeptName( entity.getDeptName() );
        sysDeptVO.setEmail( entity.getEmail() );
        sysDeptVO.setId( entity.getId() );
        sysDeptVO.setLeader( entity.getLeader() );
        sysDeptVO.setParentId( entity.getParentId() );
        sysDeptVO.setPhone( entity.getPhone() );
        sysDeptVO.setSort( entity.getSort() );
        sysDeptVO.setStatus( entity.getStatus() );

        return sysDeptVO;
    }

    @Override
    public List<SysDeptVO> toVOList(List<SysDept> list) {
        if ( list == null ) {
            return null;
        }

        List<SysDeptVO> list1 = new ArrayList<SysDeptVO>( list.size() );
        for ( SysDept sysDept : list ) {
            list1.add( toVO( sysDept ) );
        }

        return list1;
    }
}
