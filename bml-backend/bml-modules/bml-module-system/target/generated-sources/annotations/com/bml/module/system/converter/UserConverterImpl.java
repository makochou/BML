package com.bml.module.system.converter;

import com.bml.module.system.dto.SysUserDTO;
import com.bml.module.system.entity.SysUser;
import com.bml.module.system.vo.SysUserVO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-11T00:11:15+0800",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
@Component
public class UserConverterImpl implements UserConverter {

    @Override
    public SysUser toEntity(SysUserDTO dto) {
        if ( dto == null ) {
            return null;
        }

        SysUser sysUser = new SysUser();

        sysUser.setId( dto.getId() );
        sysUser.setAvatar( dto.getAvatar() );
        sysUser.setDeptId( dto.getDeptId() );
        sysUser.setEmail( dto.getEmail() );
        sysUser.setGender( dto.getGender() );
        sysUser.setNickname( dto.getNickname() );
        sysUser.setPassword( dto.getPassword() );
        sysUser.setPhone( dto.getPhone() );
        sysUser.setStatus( dto.getStatus() );
        sysUser.setUsername( dto.getUsername() );

        return sysUser;
    }

    @Override
    public SysUserVO toVO(SysUser entity) {
        if ( entity == null ) {
            return null;
        }

        SysUserVO sysUserVO = new SysUserVO();

        sysUserVO.setAvatar( entity.getAvatar() );
        sysUserVO.setCreateTime( entity.getCreateTime() );
        sysUserVO.setDeptId( entity.getDeptId() );
        sysUserVO.setEmail( entity.getEmail() );
        sysUserVO.setGender( entity.getGender() );
        sysUserVO.setId( entity.getId() );
        sysUserVO.setLoginDate( entity.getLoginDate() );
        sysUserVO.setLoginIp( entity.getLoginIp() );
        sysUserVO.setNickname( entity.getNickname() );
        sysUserVO.setPhone( entity.getPhone() );
        sysUserVO.setStatus( entity.getStatus() );
        sysUserVO.setUsername( entity.getUsername() );

        return sysUserVO;
    }

    @Override
    public List<SysUserVO> toVOList(List<SysUser> list) {
        if ( list == null ) {
            return null;
        }

        List<SysUserVO> list1 = new ArrayList<SysUserVO>( list.size() );
        for ( SysUser sysUser : list ) {
            list1.add( toVO( sysUser ) );
        }

        return list1;
    }
}
