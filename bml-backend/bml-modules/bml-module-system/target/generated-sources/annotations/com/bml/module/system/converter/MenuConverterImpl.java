package com.bml.module.system.converter;

import com.bml.module.system.dto.SysMenuDTO;
import com.bml.module.system.entity.SysMenu;
import com.bml.module.system.vo.SysMenuVO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-10T09:45:14+0800",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Oracle Corporation)"
)
public class MenuConverterImpl implements MenuConverter {

    @Override
    public SysMenu toEntity(SysMenuDTO dto) {
        if ( dto == null ) {
            return null;
        }

        SysMenu sysMenu = new SysMenu();

        sysMenu.setId( dto.getId() );
        sysMenu.setParentId( dto.getParentId() );
        sysMenu.setMenuName( dto.getMenuName() );
        sysMenu.setMenuType( dto.getMenuType() );
        sysMenu.setPath( dto.getPath() );
        sysMenu.setComponent( dto.getComponent() );
        sysMenu.setPerms( dto.getPerms() );
        sysMenu.setIcon( dto.getIcon() );
        sysMenu.setSort( dto.getSort() );
        sysMenu.setVisible( dto.getVisible() );
        sysMenu.setStatus( dto.getStatus() );
        sysMenu.setIsFrame( dto.getIsFrame() );
        sysMenu.setRemark( dto.getRemark() );

        return sysMenu;
    }

    @Override
    public SysMenuVO toVO(SysMenu entity) {
        if ( entity == null ) {
            return null;
        }

        SysMenuVO sysMenuVO = new SysMenuVO();

        sysMenuVO.setId( entity.getId() );
        sysMenuVO.setParentId( entity.getParentId() );
        sysMenuVO.setMenuName( entity.getMenuName() );
        sysMenuVO.setMenuType( entity.getMenuType() );
        sysMenuVO.setPath( entity.getPath() );
        sysMenuVO.setComponent( entity.getComponent() );
        sysMenuVO.setPerms( entity.getPerms() );
        sysMenuVO.setIcon( entity.getIcon() );
        sysMenuVO.setSort( entity.getSort() );
        sysMenuVO.setVisible( entity.getVisible() );
        sysMenuVO.setStatus( entity.getStatus() );
        sysMenuVO.setIsFrame( entity.getIsFrame() );
        sysMenuVO.setRemark( entity.getRemark() );
        sysMenuVO.setCreateTime( entity.getCreateTime() );
        sysMenuVO.setChildren( toVOList( entity.getChildren() ) );

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
