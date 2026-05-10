package com.bml.module.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bml.core.common.result.PageResult;
import com.bml.module.system.dto.SysConfigDTO;
import com.bml.module.system.entity.SysConfig;
import com.bml.module.system.mapper.SysConfigMapper;
import com.bml.module.system.vo.SysConfigVO;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置服务
 * <p>
 * 提供基于 {@code sys_config} 表的键值对配置读写能力。
 * 配置键名全局唯一，支持按 key 前缀批量查询，适用于验证码开关、登录页图片等运行时可修改的系统参数。
 * </p>
 *
 * @author BML Team
 */
@Service
public class SysConfigService {

    private static final int DEFAULT_PAGE_NUM = 1;

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAX_PAGE_SIZE = 200;

    @Resource
    private SysConfigMapper configMapper;

    /**
     * 分页查询参数配置。
     *
     * @param dto 查询参数
     * @return 参数配置分页结果
     */
    public PageResult<SysConfigVO> selectConfigPage(SysConfigDTO dto) {
        SysConfigDTO safeDto = dto == null ? new SysConfigDTO() : dto;
        int pageNum = normalizePageNum(safeDto.getPageNum());
        int pageSize = normalizePageSize(safeDto.getPageSize());
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<SysConfig>()
                .like(StrUtil.isNotBlank(safeDto.getConfigName()), SysConfig::getConfigName, safeDto.getConfigName())
                .like(StrUtil.isNotBlank(safeDto.getConfigKey()), SysConfig::getConfigKey, safeDto.getConfigKey())
                .eq(safeDto.getConfigType() != null, SysConfig::getConfigType, safeDto.getConfigType())
                .orderByDesc(SysConfig::getCreateTime, SysConfig::getId);
        Page<SysConfig> page = configMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(), page.getTotal(), pageNum, pageSize);
    }

    /**
     * 查询参数配置详情。
     *
     * @param id 配置ID
     * @return 参数配置详情
     */
    public SysConfigVO selectConfigById(Long id) {
        return id == null ? null : toVO(configMapper.selectById(id));
    }

    /**
     * 新增参数配置。
     *
     * @param dto 参数配置表单
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean insertConfig(SysConfigDTO dto) {
        checkConfigKeyUnique(dto);
        SysConfig config = toEntity(dto);
        if (config.getConfigType() == null) {
            config.setConfigType(0);
        }
        return configMapper.insert(config) > 0;
    }

    /**
     * 修改参数配置。
     *
     * @param dto 参数配置表单
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConfig(SysConfigDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new IllegalArgumentException("配置ID不能为空");
        }
        checkConfigKeyUnique(dto);
        return configMapper.updateById(toEntity(dto)) > 0;
    }

    /**
     * 删除参数配置。
     *
     * @param id 配置ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConfig(Long id) {
        SysConfig config = id == null ? null : configMapper.selectById(id);
        if (config == null) {
            return false;
        }
        if (Integer.valueOf(1).equals(config.getConfigType())) {
            throw new IllegalArgumentException("系统内置配置不允许删除");
        }
        return configMapper.deleteById(id) > 0;
    }

    /**
     * 根据配置键名查询配置值。
     *
     * @param configKey 配置键名
     * @return 配置值，不存在时返回 null
     */
    public String getConfigValue(String configKey) {
        SysConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>()
                        .eq(SysConfig::getConfigKey, configKey)
                        .last("LIMIT 1"));
        return config != null ? config.getConfigValue() : null;
    }

    /**
     * 根据配置键名查询配置值，不存在时返回默认值。
     *
     * @param configKey    配置键名
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getConfigValue(String configKey, String defaultValue) {
        String value = getConfigValue(configKey);
        return value != null ? value : defaultValue;
    }

    /**
     * 新增或更新配置项。
     * <p>
     * 如果 configKey 已存在则更新 configValue，否则新增一条记录。
     * </p>
     *
     * @param configKey   配置键名
     * @param configValue 配置键值
     * @param configName  配置名称（仅新增时使用）
     */
    public void upsertConfig(String configKey, String configValue, String configName) {
        SysConfig existing = configMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>()
                        .eq(SysConfig::getConfigKey, configKey)
                        .last("LIMIT 1"));
        if (existing != null) {
            existing.setConfigValue(configValue);
            configMapper.updateById(existing);
        } else {
            SysConfig config = new SysConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setConfigName(configName != null ? configName : configKey);
            config.setConfigType(0);
            configMapper.insert(config);
        }
    }

    /**
     * 根据键名前缀批量查询配置。
     *
     * @param keyPrefix 键名前缀
     * @return key-value Map
     */
    public Map<String, String> getConfigsByPrefix(String keyPrefix) {
        List<SysConfig> list = configMapper.selectList(
                new LambdaQueryWrapper<SysConfig>()
                        .likeRight(SysConfig::getConfigKey, keyPrefix));
        return list.stream().collect(
                Collectors.toMap(SysConfig::getConfigKey,
                        c -> c.getConfigValue() != null ? c.getConfigValue() : ""));
    }

    private void checkConfigKeyUnique(SysConfigDTO dto) {
        long count = configMapper.selectCount(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, dto.getConfigKey())
                .ne(dto.getId() != null, SysConfig::getId, dto.getId()));
        if (count > 0) {
            throw new IllegalArgumentException("配置键名已存在");
        }
    }

    private SysConfig toEntity(SysConfigDTO dto) {
        SysConfig entity = new SysConfig();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    private SysConfigVO toVO(SysConfig entity) {
        if (entity == null) {
            return null;
        }
        SysConfigVO vo = new SysConfigVO();
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
