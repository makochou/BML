package com.bml.module.api.service;

import com.bml.core.common.exception.BusinessException;
import com.bml.module.api.dto.CreateSysApiAccountCommand;
import com.bml.module.api.dto.UpdateSysApiAccountCommand;
import com.bml.module.api.entity.SysApiAccount;
import com.bml.module.api.mapper.SysApiAccountMapper;
import com.bml.module.api.mapper.SysApiPermissionMapper;
import com.bml.module.api.vo.ApiCredentialVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysApiAccountServiceTest {

    private final SysApiAccountMapper accountMapper = mock(SysApiAccountMapper.class);
    private final SysApiPermissionMapper permissionMapper = mock(SysApiPermissionMapper.class);
    private final ApiSecretCryptoService cryptoService = mock(ApiSecretCryptoService.class);

    private SysApiAccountService service;

    @BeforeEach
    void setUp() {
        service = new SysApiAccountService(cryptoService, permissionMapper);
        ReflectionTestUtils.setField(service, "baseMapper", accountMapper);
    }

    @Test
    void shouldCreateAccountAndPersistEnvironmentWhitelistFields() {
        CreateSysApiAccountCommand command = buildCreateCommand();

        when(accountMapper.selectCount(any())).thenReturn(0L);
        when(cryptoService.encrypt(any())).thenReturn("cipher-secret");
        doAnswer(invocation -> {
            SysApiAccount entity = invocation.getArgument(0);
            entity.setId(1001L);
            return 1;
        }).when(accountMapper).insert(any(SysApiAccount.class));

        ApiCredentialVO credential = service.createAccount(command);

        assertEquals(1001L, credential.getId());
        assertEquals("Partner-Huadong", credential.getAccountName());
        assertEquals("Enterprise Portal", credential.getSystemName());
        assertEquals("ENTERPRISE_PORTAL", credential.getSystemCode());
        assertEquals("Alice", credential.getOwnerName());
        assertEquals("13800000000", credential.getOwnerContact());
        assertEquals("production", credential.getAccessEnvironment());
        assertEquals(List.of("203.0.113.10", "10.0.0.0/24"), credential.getIpWhitelist());
        assertEquals(List.of("192.168.10.10"), credential.getEnvironmentIpWhitelist().get("test"));
        assertEquals(List.of("172.16.0.0/24"), credential.getEnvironmentIpWhitelist().get("staging"));
        assertEquals(List.of("203.0.113.10", "10.0.0.0/24"), credential.getEnvironmentIpWhitelist().get("production"));
        assertEquals("v1", credential.getSignVersion());
        assertEquals("https://enterprise.example.com/open-api/callback", credential.getCallbackUrl());
        assertFalse(credential.getAccessKey().isBlank());
        assertFalse(credential.getSecretKey().isBlank());
        verify(cryptoService).encrypt(credential.getSecretKey());

        ArgumentCaptor<SysApiAccount> captor = ArgumentCaptor.forClass(SysApiAccount.class);
        verify(accountMapper).insert(captor.capture());
        SysApiAccount persisted = captor.getValue();
        assertEquals("web,app", persisted.getClientTypes());
        assertEquals("ENTERPRISE_PORTAL", persisted.getSystemCode());
        assertEquals("production", persisted.getAccessEnvironment());
        assertEquals("203.0.113.10,10.0.0.0/24", persisted.getIpWhitelist());
        assertEquals("192.168.10.10", persisted.getTestIpWhitelist());
        assertEquals("172.16.0.0/24", persisted.getStagingIpWhitelist());
        assertEquals("203.0.113.10,10.0.0.0/24", persisted.getProductionIpWhitelist());
    }

    @Test
    void shouldRejectDuplicateAccountNameWhenCreating() {
        CreateSysApiAccountCommand command = buildCreateCommand();
        command.setAccountName("duplicated-account");

        when(accountMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.createAccount(command));
        verify(accountMapper, never()).insert(any(SysApiAccount.class));
    }

    @Test
    void shouldRejectUpdateWhenAccountNotFound() {
        UpdateSysApiAccountCommand command = new UpdateSysApiAccountCommand();
        command.setAccountName("missing-account");
        command.setOwnerName("Alice");
        command.setOwnerContact("13800000000");
        command.setSystemName("Enterprise Portal");
        command.setSystemCode("ENTERPRISE_PORTAL");
        command.setAccessEnvironment("production");
        command.setSignVersion("v1");

        when(accountMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.updateAccount(999L, command));
    }

    @Test
    void shouldResetSecretAndKeepEffectiveEnvironmentWhitelist() {
        SysApiAccount account = new SysApiAccount();
        account.setId(2001L);
        account.setAccountName("partner-account");
        account.setAccessKey("ak_2001");
        account.setSecretKey("old-cipher");
        account.setSystemName("Enterprise Portal");
        account.setSystemCode("ENTERPRISE_PORTAL");
        account.setOwnerName("Alice");
        account.setOwnerContact("13800000000");
        account.setAccessEnvironment("staging");
        account.setIpWhitelist("198.51.100.10");
        account.setTestIpWhitelist("192.168.1.10");
        account.setStagingIpWhitelist("172.16.0.0/24");
        account.setProductionIpWhitelist("203.0.113.10");
        account.setSignVersion("v1");
        account.setCallbackUrl("https://enterprise.example.com/open-api/callback");

        when(accountMapper.selectById(2001L)).thenReturn(account);
        when(cryptoService.encrypt(any())).thenReturn("new-cipher");
        when(accountMapper.updateById(any(SysApiAccount.class))).thenReturn(1);

        ApiCredentialVO credential = service.resetSecret(2001L);

        assertEquals("ak_2001", credential.getAccessKey());
        assertEquals("ENTERPRISE_PORTAL", credential.getSystemCode());
        assertEquals("staging", credential.getAccessEnvironment());
        assertEquals(List.of("172.16.0.0/24"), credential.getIpWhitelist());
        assertEquals(List.of("192.168.1.10"), credential.getEnvironmentIpWhitelist().get("test"));
        assertEquals(List.of("172.16.0.0/24"), credential.getEnvironmentIpWhitelist().get("staging"));
        assertEquals(List.of("203.0.113.10"), credential.getEnvironmentIpWhitelist().get("production"));
        assertFalse(credential.getSecretKey().isBlank());

        ArgumentCaptor<SysApiAccount> captor = ArgumentCaptor.forClass(SysApiAccount.class);
        verify(accountMapper).updateById(captor.capture());
        assertEquals("new-cipher", captor.getValue().getSecretKey());
    }

    @Test
    void shouldUpdateAccountStatus() {
        SysApiAccount account = new SysApiAccount();
        account.setId(3001L);
        account.setStatus(1);

        when(accountMapper.selectById(3001L)).thenReturn(account);
        when(accountMapper.updateById(any(SysApiAccount.class))).thenReturn(1);

        service.updateAccountStatus(3001L, 0);

        ArgumentCaptor<SysApiAccount> captor = ArgumentCaptor.forClass(SysApiAccount.class);
        verify(accountMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
    }

    private CreateSysApiAccountCommand buildCreateCommand() {
        CreateSysApiAccountCommand command = new CreateSysApiAccountCommand();
        command.setAccountName("Partner-Huadong");
        command.setAccountType(2);
        command.setClientTypes(List.of("web", "app"));
        command.setOwnerName("Alice");
        command.setOwnerContact("13800000000");
        command.setSystemName("Enterprise Portal");
        command.setSystemCode("enterprise_portal");
        command.setAccessEnvironment("production");
        command.setEnvironmentIpWhitelist(Map.of(
                "test", List.of("192.168.10.10"),
                "staging", List.of("172.16.0.0/24"),
                "production", List.of("203.0.113.10", "10.0.0.0/24")));
        command.setSignVersion("v1");
        command.setCallbackUrl("https://enterprise.example.com/open-api/callback");
        command.setRateLimit(3000);
        command.setStatus(1);
        command.setRemark("external partner");
        return command;
    }
}
