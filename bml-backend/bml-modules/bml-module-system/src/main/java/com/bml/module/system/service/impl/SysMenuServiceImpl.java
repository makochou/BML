package com.bml.module.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.constant.GlobalConstants;
import com.bml.module.system.entity.SysMenu;
import com.bml.module.system.mapper.SysMenuMapper;
import com.bml.module.system.service.SysMenuService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单管理 服务实现
 *
 * @author BML Team
 */
@Service
public class SysMenuServiceImpl extends BaseServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<SysMenu> selectMenuList(com.bml.module.system.dto.SysMenuDTO menu, Long userId) {
        List<SysMenu> menuList = null;
        // 管理员显示所有菜单信息
        if (GlobalConstants.SYSTEM_USER_ID.equals(userId)) {
            menuList = this.lambdaQuery()
                    .like(StrUtil.isNotEmpty(menu.getMenuName()), SysMenu::getMenuName, menu.getMenuName())
                    .eq(StrUtil.isNotEmpty(menu.getMenuType()), SysMenu::getMenuType, menu.getMenuType())
                    .eq(menu.getStatus() != null, SysMenu::getStatus, menu.getStatus())
                    .eq(menu.getVisible() != null, SysMenu::getVisible, menu.getVisible())
                    .orderByAsc(SysMenu::getParentId, SysMenu::getSort)
                    .list();
        } else {
            // TODO: User permission filtering
            menuList = baseMapper.selectMenuTreeByUserId(userId);
        }
        return menuList;
    }

    @Override
    public Set<String> selectMenuPermsByUserId(Long userId) {
        List<String> perms = baseMapper.selectMenuPermsByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms) {
            if (StrUtil.isNotEmpty(perm)) {
                permsSet.addAll(Arrays.asList(perm.trim().split(",")));
            }
        }
        return permsSet;
    }

    @Override
    public List<SysMenu> selectMenuTreeByUserId(Long userId) {
        List<SysMenu> menus = null;
        if (GlobalConstants.SYSTEM_USER_ID.equals(userId)) {
            // 超级管理员显示所有
            menus = this.lambdaQuery()
                    .in(SysMenu::getMenuType, "M", "C")
                    .eq(SysMenu::getStatus, 1)
                    .eq(SysMenu::getDeleted, 0)
                    .orderByAsc(SysMenu::getParentId, SysMenu::getSort)
                    .list();
        } else {
            menus = baseMapper.selectMenuTreeByUserId(userId);
        }
        return getChildPerms(menus, 0L);
    }

    @Override
    public List<SysMenu> buildMenus(List<SysMenu> menus) {
        return menus;
    }

    @Override
    public boolean checkMenuNameUnique(com.bml.module.system.dto.SysMenuDTO menu) {
        long count = this.lambdaQuery()
                .eq(SysMenu::getMenuName, menu.getMenuName())
                .eq(SysMenu::getParentId, menu.getParentId())
                .ne(menu.getId() != null, SysMenu::getId, menu.getId())
                .count();
        return count > 0;
    }

    @Override
    public boolean insertMenu(com.bml.module.system.dto.SysMenuDTO menuDto) {
        SysMenu menu = com.bml.module.system.converter.MenuConverter.INSTANCE.toEntity(menuDto);
        return this.save(menu);
    }

    @Override
    public boolean updateMenu(com.bml.module.system.dto.SysMenuDTO menuDto) {
        SysMenu menu = com.bml.module.system.converter.MenuConverter.INSTANCE.toEntity(menuDto);
        return this.updateById(menu);
    }

    /**
     * 根据父节点的ID获取所有子节点
     */
    public List<SysMenu> getChildPerms(List<SysMenu> list, long parentId) {
        List<SysMenu> returnList = new ArrayList<>();
        for (Iterator<SysMenu> iterator = list.iterator(); iterator.hasNext();) {
            SysMenu t = iterator.next();
            // 一、根据传入的某个父节点ID,遍历该父节点的所有子节点
            if (t.getParentId() == parentId) {
                recursionFn(list, t);
                returnList.add(t);
            }
        }
        return returnList;
    }

    /**
     * 递归列表
     */
    private void recursionFn(List<SysMenu> list, SysMenu t) {
        // 得到子节点列表
        List<SysMenu> childList = getChildList(list, t);
        t.setChildren(childList);
        for (SysMenu tChild : childList) {
            if (hasChild(list, tChild)) {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<SysMenu> getChildList(List<SysMenu> list, SysMenu t) {
        List<SysMenu> tlist = new ArrayList<>();
        Iterator<SysMenu> it = list.iterator();
        while (it.hasNext()) {
            SysMenu n = it.next();
            if (n.getParentId().longValue() == t.getId().longValue()) {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<SysMenu> list, SysMenu t) {
        return getChildList(list, t).size() > 0;
    }
}
