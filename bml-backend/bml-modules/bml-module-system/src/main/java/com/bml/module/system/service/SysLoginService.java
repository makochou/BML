package com.bml.module.system.service;

import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.common.exception.BusinessException;
import com.bml.core.framework.security.model.LoginUser;
import com.bml.core.framework.security.model.TokenVO;
import com.bml.core.framework.security.service.TokenService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * 登录校验服务
 * <p>
 * 负责处理用户登录认证的核心逻辑：
 * <ol>
 * <li>通过 Spring Security 的 {@link AuthenticationManager} 进行密码验证</li>
 * <li>验证通过后，调用 {@link TokenService} 生成双令牌（AccessToken + RefreshToken）</li>
 * <li>将用户完整信息（含权限列表）缓存到 Redis</li>
 * </ol>
 * </p>
 *
 * <h3>认证流程：</h3>
 * 
 * <pre>
 * AuthController.login()
 *   └─ SysLoginService.login()
 *        ├─ AuthenticationManager.authenticate()  ← Spring Security 密码校验
 *        │     └─ UserDetailsServiceImpl.loadUserByUsername()  ← 加载用户+权限
 *        └─ TokenService.createToken()  ← 生成双Token + 缓存Redis
 * </pre>
 *
 * @author BML Team
 */
@Service
public class SysLoginService {

    private static final Logger log = LoggerFactory.getLogger(SysLoginService.class);

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private TokenService tokenService;

    /**
     * 登录验证
     * <p>
     * 执行流程：
     * <ol>
     * <li>调用 {@code authenticationManager.authenticate()} 进行账号密码验证</li>
     * <li>该方法内部会调用 {@code UserDetailsServiceImpl.loadUserByUsername()}</li>
     * <li>验证通过后获取 {@link LoginUser} 对象（包含用户信息和权限列表）</li>
     * <li>调用 {@code tokenService.createToken()} 生成双令牌并缓存用户信息到 Redis</li>
     * </ol>
     * </p>
     *
     * @param username 账号
     * @param password 密码（明文，由 BCrypt 比对）
     * @return Token 响应对象，包含 accessToken、refreshToken、expiresIn
     * @throws BusinessException 登录失败时抛出，包含具体错误信息
     */
    public TokenVO login(String username, String password) {
        // 1. 用户验证
        Authentication authentication;
        try {
            // 调用 Spring Security 认证管理器
            // 内部流程: AuthenticationManager → UserDetailsServiceImpl.loadUserByUsername() →
            // BCrypt密码比对
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException e) {
            // 账号被禁用
            log.warn("登录失败：用户[{}]已被禁用", username);
            throw new BusinessException(GlobalErrorCode.ACCOUNT_DISABLED);
        } catch (LockedException e) {
            // 账号被锁定
            log.warn("登录失败：用户[{}]已被锁定", username);
            throw new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED);
        } catch (BadCredentialsException e) {
            // 密码错误
            log.warn("登录失败：用户[{}]密码错误", username);
            throw new BusinessException("账号或密码错误");
        } catch (Exception e) {
            // 其他未预期的认证异常
            log.error("登录失败：认证异常", e);
            throw new BusinessException("登录失败：" + e.getMessage());
        }

        // 2. 认证成功，获取登录用户信息
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        log.info("用户[{}]登录成功", username);

        // 3. 生成双 Token 并缓存到 Redis
        return tokenService.createToken(loginUser);
    }
}
