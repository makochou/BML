package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.module.system.dto.SysUserDTO;
import com.bml.module.system.entity.SysUser;

import java.util.List;

/**
 * 用户管理 服务接口
 *
 * @author BML Team
 */
public interface SysUserService extends BaseService<SysUser> {

    /**
     * 根据条件分页查询用户列表
     */
    List<SysUser> selectUserList(SysUserDTO user);

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
}
