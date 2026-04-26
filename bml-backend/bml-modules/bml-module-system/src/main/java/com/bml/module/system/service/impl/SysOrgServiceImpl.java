package com.bml.module.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.common.exception.BusinessException;
import com.bml.module.system.converter.OrgConverter;
import com.bml.module.system.dto.SysOrgDTO;
import com.bml.module.system.entity.SysOrg;
import com.bml.module.system.mapper.SysOrgMapper;
import com.bml.module.system.service.SysOrgService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 机构管理 服务实现
 * <p>
 * 提供机构的查询、新增、修改、删除以及机构树构建等功能。
 * 机构采用树形结构，通过 parentId + ancestors 字段维护层级关系。
 * </p>
 *
 * @author BML Team
 */
@Service
public class SysOrgServiceImpl extends BaseServiceImpl<SysOrgMapper, SysOrg> implements SysOrgService {

    /**
     * 查询机构列表
     * <p>
     * 支持按机构名称（模糊）、机构编码（模糊）和状态进行条件过滤。
     * 结果按 parentId 和 sort 排序，便于前端构建树形结构。
     * </p>
     */
    @Override
    public List<SysOrg> selectOrgList(SysOrgDTO org) {
        LambdaQueryWrapper<SysOrg> lqw = new LambdaQueryWrapper<>();
        if (org != null) {
            lqw.like(StrUtil.isNotBlank(org.getOrgName()), SysOrg::getOrgName, org.getOrgName());
            lqw.like(StrUtil.isNotBlank(org.getOrgCode()), SysOrg::getOrgCode, org.getOrgCode());
            lqw.eq(org.getStatus() != null, SysOrg::getStatus, org.getStatus());
        }
        lqw.orderByAsc(SysOrg::getParentId, SysOrg::getSort);
        return baseMapper.selectList(lqw);
    }

    /**
     * 构建机构树结构
     * <p>
     * 根节点为 parentId=0 的机构。
     * </p>
     */
    @Override
    public List<SysOrg> buildOrgTree(List<SysOrg> orgs) {
        if (CollUtil.isEmpty(orgs)) {
            return new ArrayList<>();
        }
        return orgs.stream()
                .filter(o -> Objects.equals(o.getParentId(), GlobalConstants.ROOT_NODE_ID))
                .peek(o -> o.setChildren(buildChildren(o, orgs)))
                .collect(Collectors.toList());
    }

    /**
     * 根据机构ID查询所有子机构
     */
    @Override
    public List<SysOrg> selectChildrenOrgById(Long orgId) {
        return baseMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                .apply("find_in_set({0}, ancestors)", orgId));
    }

    /**
     * 校验同一父级下机构名称是否唯一
     *
     * @return {@code true} 表示名称已存在（不唯一）
     */
    @Override
    public boolean checkOrgNameUnique(SysOrgDTO org) {
        long count = this.lambdaQuery()
                .eq(SysOrg::getOrgName, org.getOrgName())
                .eq(SysOrg::getParentId, org.getParentId())
                .ne(org.getId() != null, SysOrg::getId, org.getId())
                .count();
        return count > 0;
    }

    /**
     * 新增机构
     * <p>
     * 自动设置 ancestors 字段（祖级列表）。
     * </p>
     */
    @Override
    public boolean insertOrg(SysOrgDTO orgDto) {
        SysOrg org = OrgConverter.INSTANCE.toEntity(orgDto);
        SysOrg parentOrg = baseMapper.selectById(org.getParentId());
        if (parentOrg != null && GlobalConstants.STATUS_DISABLE.equals(parentOrg.getStatus())) {
            throw new BusinessException("父机构已停用，不允许新增子机构");
        }
        org.setAncestors(parentOrg == null ? "0" : parentOrg.getAncestors() + "," + org.getParentId());
        return this.save(org);
    }

    /**
     * 修改机构
     * <p>
     * 如果父级发生变化，会级联更新所有子机构的 ancestors 字段。
     * </p>
     */
    @Override
    public boolean updateOrg(SysOrgDTO orgDto) {
        SysOrg org = OrgConverter.INSTANCE.toEntity(orgDto);
        SysOrg newParentOrg = baseMapper.selectById(org.getParentId());
        SysOrg oldOrg = baseMapper.selectById(org.getId());
        if (newParentOrg != null && oldOrg != null) {
            String newAncestors = newParentOrg.getAncestors() + "," + newParentOrg.getId();
            String oldAncestors = oldOrg.getAncestors();
            org.setAncestors(newAncestors);
            updateOrgChildren(org.getId(), newAncestors, oldAncestors);
        }
        return this.updateById(org);
    }

    /**
     * 校验机构编码是否唯一（全局唯一）
     *
     * @return {@code true} 表示编码唯一，{@code false} 表示已存在
     */
    @Override
    public boolean checkOrgCodeUnique(SysOrgDTO dto) {
        LambdaQueryWrapper<SysOrg> lqw = new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getOrgCode, dto.getOrgCode())
                .ne(dto.getId() != null, SysOrg::getId, dto.getId());
        return this.count(lqw) == 0;
    }

    /**
     * 校验机构是否存在子机构
     *
     * @return {@code true} 表示存在子机构
     */
    @Override
    public boolean checkOrgHasChild(Long orgId) {
        return this.count(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getParentId, orgId)) > 0;
    }

    // ======================== 私有方法 ========================

    /**
     * 级联更新子机构的 ancestors 字段
     */
    private void updateOrgChildren(Long orgId, String newAncestors, String oldAncestors) {
        List<SysOrg> children = baseMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                .apply("find_in_set({0}, ancestors)", orgId));
        for (SysOrg child : children) {
            child.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
        }
        if (CollUtil.isNotEmpty(children)) {
            updateBatchById(children);
        }
    }

    /**
     * 递归构建子机构列表
     */
    private List<SysOrg> buildChildren(SysOrg root, List<SysOrg> all) {
        return all.stream()
                .filter(o -> Objects.equals(o.getParentId(), root.getId()))
                .peek(o -> o.setChildren(buildChildren(o, all)))
                .collect(Collectors.toList());
    }
}
