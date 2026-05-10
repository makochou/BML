package com.bml.module.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.bml.module.system.service.SysCacheService;
import com.bml.module.system.vo.SysCacheVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SysCacheServiceImpl implements SysCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public SysCacheVO overview(String pattern) {
        SysCacheVO vo = new SysCacheVO();
        vo.setDbSize(dbSize());
        Properties info = redisTemplate.execute(RedisConnection::serverCommands).info();
        Map<String, Object> infoMap = new LinkedHashMap<>();
        if (info != null) {
            info.forEach((key, value) -> infoMap.put(String.valueOf(key), value));
            vo.setRedisVersion(info.getProperty("redis_version"));
            vo.setUsedMemoryHuman(info.getProperty("used_memory_human"));
            vo.setConnectedClients(info.getProperty("connected_clients"));
            vo.setUptimeInDays(info.getProperty("uptime_in_days"));
        }
        vo.setInfo(infoMap);
        vo.setKeys(keys(pattern));
        return vo;
    }

    @Override
    public List<String> keys(String pattern) {
        String safePattern = StrUtil.blankToDefault(pattern, "*");
        Set<String> keys = redisTemplate.keys(safePattern);
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return keys.stream().sorted().limit(500).toList();
    }

    @Override
    public boolean deleteKey(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    @Override
    public boolean clearPrefix(String prefix) {
        if (StrUtil.isBlank(prefix)) {
            return false;
        }
        List<String> matchedKeys = new ArrayList<>(keys(prefix + "*"));
        if (matchedKeys.isEmpty()) {
            return true;
        }
        redisTemplate.delete(matchedKeys);
        return true;
    }

    private Long dbSize() {
        Long size = redisTemplate.execute((RedisCallback<Long>) connection -> connection.serverCommands().dbSize());
        return size == null ? 0L : size;
    }
}
