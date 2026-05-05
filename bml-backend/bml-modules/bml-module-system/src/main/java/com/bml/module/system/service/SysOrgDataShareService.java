package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.module.system.entity.SysOrgDataShare;

import java.util.List;

/**
 * 机构数据共享规则 服务接口
 * <p>
 * 提供机构间数据共享规则的管理功能，包括查询、配置、删除共享规则。
 * 支持全模块共享和指定模块共享两种粒度。
 * </p>
 *
 * @author BML Team
 */
public interface SysOrgDataShareService extends BaseService<SysOrgDataShare> {

    /**
     * 根据源机构ID查询共享规则列表
     *
     * @param sourceOrgId 源机构ID
     * @return 共享规则列表
     */
    List<SysOrgDataShare> selectBySourceOrg(Long sourceOrgId);

    /**
     * 根据目标机构ID查询哪些机构共享了数据给它
     *
     * @param targetOrgId 目标机构ID
     * @return 共享规则列表
     */
    List<SysOrgDataShare> selectByTargetOrg(Long targetOrgId);

    /**
     * 查询指定目标机构在特定模块下可访问的源机构ID列表
     * <p>
     * 用于 DataScopeAspect 查询时获取额外可见的机构范围。
     * </p>
     *
     * @param targetOrgId 目标机构ID（当前用户所属机构）
     * @param moduleCode  模块编码（如 finance, inventory）
     * @return 可访问的源机构ID列表
     */
    List<Long> selectAccessibleOrgIds(Long targetOrgId, String moduleCode);

    /**
     * 新增共享规则
     *
     * @param share 共享规则
     * @return 是否成功
     */
    boolean insertShare(SysOrgDataShare share);

    /**
     * 修改共享规则
     *
     * @param share 共享规则
     * @return 是否成功
     */
    boolean updateShare(SysOrgDataShare share);
}
