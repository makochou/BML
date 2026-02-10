package com.bml.module.system.converter;

import com.bml.module.system.dto.SysRoleDTO;
import com.bml.module.system.entity.SysRole;
import com.bml.module.system.vo.SysRoleVO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-11T00:11:15+0800",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
public class RoleConverterImpl implements RoleConverter {

    @Override
    public SysRole toEntity(SysRoleDTO dto) {
        if ( dto == null ) {
            return null;
        }

        SysRole sysRole = new SysRole();

        sysRole.setId( dto.getId() );
        sysRole.setDataScope( dto.getDataScope() );
        List<Long> list = dto.getMenuIds();
        if ( list != null ) {
            sysRole.setMenuIds( new ArrayList<Long>( list ) );
        }
        sysRole.setRemark( dto.getRemark() );
        sysRole.setRoleCode( dto.getRoleCode() );
        sysRole.setRoleName( dto.getRoleName() );
        sysRole.setSort( dto.getSort() );
        sysRole.setStatus( dto.getStatus() );

        return sysRole;
    }

    @Override
    public SysRoleVO toVO(SysRole entity) {
        if ( entity == null ) {
            return null;
        }

        SysRoleVO sysRoleVO = new SysRoleVO();

        sysRoleVO.setCreateTime( entity.getCreateTime() );
        sysRoleVO.setDataScope( entity.getDataScope() );
        sysRoleVO.setId( entity.getId() );
        sysRoleVO.setRemark( entity.getRemark() );
        sysRoleVO.setRoleCode( entity.getRoleCode() );
        sysRoleVO.setRoleName( entity.getRoleName() );
        sysRoleVO.setSort( entity.getSort() );
        sysRoleVO.setStatus( entity.getStatus() );

        return sysRoleVO;
    }

    @Override
    public List<SysRoleVO> toVOList(List<SysRole> list) {
        if ( list == null ) {
            return null;
        }

        List<SysRoleVO> list1 = new ArrayList<SysRoleVO>( list.size() );
        for ( SysRole sysRole : list ) {
            list1.add( toVO( sysRole ) );
        }

        return list1;
    }
}
