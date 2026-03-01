package com.bml.module.api.service;

import com.bml.module.api.entity.SysApiAccount;
import com.bml.module.api.entity.SysApiCallbackLog;
import com.bml.module.api.mapper.SysApiCallbackLogMapper;
import com.bml.module.api.support.ApiCallbackStatusSupport;
import com.bml.module.api.vo.ApiCallbackLogVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysApiCallbackLogServiceTest {

    private final SysApiCallbackLogMapper callbackLogMapper = mock(SysApiCallbackLogMapper.class);
    private final SysApiAccountService accountService = mock(SysApiAccountService.class);
    private final ApiCallbackHttpClient callbackHttpClient = mock(ApiCallbackHttpClient.class);

    private SysApiCallbackLogService service;

    @BeforeEach
    void setUp() {
        service = new SysApiCallbackLogService(accountService, callbackHttpClient, new ObjectMapper());
        ReflectionTestUtils.setField(service, "baseMapper", callbackLogMapper);
        ReflectionTestUtils.setField(service, "retryEnabled", true);
        ReflectionTestUtils.setField(service, "maxRetryCount", 3);
        ReflectionTestUtils.setField(service, "retryIntervalSeconds", 60L);
        ReflectionTestUtils.setField(service, "retryBatchSize", 20);
    }

    @Test
    void shouldTriggerTestCallbackAndMarkSuccess() {
        SysApiAccount account = buildAccount();
        when(accountService.getRequiredAccount(1L)).thenReturn(account);
        doAnswer(invocation -> {
            SysApiCallbackLog log = invocation.getArgument(0);
            log.setId(101L);
            log.setCreateTime(LocalDateTime.now());
            return 1;
        }).when(callbackLogMapper).insert(any(SysApiCallbackLog.class));
        when(callbackLogMapper.updateById(any(SysApiCallbackLog.class))).thenReturn(1);
        when(callbackHttpClient.postJson(eq("https://enterprise.example.com/callback"), any(), any()))
                .thenReturn(new ApiCallbackHttpResponse(200, "{\"code\":0}"));

        ApiCallbackLogVO result = service.triggerTestCallback(1L);

        assertEquals(ApiCallbackStatusSupport.SUCCESS, result.getCallbackStatus());
        assertEquals(200, result.getResponseStatusCode());
        assertEquals("https://enterprise.example.com/callback", result.getCallbackUrl());
        assertEquals(0, result.getRetryCount());
        assertNotNull(result.getSuccessTime());
        assertNull(result.getNextRetryTime());

        ArgumentCaptor<SysApiCallbackLog> captor = ArgumentCaptor.forClass(SysApiCallbackLog.class);
        verify(callbackLogMapper).updateById(captor.capture());
        assertEquals(ApiCallbackStatusSupport.SUCCESS, captor.getValue().getCallbackStatus());
    }

    @Test
    void shouldScheduleRetryWhenCallbackFails() {
        SysApiAccount account = buildAccount();
        when(accountService.getRequiredAccount(1L)).thenReturn(account);
        doAnswer(invocation -> {
            SysApiCallbackLog log = invocation.getArgument(0);
            log.setId(102L);
            return 1;
        }).when(callbackLogMapper).insert(any(SysApiCallbackLog.class));
        when(callbackLogMapper.updateById(any(SysApiCallbackLog.class))).thenReturn(1);
        when(callbackHttpClient.postJson(eq("https://enterprise.example.com/callback"), any(), any()))
                .thenThrow(new RuntimeException("timeout"));

        ApiCallbackLogVO result = service.triggerTestCallback(1L);

        assertEquals(ApiCallbackStatusSupport.RETRYING, result.getCallbackStatus());
        assertEquals(1, result.getRetryCount());
        assertNotNull(result.getNextRetryTime());
        assertEquals("timeout", result.getLastErrorMessage());
        assertNull(result.getSuccessTime());
    }

    @Test
    void shouldRetryExistingRetryableLog() {
        SysApiCallbackLog callbackLog = buildRetryableLog();
        when(callbackLogMapper.selectById(201L)).thenReturn(callbackLog);
        when(callbackLogMapper.updateById(any(SysApiCallbackLog.class))).thenReturn(1);
        when(callbackHttpClient.postJson(eq("https://enterprise.example.com/callback"), any(), any()))
                .thenReturn(new ApiCallbackHttpResponse(200, "{\"code\":0}"));

        ApiCallbackLogVO result = service.retryNow(201L);

        assertEquals(ApiCallbackStatusSupport.SUCCESS, result.getCallbackStatus());
        assertEquals(1, result.getRetryCount());
        assertNotNull(result.getSuccessTime());
        assertNull(result.getNextRetryTime());
    }

    @Test
    void shouldProcessDueRetryLogs() {
        SysApiCallbackLog callbackLog = buildRetryableLog();
        callbackLog.setNextRetryTime(LocalDateTime.now().minusSeconds(5));

        when(callbackLogMapper.selectList(any())).thenReturn(List.of(callbackLog));
        when(callbackLogMapper.updateById(any(SysApiCallbackLog.class))).thenReturn(1);
        when(callbackHttpClient.postJson(eq("https://enterprise.example.com/callback"), any(), any()))
                .thenReturn(new ApiCallbackHttpResponse(200, "{\"code\":0}"));

        service.processRetryQueue();

        verify(callbackLogMapper).selectList(any());
        verify(callbackLogMapper).updateById(any(SysApiCallbackLog.class));
    }

    private SysApiAccount buildAccount() {
        SysApiAccount account = new SysApiAccount();
        account.setId(1L);
        account.setAccountName("partner-account");
        account.setSystemName("Enterprise Portal");
        account.setSystemCode("ENTERPRISE_PORTAL");
        account.setCallbackUrl("https://enterprise.example.com/callback");
        return account;
    }

    private SysApiCallbackLog buildRetryableLog() {
        SysApiCallbackLog callbackLog = new SysApiCallbackLog();
        callbackLog.setId(201L);
        callbackLog.setAccountId(1L);
        callbackLog.setAccountName("partner-account");
        callbackLog.setSystemName("Enterprise Portal");
        callbackLog.setSystemCode("ENTERPRISE_PORTAL");
        callbackLog.setBusinessType("account_test");
        callbackLog.setEventType("manual_test_callback");
        callbackLog.setCallbackUrl("https://enterprise.example.com/callback");
        callbackLog.setHttpMethod("POST");
        callbackLog.setRequestHeaders("{\"Content-Type\":\"application/json\"}");
        callbackLog.setRequestBody("{\"message\":\"test\"}");
        callbackLog.setCallbackStatus(ApiCallbackStatusSupport.RETRYING);
        callbackLog.setRetryCount(1);
        callbackLog.setMaxRetryCount(3);
        callbackLog.setNextRetryTime(LocalDateTime.now().minusSeconds(5));
        return callbackLog;
    }
}
