package com.bml.module.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.bml.core.common.constant.TokenConstants;
import com.bml.core.framework.security.model.LoginUser;
import com.bml.core.framework.security.service.TokenService;
import com.bml.module.system.service.SysOnlineUserService;
import com.bml.module.system.vo.SysOnlineUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SysOnlineUserServiceImpl implements SysOnlineUserService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final TokenService tokenService;

    @Override
    public List<SysOnlineUserVO> listOnlineUsers(String username) {
        Set<String> keys = redisTemplate.keys(TokenConstants.LOGIN_TOKEN_KEY + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<SysOnlineUserVO> users = new ArrayList<>();
        for (String key : keys) {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof LoginUser loginUser) {
                if (StrUtil.isNotBlank(username) && !StrUtil.containsIgnoreCase(loginUser.getUsername(), username)) {
                    continue;
                }
                SysOnlineUserVO vo = new SysOnlineUserVO();
                vo.setUserKey(loginUser.getUserKey());
                vo.setUserId(loginUser.getUserId());
                vo.setUsername(loginUser.getUsername());
                vo.setDeptId(loginUser.getDeptId());
                vo.setStatus(loginUser.getStatus());
                vo.setLoginTime(toLocalDateTime(loginUser.getLoginTime()));
                vo.setExpireTime(toLocalDateTime(loginUser.getExpireTime()));
                vo.setTtlSeconds(redisTemplate.getExpire(key, TimeUnit.SECONDS));
                users.add(vo);
            }
        }
        users.sort(Comparator.comparing(SysOnlineUserVO::getLoginTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return users;
    }

    @Override
    public boolean forceLogout(String userKey) {
        if (StrUtil.isBlank(userKey)) {
            return false;
        }
        tokenService.deleteLoginUser(userKey);
        return true;
    }

    private LocalDateTime toLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
}
