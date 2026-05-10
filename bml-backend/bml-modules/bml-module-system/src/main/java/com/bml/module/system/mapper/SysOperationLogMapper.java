package com.bml.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bml.module.system.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统操作日志 Mapper。
 *
 * @author BML Team
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {
}
