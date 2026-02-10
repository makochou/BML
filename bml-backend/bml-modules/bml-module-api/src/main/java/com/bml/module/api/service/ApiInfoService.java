package com.bml.module.api.service;

import com.bml.core.base.service.BaseService;
import com.bml.module.api.dto.ApiInfoDTO;
import com.bml.module.api.entity.ApiInfo;
import com.bml.module.api.vo.ApiInfoVO;

import java.util.List;

/**
 * API详情 服务接口
 *
 * @author BML Team
 */
public interface ApiInfoService extends BaseService<ApiInfo> {

    /**
     * 查询API列表
     */
    List<ApiInfoVO> selectApiList(ApiInfoDTO dto);

    /**
     * 新增API
     */
    boolean insertApi(ApiInfoDTO dto);

    /**
     * 修改API
     */
    boolean updateApi(ApiInfoDTO dto);
}
