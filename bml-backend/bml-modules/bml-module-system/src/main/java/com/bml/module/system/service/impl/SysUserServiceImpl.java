package com.bml.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.exception.BusinessException;
import com.bml.module.system.entity.SysUser;
import com.bml.module.system.mapper.SysUserMapper;
import com.bml.module.system.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户管理 服务实现
 *
 * @author BML Team
 */
@Service
public class SysUserServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public java.util.List<SysUser> selectUserList(com.bml.module.system.dto.SysUserDTO user) {
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
        // TODO: Add Data Scope filtering
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public SysUser selectUserByUserName(String userName) {
        return baseMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, userName));
    }

    @Override
    public void checkUserAllowed(SysUser user) {
        if (user.getId() != null && user.getId() == 1L) {
            throw new BusinessException("不允许操作超级管理员用户");
        }
    }

    @Resource
    private com.bml.module.system.mapper.SysUserRoleMapper userRoleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertUser(com.bml.module.system.dto.SysUserDTO userDto) {
        // DTO -> Entity
        SysUser user = com.bml.module.system.converter.UserConverter.INSTANCE.toEntity(userDto);
        // Encrypt password
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(com.bml.core.framework.security.utils.SecurityUtils.encryptPassword(user.getPassword()));
        }
        // 新增用户信息
        int rows = baseMapper.insert(user);
        // 新增用户角色关联
        insertUserRole(user.getId(), userDto.getRoleIds());
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(com.bml.module.system.dto.SysUserDTO userDto) {
        SysUser user = com.bml.module.system.converter.UserConverter.INSTANCE.toEntity(userDto);
        // 校验用户是否允许操作
        checkUserAllowed(user);
        // 删除用户与角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<com.bml.module.system.entity.SysUserRole>()
                .eq(com.bml.module.system.entity.SysUserRole::getUserId, user.getId()));
        // 新增用户与角色关联
        insertUserRole(user.getId(), userDto.getRoleIds());
        return baseMapper.updateById(user) > 0;
    }

    public void insertUserRole(Long userId, java.util.List<Long> roleIds) {
        if (roleIds != null && !roleIds.isEmpty()) {
            // Add roles
            for (Long roleId : roleIds) {
                com.bml.module.system.entity.SysUserRole ur = new com.bml.module.system.entity.SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }
}
