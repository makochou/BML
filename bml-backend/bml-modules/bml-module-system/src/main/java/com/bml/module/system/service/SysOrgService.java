package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.module.system.dto.SysOrgDTO;
import com.bml.module.system.entity.SysOrg;

import java.util.List;

/**
 * 机构管理 服务接口
 *
 * @author BML Team
 */
public interface SysOrgService extends BaseService<SysOrg> {

    /**
     * 查询机构列表
     */
    List<SysOrg> selectOrgList(SysOrgDTO org);

    /**
     * 构建前端所需要树结构
     */
    List<SysOrg> buildOrgTree(List<SysOrg> orgs);

    /**
     * 根据ID查询所有子机构（正常状态）
     */
    List<SysOrg> selectChildrenOrgById(Long orgId);

    /**
     * 校验机构名称是否唯一
     *
     * @return {@code true} 表示名称已存在（不唯一）
     */
    boolean checkOrgNameUnique(SysOrgDTO org);

    /**
     * 新增机构
     */
    boolean insertOrg(SysOrgDTO orgDto);

    /**
     * 修改机构
     */
    boolean updateOrg(SysOrgDTO orgDto);

    /**
     * 校验机构编码是否唯一（全局唯一）
     *
     * @return {@code true} 表示编码唯一，{@code false} 表示已存在
     */
    boolean checkOrgCodeUnique(SysOrgDTO dto);

    /**
     * 校验机构是否存在子机构
     *
     * @return {@code true} 表示存在子机构
     */
    boolean checkOrgHasChild(Long orgId);
}
