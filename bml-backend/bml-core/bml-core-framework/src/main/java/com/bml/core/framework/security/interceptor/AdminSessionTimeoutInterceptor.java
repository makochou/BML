package com.bml.core.framework.security.interceptor;

import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.common.exception.BusinessException;
import com.bml.core.framework.config.AdminProperties;
import com.bml.core.framework.security.model.LoginUser;
import com.bml.core.framework.security.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 中台管理员会话超时拦截器
 * <p>
 * 用于控制中台管理员的空闲超时时长，当管理员在配置的时间内没有任何操作时，
 * 自动使其会话失效，需要重新登录。
 * </p>
 *
 * <h3>工作原理：</h3>
 * <ol>
 *   <li>每次请求到达时，检查当前用户是否为中台管理员（userId = -1）</li>
 *   <li>从 Redis 中获取管理员的最后活动时间戳</li>
 *   <li>计算距离上次活动的时间间隔</li>
 *   <li>如果超过配置的超时时长，抛出会话过期异常，强制重新登录</li>
 *   <li>如果未超时，更新最后活动时间戳到 Redis</li>
 * </ol>
 *
 * <h3>Redis Key 格式：</h3>
 * <pre>
 * admin_session_activity:{userKey}  →  最后活动时间戳（毫秒）
 * </pre>
 *
 * <h3>配置项：</h3>
 * <pre>
 * bml:
 *   admin:
 *     session-timeout-minutes: 30  # 会话超时时长（分钟），0 表示不限制
 * </pre>
 *
 * <h3>与业务系统的区别：</h3>
 * <ul>
 *   <li>中台管理员：通过此拦截器 + Redis 时间戳实现，配置在 {@code application.yml}</li>
 *   <li>业务系统用户：通过前端 {@code useIdleTimeout} + 数据库 {@code sys_config} 表实现</li>
 * </ul>
 *
 * @author BML Team
 * @see AdminProperties#getSessionTimeoutMinutes()
 */
@Component
public class AdminSessionTimeoutInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AdminSessionTimeoutInterceptor.class);

    /**
     * Redis Key 前缀：管理员会话活动时间
     */
    private static final String REDIS_KEY_PREFIX = "admin_session_activity:";

    private final AdminProperties adminProperties;
    private final TokenService tokenService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 构造函数注入依赖
     *
     * @param adminProperties 管理员配置属性
     * @param tokenService    Token 服务
     * @param redisTemplate   Redis 操作模板
     */
    public AdminSessionTimeoutInterceptor(
            AdminProperties adminProperties,
            TokenService tokenService,
            RedisTemplate<String, Object> redisTemplate) {
        this.adminProperties = adminProperties;
        this.tokenService = tokenService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 请求处理前的拦截逻辑
     * <p>
     * 检查中台管理员的会话是否超时，如果超时则抛出异常，否则更新最后活动时间。
     * </p>
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @return {@code true} 表示继续执行后续处理器，{@code false} 表示中断请求
     * @throws BusinessException 会话超时时抛出
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // 1. 从 SecurityContext 获取当前登录用户
            //    不使用 SecurityUtils.getLoginUser()，因为该方法在未认证时会抛出 BusinessException，
            //    而此拦截器需要兼容 permitAll 白名单路径（如许可证上传等无需登录的接口）。
            LoginUser loginUser = getLoginUserQuietly();
            if (loginUser == null) {
                // 未登录或 Token 无效，放行（由 Spring Security 处理）
                return true;
            }

            // 2. 判断是否为中台管理员
            if (!GlobalConstants.ADMIN_USER_ID.equals(loginUser.getUserId())) {
                // 非中台管理员（业务系统用户），不进行会话超时检查
                return true;
            }

            // 3. 获取配置的超时时长（分钟）
            Integer timeoutMinutes = adminProperties.getSessionTimeoutMinutes();
            if (timeoutMinutes == null || timeoutMinutes <= 0) {
                // 未配置或配置为 0，表示不限制会话时长
                return true;
            }

            // 4. 从 Redis 获取最后活动时间
            String redisKey = getRedisKey(loginUser.getUserKey());
            Object lastActivityObj = redisTemplate.opsForValue().get(redisKey);
            long currentTime = System.currentTimeMillis();

            if (lastActivityObj != null) {
                // 4.1 存在最后活动时间，检查是否超时
                long lastActivityTime = Long.parseLong(lastActivityObj.toString());
                long idleTime = currentTime - lastActivityTime;
                long timeoutMillis = timeoutMinutes * 60 * 1000L;

                if (idleTime > timeoutMillis) {
                    // 会话超时，清除 Redis 中的活动时间记录和用户缓存
                    redisTemplate.delete(redisKey);
                    tokenService.deleteLoginUser(loginUser.getUserKey());
                    log.info("中台管理员[{}]会话超时（空闲 {} 分钟），已自动登出",
                            loginUser.getUsername(), idleTime / 60000);
                    throw new BusinessException(GlobalErrorCode.TOKEN_EXPIRED);
                }
            }

            // 5. 更新最后活动时间到 Redis
            // 设置过期时间为超时时长的 2 倍，确保 Redis 不会过早删除记录
            long expireSeconds = timeoutMinutes * 60 * 2L;
            redisTemplate.opsForValue().set(redisKey, currentTime, expireSeconds, TimeUnit.SECONDS);

            return true;

        } catch (BusinessException e) {
            // 重新抛出业务异常，由全局异常处理器统一处理
            throw e;
        } catch (Exception e) {
            // 其他异常不影响正常请求流程，仅记录日志
            log.error("检查中台管理员会话超时时发生异常", e);
            return true;
        }
    }

    /**
     * 安全地从 SecurityContext 中获取当前登录用户。
     * <p>
     * 与 {@link SecurityUtils#getLoginUser()} 不同，此方法在以下场景下
     * 返回 {@code null} 而不是抛出异常：
     * <ul>
     *   <li>未认证（匿名访问 / Token 缺失或过期）</li>
     *   <li>Principal 类型不是 {@link LoginUser}（如 {@code "anonymousUser"}）</li>
     * </ul>
     * 这样可以确保 permitAll 白名单路径上拦截器正常放行，而不会抛出"用户信息类型异常"。
     * </p>
     *
     * @return 当前登录用户，无法获取时返回 {@code null}
     */
    private LoginUser getLoginUserQuietly() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
                return loginUser;
            }
        } catch (Exception e) {
            log.trace("获取当前登录用户信息失败（可能处于匿名访问阶段）", e);
        }
        return null;
    }

    /**
     * 构建 Redis Key
     *
     * @param userKey 用户唯一标识（UUID）
     * @return 完整的 Redis Key，格式：{@code admin_session_activity:{userKey}}
     */
    private String getRedisKey(String userKey) {
        return REDIS_KEY_PREFIX + userKey;
    }
}
