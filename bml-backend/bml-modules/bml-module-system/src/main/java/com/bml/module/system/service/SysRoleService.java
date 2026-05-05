package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.core.common.result.PageResult;
import com.bml.module.system.dto.SysRoleDTO;
import com.bml.module.system.entity.SysRole;
import com.bml.module.system.vo.SysRoleVO;

import java.util.List;
import java.util.Set;

/**
 * 角色管理 服务接口
 *
 * @author BML Team
 */
public interface SysRoleService extends BaseService<SysRole> {

    /**
     * 根据条件查询角色列表（不分页）
     */
    List<SysRole> selectRoleList(SysRoleDTO role);

    /**
     * 分页查询角色列表
     *
     * @param dto      查询条件
     * @param pageNum  当前页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<SysRoleVO> selectRolePage(SysRoleDTO dto, int pageNum, int pageSize);

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

    /**
     * 根据角色ID查询完全勾选的菜单ID列表（halfCheck=0）
     */
    List<Long> selectMenuIdsByRoleId(Long roleId);

    /**
     * 根据角色ID查询半选的菜单ID列表（halfCheck=1）
     */
    List<Long> selectHalfCheckMenuIdsByRoleId(Long roleId);

    /**
     * 根据角色ID查询自定义数据权限的机构ID列表
     */
    List<Long> selectCustomOrgIdsByRoleId(Long roleId);

    /**
     * 根据角色ID查询自定义数据权限的部门ID列表
     */
    List<Long> selectCustomDeptIdsByRoleId(Long roleId);
}
