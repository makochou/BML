package com.bml.module.system.converter;

import com.bml.module.system.dto.SysUserDTO;
import com.bml.module.system.entity.SysUser;
import com.bml.module.system.vo.SysUserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 用户对象转换器
 *
 * @author BML Team
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

    /**
     * DTO 转 Entity
     */
    SysUser toEntity(SysUserDTO dto);

    /**
     * Entity 转 VO
     */
    @Mapping(target = "deptName", ignore = true) // 可以在Service重手动填充，或关联查询
    @Mapping(target = "roleNames", ignore = true)
    SysUserVO toVO(SysUser entity);

    /**
     * Entity List 转 VO List
     */
    List<SysUserVO> toVOList(List<SysUser> list);
}
