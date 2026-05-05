package com.bml.module.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.result.PageResult;
import com.bml.module.system.converter.PostConverter;
import com.bml.module.system.dto.SysPostDTO;
import com.bml.module.system.entity.SysOrg;
import com.bml.module.system.entity.SysPost;
import com.bml.module.system.mapper.SysOrgMapper;
import com.bml.module.system.mapper.SysPostMapper;
import com.bml.module.system.service.SysPostService;
import com.bml.module.system.vo.SysPostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 岗位管理 服务实现
 * <p>
 * 岗位为扁平列表结构，支持按名称、编码模糊查询和状态过滤。
 * </p>
 *
 * @author BML Team
 */
@Service
@RequiredArgsConstructor
public class SysPostServiceImpl extends BaseServiceImpl<SysPostMapper, SysPost> implements SysPostService {

    private final SysOrgMapper orgMapper;

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

    /**
     * 分页查询岗位列表
     */
    @Override
    public PageResult<SysPostVO> selectPostPage(SysPostDTO dto, int pageNum, int pageSize) {
        Page<SysPost> pageObj = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysPost> lqw = new LambdaQueryWrapper<>();
        if (dto != null) {
            lqw.like(StrUtil.isNotBlank(dto.getPostName()), SysPost::getPostName, dto.getPostName());
            lqw.like(StrUtil.isNotBlank(dto.getPostCode()), SysPost::getPostCode, dto.getPostCode());
            lqw.eq(dto.getOrgId() != null, SysPost::getOrgId, dto.getOrgId());
            lqw.eq(StrUtil.isNotBlank(dto.getPostCategory()), SysPost::getPostCategory, dto.getPostCategory());
            lqw.eq(dto.getStatus() != null, SysPost::getStatus, dto.getStatus());
        }
        lqw.orderByAsc(SysPost::getSort);
        Page<SysPost> result = this.page(pageObj, lqw);
        List<SysPostVO> records = PostConverter.INSTANCE.toVOList(result.getRecords());
        // 批量填充所属机构名称
        fillOrgName(records, result.getRecords());
        return PageResult.of(records, result.getTotal(), pageNum, pageSize);
    }

    /**
     * 批量填充岗位 VO 列表中的所属机构名称（orgName）
     * <p>
     * 从原始实体列表中收集所有不为 null 的 orgId，一次性查询 sys_org 表，
     * 构建 orgId → orgName 映射后回填到每个 VO 的 orgName 字段。
     * 该方法可有效避免逐条查询产生的 N+1 性能问题。
     * </p>
     *
     * @param voList   岗位 VO 列表（会直接修改列表中各元素的 orgName）
     * @param entities 岗位实体列表（用于获取 orgId）
     */
    private void fillOrgName(List<SysPostVO> voList, List<SysPost> entities) {
        if (CollUtil.isEmpty(entities)) {
            return;
        }
        // 收集所有不为 null 的机构ID（去重）
        Set<Long> orgIds = entities.stream()
                .map(SysPost::getOrgId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (orgIds.isEmpty()) {
            return;
        }
        // 批量查询机构信息，构建 orgId → orgName 映射
        Map<Long, String> orgNameMap = orgMapper.selectBatchIds(orgIds).stream()
                .collect(Collectors.toMap(SysOrg::getId, SysOrg::getOrgName, (a, b) -> a));
        // 回填 orgName
        for (int i = 0; i < voList.size(); i++) {
            SysPost entity = entities.get(i);
            if (entity.getOrgId() != null) {
                voList.get(i).setOrgName(orgNameMap.get(entity.getOrgId()));
            }
        }
    }
}
