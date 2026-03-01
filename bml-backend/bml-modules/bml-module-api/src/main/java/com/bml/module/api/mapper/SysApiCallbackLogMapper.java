package com.bml.module.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bml.module.api.entity.SysApiCallbackLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * API 回调日志 Mapper。
 */
@Mapper
public interface SysApiCallbackLogMapper extends BaseMapper<SysApiCallbackLog> {
}
