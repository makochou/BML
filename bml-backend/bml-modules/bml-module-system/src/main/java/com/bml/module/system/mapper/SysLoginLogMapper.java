package com.bml.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bml.module.system.entity.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统登录日志 Mapper。
 *
 * @author BML Team
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {
}
