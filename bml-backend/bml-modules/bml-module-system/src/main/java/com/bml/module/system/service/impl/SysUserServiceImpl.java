package com.bml.module.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.constant.GlobalConstants;
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
        // 预留：可根据业务需求添加用户操作限制（如禁止删除初始管理员等）
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertUser(SysUserDTO userDto) {
        // 许可证配额校验1：前台业务用户数量不得超过许可证 maxTotalUsers 上限
        long currentUserCount = this.count();
        licenseQuotaChecker.checkUserQuota(currentUserCount);

        // 许可证配额校验2：如果是由 API 账号创建（userId < 0），需校验 API 用户全局额度
        try {
            Long currentUserId = SecurityUtils.getUserId();
            if (currentUserId != null && currentUserId < 0) {
                long activeApiUserCount = baseMapper.selectCount(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getStatus, GlobalConstants.STATUS_NORMAL)
                                .lt(SysUser::getCreateBy, 0L));
                licenseQuotaChecker.checkApiUserQuota(activeApiUserCount);
            }
        } catch (Exception e) {
            // 忽略非认证环境调用
        }

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

        // 许可证配额校验：如果用户状态变更为「启用」，检查活跃用户数是否会超出许可证配额
        if (GlobalConstants.STATUS_NORMAL.equals(user.getStatus())) {
            SysUser existingUser = baseMapper.selectById(user.getId());
            // 仅在从「停用」变更为「启用」时校验（避免不必要的查询）
            if (existingUser != null && !GlobalConstants.STATUS_NORMAL.equals(existingUser.getStatus())) {
                long activeUserCount = baseMapper.selectCount(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getStatus, GlobalConstants.STATUS_NORMAL));
                licenseQuotaChecker.checkEnableUserQuota(activeUserCount);

                // 如果该用户原本是由 API 账号创建的，还要占用 API 创建用户的配额
                if (existingUser.getCreateBy() != null && existingUser.getCreateBy() < 0) {
                    long activeApiUserCount = baseMapper.selectCount(
                            new LambdaQueryWrapper<SysUser>()
                                    .eq(SysUser::getStatus, GlobalConstants.STATUS_NORMAL)
                                    .lt(SysUser::getCreateBy, 0L));
                    licenseQuotaChecker.checkEnableApiUserQuota(activeApiUserCount);
                }
            }
        }

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
