package com.bml.module.api.mapper;

import com.bml.core.base.mapper.BmlBaseMapper;
import com.bml.module.api.entity.ApiApp;
import org.apache.ibatis.annotations.Mapper;

/**
 * API应用管理 Mapper 接口
 *
 * @author BML Team
 */
@Mapper
public interface ApiAppMapper extends BmlBaseMapper<ApiApp> {
}
