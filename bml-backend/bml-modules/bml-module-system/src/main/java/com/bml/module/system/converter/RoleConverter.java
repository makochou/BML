package com.bml.module.system.converter;

import com.bml.module.system.dto.SysRoleDTO;
import com.bml.module.system.entity.SysRole;
import com.bml.module.system.vo.SysRoleVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 角色对象转换器
 * <p>
 * 基于 MapStruct 自动生成 DTO ↔ Entity ↔ VO 之间的转换代码。
 * 使用 {@code Mappers.getMapper()} 获取实例（无 Spring 容器依赖）。
 * </p>
 * <p>
 * <b>使用方式：</b> {@code RoleConverter.INSTANCE.toEntity(dto)}
 * </p>
 *
 * @author BML Team
 */
@Mapper(unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface RoleConverter {

    /** 转换器实例 */
    RoleConverter INSTANCE = Mappers.getMapper(RoleConverter.class);

    /**
     * DTO 转 Entity
     *
     * @param dto 角色传输对象
     * @return 角色实体
     */
    SysRole toEntity(SysRoleDTO dto);

    /**
     * Entity 转 VO
     *
     * @param entity 角色实体
     * @return 角色视图对象
     */
    SysRoleVO toVO(SysRole entity);

    /**
     * Entity 列表 转 VO 列表
     *
     * @param list 角色实体列表
     * @return 角色视图对象列表
     */
    List<SysRoleVO> toVOList(List<SysRole> list);
}
