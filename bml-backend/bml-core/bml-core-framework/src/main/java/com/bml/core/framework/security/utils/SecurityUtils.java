package com.bml.core.framework.security.utils;

import com.bml.core.common.exception.BusinessException;
import com.bml.core.framework.security.model.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 安全服务工具类
 * <p>
 * 提供从 Spring Security 上下文中获取当前登录用户信息的快捷方法，
 * 以及密码加密/校验的工具方法。
 * </p>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 * <li>在 Service/Controller 中通过 {@link #getLoginUser()} 获取当前登录用户</li>
 * <li>在用户新增/修改密码时通过 {@link #encryptPassword(String)} 加密</li>
 * </ul>
 * </p>
 *
 * @author BML Team
 */
public class SecurityUtils {

    /**
     * BCryptPasswordEncoder 静态实例，避免每次调用都创建新对象
     * <p>
     * BCryptPasswordEncoder 是线程安全的，可以安全地作为静态常量复用。
     * </p>
     */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 获取当前登录用户的用户名
     *
     * @return 用户名
     * @throws BusinessException 当未认证或认证信息异常时抛出
     */
    public static String getUsername() {
        try {
            return getLoginUser().getUsername();
        } catch (Exception e) {
            throw new BusinessException("获取用户账户异常");
        }
    }

    /**
     * 获取当前登录用户的用户ID
     *
     * @return 用户ID
     * @throws BusinessException 当未认证或认证信息异常时抛出
     */
    public static Long getUserId() {
        try {
            return getLoginUser().getUserId();
        } catch (Exception e) {
            throw new BusinessException("获取用户ID异常");
        }
    }

    /**
     * 获取当前登录用户信息
     * <p>
     * 从 Spring Security 的 {@link SecurityContextHolder} 中获取 {@link LoginUser} 对象。
     * 该对象包含用户ID、用户名、权限列表等完整信息。
     * </p>
     *
     * @return 当前登录用户
     * @throws BusinessException 当未认证或 Principal 类型不匹配时抛出
     */
    public static LoginUser getLoginUser() {
        try {
            Object principal = getAuthentication().getPrincipal();
            if (principal instanceof LoginUser loginUser) {
                return loginUser;
            }
            throw new BusinessException("用户信息类型异常");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("获取用户信息异常");
        }
    }

    /**
     * 获取当前 Authentication 对象
     *
     * @return Spring Security Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 生成 BCrypt 加密密码
     * <p>
     * 使用 BCrypt 算法对明文密码进行加密，每次加密结果不同（因为 BCrypt 自动加盐）。
     * </p>
     *
     * @param password 明文密码
     * @return BCrypt 加密后的密码字符串
     */
    public static String encryptPassword(String password) {
        return PASSWORD_ENCODER.encode(password);
    }

    /**
     * 校验明文密码与加密密码是否匹配
     *
     * @param rawPassword     明文密码
     * @param encodedPassword BCrypt 加密后的密码
     * @return {@code true} 匹配，{@code false} 不匹配
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        return PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
    }
}
