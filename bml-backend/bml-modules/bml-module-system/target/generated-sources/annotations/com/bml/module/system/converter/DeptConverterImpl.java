package com.bml.module.system.converter;

import com.bml.module.system.dto.SysDeptDTO;
import com.bml.module.system.entity.SysDept;
import com.bml.module.system.vo.SysDeptVO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-10T09:45:14+0800",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Oracle Corporation)"
)
public class DeptConverterImpl implements DeptConverter {

    @Override
    public SysDept toEntity(SysDeptDTO dto) {
        if ( dto == null ) {
            return null;
        }

        SysDept sysDept = new SysDept();

        sysDept.setId( dto.getId() );
        sysDept.setParentId( dto.getParentId() );
        sysDept.setDeptName( dto.getDeptName() );
        sysDept.setSort( dto.getSort() );
        sysDept.setLeader( dto.getLeader() );
        sysDept.setPhone( dto.getPhone() );
        sysDept.setEmail( dto.getEmail() );
        sysDept.setStatus( dto.getStatus() );

        return sysDept;
    }

    @Override
    public SysDeptVO toVO(SysDept entity) {
        if ( entity == null ) {
            return null;
        }

        SysDeptVO sysDeptVO = new SysDeptVO();

        sysDeptVO.setId( entity.getId() );
        sysDeptVO.setParentId( entity.getParentId() );
        sysDeptVO.setDeptName( entity.getDeptName() );
        sysDeptVO.setSort( entity.getSort() );
        sysDeptVO.setLeader( entity.getLeader() );
        sysDeptVO.setPhone( entity.getPhone() );
        sysDeptVO.setEmail( entity.getEmail() );
        sysDeptVO.setStatus( entity.getStatus() );
        sysDeptVO.setCreateTime( entity.getCreateTime() );
        sysDeptVO.setChildren( toVOList( entity.getChildren() ) );

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
