package com.bml.module.system.service.impl;

import com.bml.core.framework.license.BmlLicenseHolder;
import com.bml.module.system.entity.SysMenu;
import com.bml.module.system.vo.RouterVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SysMenuServiceImplRouteBuildTest {

    private final SysMenuServiceImpl menuService = new SysMenuServiceImpl();

    @BeforeEach
    void setUp() throws Exception {
        // 注入 mock 的 licenseHolder（字段注入，需反射设置）
        BmlLicenseHolder mockHolder = mock(BmlLicenseHolder.class);
        when(mockHolder.isEnabled()).thenReturn(false);
        Field field = SysMenuServiceImpl.class.getDeclaredField("licenseHolder");
        field.setAccessible(true);
        field.set(menuService, mockHolder);
    }

    @Test
    void shouldBuildDirectoryAndChildRoute() {
        SysMenu root = new SysMenu();
        root.setId(1L);
        root.setMenuName("系统管理");
        root.setMenuType("M");
        root.setPath("/system");
        root.setIcon("setting");

        SysMenu child = new SysMenu();
        child.setId(100L);
        child.setMenuName("用户管理");
        child.setMenuType("C");
        child.setPath("user");
        child.setComponent("system/user/index");
        child.setIcon("user");
        root.setChildren(List.of(child));

        List<RouterVO> routes = menuService.buildMenus(List.of(root));
        assertEquals(1, routes.size());
        assertEquals("/system", routes.get(0).getPath());
        assertEquals("Layout", routes.get(0).getComponent());
        assertEquals(1, routes.get(0).getChildren().size());
        assertEquals("user", routes.get(0).getChildren().get(0).getPath());
        assertEquals("system/user/index", routes.get(0).getChildren().get(0).getComponent());
    }

    @Test
    void shouldUsePresetRouteNameForKnownPages() {
        SysMenu dashboard = new SysMenu();
        dashboard.setId(230L);
        dashboard.setMenuName("工作台");
        dashboard.setMenuType("C");
        dashboard.setPath("dashboard");
        dashboard.setComponent("dashboard/Workplace");
        dashboard.setIcon("dashboard");

        List<RouterVO> routes = menuService.buildMenus(List.of(dashboard));
        assertEquals(1, routes.size());
        assertEquals("Dashboard", routes.get(0).getName());
        assertFalse(routes.get(0).getHidden());
    }
}
