package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.module.system.dto.SysMenuDTO;
import com.bml.module.system.entity.SysMenu;

import java.util.List;
import java.util.Set;

/**
 * 菜单管理 服务接口
 *
 * @author BML Team
 */
public interface SysMenuService extends BaseService<SysMenu> {

    /**
     * 根据用户ID查询菜单列表 (用于管理)
     */
    List<SysMenu> selectMenuList(SysMenuDTO menu, Long userId);

    /**
     * 根据用户ID查询菜单权限
     */
    Set<String> selectMenuPermsByUserId(Long userId);

    /**
     * 根据用户ID查询菜单树信息
     */
    List<SysMenu> selectMenuTreeByUserId(Long userId);

    /**
     * 构建前端路由所需要的菜单
     */
    List<SysMenu> buildMenus(List<SysMenu> menus);

    /**
     * 校验菜单名称是否唯一
     */
    boolean checkMenuNameUnique(SysMenuDTO menu);

    /**
     * 新增菜单
     */
    boolean insertMenu(SysMenuDTO menuDto);

    /**
     * 修改菜单
     */
    boolean updateMenu(SysMenuDTO menuDto);
}
