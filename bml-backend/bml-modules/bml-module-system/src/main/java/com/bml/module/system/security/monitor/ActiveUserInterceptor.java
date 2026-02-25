package com.bml.module.system.security.monitor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 活跃用户访问追踪拦截器
 */
@Component
public class ActiveUserInterceptor implements HandlerInterceptor {

    @Autowired
    private ActiveUserTracker activeUserTracker;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String uri = request.getRequestURI();
        if (uri.contains(".")) {
            return true;
        }

        String userIp = JakartaServletUtil.getClientIP(request);
        String token = request.getHeader("Authorization");
        String fingerprint = StrUtil.isNotBlank(token) ? token : userIp;

        activeUserTracker.recordUserActivity(fingerprint);

        return true;
    }
}
