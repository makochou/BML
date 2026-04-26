package com.bml.module.system.datascope;

import com.bml.core.framework.security.model.LoginUser;
import com.bml.module.system.entity.SysRole;
import com.bml.module.system.entity.SysUser;
import com.bml.module.system.service.SysRoleService;
import com.bml.module.system.service.SysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class DataScopeAspectTest {

    private SysRoleService roleService;
    private SysUserService userService;
    private DataScopeAspect dataScopeAspect;
    private DataScope dataScope;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        roleService = Mockito.mock(SysRoleService.class);
        userService = Mockito.mock(SysUserService.class);
        dataScopeAspect = new DataScopeAspect(roleService, userService);
        Method method = DummyService.class.getDeclaredMethod("query");
        dataScope = method.getAnnotation(DataScope.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnEmptySqlForSystemAdmin() {
        // 超级管理员判断现在基于两种方式：
        // 1. userId == SYSTEM_USER_ID（当前为 2L，即 bml 用户）
        // 2. 权限集合中包含 *:*:* 通配符
        // 此处使用 SYSTEM_USER_ID=2L 验证方式1
        mockLoginUser(2L, 10L);
        String sql = ReflectionTestUtils.invokeMethod(dataScopeAspect, "buildDataScopeSql", dataScope);
        assertEquals("", sql);
    }

    @Test
    void shouldReturnEmptySqlForSuperAdminByPermission() {
        // 验证方式2：通过 *:*:* 权限标识识别超级管理员（不依赖固定 userId）
        LoginUser loginUser = new LoginUser(999L, 10L, "tester", "pwd", 1, Set.of("*:*:*"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
        String sql = ReflectionTestUtils.invokeMethod(dataScopeAspect, "buildDataScopeSql", dataScope);
        assertEquals("", sql);
    }

    @Test
    void shouldReturnDenyAllSqlWhenNoRole() {
        // 使用一个既不是 SYSTEM_USER_ID 也没有 *:*:* 权限的普通用户
        // 且没有任何角色，期望返回拒绝所有的 SQL
        mockLoginUser(999L, 10L);
        when(roleService.selectRolesByUserId(999L)).thenReturn(Collections.emptyList());
        String sql = ReflectionTestUtils.invokeMethod(dataScopeAspect, "buildDataScopeSql", dataScope);
        assertEquals("1 = 0", sql);
    }

    @Test
    void shouldBuildDeptScopeSql() {
        mockLoginUser(999L, 100L);

        SysRole role = new SysRole();
        role.setId(11L);
        role.setDataScope(5);
        when(roleService.selectRolesByUserId(999L)).thenReturn(List.of(role));

        SysUser user = new SysUser();
        user.setId(999L);
        user.setOrgId(9L);
        when(userService.getById(999L)).thenReturn(user);

        String sql = ReflectionTestUtils.invokeMethod(dataScopeAspect, "buildDataScopeSql", dataScope);
        assertEquals("(dept_id = 100)", sql);
    }

    private void mockLoginUser(Long userId, Long deptId) {
        LoginUser loginUser = new LoginUser(userId, deptId, "tester", "pwd", 1, Set.of("system:user:list"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }

    private static class DummyService {

        @DataScope(deptColumn = "dept_id", orgColumn = "org_id", userColumn = "id", creatorColumn = "create_by")
        public void query() {
            // test only
        }
    }
}
