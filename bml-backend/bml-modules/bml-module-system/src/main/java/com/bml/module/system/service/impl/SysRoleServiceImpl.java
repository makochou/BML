package com.bml.module.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.module.system.entity.SysRole;
import com.bml.module.system.entity.SysRoleMenu;
import com.bml.module.system.mapper.SysRoleMapper;
import com.bml.module.system.mapper.SysRoleMenuMapper;
import com.bml.module.system.service.SysRoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 角色管理 服务实现
 *
 * @author BML Team
 */
@Service
public class SysRoleServiceImpl extends BaseServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Resource
    private SysRoleMenuMapper roleMenuMapper;

    @Override
    public List<SysRole> selectRolesByUserId(Long userId) {
        return baseMapper.selectRolesByUserId(userId);
    }

    @Override
    public Set<String> selectRolePermissionByUserId(Long userId) {
        List<SysRole> perms = baseMapper.selectRolesByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (SysRole perm : perms) {
            if (perm != null && StrUtil.isNotEmpty(perm.getRoleCode())) {
                permsSet.addAll(Arrays.asList(perm.getRoleCode().trim().split(",")));
            }
        }
        return permsSet;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SysRole role) {
        // 新增角色信息
        baseMapper.insert(role);
        // 新增角色菜单关联
        return insertRoleMenu(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(SysRole role) {
        // 修改角色信息
        baseMapper.updateById(role);
        // 删除角色与菜单关联
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, role.getId()));
        return insertRoleMenu(role);
    }

    @Override
    public List<SysRole> selectRoleList(com.bml.module.system.dto.SysRoleDTO role) {
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotEmpty(role.getRoleName())) {
            queryWrapper.like(SysRole::getRoleName, role.getRoleName());
        }
        if (StrUtil.isNotEmpty(role.getRoleCode())) {
            queryWrapper.like(SysRole::getRoleCode, role.getRoleCode());
        }
        if (role.getStatus() != null) {
            queryWrapper.eq(SysRole::getStatus, role.getStatus());
        }
        // TODO: Add Data Scope filtering
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public boolean insertRole(com.bml.module.system.dto.SysRoleDTO roleDto) {
        SysRole role = com.bml.module.system.converter.RoleConverter.INSTANCE.toEntity(roleDto);
        return save(role);
    }

    @Override
    public boolean updateRole(com.bml.module.system.dto.SysRoleDTO roleDto) {
        SysRole role = com.bml.module.system.converter.RoleConverter.INSTANCE.toEntity(roleDto);
        return updateById(role);
    }

    public boolean insertRoleMenu(SysRole role) {
        boolean rows = true;
        List<Long> menuIds = role.getMenuIds();
        if (CollUtil.isNotEmpty(menuIds)) {
            // 新增用户与角色管理
            for (Long menuId : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(role.getId());
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }
        return rows;
    }
}
