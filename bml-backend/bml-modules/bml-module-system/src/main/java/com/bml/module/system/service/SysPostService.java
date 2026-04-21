package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.module.system.dto.SysPostDTO;
import com.bml.module.system.entity.SysPost;

import java.util.List;

/**
 * 岗位管理 服务接口
 *
 * @author BML Team
 */
public interface SysPostService extends BaseService<SysPost> {

    /**
     * 查询岗位列表
     */
    List<SysPost> selectPostList(SysPostDTO post);

    /**
     * 校验岗位编码是否唯一
     *
     * @return {@code true} 表示编码已存在（不唯一）
     */
    boolean checkPostCodeUnique(SysPostDTO post);

    /**
     * 新增岗位
     */
    boolean insertPost(SysPostDTO postDto);

    /**
     * 修改岗位
     */
    boolean updatePost(SysPostDTO postDto);
}
