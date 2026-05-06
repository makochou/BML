package com.bml.module.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.result.PageResult;
import com.bml.module.system.converter.RoleConverter;
import com.bml.module.system.datascope.DataScope;
import com.bml.module.system.datascope.DataScopeContext;
import com.bml.module.system.dto.SysRoleDTO;
import com.bml.module.system.entity.SysRole;
import com.bml.module.system.entity.SysRoleDept;
import com.bml.module.system.entity.SysRoleMenu;
import com.bml.module.system.entity.SysRoleOrg;
import com.bml.module.system.mapper.SysRoleDeptMapper;
import com.bml.module.system.mapper.SysRoleMapper;
import com.bml.module.system.mapper.SysRoleMenuMapper;
import com.bml.module.system.mapper.SysRoleOrgMapper;
import com.bml.module.system.service.SysRoleService;
import com.bml.module.system.vo.SysRoleVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色管理 服务实现
 * <p>
 * 提供角色的查询、新增、修改、删除等功能。
 * 新增/修改角色时同步维护角色与菜单的关联关系（sys_role_menu 中间表）。
 * 当数据范围为自定义（dataScope=7）时，同步维护角色与机构的关联关系（sys_role_org 中间表）。
 * </p>
 *
 * @author BML Team
 */
@Service
public class SysRoleServiceImpl extends BaseServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Resource
    private SysRoleMenuMapper roleMenuMapper;

    @Resource
    private SysRoleOrgMapper roleOrgMapper;

    @Resource
    private SysRoleDeptMapper roleDeptMapper;

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
    @DataScope(creatorColumn = "create_by")
    public List<SysRole> selectRoleList(SysRoleDTO role) {
        if (role == null) {
            role = new SysRoleDTO();
        }
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
        String dataScopeSql = DataScopeContext.getDataScopeSql();
        if (StrUtil.isNotBlank(dataScopeSql)) {
            queryWrapper.apply(dataScopeSql);
        }
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 分页查询角色列表
     * <p>
     * 与 {@link #selectRoleList} 使用相同的查询条件和数据权限逻辑，
     * 额外支持 MyBatis-Plus 的分页插件进行物理分页。
     * </p>
     */
    @Override
    @DataScope(creatorColumn = "create_by")
    public PageResult<SysRoleVO> selectRolePage(SysRoleDTO dto, int pageNum, int pageSize) {
        if (dto == null) {
            dto = new SysRoleDTO();
        }
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotEmpty(dto.getRoleName())) {
            queryWrapper.like(SysRole::getRoleName, dto.getRoleName());
        }
        if (StrUtil.isNotEmpty(dto.getRoleCode())) {
            queryWrapper.like(SysRole::getRoleCode, dto.getRoleCode());
        }
        if (dto.getStatus() != null) {
            queryWrapper.eq(SysRole::getStatus, dto.getStatus());
        }
        // 注入数据权限 SQL
        String dataScopeSql = DataScopeContext.getDataScopeSql();
        if (StrUtil.isNotBlank(dataScopeSql)) {
            queryWrapper.apply(dataScopeSql);
        }
        queryWrapper.orderByAsc(SysRole::getSort);
        Page<SysRole> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        List<SysRoleVO> records = RoleConverter.INSTANCE.toVOList(page.getRecords());
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    /**
     * 根据角色ID查询完全勾选的菜单ID列表（halfCheck=0）
     * <p>
     * 仅返回完全勾选的节点，用于前端 a-tree 的 checkedKeys。
     * 树组件会根据 checkedKeys 自动推导父节点的半选状态。
     * </p>
     *
     * @param roleId 角色ID
     * @return 完全勾选的菜单ID列表
     */
    @Override
    public List<Long> selectMenuIdsByRoleId(Long roleId) {
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId)
                        .and(w -> w.eq(SysRoleMenu::getHalfCheck, 0).or().isNull(SysRoleMenu::getHalfCheck)));
        return roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
    }

    /**
     * 根据角色ID查询半选的菜单ID列表（halfCheck=1）
     * <p>
     * 返回半选（indeterminate）状态的父节点 ID，
     * 前端加载时不将这些 ID 设为 checkedKeys，
     * 树组件会自动将它们显示为半选状态。
     * </p>
     *
     * @param roleId 角色ID
     * @return 半选的菜单ID列表
     */
    @Override
    public List<Long> selectHalfCheckMenuIdsByRoleId(Long roleId) {
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId)
                        .eq(SysRoleMenu::getHalfCheck, 1));
        return roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
    }

    /**
     * 新增角色（含菜单关联和机构关联）
     * <p>
     * 先保存角色信息，再批量插入角色与菜单的关联记录。
     * 若 customOrgIds 不为空，则同步维护角色与机构的关联关系。
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
        save(role);
        Long roleId = role.getId();
        // 新增角色菜单关联（完全勾选 + 半选）
        insertRoleMenu(roleId, roleDto.getMenuIds(), 0);
        insertRoleMenu(roleId, roleDto.getHalfCheckMenuIds(), 1);
        // 若有自定义机构范围，维护机构关联
        if (CollUtil.isNotEmpty(roleDto.getCustomOrgIds())) {
            saveRoleOrgs(roleId, roleDto.getCustomOrgIds());
        }
        // 若有自定义部门范围，维护部门关联
        if (CollUtil.isNotEmpty(roleDto.getCustomDeptIds())) {
            saveRoleDepts(roleId, roleDto.getCustomDeptIds());
        }
        return true;
    }

    /**
     * 修改角色（含菜单关联重建和机构关联重建）
     * <p>
     * 先更新角色信息，再删除旧的菜单关联，最后重新插入新的关联。
     * 同时重建机构关联关系。整个操作在事务中执行，保证数据一致性。
     * </p>
     *
     * @param roleDto 角色 DTO
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(SysRoleDTO roleDto) {
        SysRole role = RoleConverter.INSTANCE.toEntity(roleDto);
        Long roleId = role.getId();
        // 修改角色信息
        updateById(role);
        // 删除角色与菜单关联
        deleteRoleMenusByRoleId(roleId);
        // 重新插入角色与菜单关联（完全勾选 + 半选）
        insertRoleMenu(roleId, roleDto.getMenuIds(), 0);
        insertRoleMenu(roleId, roleDto.getHalfCheckMenuIds(), 1);
        // 重建机构关联
        saveRoleOrgs(roleId, roleDto.getCustomOrgIds());
        // 重建部门关联
        saveRoleDepts(roleId, roleDto.getCustomDeptIds());
        return true;
    }

    /**
     * 维护角色与机构的关联关系
     * <p>
     * 先删除 sys_role_org 中 role_id = roleId 的所有记录，
     * 再批量插入新的 (roleId, orgId) 记录。
     * </p>
     *
     * @param roleId 角色ID
     * @param orgIds 机构ID列表
     */
    private void saveRoleOrgs(Long roleId, List<Long> orgIds) {
        // 删除旧的机构关联
        roleOrgMapper.delete(
                new LambdaQueryWrapper<SysRoleOrg>().eq(SysRoleOrg::getRoleId, roleId));
        // 批量插入新的机构关联
        if (CollUtil.isNotEmpty(orgIds)) {
            for (Long orgId : orgIds) {
                SysRoleOrg roleOrg = new SysRoleOrg();
                roleOrg.setRoleId(roleId);
                roleOrg.setOrgId(orgId);
                roleOrgMapper.insert(roleOrg);
            }
        }
    }

    /**
     * 删除角色与菜单的关联关系
     *
     * @param roleId 角色ID
     */
    private void deleteRoleMenusByRoleId(Long roleId) {
        roleMenuMapper.delete(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
    }

    /**
     * 根据角色ID查询自定义数据权限的机构ID列表
     *
     * @param roleId 角色ID
     * @return 机构ID列表
     */
    @Override
    public List<Long> selectCustomOrgIdsByRoleId(Long roleId) {
        List<SysRoleOrg> roleOrgs = roleOrgMapper.selectList(
                new LambdaQueryWrapper<SysRoleOrg>().eq(SysRoleOrg::getRoleId, roleId));
        return roleOrgs.stream().map(SysRoleOrg::getOrgId).collect(Collectors.toList());
    }

    /**
     * 根据角色ID查询自定义数据权限的部门ID列表
     *
     * @param roleId 角色ID
     * @return 部门ID列表
     */
    @Override
    public List<Long> selectCustomDeptIdsByRoleId(Long roleId) {
        List<SysRoleDept> roleDepts = roleDeptMapper.selectList(
                new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId));
        return roleDepts.stream().map(SysRoleDept::getDeptId).collect(Collectors.toList());
    }

    /**
     * 维护角色与部门的关联关系
     * <p>
     * 先删除 sys_role_dept 中 role_id = roleId 的所有记录，
     * 再批量插入新的 (roleId, deptId) 记录。
     * </p>
     *
     * @param roleId  角色ID
     * @param deptIds 部门ID列表
     */
    private void saveRoleDepts(Long roleId, List<Long> deptIds) {
        // 删除旧的部门关联
        roleDeptMapper.delete(
                new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId));
        // 批量插入新的部门关联
        if (CollUtil.isNotEmpty(deptIds)) {
            for (Long deptId : deptIds) {
                SysRoleDept roleDept = new SysRoleDept();
                roleDept.setRoleId(roleId);
                roleDept.setDeptId(deptId);
                roleDeptMapper.insert(roleDept);
            }
        }
    }

    /**
     * 批量插入角色与菜单关联
     *
     * @param roleId    角色ID
     * @param menuIds   菜单ID列表
     * @param halfCheck 半选标记：0=完全勾选，1=半选
     */
    private void insertRoleMenu(Long roleId, List<Long> menuIds, int halfCheck) {
        if (CollUtil.isNotEmpty(menuIds)) {
            for (Long menuId : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                rm.setHalfCheck(halfCheck);
                roleMenuMapper.insert(rm);
            }
        }
    }
}
