package com.bml.module.system.security;

import com.bml.module.system.entity.SysUser;
import com.bml.module.system.service.SysUserService;
import com.bml.module.system.service.SysMenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 用户详情服务
 *
 * @author BML Team
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final SysUserService userService;
    
    private final SysMenuService permissionService;

    public UserDetailsServiceImpl(SysUserService userService, SysMenuService permissionService) {
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userService.selectUserByUserName(username);
        if (user == null) {
            log.info("登录用户：{} 不存在.", username);
            throw new UsernameNotFoundException("登录用户：" + username + " 不存在");
        } else if (user.getDeleted() == 1) {
            log.info("登录用户：{} 已被删除.", username);
            throw new UsernameNotFoundException("对不起，您的账号：" + username + " 已被删除");
        } else if (user.getStatus() == 0) {
            log.info("登录用户：{} 已被停用.", username);
            throw new UsernameNotFoundException("对不起，您的账号：" + username + " 已停用");
        }

        return createLoginUser(user);
    }

    public UserDetails createLoginUser(SysUser user) {
        return new com.bml.core.framework.security.model.LoginUser(
            user.getId(), 
            user.getDeptId(), 
            user.getUsername(), 
            user.getPassword(), 
            user.getStatus(),
            permissionService.selectMenuPermsByUserId(user.getId())
        );
    }
}
