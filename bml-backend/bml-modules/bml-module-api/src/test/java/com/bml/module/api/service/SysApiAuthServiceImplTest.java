package com.bml.module.api.service;

import com.bml.core.framework.service.model.OpenApiAppAuth;
import com.bml.module.api.entity.SysApiAccount;
import com.bml.module.api.entity.SysApiRegistry;
import com.bml.module.api.mapper.SysApiAccountMapper;
import com.bml.module.api.mapper.SysApiPermissionMapper;
import com.bml.module.api.mapper.SysApiRegistryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SysApiAuthServiceImplTest {

    private final SysApiAccountMapper accountMapper = mock(SysApiAccountMapper.class);
    private final SysApiRegistryMapper registryMapper = mock(SysApiRegistryMapper.class);
    private final SysApiPermissionMapper permissionMapper = mock(SysApiPermissionMapper.class);
    private final ApiSecretCryptoService secretCryptoService = mock(ApiSecretCryptoService.class);

    private SysApiAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SysApiAuthServiceImpl();
        ReflectionTestUtils.setField(service, "apiAccountMapper", accountMapper);
        ReflectionTestUtils.setField(service, "apiRegistryMapper", registryMapper);
        ReflectionTestUtils.setField(service, "apiPermissionMapper", permissionMapper);
        ReflectionTestUtils.setField(service, "secretCryptoService", secretCryptoService);
    }

    @Test
    void shouldReturnNullWhenAccountNotFound() {
        when(accountMapper.selectOne(any())).thenReturn(null);

        OpenApiAppAuth auth = service.getAppAuth("missing-ak");

        assertNull(auth);
    }

    @Test
    void shouldReturnNullForExpiredAccount() {
        SysApiAccount account = new SysApiAccount();
        account.setId(1L);
        account.setStatus(1);
        account.setExpireTime(LocalDateTime.now().minusMinutes(5));

        when(accountMapper.selectOne(any())).thenReturn(account);

        OpenApiAppAuth auth = service.getAppAuth("expired-ak");

        assertNull(auth);
    }

    @Test
    void shouldResolveEffectiveWhitelistFromCurrentEnvironment() {
        SysApiAccount account = new SysApiAccount();
        account.setId(10L);
        account.setStatus(1);
        account.setSecretKey("cipher-secret");
        account.setAccessEnvironment("production");
        account.setIpWhitelist("198.51.100.10");
        account.setTestIpWhitelist("192.168.1.10");
        account.setStagingIpWhitelist("172.16.0.0/24");
        account.setProductionIpWhitelist("203.0.113.10,10.0.0.0/24");
        account.setSignVersion(null);

        when(accountMapper.selectOne(any())).thenReturn(account);
        when(secretCryptoService.decrypt("cipher-secret")).thenReturn("plain-secret");

        OpenApiAppAuth auth = service.getAppAuth("ak-test");

        assertNotNull(auth);
        assertEquals(10L, auth.getAccountId());
        assertEquals("plain-secret", auth.getSecretKey());
        assertEquals("v1", auth.getSignVersion());
        assertEquals("203.0.113.10,10.0.0.0/24", auth.getIpWhitelist());
    }

    @Test
    void shouldReturnFalseWhenRegistryMissing() {
        when(registryMapper.selectOne(any())).thenReturn(null);

        boolean authorized = service.isApiAuthorized(100L, "/system/user/list", "GET");

        assertFalse(authorized);
    }

    @Test
    void shouldReturnTrueWhenRegistryAuthorized() {
        SysApiRegistry registry = new SysApiRegistry();
        registry.setId(88L);

        when(registryMapper.selectOne(any())).thenReturn(registry);
        when(permissionMapper.countPermission(100L, 88L)).thenReturn(1L);

        boolean authorized = service.isApiAuthorized(100L, "/system/user/list", "GET");

        assertTrue(authorized);
    }
}
