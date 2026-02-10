package com.bml.module.system.mapper;

import com.bml.core.base.mapper.BmlBaseMapper;
import com.bml.module.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单表 Mapper 接口
 *
 * @author BML Team
 */
@Mapper
public interface SysMenuMapper extends BmlBaseMapper<SysMenu> {

    /**
     * 根据用户ID查询菜单权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<String> selectMenuPermsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询菜单树信息
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    List<SysMenu> selectMenuTreeByUserId(@Param("userId") Long userId);
}
