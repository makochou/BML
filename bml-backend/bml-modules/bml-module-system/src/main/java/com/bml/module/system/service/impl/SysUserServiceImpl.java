package com.bml.module.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.common.result.PageResult;
import com.bml.core.framework.license.LicenseQuotaChecker;
import com.bml.core.framework.security.utils.SecurityUtils;
import com.bml.module.system.converter.UserConverter;
import com.bml.module.system.dto.SysUserDTO;
import com.bml.module.system.entity.*;
import com.bml.module.system.datascope.DataScope;
import com.bml.module.system.datascope.DataScopeContext;
import com.bml.module.system.mapper.*;
import com.bml.module.system.service.SysUserService;
import com.bml.module.system.vo.SysUserVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SysUserServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Resource
    private SysUserRoleMapper userRoleMapper;

    @Resource
    private SysRoleMapper roleMapper;

    @Resource
    private SysOrgMapper orgMapper;

    @Resource
    private SysDeptMapper deptMapper;

    @Resource
    private SysPostMapper postMapper;

    @Resource
    private LicenseQuotaChecker licenseQuotaChecker;

    @Override
    @DataScope(deptColumn = "dept_id", orgColumn = "org_id", userColumn = "id", creatorColumn = "create_by")
    public List<SysUserVO> selectUserList(SysUserDTO user) {
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
        List<SysUser> users = baseMapper.selectList(queryWrapper);
        return enrichUserVOList(users);
    }

    /**
     * 将用户列表转换为 VO 并通过批量查询填充关联名称字段（orgName/deptName/postName/roleIds/roleNames）
     */
    private List<SysUserVO> enrichUserVOList(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 收集所有 ID 集合（过滤 null）
        Set<Long> orgIds = users.stream().map(SysUser::getOrgId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> deptIds = users.stream().map(SysUser::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> postIds = users.stream().map(SysUser::getPostId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = users.stream().map(SysUser::getId).filter(Objects::nonNull).collect(Collectors.toSet());

        // 2. 批量查询机构名称
        Map<Long, String> orgNameMap = new HashMap<>();
        if (!orgIds.isEmpty()) {
            orgMapper.selectBatchIds(orgIds).forEach(o -> orgNameMap.put(o.getId(), o.getOrgName()));
        }

        // 3. 批量查询部门名称
        Map<Long, String> deptNameMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            deptMapper.selectBatchIds(deptIds).forEach(d -> deptNameMap.put(d.getId(), d.getDeptName()));
        }

        // 4. 批量查询岗位名称
        Map<Long, String> postNameMap = new HashMap<>();
        if (!postIds.isEmpty()) {
            postMapper.selectBatchIds(postIds).forEach(p -> postNameMap.put(p.getId(), p.getPostName()));
        }

        // 5. 批量查询用户角色关联
        Map<Long, List<Long>> userRoleIdsMap = new HashMap<>();
        Map<Long, List<String>> userRoleNamesMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUserRole> userRoles = userRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds));

            Set<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
            Map<Long, String> roleNameMap = new HashMap<>();
            if (!roleIds.isEmpty()) {
                roleMapper.selectBatchIds(roleIds).forEach(r -> roleNameMap.put(r.getId(), r.getRoleName()));
            }

            for (SysUserRole ur : userRoles) {
                userRoleIdsMap.computeIfAbsent(ur.getUserId(), k -> new ArrayList<>()).add(ur.getRoleId());
                String roleName = roleNameMap.get(ur.getRoleId());
                if (roleName != null) {
                    userRoleNamesMap.computeIfAbsent(ur.getUserId(), k -> new ArrayList<>()).add(roleName);
                }
            }
        }

        // 6. 组装 VO
        List<SysUserVO> result = new ArrayList<>(users.size());
        for (SysUser u : users) {
            SysUserVO vo = UserConverter.INSTANCE.toVO(u);
            vo.setOrgName(orgNameMap.get(u.getOrgId()));
            vo.setDeptName(deptNameMap.get(u.getDeptId()));
            vo.setPostName(postNameMap.get(u.getPostId()));
            vo.setRoleIds(userRoleIdsMap.getOrDefault(u.getId(), Collections.emptyList()));
            vo.setRoleNames(userRoleNamesMap.getOrDefault(u.getId(), Collections.emptyList()));
            result.add(vo);
        }
        return result;
    }

    /**
     * 分页查询用户列表（含机构/部门/岗位/角色关联名称 + 数据权限过滤）
     * <p>
     * 与 {@link #selectUserList} 使用相同的查询条件和数据权限逻辑，
     * 额外支持 MyBatis-Plus 的分页插件进行物理分页。
     * </p>
     */
    @Override
    @DataScope(deptColumn = "dept_id", orgColumn = "org_id", userColumn = "id", creatorColumn = "create_by")
    public PageResult<SysUserVO> selectUserPage(SysUserDTO dto, int pageNum, int pageSize) {
        if (dto == null) {
            dto = new SysUserDTO();
        }
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getUsername())) {
            queryWrapper.like(SysUser::getUsername, dto.getUsername());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            queryWrapper.like(SysUser::getPhone, dto.getPhone());
        }
        if (dto.getStatus() != null) {
            queryWrapper.eq(SysUser::getStatus, dto.getStatus());
        }
        if (dto.getDeptId() != null) {
            queryWrapper.eq(SysUser::getDeptId, dto.getDeptId());
        }
        if (dto.getOrgId() != null) {
            queryWrapper.eq(SysUser::getOrgId, dto.getOrgId());
        }
        // 注入数据权限 SQL
        String dataScopeSql = DataScopeContext.getDataScopeSql();
        if (StrUtil.isNotBlank(dataScopeSql)) {
            queryWrapper.apply(dataScopeSql);
        }
        Page<SysUser> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        List<SysUserVO> records = enrichUserVOList(page.getRecords());
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
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

    @Override
    public SysUserVO selectUserById(Long userId) {
        SysUser user = baseMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        List<SysUserVO> vos = enrichUserVOList(Collections.singletonList(user));
        return vos.isEmpty() ? null : vos.get(0);
    }

    @Override
    public void resetUserPassword(Long userId, String newPassword) {
        String encrypted = SecurityUtils.encryptPassword(newPassword);
        baseMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getPassword, encrypted));
    }
}
