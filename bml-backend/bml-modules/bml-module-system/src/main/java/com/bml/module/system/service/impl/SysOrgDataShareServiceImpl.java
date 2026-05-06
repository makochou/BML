package com.bml.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.module.system.entity.SysOrgDataShare;
import com.bml.module.system.mapper.SysOrgDataShareMapper;
import com.bml.module.system.service.SysOrgDataShareService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构数据共享规则 服务实现
 * <p>
 * 管理机构间的数据共享规则，用于在数据隔离的前提下开放定向共享通道。
 * </p>
 *
 * @author BML Team
 */
@Service
public class SysOrgDataShareServiceImpl
        extends BaseServiceImpl<SysOrgDataShareMapper, SysOrgDataShare>
        implements SysOrgDataShareService {

    /**
     * 根据源机构ID查询共享规则列表
     */
    @Override
    public List<SysOrgDataShare> selectBySourceOrg(Long sourceOrgId) {
        return list(new LambdaQueryWrapper<SysOrgDataShare>()
                .eq(SysOrgDataShare::getSourceOrgId, sourceOrgId)
                .eq(SysOrgDataShare::getStatus, 1)
                .orderByDesc(SysOrgDataShare::getCreateTime));
    }

    /**
     * 根据目标机构ID查询哪些机构共享了数据给它
     */
    @Override
    public List<SysOrgDataShare> selectByTargetOrg(Long targetOrgId) {
        return list(new LambdaQueryWrapper<SysOrgDataShare>()
                .eq(SysOrgDataShare::getTargetOrgId, targetOrgId)
                .eq(SysOrgDataShare::getStatus, 1)
                .orderByDesc(SysOrgDataShare::getCreateTime));
    }

    /**
     * 查询指定目标机构在特定模块下可访问的源机构ID列表
     * <p>
     * 规则：
     * 1. share_type=1（全模块共享）：不论 moduleCode 值，均生效
     * 2. share_type=2（指定模块共享）：module_code 字段包含传入的 moduleCode
     * 3. 过期的规则不计入（expire_time 不为 NULL 且已过期）
     * </p>
     */
    @Override
    public List<Long> selectAccessibleOrgIds(Long targetOrgId, String moduleCode) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<SysOrgDataShare> wrapper = new LambdaQueryWrapper<SysOrgDataShare>()
                .eq(SysOrgDataShare::getTargetOrgId, targetOrgId)
                .eq(SysOrgDataShare::getStatus, 1)
                .and(w -> w.isNull(SysOrgDataShare::getExpireTime)
                        .or().gt(SysOrgDataShare::getExpireTime, now));

        List<SysOrgDataShare> shares = list(wrapper);
        return shares.stream()
                .filter(s -> isModuleAccessible(s, moduleCode))
                .map(SysOrgDataShare::getSourceOrgId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 新增共享规则
     */
    @Override
    public boolean insertShare(SysOrgDataShare share) {
        return save(share);
    }

    /**
     * 修改共享规则
     */
    @Override
    public boolean updateShare(SysOrgDataShare share) {
        return updateById(share);
    }

    /**
     * 判断共享规则是否覆盖指定模块
     */
    private boolean isModuleAccessible(SysOrgDataShare share, String moduleCode) {
        // 全模块共享：不限模块
        if (share.getShareType() != null && share.getShareType() == 1) {
            return true;
        }
        // 指定模块共享：判断 module_code 是否包含目标模块
        if (moduleCode == null || moduleCode.isEmpty()) {
            return true;
        }
        String modules = share.getModuleCode();
        if (modules == null || modules.isEmpty()) {
            return false;
        }
        for (String m : modules.split(",")) {
            if (m.trim().equalsIgnoreCase(moduleCode.trim())) {
                return true;
            }
        }
        return false;
    }
}
