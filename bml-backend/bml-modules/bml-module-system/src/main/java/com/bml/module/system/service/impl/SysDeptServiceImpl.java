package com.bml.module.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.module.system.entity.SysDept;
import com.bml.module.system.mapper.SysDeptMapper;
import com.bml.module.system.service.SysDeptService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门管理 服务实现
 *
 * @author BML Team
 */
@Service
public class SysDeptServiceImpl extends BaseServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

    @Override
    public List<SysDept> selectDeptList(com.bml.module.system.dto.SysDeptDTO dept) {
        LambdaQueryWrapper<SysDept> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SysDept::getDeleted, 0);
        if (dept != null) {
            lqw.like(StrUtil.isNotBlank(dept.getDeptName()), SysDept::getDeptName, dept.getDeptName());
            lqw.eq(dept.getStatus() != null, SysDept::getStatus, dept.getStatus());
        }
        lqw.orderByAsc(SysDept::getParentId, SysDept::getSort);
        return baseMapper.selectList(lqw);
    }

    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts) {
        if (CollUtil.isEmpty(depts)) {
            return CollUtil.newArrayList();
        }
        return depts.stream().filter(d -> d.getParentId() == 0).peek(d -> d.setChildren(getChildren(d, depts)))
                .collect(Collectors.toList());
    }

    @Override
    public boolean checkDeptNameUnique(com.bml.module.system.dto.SysDeptDTO dept) {
        long count = this.lambdaQuery()
                .eq(SysDept::getDeptName, dept.getDeptName())
                .eq(SysDept::getParentId, dept.getParentId())
                .ne(dept.getId() != null, SysDept::getId, dept.getId())
                .eq(SysDept::getDeleted, 0)
                .count();
        return count > 0;
    }

    @Override
    public boolean insertDept(com.bml.module.system.dto.SysDeptDTO deptDto) {
        SysDept dept = com.bml.module.system.converter.DeptConverter.INSTANCE.toEntity(deptDto);
        SysDept info = baseMapper.selectById(dept.getParentId());
        // 如果父节点不为正常状态,则不允许新增子节点
        if (info != null && info.getStatus() == 0) {
            throw new RuntimeException("部门停用，不允许新增");
        }
        dept.setAncestors(info == null ? "0" : info.getAncestors() + "," + dept.getParentId());
        return this.save(dept);
    }

    @Override
    public boolean updateDept(com.bml.module.system.dto.SysDeptDTO deptDto) {
        SysDept dept = com.bml.module.system.converter.DeptConverter.INSTANCE.toEntity(deptDto);
        SysDept newParentDept = baseMapper.selectById(dept.getParentId());
        SysDept oldDept = baseMapper.selectById(dept.getId());
        if (newParentDept != null && oldDept != null) {
            String newAncestors = newParentDept.getAncestors() + "," + newParentDept.getId();
            String oldAncestors = oldDept.getAncestors();
            dept.setAncestors(newAncestors);
            updateDeptChildren(dept.getId(), newAncestors, oldAncestors);
        }
        return this.updateById(dept);
    }

    /**
     * 修改子元素关系
     * 
     * @param deptId       被修改的部门ID
     * @param newAncestors 新的父ID集合
     * @param oldAncestors 旧的父ID集合
     */
    public void updateDeptChildren(Long deptId, String newAncestors, String oldAncestors) {
        List<SysDept> children = baseMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .apply("find_in_set({0}, ancestors)", deptId));
        for (SysDept child : children) {
            child.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
        }
        if (CollUtil.isNotEmpty(children)) {
            updateBatchById(children);
        }
    }

    /**
     * 递归获取子节点
     */
    private List<SysDept> getChildren(SysDept root, List<SysDept> all) {
        return all.stream().filter(d -> d.getParentId().equals(root.getId()))
                .peek(d -> d.setChildren(getChildren(d, all))).collect(Collectors.toList());
    }

    @Override
    public List<SysDept> selectChildrenDeptById(Long deptId) {
        return baseMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeleted, 0)
                .apply("find_in_set({0}, ancestors)", deptId));
    }
}
