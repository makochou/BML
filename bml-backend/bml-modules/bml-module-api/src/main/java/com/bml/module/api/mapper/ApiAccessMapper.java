package com.bml.module.api.mapper;

import com.bml.core.base.mapper.BmlBaseMapper;
import com.bml.module.api.entity.ApiAccess;
import org.apache.ibatis.annotations.Mapper;

/**
 * API应用权限关联 Mapper 接口
 *
 * @author BML Team
 */
@Mapper
public interface ApiAccessMapper extends BmlBaseMapper<ApiAccess> {
}
