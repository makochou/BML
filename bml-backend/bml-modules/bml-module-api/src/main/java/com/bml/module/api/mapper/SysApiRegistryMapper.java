package com.bml.module.api.mapper;

import com.bml.core.base.mapper.BmlBaseMapper;
import com.bml.module.api.entity.SysApiRegistry;
import org.apache.ibatis.annotations.Mapper;

/**
 * 开放接口目录数据访问层。
 */
@Mapper
public interface SysApiRegistryMapper extends BmlBaseMapper<SysApiRegistry> {
}
