package com.bml.module.system.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MonitorPermissionAnnotationTest {

    @Test
    void shouldProtectServerMonitorEndpoints() throws Exception {
        assertPreAuthorize(SysServerMonitorController.class, "getServerInfo", "@ss.hasPermi('monitor:server:list')");
        assertPreAuthorize(SysServerMonitorController.class, "cleanJvmMemory", "@ss.hasPermi('monitor:server:gc')");
    }

    @Test
    void shouldProtectAlertEndpoints() throws Exception {
        assertPreAuthorize(SysAlertController.class, "getUnreadCount", "@ss.hasPermi('monitor:alert:list')");
        assertPreAuthorize(SysAlertController.class, "getRecentAlerts", "@ss.hasPermi('monitor:alert:list')", Integer.class);
        assertPreAuthorize(SysAlertController.class, "getLatestAlerts", "@ss.hasPermi('monitor:alert:list')", Long.class);
        assertPreAuthorize(SysAlertController.class, "markAsRead", "@ss.hasPermi('monitor:alert:edit')", Long.class);
        assertPreAuthorize(SysAlertController.class, "markAllAsRead", "@ss.hasPermi('monitor:alert:edit')");
        assertPreAuthorize(SysAlertController.class, "getAlertDates", "@ss.hasPermi('monitor:alert:list')");
        assertPreAuthorize(SysAlertController.class, "getAlertsByDate", "@ss.hasPermi('monitor:alert:list')",
                java.time.LocalDate.class);
        assertPreAuthorize(SysAlertController.class, "deleteAlert", "@ss.hasPermi('monitor:alert:remove')", Long.class);
    }

    private void assertPreAuthorize(Class<?> clazz, String methodName, String expectedValue, Class<?>... parameterTypes)
            throws Exception {
        Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, clazz.getSimpleName() + "#" + methodName + " missing @PreAuthorize");
        assertEquals(expectedValue, annotation.value());
    }
}
