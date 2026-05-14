package com.bml.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bml.module.system.entity.SysUserMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户菜单权限关联 Mapper 接口。
 * <p>
 * 提供用户个人功能授权的数据库操作。
 * </p>
 *
 * @author BML Team
 */
@Mapper
public interface SysUserMenuMapper extends BaseMapper<SysUserMenu> {
}
