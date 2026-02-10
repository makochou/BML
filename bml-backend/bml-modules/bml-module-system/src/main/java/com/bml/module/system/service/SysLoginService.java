package com.bml.module.system.service;

import com.bml.core.common.exception.BusinessException;
import com.bml.core.framework.security.utils.JwtUtils;
import com.bml.module.system.entity.SysUser;
import com.bml.core.framework.security.model.LoginUser;
import jakarta.annotation.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录校验方法
 *
 * @author BML Team
 */
@Service
public class SysLoginService {

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private JwtUtils jwtUtils;
    
    @Resource
    private SysUserService userService;

    /**
     * 登录验证
     *
     * @param username 用户名
     * @param password 密码
     * @return 结果
     */
    public String login(String username, String password) {
        // 用户验证
        Authentication authentication = null;
        try {
            // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
        
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        // 生成token
        return createToken(loginUser);
    }
    
    /**
     * 创建Token
     */
    public String createToken(LoginUser loginUser) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", loginUser.getUserId());
        claims.put("username", loginUser.getUsername());
        return jwtUtils.createToken(claims, loginUser.getUsername());
    }
}
