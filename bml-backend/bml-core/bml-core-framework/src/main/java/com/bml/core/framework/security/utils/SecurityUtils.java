package com.bml.core.framework.security.utils;

import com.bml.core.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.bml.core.framework.security.model.LoginUser;

/**
 * 安全服务工具类
 *
 * @author BML Team
 */
public class SecurityUtils {

    /**
     * 获取用户账户
     **/
    public static String getUsername() {
        try {
            return ((LoginUser) getLoginUser()).getUsername();
        } catch (Exception e) {
            throw new BusinessException("获取用户账户异常");
        }
    }

    /**
     * 获取用户ID
     **/
    public static Long getUserId() {
        try {
            return ((LoginUser) getLoginUser()).getUserId();
        } catch (Exception e) {
            throw new BusinessException("获取用户ID异常");
        }
    }

    /**
     * 获取用户
     **/
    public static <T> T getLoginUser() {
        try {
            return (T) getAuthentication().getPrincipal();
        } catch (Exception e) {
            throw new BusinessException("获取用户信息异常");
        }
    }

    /**
     * 获取Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 生成BCryptPasswordEncoder密码
     *
     * @param password 密码
     * @return 加密字符串
     */
    public static String encryptPassword(String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }

    /**
     * 判断密码是否相同
     *
     * @param rawPassword 真实密码
     * @param encodedPassword 加密后字符
     * @return 结果
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
