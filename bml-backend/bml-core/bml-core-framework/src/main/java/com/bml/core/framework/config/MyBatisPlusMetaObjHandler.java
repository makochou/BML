package com.bml.core.framework.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.bml.core.framework.security.model.LoginUser;
import org.apache.ibatis.reflection.MetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 元对象处理器（自动填充）
 * <p>
 * 配合 {@link com.bml.core.base.entity.BaseEntity} 中的
 * {@code @TableField(fill = ...)} 注解，
 * 在 INSERT 和 UPDATE 操作时自动填充审计字段：
 * <ul>
 * <li>INSERT：自动填充 createBy、createTime、updateBy、updateTime</li>
 * <li>UPDATE：自动填充 updateBy、updateTime</li>
 * </ul>
 * </p>
 * <p>
 * <b>注意：</b>
 * {@code strictInsertFill / strictUpdateFill} 只在字段值为 null 时才会填充，
 * 如果业务代码已手动设置了值则不会被覆盖。
 * </p>
 *
 * @author BML Team
 */
@Component
public class MyBatisPlusMetaObjHandler implements MetaObjectHandler {

    private static final Logger log = LoggerFactory.getLogger(MyBatisPlusMetaObjHandler.class);

    /**
     * 插入时自动填充
     * <p>
     * 设置创建人、创建时间、更新人、更新时间。
     * </p>
     *
     * @param metaObject 元对象（MyBatis-Plus 封装的实体元数据）
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("MyBatis-Plus 插入自动填充执行");
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = getCurrentUserId();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createBy", Long.class, currentUserId);
        this.strictInsertFill(metaObject, "updateBy", Long.class, currentUserId);
    }

    /**
     * 更新时自动填充
     * <p>
     * 仅设置更新人和更新时间，创建人和创建时间不变。
     * </p>
     *
     * @param metaObject 元对象（MyBatis-Plus 封装的实体元数据）
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("MyBatis-Plus 更新自动填充执行");
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateBy", Long.class, getCurrentUserId());
    }

    /**
     * 获取当前登录用户ID
     * <p>
     * 从 Spring Security 上下文中获取当前认证用户的 ID。
     * 在以下场景可能返回 null：
     * <ul>
     * <li>应用启动初始化阶段</li>
     * <li>匿名访问（未认证）的请求</li>
     * <li>定时任务等非 HTTP 请求线程</li>
     * </ul>
     * </p>
     *
     * @return 当前用户ID，无法获取时返回 null
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
                return loginUser.getUserId();
            }
        } catch (Exception e) {
            // 启动阶段或匿名访问时安全上下文可能为空，此处静默忽略
            log.trace("无法获取当前用户ID，可能处于启动或匿名访问阶段", e);
        }
        return null;
    }
}
