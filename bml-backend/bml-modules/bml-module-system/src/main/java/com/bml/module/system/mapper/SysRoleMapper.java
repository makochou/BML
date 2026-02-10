package com.bml.module.system.mapper;

import com.bml.core.base.mapper.BmlBaseMapper;
import com.bml.module.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色表 Mapper 接口
 *
 * @author BML Team
 */
@Mapper
public interface SysRoleMapper extends BmlBaseMapper<SysRole> {

    /**
     * 根据用户ID查询角色权限
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);
}
