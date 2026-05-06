package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.core.common.result.PageResult;
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
     * 分页查询用户列表（含机构/部门/岗位/角色关联名称 + 数据权限过滤）
     *
     * @param dto      查询条件
     * @param pageNum  当前页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<SysUserVO> selectUserPage(SysUserDTO dto, int pageNum, int pageSize);

    /**
     * 根据账号查询用户
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

    /**
     * 修改当前用户的个人信息（账号、用户名）
     *
     * @param userId      当前登录用户ID
     * @param newUsername  新账号
     * @param newNickname 新用户名
     * @return 是否修改成功
     */
    boolean updateProfile(Long userId, String newUsername, String newNickname);

    /**
     * 修改当前用户密码
     *
     * @param userId      当前登录用户ID
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}
