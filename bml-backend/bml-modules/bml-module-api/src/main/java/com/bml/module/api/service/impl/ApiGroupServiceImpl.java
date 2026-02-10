package com.bml.module.api.service.impl;

import cn.hutool.core.util.StrUtil;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.module.api.converter.ApiGroupConverter;
import com.bml.module.api.dto.ApiGroupDTO;
import com.bml.module.api.entity.ApiGroup;
import com.bml.module.api.mapper.ApiGroupMapper;
import com.bml.module.api.service.ApiGroupService;
import com.bml.module.api.vo.ApiGroupVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * API分组 服务实现
 *
 * @author BML Team
 */
@Service
public class ApiGroupServiceImpl extends BaseServiceImpl<ApiGroupMapper, ApiGroup> implements ApiGroupService {

    @Override
    public List<ApiGroupVO> selectGroupList(ApiGroupDTO dto) {
        List<ApiGroup> list = this.lambdaQuery()
                .like(StrUtil.isNotBlank(dto.getName()), ApiGroup::getName, dto.getName())
                .eq(dto.getStatus() != null, ApiGroup::getStatus, dto.getStatus())
                .eq(ApiGroup::getDeleted, 0)
                .orderByAsc(ApiGroup::getSort)
                .list();
        return ApiGroupConverter.INSTANCE.toVOList(list);
    }

    @Override
    public boolean insertGroup(ApiGroupDTO dto) {
        ApiGroup group = ApiGroupConverter.INSTANCE.toEntity(dto);
        return this.save(group);
    }

    @Override
    public boolean updateGroup(ApiGroupDTO dto) {
        ApiGroup group = ApiGroupConverter.INSTANCE.toEntity(dto);
        return this.updateById(group);
    }
}
