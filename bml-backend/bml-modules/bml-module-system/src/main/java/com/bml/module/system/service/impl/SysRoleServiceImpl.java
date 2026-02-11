package com.bml.module.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.module.system.converter.RoleConverter;
import com.bml.module.system.dto.SysRoleDTO;
import com.bml.module.system.entity.SysRole;
import com.bml.module.system.entity.SysRoleMenu;
import com.bml.module.system.mapper.SysRoleMapper;
import com.bml.module.system.mapper.SysRoleMenuMapper;
import com.bml.module.system.service.SysRoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 角色管理 服务实现
 * <p>
 * 提供角色的查询、新增、修改、删除等功能。
 * 新增/修改角色时同步维护角色与菜单的关联关系（sys_role_menu 中间表）。
 * </p>
 *
 * @author BML Team
 */
@Service
public class SysRoleServiceImpl extends BaseServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Resource
    private SysRoleMenuMapper roleMenuMapper;

    /**
     * 根据用户ID查询角色列表
     *
     * @param userId 用户ID
     * @return 用户拥有的角色列表
     */
    @Override
    public List<SysRole> selectRolesByUserId(Long userId) {
        return baseMapper.selectRolesByUserId(userId);
    }

    /**
     * 根据用户ID查询角色权限标识集合
     * <p>
     * 查询用户关联的所有角色的 roleCode，支持逗号分隔的多角色编码拆分。
     * 返回的权限标识用于 Spring Security 的角色鉴权。
     * </p>
     *
     * @param userId 用户ID
     * @return 角色权限标识集合
     */
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

    /**
     * 根据条件查询角色列表
     * <p>
     * 支持按角色名称（模糊）、角色编码（模糊）、状态进行过滤。
     * </p>
     *
     * @param role 查询条件 DTO
     * @return 角色列表
     */
    @Override
    public List<SysRole> selectRoleList(SysRoleDTO role) {
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
        // TODO: 添加数据权限过滤（Data Scope）
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 新增角色（含菜单关联）
     * <p>
     * 先保存角色信息，再批量插入角色与菜单的关联记录。
     * </p>
     *
     * @param roleDto 角色 DTO
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertRole(SysRoleDTO roleDto) {
        SysRole role = RoleConverter.INSTANCE.toEntity(roleDto);
        // 新增角色信息
        baseMapper.insert(role);
        // 新增角色菜单关联
        return insertRoleMenu(role.getId(), roleDto.getMenuIds());
    }

    /**
     * 修改角色（含菜单关联重建）
     * <p>
     * 先更新角色信息，再删除旧的菜单关联，最后重新插入新的关联。
     * 整个操作在事务中执行，保证数据一致性。
     * </p>
     *
     * @param roleDto 角色 DTO
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(SysRoleDTO roleDto) {
        SysRole role = RoleConverter.INSTANCE.toEntity(roleDto);
        // 修改角色信息
        baseMapper.updateById(role);
        // 删除角色与菜单关联
        roleMenuMapper.delete(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, role.getId()));
        // 重新插入角色与菜单关联
        return insertRoleMenu(role.getId(), roleDto.getMenuIds());
    }

    /**
     * 批量插入角色与菜单关联
     *
     * @param roleId  角色ID
     * @param menuIds 菜单ID列表
     * @return 是否成功（menuIds 为空时也返回 true，表示无需插入）
     */
    private boolean insertRoleMenu(Long roleId, List<Long> menuIds) {
        if (CollUtil.isNotEmpty(menuIds)) {
            for (Long menuId : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }
        return true;
    }
}
