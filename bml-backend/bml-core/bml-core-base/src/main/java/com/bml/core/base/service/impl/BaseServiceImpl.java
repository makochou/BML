package com.bml.core.base.service.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bml.core.base.service.BaseService;

/**
 * 基础服务实现类
 *
 * @param <M> Mapper类型
 * @param <T> 实体类型
 * @author BML Team
 */
public abstract class BaseServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements BaseService<T> {
}
