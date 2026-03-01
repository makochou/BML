package com.bml.module.api.mapper;

import com.bml.core.base.mapper.BmlBaseMapper;
import com.bml.module.api.entity.SysApiAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * API账号数据访问层。
 */
@Mapper
public interface SysApiAccountMapper extends BmlBaseMapper<SysApiAccount> {
}
