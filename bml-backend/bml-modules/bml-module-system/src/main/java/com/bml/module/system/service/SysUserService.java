package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.module.system.dto.SysUserDTO;
import com.bml.module.system.entity.SysUser;
import com.bml.module.system.vo.SysUserVO;

import java.util.List;

/**
 * 用户管理 服务接口
 *
 * @author BML Team
 */
public interface SysUserService extends BaseService<SysUser> {

    /**
     * 根据条件查询用户列表（含机构/部门/岗位/角色关联名称）
     */
    List<SysUserVO> selectUserList(SysUserDTO user);

    /**
     * 根据用户名查询用户
     */
    SysUser selectUserByUserName(String userName);

    /**
     * 校验用户是否允许操作
     */
    void checkUserAllowed(SysUser user);

    /**
     * 新增用户
     */
    boolean insertUser(SysUserDTO userDto);

    /**
     * 修改用户
     */
    boolean updateUser(SysUserDTO userDto);

    /**
     * 根据用户ID查询用户详情（含机构/部门/岗位/角色关联名称）
     */
    SysUserVO selectUserById(Long userId);

    /**
     * 重置用户密码
     */
    void resetUserPassword(Long userId, String newPassword);
}
