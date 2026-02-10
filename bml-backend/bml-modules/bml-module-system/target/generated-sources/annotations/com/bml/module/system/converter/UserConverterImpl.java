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
    date = "2026-02-10T09:45:13+0800",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Oracle Corporation)"
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
        sysUser.setUsername( dto.getUsername() );
        sysUser.setPassword( dto.getPassword() );
        sysUser.setNickname( dto.getNickname() );
        sysUser.setEmail( dto.getEmail() );
        sysUser.setPhone( dto.getPhone() );
        sysUser.setGender( dto.getGender() );
        sysUser.setAvatar( dto.getAvatar() );
        sysUser.setStatus( dto.getStatus() );
        sysUser.setDeptId( dto.getDeptId() );

        return sysUser;
    }

    @Override
    public SysUserVO toVO(SysUser entity) {
        if ( entity == null ) {
            return null;
        }

        SysUserVO sysUserVO = new SysUserVO();

        sysUserVO.setId( entity.getId() );
        sysUserVO.setUsername( entity.getUsername() );
        sysUserVO.setNickname( entity.getNickname() );
        sysUserVO.setEmail( entity.getEmail() );
        sysUserVO.setPhone( entity.getPhone() );
        sysUserVO.setGender( entity.getGender() );
        sysUserVO.setAvatar( entity.getAvatar() );
        sysUserVO.setStatus( entity.getStatus() );
        sysUserVO.setDeptId( entity.getDeptId() );
        sysUserVO.setCreateTime( entity.getCreateTime() );
        sysUserVO.setLoginIp( entity.getLoginIp() );
        sysUserVO.setLoginDate( entity.getLoginDate() );

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
