package com.bml.module.api.service;

import com.bml.core.base.service.BaseService;
import com.bml.module.api.dto.ApiGroupDTO;
import com.bml.module.api.entity.ApiGroup;
import com.bml.module.api.vo.ApiGroupVO;

import java.util.List;

/**
 * API分组 服务接口
 *
 * @author BML Team
 */
public interface ApiGroupService extends BaseService<ApiGroup> {

    /**
     * 查询分组列表
     */
    List<ApiGroupVO> selectGroupList(ApiGroupDTO dto);

    /**
     * 新增分组
     */
    boolean insertGroup(ApiGroupDTO dto);

    /**
     * 修改分组
     */
    boolean updateGroup(ApiGroupDTO dto);
}
