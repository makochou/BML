package com.bml.module.api.service;

import com.bml.module.api.entity.SysApiAccount;
import com.bml.module.api.entity.SysApiRegistry;
import com.bml.module.api.mapper.SysApiAccountMapper;
import com.bml.module.api.mapper.SysApiPermissionMapper;
import com.bml.module.api.mapper.SysApiRegistryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SysApiAuthServiceImplTest {

    @Test
    void shouldReturnNullForExpiredAccount() {
        SysApiAccountMapper accountMapper = mock(SysApiAccountMapper.class);
        SysApiRegistryMapper registryMapper = mock(SysApiRegistryMapper.class);
        SysApiPermissionMapper permissionMapper = mock(SysApiPermissionMapper.class);
        ApiSecretCryptoService cryptoService = mock(ApiSecretCryptoService.class);

        SysApiAccount account = new SysApiAccount();
        account.setId(1L);
        account.setAccessKey("ak-test");
        account.setSecretKey("cipher");
        account.setStatus(1);
        account.setExpireTime(LocalDateTime.now().minusMinutes(1));
        when(accountMapper.selectOne(any())).thenReturn(account);

        SysApiAuthServiceImpl service = buildService(accountMapper, registryMapper, permissionMapper, cryptoService);

        assertNull(service.getAppAuth("ak-test"));
    }

    @Test
    void shouldValidateAuthorizedApi() {
        SysApiAccountMapper accountMapper = mock(SysApiAccountMapper.class);
        SysApiRegistryMapper registryMapper = mock(SysApiRegistryMapper.class);
        SysApiPermissionMapper permissionMapper = mock(SysApiPermissionMapper.class);
        ApiSecretCryptoService cryptoService = mock(ApiSecretCryptoService.class);

        SysApiAccount account = new SysApiAccount();
        account.setId(1L);
        account.setAccessKey("ak-test");
        account.setSecretKey("cipher");
        account.setStatus(1);
        account.setExpireTime(LocalDateTime.now().plusMinutes(10));
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(cryptoService.decrypt("cipher")).thenReturn("plain-secret");

        SysApiRegistry registry = new SysApiRegistry();
        registry.setId(9L);
        when(registryMapper.selectOne(any())).thenReturn(registry);
        when(permissionMapper.countPermission(1L, 9L)).thenReturn(1L);

        SysApiAuthServiceImpl service = buildService(accountMapper, registryMapper, permissionMapper, cryptoService);

        assertEquals("plain-secret", service.getAppAuth("ak-test").getSecretKey());
        assertTrue(service.isApiAuthorized(1L, "/open/api/demo", "GET"));
        when(permissionMapper.countPermission(1L, 9L)).thenReturn(0L);
        assertFalse(service.isApiAuthorized(1L, "/open/api/demo", "GET"));
    }

    private SysApiAuthServiceImpl buildService(SysApiAccountMapper accountMapper,
            SysApiRegistryMapper registryMapper,
            SysApiPermissionMapper permissionMapper,
            ApiSecretCryptoService cryptoService) {
        SysApiAuthServiceImpl service = new SysApiAuthServiceImpl();
        ReflectionTestUtils.setField(service, "apiAccountMapper", accountMapper);
        ReflectionTestUtils.setField(service, "apiRegistryMapper", registryMapper);
        ReflectionTestUtils.setField(service, "apiPermissionMapper", permissionMapper);
        ReflectionTestUtils.setField(service, "secretCryptoService", cryptoService);
        return service;
    }
}
