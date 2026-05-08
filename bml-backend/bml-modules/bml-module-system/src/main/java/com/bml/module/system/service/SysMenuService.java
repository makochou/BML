package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.module.system.dto.SysMenuDTO;
import com.bml.module.system.entity.SysMenu;
import com.bml.module.system.vo.RouterVO;

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
    List<RouterVO> buildMenus(List<SysMenu> menus);

    /**
     * 查询权限分配面板所需的扁平菜单列表（业务系统角色授权专用）
     * <p>
     * 仅返回业务系统菜单（即 path='system' 目录及其所有子孙菜单），
     * 不包含中台管理的菜单（工作台、资产目录、授权治理等）。
     * 按 parentId + sort 排序，不组装树结构，由前端按 menuType 分类。
     * </p>
     *
     * @return 扁平菜单列表（仅业务系统菜单）
     */
    List<SysMenu> selectPermissionMenuList();
}
