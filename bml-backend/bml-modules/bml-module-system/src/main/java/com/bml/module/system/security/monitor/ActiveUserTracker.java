package com.bml.module.system.security.monitor;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;
import java.util.Map;

/**
 * 实时活跃访客追踪器
 * 基于纯内存滑动窗口维护真实在线用户的连接数（防爆破、防僵尸连接）
 */
@Component
public class ActiveUserTracker {

    private final ConcurrentHashMap<String, Long> activeSessions = new ConcurrentHashMap<>();
    private static final long EXPIRE_WINDOW_MS = 3 * 60 * 1000;

    public ActiveUserTracker() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ActiveUserTracker-Cleaner");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::evictExpiredSessions, 1, 1, TimeUnit.MINUTES);
    }

    public void recordUserActivity(String userFingerprint) {
        if (userFingerprint == null || userFingerprint.isEmpty()) {
            return;
        }
        activeSessions.put(userFingerprint, System.currentTimeMillis());
    }

    public int getActiveUserCount() {
        return activeSessions.size();
    }

    private void evictExpiredSessions() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = activeSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() > EXPIRE_WINDOW_MS) {
                iterator.remove();
            }
        }
    }
}
