package com.bml.module.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.module.system.converter.PostConverter;
import com.bml.module.system.dto.SysPostDTO;
import com.bml.module.system.entity.SysPost;
import com.bml.module.system.mapper.SysPostMapper;
import com.bml.module.system.service.SysPostService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 岗位管理 服务实现
 * <p>
 * 岗位为扁平列表结构，支持按名称、编码模糊查询和状态过滤。
 * </p>
 *
 * @author BML Team
 */
@Service
public class SysPostServiceImpl extends BaseServiceImpl<SysPostMapper, SysPost> implements SysPostService {

    /**
     * 查询岗位列表
     */
    @Override
    public List<SysPost> selectPostList(SysPostDTO post) {
        LambdaQueryWrapper<SysPost> lqw = new LambdaQueryWrapper<>();
        if (post != null) {
            lqw.like(StrUtil.isNotBlank(post.getPostName()), SysPost::getPostName, post.getPostName());
            lqw.like(StrUtil.isNotBlank(post.getPostCode()), SysPost::getPostCode, post.getPostCode());
            lqw.eq(post.getStatus() != null, SysPost::getStatus, post.getStatus());
        }
        lqw.orderByAsc(SysPost::getSort);
        return baseMapper.selectList(lqw);
    }

    /**
     * 校验岗位编码是否唯一
     *
     * @return {@code true} 表示编码已存在（不唯一）
     */
    @Override
    public boolean checkPostCodeUnique(SysPostDTO post) {
        long count = this.lambdaQuery()
                .eq(SysPost::getPostCode, post.getPostCode())
                .ne(post.getId() != null, SysPost::getId, post.getId())
                .count();
        return count > 0;
    }

    /**
     * 新增岗位
     */
    @Override
    public boolean insertPost(SysPostDTO postDto) {
        return this.save(PostConverter.INSTANCE.toEntity(postDto));
    }

    /**
     * 修改岗位
     */
    @Override
    public boolean updatePost(SysPostDTO postDto) {
        return this.updateById(PostConverter.INSTANCE.toEntity(postDto));
    }
}
