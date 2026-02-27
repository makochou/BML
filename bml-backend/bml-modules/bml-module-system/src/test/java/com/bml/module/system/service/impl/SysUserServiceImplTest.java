package com.bml.module.system.service.impl;

import com.bml.module.system.dto.SysUserDTO;
import com.bml.module.system.entity.SysUser;
import com.bml.module.system.mapper.SysUserMapper;
import com.bml.module.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysUserServiceImplTest {

    @Test
    void shouldEncryptPasswordWhenUpdatingUser() {
        AtomicReference<SysUser> updatedUserRef = new AtomicReference<>();

        SysUserMapper userMapper = (SysUserMapper) Proxy.newProxyInstance(
                SysUserMapper.class.getClassLoader(),
                new Class[]{SysUserMapper.class},
                (proxy, method, args) -> {
                    if ("updateById".equals(method.getName())) {
                        updatedUserRef.set((SysUser) args[0]);
                        return 1;
                    }
                    return defaultValue(method.getReturnType());
                });

        SysUserRoleMapper userRoleMapper = (SysUserRoleMapper) Proxy.newProxyInstance(
                SysUserRoleMapper.class.getClassLoader(),
                new Class[]{SysUserRoleMapper.class},
                (proxy, method, args) -> {
                    if ("delete".equals(method.getName()) || "insert".equals(method.getName())) {
                        return 1;
                    }
                    return defaultValue(method.getReturnType());
                });

        SysUserServiceImpl service = new SysUserServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
        ReflectionTestUtils.setField(service, "userRoleMapper", userRoleMapper);

        SysUserDTO dto = new SysUserDTO();
        dto.setId(2L);
        dto.setPassword("PlainPassword123!");
        dto.setRoleIds(List.of(1L, 2L));

        service.updateUser(dto);

        SysUser updatedUser = updatedUserRef.get();
        assertNotEquals("PlainPassword123!", updatedUser.getPassword());
        assertTrue(updatedUser.getPassword().startsWith("$2"));
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Double.TYPE) {
            return 0D;
        }
        if (returnType == Float.TYPE) {
            return 0F;
        }
        if (returnType == Short.TYPE) {
            return (short) 0;
        }
        if (returnType == Byte.TYPE) {
            return (byte) 0;
        }
        if (returnType == Character.TYPE) {
            return (char) 0;
        }
        return null;
    }
}
