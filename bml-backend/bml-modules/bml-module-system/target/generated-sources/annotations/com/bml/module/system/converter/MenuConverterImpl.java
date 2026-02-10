package com.bml.module.system.converter;

import com.bml.module.system.dto.SysMenuDTO;
import com.bml.module.system.entity.SysMenu;
import com.bml.module.system.vo.SysMenuVO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-11T00:11:15+0800",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
public class MenuConverterImpl implements MenuConverter {

    @Override
    public SysMenu toEntity(SysMenuDTO dto) {
        if ( dto == null ) {
            return null;
        }

        SysMenu sysMenu = new SysMenu();

        sysMenu.setId( dto.getId() );
        sysMenu.setComponent( dto.getComponent() );
        sysMenu.setIcon( dto.getIcon() );
        sysMenu.setIsFrame( dto.getIsFrame() );
        sysMenu.setMenuName( dto.getMenuName() );
        sysMenu.setMenuType( dto.getMenuType() );
        sysMenu.setParentId( dto.getParentId() );
        sysMenu.setPath( dto.getPath() );
        sysMenu.setPerms( dto.getPerms() );
        sysMenu.setRemark( dto.getRemark() );
        sysMenu.setSort( dto.getSort() );
        sysMenu.setStatus( dto.getStatus() );
        sysMenu.setVisible( dto.getVisible() );

        return sysMenu;
    }

    @Override
    public SysMenuVO toVO(SysMenu entity) {
        if ( entity == null ) {
            return null;
        }

        SysMenuVO sysMenuVO = new SysMenuVO();

        sysMenuVO.setChildren( toVOList( entity.getChildren() ) );
        sysMenuVO.setComponent( entity.getComponent() );
        sysMenuVO.setCreateTime( entity.getCreateTime() );
        sysMenuVO.setIcon( entity.getIcon() );
        sysMenuVO.setId( entity.getId() );
        sysMenuVO.setIsFrame( entity.getIsFrame() );
        sysMenuVO.setMenuName( entity.getMenuName() );
        sysMenuVO.setMenuType( entity.getMenuType() );
        sysMenuVO.setParentId( entity.getParentId() );
        sysMenuVO.setPath( entity.getPath() );
        sysMenuVO.setPerms( entity.getPerms() );
        sysMenuVO.setRemark( entity.getRemark() );
        sysMenuVO.setSort( entity.getSort() );
        sysMenuVO.setStatus( entity.getStatus() );
        sysMenuVO.setVisible( entity.getVisible() );

        return sysMenuVO;
    }

    @Override
    public List<SysMenuVO> toVOList(List<SysMenu> list) {
        if ( list == null ) {
            return null;
        }

        List<SysMenuVO> list1 = new ArrayList<SysMenuVO>( list.size() );
        for ( SysMenu sysMenu : list ) {
            list1.add( toVO( sysMenu ) );
        }

        return list1;
    }
}
