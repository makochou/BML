package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.module.system.dto.SysRoleDTO;
import com.bml.module.system.entity.SysRole;

import java.util.List;
import java.util.Set;

/**
 * 角色管理 服务接口
 *
 * @author BML Team
 */
public interface SysRoleService extends BaseService<SysRole> {

    /**
     * 根据条件分页查询角色数据
     */
    List<SysRole> selectRoleList(SysRoleDTO role);

    /**
     * 根据用户ID查询角色列表
     */
    List<SysRole> selectRolesByUserId(Long userId);

    /**
     * 根据用户ID查询角色权限
     */
    Set<String> selectRolePermissionByUserId(Long userId);

    /**
     * 新增角色
     */
    boolean insertRole(SysRoleDTO roleDto);

    /**
     * 修改角色
     */
    boolean updateRole(SysRoleDTO roleDto);
}
