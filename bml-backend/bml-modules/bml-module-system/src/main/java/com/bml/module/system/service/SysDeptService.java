package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.module.system.entity.SysDept;

import java.util.List;

/**
 * 部门管理 服务接口
 *
 * @author BML Team
 */
public interface SysDeptService extends BaseService<SysDept> {

    /**
     * 查询部门树列表
     */
    /**
     * 查询部门树列表
     */
    List<SysDept> selectDeptList(com.bml.module.system.dto.SysDeptDTO dept);

    /**
     * 构建前端所需要树结构
     */
    List<SysDept> buildDeptTree(List<SysDept> depts);

    /**
     * 根据ID查询所有子部门（正常状态）
     */
    List<SysDept> selectChildrenDeptById(Long deptId);

    /**
     * 校验部门名称是否唯一
     */
    boolean checkDeptNameUnique(com.bml.module.system.dto.SysDeptDTO dept);

    /**
     * 新增部门
     */
    boolean insertDept(com.bml.module.system.dto.SysDeptDTO deptDto);

    /**
     * 修改部门
     */
    boolean updateDept(com.bml.module.system.dto.SysDeptDTO deptDto);
}
