package com.bml.module.system.converter;

import com.bml.module.system.dto.SysMenuDTO;
import com.bml.module.system.entity.SysMenu;
import com.bml.module.system.vo.SysMenuVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 菜单对象转换器
 *
 * @author BML Team
 */
@Mapper
public interface MenuConverter {

    MenuConverter INSTANCE = Mappers.getMapper(MenuConverter.class);

    SysMenu toEntity(SysMenuDTO dto);

    SysMenuVO toVO(SysMenu entity);

    List<SysMenuVO> toVOList(List<SysMenu> list);
}
