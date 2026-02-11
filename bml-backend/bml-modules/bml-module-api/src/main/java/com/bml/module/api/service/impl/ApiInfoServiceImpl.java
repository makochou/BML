package com.bml.module.api.service.impl;

import cn.hutool.core.util.StrUtil;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.module.api.converter.ApiInfoConverter;
import com.bml.module.api.dto.ApiInfoDTO;
import com.bml.module.api.entity.ApiInfo;
import com.bml.module.api.mapper.ApiInfoMapper;
import com.bml.module.api.service.ApiInfoService;
import com.bml.module.api.vo.ApiInfoVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * API详情 服务实现
 *
 * @author BML Team
 */
@Service
public class ApiInfoServiceImpl extends BaseServiceImpl<ApiInfoMapper, ApiInfo> implements ApiInfoService {

    @Override
    public List<ApiInfoVO> selectApiList(ApiInfoDTO dto) {
        // @TableLogic 自动追加 deleted=0 条件，无需手动添加
        List<ApiInfo> list = this.lambdaQuery()
                .eq(dto.getGroupId() != null, ApiInfo::getGroupId, dto.getGroupId())
                .like(StrUtil.isNotBlank(dto.getName()), ApiInfo::getName, dto.getName())
                .like(StrUtil.isNotBlank(dto.getPath()), ApiInfo::getPath, dto.getPath())
                .eq(StrUtil.isNotBlank(dto.getMethod()), ApiInfo::getMethod, dto.getMethod())
                .eq(dto.getStatus() != null, ApiInfo::getStatus, dto.getStatus())
                .orderByDesc(ApiInfo::getCreateTime)
                .list();
        return ApiInfoConverter.INSTANCE.toVOList(list);
    }

    @Override
    public boolean insertApi(ApiInfoDTO dto) {
        ApiInfo api = ApiInfoConverter.INSTANCE.toEntity(dto);
        return this.save(api);
    }

    @Override
    public boolean updateApi(ApiInfoDTO dto) {
        ApiInfo api = ApiInfoConverter.INSTANCE.toEntity(dto);
        return this.updateById(api);
    }
}
