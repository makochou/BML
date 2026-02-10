package com.bml.module.system.converter;

import com.bml.module.system.dto.SysRoleDTO;
import com.bml.module.system.entity.SysRole;
import com.bml.module.system.vo.SysRoleVO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-10T09:45:13+0800",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Oracle Corporation)"
)
public class RoleConverterImpl implements RoleConverter {

    @Override
    public SysRole toEntity(SysRoleDTO dto) {
        if ( dto == null ) {
            return null;
        }

        SysRole sysRole = new SysRole();

        sysRole.setId( dto.getId() );
        sysRole.setRoleName( dto.getRoleName() );
        sysRole.setRoleCode( dto.getRoleCode() );
        sysRole.setSort( dto.getSort() );
        sysRole.setDataScope( dto.getDataScope() );
        sysRole.setStatus( dto.getStatus() );
        sysRole.setRemark( dto.getRemark() );
        List<Long> list = dto.getMenuIds();
        if ( list != null ) {
            sysRole.setMenuIds( new ArrayList<Long>( list ) );
        }

        return sysRole;
    }

    @Override
    public SysRoleVO toVO(SysRole entity) {
        if ( entity == null ) {
            return null;
        }

        SysRoleVO sysRoleVO = new SysRoleVO();

        sysRoleVO.setId( entity.getId() );
        sysRoleVO.setRoleName( entity.getRoleName() );
        sysRoleVO.setRoleCode( entity.getRoleCode() );
        sysRoleVO.setSort( entity.getSort() );
        sysRoleVO.setDataScope( entity.getDataScope() );
        sysRoleVO.setStatus( entity.getStatus() );
        sysRoleVO.setRemark( entity.getRemark() );
        sysRoleVO.setCreateTime( entity.getCreateTime() );

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
