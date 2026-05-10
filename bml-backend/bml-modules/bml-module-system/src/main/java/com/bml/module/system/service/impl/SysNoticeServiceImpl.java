package com.bml.module.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.result.PageResult;
import com.bml.module.system.dto.SysNoticeDTO;
import com.bml.module.system.entity.SysNotice;
import com.bml.module.system.mapper.SysNoticeMapper;
import com.bml.module.system.service.SysNoticeService;
import com.bml.module.system.vo.SysNoticeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysNoticeServiceImpl extends BaseServiceImpl<SysNoticeMapper, SysNotice> implements SysNoticeService {

    @Override
    public PageResult<SysNoticeVO> selectNoticePage(SysNoticeDTO dto) {
        SysNoticeDTO safeDto = dto == null ? new SysNoticeDTO() : dto;
        int pageNum = normalizePageNum(safeDto.getPageNum());
        int pageSize = normalizePageSize(safeDto.getPageSize());
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<SysNotice>()
                .like(StrUtil.isNotBlank(safeDto.getNoticeTitle()), SysNotice::getNoticeTitle, safeDto.getNoticeTitle())
                .eq(safeDto.getNoticeType() != null, SysNotice::getNoticeType, safeDto.getNoticeType())
                .eq(safeDto.getStatus() != null, SysNotice::getStatus, safeDto.getStatus())
                .orderByDesc(SysNotice::getPublishTime, SysNotice::getCreateTime, SysNotice::getId);
        Page<SysNotice> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        List<SysNoticeVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public SysNoticeVO selectNoticeById(Long id) {
        return id == null ? null : toVO(this.getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertNotice(SysNoticeDTO dto) {
        SysNotice notice = toEntity(dto);
        if (notice.getStatus() == null) {
            notice.setStatus(0);
        }
        if (Integer.valueOf(1).equals(notice.getStatus()) && notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
        return this.save(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateNotice(SysNoticeDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new IllegalArgumentException("公告ID不能为空");
        }
        SysNotice notice = toEntity(dto);
        if (Integer.valueOf(1).equals(notice.getStatus())) {
            SysNotice old = this.getById(dto.getId());
            if (old == null || old.getPublishTime() == null) {
                notice.setPublishTime(LocalDateTime.now());
            }
        }
        return this.updateById(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishNotice(Long id) {
        SysNotice notice = id == null ? null : this.getById(id);
        if (notice == null) {
            return false;
        }
        notice.setStatus(1);
        notice.setPublishTime(LocalDateTime.now());
        return this.updateById(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revokeNotice(Long id) {
        SysNotice notice = id == null ? null : this.getById(id);
        if (notice == null) {
            return false;
        }
        notice.setStatus(0);
        return this.updateById(notice);
    }

    private SysNotice toEntity(SysNoticeDTO dto) {
        SysNotice entity = new SysNotice();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    private SysNoticeVO toVO(SysNotice entity) {
        if (entity == null) {
            return null;
        }
        SysNoticeVO vo = new SysNoticeVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 200);
    }
}
