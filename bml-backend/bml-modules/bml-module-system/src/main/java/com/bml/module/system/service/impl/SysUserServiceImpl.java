package com.bml.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.common.exception.BusinessException;
import com.bml.core.framework.security.utils.SecurityUtils;
import com.bml.module.system.converter.UserConverter;
import com.bml.module.system.dto.SysUserDTO;
import com.bml.module.system.entity.SysUser;
import com.bml.module.system.entity.SysUserRole;
import com.bml.module.system.mapper.SysUserMapper;
import com.bml.module.system.mapper.SysUserRoleMapper;
import com.bml.module.system.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户管理 服务实现
 * <p>
 * 提供用户的查询、新增、修改、删除等功能。
 * 新增/修改用户时同步维护用户与角色的关联关系（sys_user_role 中间表）。
 * </p>
 *
 * @author BML Team
 */
@Service
public class SysUserServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Resource
    private SysUserRoleMapper userRoleMapper;

    /**
     * 根据条件查询用户列表
     * <p>
     * 支持按用户名（模糊）、手机号（模糊）、状态、部门ID 进行过滤。
     * </p>
     *
     * @param user 查询条件 DTO
     * @return 用户列表
     */
    @Override
    public List<SysUser> selectUserList(SysUserDTO user) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(user.getUsername())) {
            queryWrapper.like(SysUser::getUsername, user.getUsername());
        }
        if (StringUtils.hasText(user.getPhone())) {
            queryWrapper.like(SysUser::getPhone, user.getPhone());
        }
        if (user.getStatus() != null) {
            queryWrapper.eq(SysUser::getStatus, user.getStatus());
        }
        if (user.getDeptId() != null) {
            queryWrapper.eq(SysUser::getDeptId, user.getDeptId());
        }
        // TODO: 添加数据权限过滤（Data Scope）
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 根据用户名查询用户
     *
     * @param userName 用户名
     * @return 用户信息（不存在返回 null）
     */
    @Override
    public SysUser selectUserByUserName(String userName) {
        return baseMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, userName));
    }

    /**
     * 校验用户是否允许操作
     * <p>
     * 超级管理员（{@link GlobalConstants#SYSTEM_USER_ID}）不允许被修改或删除，
     * 确保系统始终有可用的管理员账号。
     * 使用 {@link Long#equals} 而非 {@code ==} 进行比较，避免包装类引用比较陷阱。
     * </p>
     *
     * @param user 用户实体
     * @throws BusinessException 当尝试操作超级管理员时抛出
     */
    @Override
    public void checkUserAllowed(SysUser user) {
        if (user.getId() != null && GlobalConstants.SYSTEM_USER_ID.equals(user.getId())) {
            throw new BusinessException("不允许操作超级管理员用户");
        }
    }

    /**
     * 新增用户
     * <p>
     * 流程：
     * <ol>
     * <li>DTO 转 Entity（通过 MapStruct 转换）</li>
     * <li>密码 BCrypt 加密</li>
     * <li>插入用户记录</li>
     * <li>维护用户角色关联关系</li>
     * </ol>
     * </p>
     *
     * @param userDto 用户 DTO
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertUser(SysUserDTO userDto) {
        SysUser user = UserConverter.INSTANCE.toEntity(userDto);
        // 密码 BCrypt 加密
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        }
        // 新增用户信息
        int rows = baseMapper.insert(user);
        // 新增用户角色关联
        insertUserRole(user.getId(), userDto.getRoleIds());
        return rows > 0;
    }

    /**
     * 修改用户
     * <p>
     * 修改时先校验是否允许操作，然后先删除旧的角色关联再插入新的。
     * 整个操作在事务中执行，保证数据一致性。
     * </p>
     *
     * @param userDto 用户 DTO
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(SysUserDTO userDto) {
        SysUser user = UserConverter.INSTANCE.toEntity(userDto);
        // 校验用户是否允许操作
        checkUserAllowed(user);
        // 删除用户与角色关联
        userRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        // 新增用户与角色关联
        insertUserRole(user.getId(), userDto.getRoleIds());
        return baseMapper.updateById(user) > 0;
    }

    /**
     * 批量插入用户角色关联
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     */
    private void insertUserRole(Long userId, List<Long> roleIds) {
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }
}
