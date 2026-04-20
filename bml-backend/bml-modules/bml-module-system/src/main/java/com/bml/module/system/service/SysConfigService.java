package com.bml.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.module.system.entity.SysConfig;
import com.bml.module.system.mapper.SysConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

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

    @Resource
    private SysConfigMapper configMapper;

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
}
