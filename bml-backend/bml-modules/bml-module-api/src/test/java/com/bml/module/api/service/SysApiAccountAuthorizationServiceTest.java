package com.bml.module.api.service;

import com.bml.core.common.exception.BusinessException;
import com.bml.module.api.dto.SaveApiAccountAuthorizationCommand;
import com.bml.module.api.entity.SysApiAccount;
import com.bml.module.api.entity.SysApiRegistry;
import com.bml.module.api.mapper.SysApiPermissionMapper;
import com.bml.module.api.vo.ApiAccountAuthorizationVO;
import com.bml.module.api.vo.ApiAuthorizationSummaryVO;
import com.bml.module.api.vo.OpenApiGroupVO;
import com.bml.module.api.vo.SysApiAccountVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysApiAccountAuthorizationServiceTest {

    private final SysApiAccountService accountService = mock(SysApiAccountService.class);
    private final SysApiPermissionMapper permissionMapper = mock(SysApiPermissionMapper.class);
    private final SysOpenApiRegistryService registryService = mock(SysOpenApiRegistryService.class);

    private final SysApiAccountAuthorizationService service =
            new SysApiAccountAuthorizationService(accountService, permissionMapper, registryService);

    @Test
    void shouldBuildAuthorizationSnapshot() {
        SysApiAccountVO accountVO = new SysApiAccountVO();
        accountVO.setId(1L);
        accountVO.setAccountName("客户A");

        OpenApiGroupVO groupVO = new OpenApiGroupVO();
        groupVO.setModuleName("api");

        when(accountService.getAccountInfo(1L)).thenReturn(accountVO);
        when(permissionMapper.selectApiIdsByAccountId(1L)).thenReturn(List.of(11L, 12L));
        when(registryService.countOpenApi(null)).thenReturn(20L);
        when(registryService.countOpenApi(1)).thenReturn(16L);
        when(registryService.listEnabledByIds(List.of(11L, 12L))).thenReturn(List.of(new SysApiRegistry()));
        when(registryService.listRegistryTree(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(groupVO));

        ApiAccountAuthorizationVO snapshot = service.getAuthorizationSnapshot(1L);

        assertEquals("客户A", snapshot.getAccount().getAccountName());
        assertEquals(List.of(11L, 12L), snapshot.getSelectedApiIds());
        assertEquals(1, snapshot.getGroups().size());
        ApiAuthorizationSummaryVO summary = snapshot.getSummary();
        assertEquals(20L, summary.getTotalApiCount());
        assertEquals(16L, summary.getEnabledApiCount());
        assertEquals(2L, summary.getSelectedApiCount());
        assertEquals(1L, summary.getSelectedEnabledApiCount());
    }

    @Test
    void shouldOverwriteAuthorizationRelations() {
        SaveApiAccountAuthorizationCommand command = new SaveApiAccountAuthorizationCommand();
        command.setApiIds(List.of(11L, 12L, 12L));

        when(accountService.getRequiredAccount(1L)).thenReturn(new SysApiAccount());
        when(registryService.listEnabledByIds(List.of(11L, 12L))).thenReturn(List.of(new SysApiRegistry(), new SysApiRegistry()));

        service.saveAuthorization(1L, command);

        verify(permissionMapper).deleteByAccountId(1L);
        verify(permissionMapper).batchInsert(1L, List.of(11L, 12L));
    }

    @Test
    void shouldRejectIllegalApiIdsWhenSavingAuthorization() {
        SaveApiAccountAuthorizationCommand command = new SaveApiAccountAuthorizationCommand();
        command.setApiIds(List.of(21L, 22L));

        when(accountService.getRequiredAccount(2L)).thenReturn(new SysApiAccount());
        when(registryService.listEnabledByIds(List.of(21L, 22L))).thenReturn(List.of(new SysApiRegistry()));

        assertThrows(BusinessException.class, () -> service.saveAuthorization(2L, command));
        verify(permissionMapper, never()).batchInsert(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void shouldAllowEmptyAuthorizationList() {
        SaveApiAccountAuthorizationCommand command = new SaveApiAccountAuthorizationCommand();
        command.setApiIds(List.of());

        when(accountService.getRequiredAccount(3L)).thenReturn(new SysApiAccount());

        service.saveAuthorization(3L, command);

        verify(permissionMapper).deleteByAccountId(3L);
        verify(permissionMapper, never()).batchInsert(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyList());
    }
}
