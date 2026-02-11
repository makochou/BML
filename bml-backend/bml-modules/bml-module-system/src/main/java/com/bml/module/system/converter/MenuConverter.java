package com.bml.module.system.converter;

import com.bml.module.system.dto.SysMenuDTO;
import com.bml.module.system.entity.SysMenu;
import com.bml.module.system.vo.SysMenuVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 菜单对象转换器
 * <p>
 * 基于 MapStruct 自动生成 DTO ↔ Entity ↔ VO 之间的转换代码。
 * 使用 {@code Mappers.getMapper()} 获取实例（无 Spring 容器依赖）。
 * </p>
 * <p>
 * <b>使用方式：</b> {@code MenuConverter.INSTANCE.toEntity(dto)}
 * </p>
 *
 * @author BML Team
 */
@Mapper
public interface MenuConverter {

    /** 转换器实例 */
    MenuConverter INSTANCE = Mappers.getMapper(MenuConverter.class);

    /**
     * DTO 转 Entity
     *
     * @param dto 菜单传输对象
     * @return 菜单实体
     */
    SysMenu toEntity(SysMenuDTO dto);

    /**
     * Entity 转 VO
     *
     * @param entity 菜单实体
     * @return 菜单视图对象
     */
    SysMenuVO toVO(SysMenu entity);

    /**
     * Entity 列表 转 VO 列表
     *
     * @param list 菜单实体列表
     * @return 菜单视图对象列表
     */
    List<SysMenuVO> toVOList(List<SysMenu> list);
}
