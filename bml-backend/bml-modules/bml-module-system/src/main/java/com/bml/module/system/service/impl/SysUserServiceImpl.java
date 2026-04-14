package com.bml.module.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.common.exception.BusinessException;
import com.bml.core.framework.license.LicenseQuotaChecker;
import com.bml.core.framework.security.utils.SecurityUtils;
import com.bml.module.system.converter.UserConverter;
import com.bml.module.system.dto.SysUserDTO;
import com.bml.module.system.entity.SysUser;
import com.bml.module.system.entity.SysUserRole;
import com.bml.module.system.datascope.DataScope;
import com.bml.module.system.datascope.DataScopeContext;
import com.bml.module.system.mapper.SysUserMapper;
import com.bml.module.system.mapper.SysUserRoleMapper;
import com.bml.module.system.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SysUserServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Resource
    private SysUserRoleMapper userRoleMapper;

    @Resource
    private LicenseQuotaChecker licenseQuotaChecker;

    @Override
    @DataScope(deptColumn = "dept_id", orgColumn = "org_id", userColumn = "id", creatorColumn = "create_by")
    public List<SysUser> selectUserList(SysUserDTO user) {
        if (user == null) {
            user = new SysUserDTO();
        }
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
        String dataScopeSql = DataScopeContext.getDataScopeSql();
        if (StrUtil.isNotBlank(dataScopeSql)) {
            queryWrapper.apply(dataScopeSql);
        }
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public SysUser selectUserByUserName(String userName) {
        return baseMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, userName));
    }

    @Override
    public void checkUserAllowed(SysUser user) {
        if (user.getId() != null && GlobalConstants.SYSTEM_USER_ID.equals(user.getId())) {
            throw new BusinessException("不允许操作超级管理员用户");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertUser(SysUserDTO userDto) {
        // 许可证配额校验：检查当前用户数量是否已达上限
        long currentUserCount = this.count();
        licenseQuotaChecker.checkUserQuota(currentUserCount);

        SysUser user = UserConverter.INSTANCE.toEntity(userDto);
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        }
        int rows = baseMapper.insert(user);
        insertUserRole(user.getId(), userDto.getRoleIds());
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(SysUserDTO userDto) {
        SysUser user = UserConverter.INSTANCE.toEntity(userDto);
        checkUserAllowed(user);
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        }
        userRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        insertUserRole(user.getId(), userDto.getRoleIds());
        return baseMapper.updateById(user) > 0;
    }

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
