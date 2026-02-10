package com.bml.core.framework.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.bml.core.framework.security.model.LoginUser;
import com.bml.core.framework.security.utils.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 元对象处理器 (自动填充)
 *
 * @author BML Team
 */
@Component
public class MyBatisPlusMetaObjHandler implements MetaObjectHandler {

    private static final Logger log = LoggerFactory.getLogger(MyBatisPlusMetaObjHandler.class);

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("start insert fill ....");
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "createBy", Long.class, getUserId());
        this.strictInsertFill(metaObject, "updateBy", Long.class, getUserId());
        // For deleted field, usually database DEFAULT 0 handles it, or LogicDelete handles it.
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("start update fill ....");
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateBy", Long.class, getUserId());
    }

    /**
     * 获取当前用户ID
     */
    private Long getUserId() {
        try {
            String username = SecurityUtils.getUsername();
            // Since SecurityUtils.getLoginUser() returns Object, better to cast and get ID directly if possible.
            // But SecurityUtils.getUsername() is safe as I fixed it.
            // If we need ID, access LoginUser directly.
            // Wait, SecurityUtils.getLoginUser() is generic.
            // Let's cast safely.
            Object principal = SecurityUtils.getLoginUser();
            if (principal instanceof LoginUser) {
                return ((LoginUser) principal).getUserId();
            }
        } catch (Exception e) {
            // Ignore (likely during startup or anonymous access)
        }
        return null;
    }
}
