package com.bml.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.module.system.entity.SysUserDataScope;
import com.bml.module.system.mapper.SysUserDataScopeMapper;
import com.bml.module.system.service.SysUserDataScopeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户个人数据权限配置 服务实现
 * <p>
 * 管理用户级别的数据权限覆盖配置。
 * 当用户存在生效中的个人数据权限时，优先使用个人配置，忽略角色数据权限。
 * </p>
 *
 * @author BML Team
 */
@Service
public class SysUserDataScopeServiceImpl
        extends BaseServiceImpl<SysUserDataScopeMapper, SysUserDataScope>
        implements SysUserDataScopeService {

    /**
     * 根据用户ID查询生效中的个人数据权限配置
     * <p>
     * 生效条件：
     * 1. status = 1（启用状态）
     * 2. expire_time 为 NULL 或大于当前时间（未过期）
     * </p>
     */
    @Override
    public SysUserDataScope selectActiveByUserId(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return getOne(new LambdaQueryWrapper<SysUserDataScope>()
                .eq(SysUserDataScope::getUserId, userId)
                .eq(SysUserDataScope::getStatus, 1)
                .and(w -> w.isNull(SysUserDataScope::getExpireTime)
                        .or().gt(SysUserDataScope::getExpireTime, now))
                .last("LIMIT 1"));
    }

    /**
     * 新增或更新用户个人数据权限配置
     */
    @Override
    public boolean saveOrUpdateByUserId(SysUserDataScope dataScope) {
        // 查询是否已存在
        SysUserDataScope existing = getOne(new LambdaQueryWrapper<SysUserDataScope>()
                .eq(SysUserDataScope::getUserId, dataScope.getUserId())
                .last("LIMIT 1"));
        if (existing != null) {
            dataScope.setId(existing.getId());
            return updateById(dataScope);
        }
        return save(dataScope);
    }

    /**
     * 根据用户ID删除个人数据权限配置（逻辑删除）
     */
    @Override
    public boolean removeByUserId(Long userId) {
        return remove(new LambdaQueryWrapper<SysUserDataScope>()
                .eq(SysUserDataScope::getUserId, userId));
    }
}
