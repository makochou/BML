package com.bml.module.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.common.exception.BusinessException;
import com.bml.module.system.converter.DeptConverter;
import com.bml.module.system.datascope.DataScope;
import com.bml.module.system.datascope.DataScopeContext;
import com.bml.module.system.dto.SysDeptDTO;
import com.bml.module.system.entity.SysDept;
import com.bml.module.system.entity.SysOrg;
import com.bml.module.system.mapper.SysDeptMapper;
import com.bml.module.system.mapper.SysOrgMapper;
import com.bml.module.system.service.SysDeptService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 部门管理 服务实现
 * <p>
 * 提供部门的查询、新增、修改、删除以及部门树构建等功能。
 * 部门采用树形结构，通过 parentId + ancestors 字段维护层级关系。
 * </p>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 * <li>MyBatis-Plus 的 {@code @TableLogic} 会自动追加 deleted=0 条件，业务代码中无需手动添加</li>
 * <li>Long 类型比较统一使用 {@link Objects#equals}，避免包装类 {@code ==} 比较陷阱</li>
 * </ul>
 * </p>
 *
 * @author BML Team
 */
@Service
public class SysDeptServiceImpl extends BaseServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

    @Resource
    private SysOrgMapper orgMapper;

    /**
     * 查询部门列表
     * <p>
     * 支持按部门名称（模糊）和状态进行条件过滤。
     * 结果按 parentId 和 sort 排序，便于前端构建树形结构。
     * </p>
     *
     * @param dept 查询条件 DTO（可为 null 表示无条件）
     * @return 部门列表
     */
    @Override
    @DataScope(deptColumn = "id", orgColumn = "org_id", creatorColumn = "create_by")
    public List<SysDept> selectDeptList(SysDeptDTO dept) {
        LambdaQueryWrapper<SysDept> lqw = new LambdaQueryWrapper<>();
        // @TableLogic 自动追加 deleted=0，无需手动添加
        if (dept != null) {
            lqw.like(StrUtil.isNotBlank(dept.getDeptName()), SysDept::getDeptName, dept.getDeptName());
            lqw.eq(dept.getStatus() != null, SysDept::getStatus, dept.getStatus());
        }
        String dataScopeSql = DataScopeContext.getDataScopeSql();
        if (StrUtil.isNotBlank(dataScopeSql)) {
            lqw.apply(dataScopeSql);
        }
        lqw.orderByAsc(SysDept::getParentId, SysDept::getSort);
        List<SysDept> depts = baseMapper.selectList(lqw);
        // 批量填充所属机构名称（orgName），避免 N+1 查询
        fillOrgName(depts);
        return depts;
    }

    /**
     * 构建部门树结构
     * <p>
     * 从平铺的部门列表中构建完整的父子树形结构。
     * 根节点为 parentId={@link GlobalConstants#ROOT_NODE_ID}（值为 0）的部门。
     * 使用 {@link Objects#equals} 进行 Long 比较，避免包装类 {@code ==} 比较陷阱。
     * </p>
     *
     * @param depts 部门平铺列表
     * @return 树形结构列表（仅包含根节点，子节点嵌套在 children 中）
     */
    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts) {
        if (CollUtil.isEmpty(depts)) {
            return new ArrayList<>();
        }
        return depts.stream()
                .filter(d -> Objects.equals(d.getParentId(), GlobalConstants.ROOT_NODE_ID))
                .peek(d -> d.setChildren(buildChildren(d, depts)))
                .collect(Collectors.toList());
    }

    /**
     * 校验同一父级下部门名称是否唯一
     *
     * @param dept 部门 DTO（包含 deptName、parentId、id）
     * @return {@code true} 表示名称已存在（不唯一），{@code false} 表示唯一
     */
    @Override
    public boolean checkDeptNameUnique(SysDeptDTO dept) {
        long count = this.lambdaQuery()
                .eq(SysDept::getDeptName, dept.getDeptName())
                .eq(SysDept::getParentId, dept.getParentId())
                .ne(dept.getId() != null, SysDept::getId, dept.getId())
                // @TableLogic 自动追加 deleted=0
                .count();
        return count > 0;
    }

    /**
     * 新增部门
     * <p>
     * 新增前会校验父部门是否处于正常状态。
     * 自动设置 ancestors 字段（祖级列表），格式如 "0,100,101"。
     * </p>
     *
     * @param deptDto 部门 DTO
     * @return 是否成功
     * @throws BusinessException 当父部门处于停用状态时抛出
     */
    @Override
    public boolean insertDept(SysDeptDTO deptDto) {
        SysDept dept = DeptConverter.INSTANCE.toEntity(deptDto);
        SysDept parentDept = baseMapper.selectById(dept.getParentId());
        // 父部门停用时不允许新增子部门
        if (parentDept != null && GlobalConstants.STATUS_DISABLE.equals(parentDept.getStatus())) {
            throw new BusinessException("父部门已停用，不允许新增子部门");
        }
        // 设置祖级列表
        dept.setAncestors(parentDept == null ? "0" : parentDept.getAncestors() + "," + dept.getParentId());
        return this.save(dept);
    }

    /**
     * 修改部门
     * <p>
     * 修改部门信息时，如果父级发生变化，会级联更新所有子部门的 ancestors 字段。
     * </p>
     *
     * @param deptDto 部门 DTO
     * @return 是否成功
     */
    @Override
    public boolean updateDept(SysDeptDTO deptDto) {
        SysDept dept = DeptConverter.INSTANCE.toEntity(deptDto);
        SysDept newParentDept = baseMapper.selectById(dept.getParentId());
        SysDept oldDept = baseMapper.selectById(dept.getId());
        if (newParentDept != null && oldDept != null) {
            String newAncestors = newParentDept.getAncestors() + "," + newParentDept.getId();
            String oldAncestors = oldDept.getAncestors();
            dept.setAncestors(newAncestors);
            // 级联更新子部门的祖级列表
            updateDeptChildren(dept.getId(), newAncestors, oldAncestors);
        }
        return this.updateById(dept);
    }

    /**
     * 根据部门ID查询所有子部门（正常状态）
     * <p>
     * 使用 MySQL 的 {@code FIND_IN_SET} 函数在 ancestors 字段中查找包含指定部门ID的记录。
     * </p>
     *
     * @param deptId 部门ID
     * @return 子部门列表
     */
    @Override
    public List<SysDept> selectChildrenDeptById(Long deptId) {
        return baseMapper.selectList(new LambdaQueryWrapper<SysDept>()
                // @TableLogic 自动追加 deleted=0
                .apply("find_in_set({0}, ancestors)", deptId));
    }

    // ======================== 私有方法 ========================

    /**
     * 批量填充部门列表中的所属机构名称（orgName）
     * <p>
     * 从部门列表中收集所有不为 null 的 orgId，一次性查询 sys_org 表，
     * 构建 orgId → orgName 映射后回填到每个部门实体的 orgName 字段。
     * 该方法可有效避免逐条查询产生的 N+1 性能问题。
     * </p>
     *
     * @param depts 部门平铺列表（会直接修改列表中各元素的 orgName）
     */
    private void fillOrgName(List<SysDept> depts) {
        if (CollUtil.isEmpty(depts)) {
            return;
        }
        // 收集所有不为 null 的机构ID（去重）
        Set<Long> orgIds = depts.stream()
                .map(SysDept::getOrgId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (orgIds.isEmpty()) {
            return;
        }
        // 批量查询机构信息，构建 orgId → orgName 映射
        List<SysOrg> orgs = orgMapper.selectBatchIds(orgIds);
        Map<Long, String> orgNameMap = orgs.stream()
                .collect(Collectors.toMap(SysOrg::getId, SysOrg::getOrgName, (a, b) -> a));
        // 回填 orgName
        depts.forEach(d -> {
            if (d.getOrgId() != null) {
                d.setOrgName(orgNameMap.get(d.getOrgId()));
            }
        });
    }

    /**
     * 级联更新子部门的 ancestors 字段
     * <p>
     * 当部门的父级发生变化时，所有子部门的 ancestors 前缀需要同步更新。
     * 使用 replaceFirst 将旧的祖级前缀替换为新的。
     * </p>
     *
     * @param deptId       被修改的部门ID
     * @param newAncestors 新的祖级列表
     * @param oldAncestors 旧的祖级列表
     */
    private void updateDeptChildren(Long deptId, String newAncestors, String oldAncestors) {
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
     * 递归构建子部门列表
     * <p>
     * 使用 {@link Objects#equals} 进行 Long 类型比较，
     * 确保 parentId 超出 Long 缓存池范围（-128~127）时仍能正确比较。
     * </p>
     *
     * @param root 根部门节点
     * @param all  所有部门平铺列表
     * @return 子部门列表（已递归构建完成）
     */
    private List<SysDept> buildChildren(SysDept root, List<SysDept> all) {
        return all.stream()
                .filter(d -> Objects.equals(d.getParentId(), root.getId()))
                .peek(d -> d.setChildren(buildChildren(d, all)))
                .collect(Collectors.toList());
    }
}
