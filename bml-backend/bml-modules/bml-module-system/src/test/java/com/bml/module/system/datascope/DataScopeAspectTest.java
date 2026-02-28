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
        mockLoginUser(1L, 10L);
        String sql = ReflectionTestUtils.invokeMethod(dataScopeAspect, "buildDataScopeSql", dataScope);
        assertEquals("", sql);
    }

    @Test
    void shouldReturnDenyAllSqlWhenNoRole() {
        mockLoginUser(2L, 10L);
        when(roleService.selectRolesByUserId(2L)).thenReturn(Collections.emptyList());
        String sql = ReflectionTestUtils.invokeMethod(dataScopeAspect, "buildDataScopeSql", dataScope);
        assertEquals("1 = 0", sql);
    }

    @Test
    void shouldBuildDeptScopeSql() {
        mockLoginUser(2L, 100L);

        SysRole role = new SysRole();
        role.setId(11L);
        role.setDataScope(5);
        when(roleService.selectRolesByUserId(2L)).thenReturn(List.of(role));

        SysUser user = new SysUser();
        user.setId(2L);
        user.setOrgId(9L);
        when(userService.getById(2L)).thenReturn(user);

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
