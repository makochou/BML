package com.bml.module.system.converter;

import com.bml.module.system.dto.SysRoleDTO;
import com.bml.module.system.entity.SysRole;
import com.bml.module.system.vo.SysRoleVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 角色对象转换器
 *
 * @author BML Team
 */
@Mapper
public interface RoleConverter {

    RoleConverter INSTANCE = Mappers.getMapper(RoleConverter.class);

    SysRole toEntity(SysRoleDTO dto);

    SysRoleVO toVO(SysRole entity);

    List<SysRoleVO> toVOList(List<SysRole> list);
}
