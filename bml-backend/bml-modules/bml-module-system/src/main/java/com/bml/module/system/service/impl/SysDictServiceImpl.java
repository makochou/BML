package com.bml.module.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.common.result.PageResult;
import com.bml.module.system.dto.SysDictDataDTO;
import com.bml.module.system.dto.SysDictTypeDTO;
import com.bml.module.system.entity.SysDictData;
import com.bml.module.system.entity.SysDictType;
import com.bml.module.system.mapper.SysDictDataMapper;
import com.bml.module.system.mapper.SysDictTypeMapper;
import com.bml.module.system.service.SysDictService;
import com.bml.module.system.vo.SysDictDataVO;
import com.bml.module.system.vo.SysDictTypeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典管理服务实现。
 * <p>
 * 采用 MyBatis-Plus 标准查询构造器实现基础 CRUD，所有分页响应统一转换为 PageResult，
 * 便于前端列表组件复用同一套分页解析逻辑。
 * </p>
 *
 * @author BML Team
 */
@Service
@RequiredArgsConstructor
public class SysDictServiceImpl implements SysDictService {

    private static final int DEFAULT_PAGE_NUM = 1;

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAX_PAGE_SIZE = 200;

    private final SysDictTypeMapper dictTypeMapper;

    private final SysDictDataMapper dictDataMapper;

    @Override
    public PageResult<SysDictTypeVO> selectDictTypePage(SysDictTypeDTO dto) {
        SysDictTypeDTO safeDto = dto == null ? new SysDictTypeDTO() : dto;
        int pageNum = normalizePageNum(safeDto.getPageNum());
        int pageSize = normalizePageSize(safeDto.getPageSize());
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<SysDictType>()
                .like(StrUtil.isNotBlank(safeDto.getDictName()), SysDictType::getDictName, safeDto.getDictName())
                .like(StrUtil.isNotBlank(safeDto.getDictType()), SysDictType::getDictType, safeDto.getDictType())
                .eq(safeDto.getStatus() != null, SysDictType::getStatus, safeDto.getStatus())
                .orderByDesc(SysDictType::getCreateTime, SysDictType::getId);
        Page<SysDictType> page = dictTypeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toTypeVO).toList(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public SysDictTypeVO selectDictTypeById(Long id) {
        return id == null ? null : toTypeVO(dictTypeMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertDictType(SysDictTypeDTO dto) {
        checkDictTypeUnique(dto);
        SysDictType entity = toTypeEntity(dto);
        if (entity.getStatus() == null) {
            entity.setStatus(GlobalConstants.STATUS_NORMAL);
        }
        return dictTypeMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDictType(SysDictTypeDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new IllegalArgumentException("字典类型ID不能为空");
        }
        checkDictTypeUnique(dto);
        String oldDictType = dictTypeMapper.selectById(dto.getId()).getDictType();
        SysDictType entity = toTypeEntity(dto);
        boolean updated = dictTypeMapper.updateById(entity) > 0;
        if (updated && StrUtil.isNotBlank(oldDictType) && !oldDictType.equals(dto.getDictType())) {
            SysDictData update = new SysDictData();
            update.setDictType(dto.getDictType());
            dictDataMapper.update(update, new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getDictType, oldDictType));
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDictType(Long id) {
        SysDictType type = id == null ? null : dictTypeMapper.selectById(id);
        if (type == null) {
            return false;
        }
        long dataCount = dictDataMapper.selectCount(new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getDictType, type.getDictType()));
        if (dataCount > 0) {
            throw new IllegalArgumentException("当前字典类型下存在字典数据，请先删除字典数据");
        }
        return dictTypeMapper.deleteById(id) > 0;
    }

    @Override
    public PageResult<SysDictDataVO> selectDictDataPage(SysDictDataDTO dto) {
        SysDictDataDTO safeDto = dto == null ? new SysDictDataDTO() : dto;
        int pageNum = normalizePageNum(safeDto.getPageNum());
        int pageSize = normalizePageSize(safeDto.getPageSize());
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<SysDictData>()
                .eq(StrUtil.isNotBlank(safeDto.getDictType()), SysDictData::getDictType, safeDto.getDictType())
                .like(StrUtil.isNotBlank(safeDto.getDictLabel()), SysDictData::getDictLabel, safeDto.getDictLabel())
                .eq(safeDto.getStatus() != null, SysDictData::getStatus, safeDto.getStatus())
                .orderByAsc(SysDictData::getSort, SysDictData::getId);
        Page<SysDictData> page = dictDataMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toDataVO).toList(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public SysDictDataVO selectDictDataById(Long id) {
        return id == null ? null : toDataVO(dictDataMapper.selectById(id));
    }

    @Override
    public List<SysDictDataVO> selectDictDataByType(String dictType) {
        if (StrUtil.isBlank(dictType)) {
            return List.of();
        }
        return dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                        .eq(SysDictData::getDictType, dictType)
                        .eq(SysDictData::getStatus, GlobalConstants.STATUS_NORMAL)
                        .orderByAsc(SysDictData::getSort, SysDictData::getId))
                .stream()
                .map(this::toDataVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertDictData(SysDictDataDTO dto) {
        checkDictTypeExists(dto.getDictType());
        checkDictDataUnique(dto);
        SysDictData entity = toDataEntity(dto);
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(GlobalConstants.STATUS_NORMAL);
        }
        return dictDataMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDictData(SysDictDataDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new IllegalArgumentException("字典数据ID不能为空");
        }
        checkDictTypeExists(dto.getDictType());
        checkDictDataUnique(dto);
        return dictDataMapper.updateById(toDataEntity(dto)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDictData(Long id) {
        return id != null && dictDataMapper.deleteById(id) > 0;
    }

    private void checkDictTypeUnique(SysDictTypeDTO dto) {
        long count = dictTypeMapper.selectCount(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictType, dto.getDictType())
                .ne(dto.getId() != null, SysDictType::getId, dto.getId()));
        if (count > 0) {
            throw new IllegalArgumentException("字典类型编码已存在");
        }
    }

    private void checkDictTypeExists(String dictType) {
        long count = dictTypeMapper.selectCount(new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, dictType));
        if (count == 0) {
            throw new IllegalArgumentException("字典类型不存在，请先维护字典类型");
        }
    }

    private void checkDictDataUnique(SysDictDataDTO dto) {
        long count = dictDataMapper.selectCount(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dto.getDictType())
                .eq(SysDictData::getDictValue, dto.getDictValue())
                .ne(dto.getId() != null, SysDictData::getId, dto.getId()));
        if (count > 0) {
            throw new IllegalArgumentException("当前字典类型下字典键值已存在");
        }
    }

    private SysDictType toTypeEntity(SysDictTypeDTO dto) {
        SysDictType entity = new SysDictType();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    private SysDictTypeVO toTypeVO(SysDictType entity) {
        if (entity == null) {
            return null;
        }
        SysDictTypeVO vo = new SysDictTypeVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private SysDictData toDataEntity(SysDictDataDTO dto) {
        SysDictData entity = new SysDictData();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    private SysDictDataVO toDataVO(SysDictData entity) {
        if (entity == null) {
            return null;
        }
        SysDictDataVO vo = new SysDictDataVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
