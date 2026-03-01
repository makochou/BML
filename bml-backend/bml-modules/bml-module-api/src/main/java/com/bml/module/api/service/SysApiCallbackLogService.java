package com.bml.module.api.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bml.core.common.exception.BusinessException;
import com.bml.core.common.result.PageResult;
import com.bml.module.api.dto.ApiCallbackLogPageQuery;
import com.bml.module.api.entity.SysApiAccount;
import com.bml.module.api.entity.SysApiCallbackLog;
import com.bml.module.api.mapper.SysApiCallbackLogMapper;
import com.bml.module.api.support.ApiCallbackStatusSupport;
import com.bml.module.api.vo.ApiCallbackLogPageVO;
import com.bml.module.api.vo.ApiCallbackLogSummaryVO;
import com.bml.module.api.vo.ApiCallbackLogVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API 回调日志服务。
 * <p>
 * 该服务统一处理回调日志记录、测试回调、失败重试和管理端分页查询，避免业务模块各自实现一套
 * “发起回调 -> 记录日志 -> 失败补偿”的重复逻辑。后续真实业务事件接入时，只需要复用这里的
 * 日志与重试能力即可。
 * </p>
 */
@Slf4j
@Service
public class SysApiCallbackLogService extends ServiceImpl<SysApiCallbackLogMapper, SysApiCallbackLog> {

    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;
    private static final int MAX_RESPONSE_BODY_LENGTH = 5000;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final String DEFAULT_HTTP_METHOD = "POST";
    private static final String TEST_BUSINESS_TYPE = "account_test";
    private static final String TEST_EVENT_TYPE = "manual_test_callback";

    private final SysApiAccountService accountService;
    private final ApiCallbackHttpClient callbackHttpClient;
    private final ObjectMapper objectMapper;

    @Value("${bml.openapi.callback.retry-enabled:true}")
    private boolean retryEnabled;

    @Value("${bml.openapi.callback.max-retry-count:3}")
    private int maxRetryCount;

    @Value("${bml.openapi.callback.retry-interval-seconds:60}")
    private long retryIntervalSeconds;

    @Value("${bml.openapi.callback.batch-size:20}")
    private int retryBatchSize;

    public SysApiCallbackLogService(SysApiAccountService accountService,
            ApiCallbackHttpClient callbackHttpClient,
            ObjectMapper objectMapper) {
        this.accountService = accountService;
        this.callbackHttpClient = callbackHttpClient;
        this.objectMapper = objectMapper;
    }

    public ApiCallbackLogPageVO pageLogs(Long accountId, ApiCallbackLogPageQuery query) {
        accountService.getRequiredAccount(accountId);
        long pageNum = normalizePageNum(query == null ? null : query.getPageNum());
        long pageSize = normalizePageSize(query == null ? null : query.getPageSize());

        LambdaQueryWrapper<SysApiCallbackLog> wrapper = buildQueryWrapper(accountId,
                query == null ? null : query.getCallbackStatus());
        long total = this.count(wrapper);
        PageResult<ApiCallbackLogVO> pageResult;
        if (total <= 0) {
            pageResult = PageResult.empty(pageNum, pageSize);
        } else {
            long offset = (pageNum - 1L) * pageSize;
            List<ApiCallbackLogVO> records = this.list(buildQueryWrapper(accountId,
                    query == null ? null : query.getCallbackStatus())
                    .last("LIMIT " + offset + "," + pageSize))
                    .stream()
                    .map(this::toVo)
                    .toList();
            pageResult = PageResult.of(records, total, pageNum, pageSize);
        }
        return ApiCallbackLogPageVO.builder()
                .page(pageResult)
                .summary(buildSummary(accountId))
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiCallbackLogVO triggerTestCallback(Long accountId) {
        SysApiAccount account = accountService.getRequiredAccount(accountId);
        if (StrUtil.isBlank(account.getCallbackUrl())) {
            throw new BusinessException("当前 API 账号未配置业务回调地址");
        }
        SysApiCallbackLog callbackLog = buildTestCallbackLog(account);
        this.save(callbackLog);
        return executeCallback(callbackLog);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiCallbackLogVO retryNow(Long logId) {
        SysApiCallbackLog callbackLog = getRequiredLog(logId);
        if (!ApiCallbackStatusSupport.isRetryable(callbackLog.getCallbackStatus())) {
            throw new BusinessException("当前回调日志状态不支持重试");
        }
        return executeCallback(callbackLog);
    }

    /**
     * 定时扫描待重试日志。
     * <p>
     * 只有状态处于“重试中”且已达到下一次重试时间的记录会被再次投递，这样既能避免无限重试，
     * 也能保证失败回调在不阻塞主流程的前提下完成补偿。
     * </p>
     */
    @Scheduled(fixedDelayString = "${bml.openapi.callback.processor-fixed-delay-ms:30000}")
    public void processRetryQueue() {
        if (!retryEnabled) {
            return;
        }
        List<SysApiCallbackLog> dueLogs = this.list(new LambdaQueryWrapper<SysApiCallbackLog>()
                .eq(SysApiCallbackLog::getCallbackStatus, ApiCallbackStatusSupport.RETRYING)
                .isNotNull(SysApiCallbackLog::getNextRetryTime)
                .le(SysApiCallbackLog::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(SysApiCallbackLog::getNextRetryTime, SysApiCallbackLog::getId)
                .last("LIMIT " + Math.max(retryBatchSize, 1)));
        for (SysApiCallbackLog callbackLog : dueLogs) {
            try {
                executeCallback(callbackLog);
            } catch (Exception exception) {
                log.error("API 回调自动重试失败，logId={}", callbackLog.getId(), exception);
            }
        }
    }

    private LambdaQueryWrapper<SysApiCallbackLog> buildQueryWrapper(Long accountId, Integer callbackStatus) {
        return new LambdaQueryWrapper<SysApiCallbackLog>()
                .eq(SysApiCallbackLog::getAccountId, accountId)
                .eq(callbackStatus != null, SysApiCallbackLog::getCallbackStatus, callbackStatus)
                .orderByDesc(SysApiCallbackLog::getCreateTime, SysApiCallbackLog::getId);
    }

    private ApiCallbackLogSummaryVO buildSummary(Long accountId) {
        return ApiCallbackLogSummaryVO.builder()
                .totalCount(this.lambdaQuery().eq(SysApiCallbackLog::getAccountId, accountId).count())
                .pendingCount(this.lambdaQuery()
                        .eq(SysApiCallbackLog::getAccountId, accountId)
                        .eq(SysApiCallbackLog::getCallbackStatus, ApiCallbackStatusSupport.PENDING)
                        .count())
                .retryingCount(this.lambdaQuery()
                        .eq(SysApiCallbackLog::getAccountId, accountId)
                        .eq(SysApiCallbackLog::getCallbackStatus, ApiCallbackStatusSupport.RETRYING)
                        .count())
                .successCount(this.lambdaQuery()
                        .eq(SysApiCallbackLog::getAccountId, accountId)
                        .eq(SysApiCallbackLog::getCallbackStatus, ApiCallbackStatusSupport.SUCCESS)
                        .count())
                .failedCount(this.lambdaQuery()
                        .eq(SysApiCallbackLog::getAccountId, accountId)
                        .eq(SysApiCallbackLog::getCallbackStatus, ApiCallbackStatusSupport.FAILED)
                        .count())
                .build();
    }

    private SysApiCallbackLog buildTestCallbackLog(SysApiAccount account) {
        LocalDateTime now = LocalDateTime.now();
        SysApiCallbackLog callbackLog = new SysApiCallbackLog();
        callbackLog.setAccountId(account.getId());
        callbackLog.setAccountName(account.getAccountName());
        callbackLog.setSystemName(account.getSystemName());
        callbackLog.setSystemCode(account.getSystemCode());
        callbackLog.setBusinessType(TEST_BUSINESS_TYPE);
        callbackLog.setEventType(TEST_EVENT_TYPE);
        callbackLog.setCallbackUrl(account.getCallbackUrl());
        callbackLog.setHttpMethod(DEFAULT_HTTP_METHOD);
        callbackLog.setRequestHeaders(serializeJson(buildDefaultHeaders()));
        callbackLog.setRequestBody(buildTestPayload(account, now));
        callbackLog.setCallbackStatus(ApiCallbackStatusSupport.PENDING);
        callbackLog.setRetryCount(0);
        callbackLog.setMaxRetryCount(Math.max(maxRetryCount, 0));
        callbackLog.setNextRetryTime(now);
        callbackLog.setLastErrorMessage(null);
        return callbackLog;
    }

    private Map<String, String> buildDefaultHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Bml-Callback-Mode", "test");
        headers.put("X-Bml-Callback-Source", "bml-admin");
        return headers;
    }

    private String buildTestPayload(SysApiAccount account, LocalDateTime triggerTime) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", TEST_EVENT_TYPE);
        payload.put("businessType", TEST_BUSINESS_TYPE);
        payload.put("triggerTime", triggerTime.toString());
        payload.put("accountId", account.getId());
        payload.put("accountName", account.getAccountName());
        payload.put("systemName", account.getSystemName());
        payload.put("systemCode", account.getSystemCode());
        payload.put("message", "这是一条由 BML 中台发起的测试回调，请用于验证地址可达性、签收逻辑和回调日志留痕。");
        return serializeJson(payload);
    }

    private ApiCallbackLogVO executeCallback(SysApiCallbackLog callbackLog) {
        LocalDateTime now = LocalDateTime.now();
        try {
            ApiCallbackHttpResponse response = callbackHttpClient.postJson(
                    callbackLog.getCallbackUrl(),
                    deserializeHeaders(callbackLog.getRequestHeaders()),
                    callbackLog.getRequestBody());
            callbackLog.setLastCallbackTime(now);
            callbackLog.setResponseStatusCode(response.statusCode());
            callbackLog.setResponseBody(truncateText(response.responseBody(), MAX_RESPONSE_BODY_LENGTH));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                callbackLog.setCallbackStatus(ApiCallbackStatusSupport.SUCCESS);
                callbackLog.setSuccessTime(now);
                callbackLog.setNextRetryTime(null);
                callbackLog.setLastErrorMessage(null);
            } else {
                applyFailure(callbackLog, now, "回调响应非成功状态: HTTP " + response.statusCode());
            }
        } catch (Exception exception) {
            callbackLog.setResponseBody(null);
            callbackLog.setResponseStatusCode(null);
            applyFailure(callbackLog, now, resolveErrorMessage(exception));
        }
        this.updateById(callbackLog);
        return toVo(callbackLog);
    }

    private void applyFailure(SysApiCallbackLog callbackLog, LocalDateTime now, String errorMessage) {
        int nextRetryCount = (callbackLog.getRetryCount() == null ? 0 : callbackLog.getRetryCount()) + 1;
        int currentMaxRetryCount = callbackLog.getMaxRetryCount() == null ? Math.max(maxRetryCount, 0)
                : callbackLog.getMaxRetryCount();
        callbackLog.setRetryCount(nextRetryCount);
        callbackLog.setLastCallbackTime(now);
        callbackLog.setSuccessTime(null);
        callbackLog.setLastErrorMessage(truncateText(errorMessage, MAX_ERROR_MESSAGE_LENGTH));

        if (retryEnabled && nextRetryCount < currentMaxRetryCount) {
            callbackLog.setCallbackStatus(ApiCallbackStatusSupport.RETRYING);
            callbackLog.setNextRetryTime(now.plusSeconds(Math.max(retryIntervalSeconds, 1L) * nextRetryCount));
        } else {
            callbackLog.setCallbackStatus(ApiCallbackStatusSupport.FAILED);
            callbackLog.setNextRetryTime(null);
        }
    }

    private SysApiCallbackLog getRequiredLog(Long logId) {
        if (logId == null) {
            throw new BusinessException("回调日志 ID 不能为空");
        }
        SysApiCallbackLog callbackLog = this.getById(logId);
        if (callbackLog == null) {
            throw new BusinessException("回调日志不存在");
        }
        return callbackLog;
    }

    private Map<String, String> deserializeHeaders(String requestHeaders) {
        if (StrUtil.isBlank(requestHeaders)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(requestHeaders,
                    objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, String.class));
        } catch (JsonProcessingException exception) {
            throw new BusinessException("回调请求头日志格式不正确");
        }
    }

    private String serializeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("序列化回调内容失败");
        }
    }

    private ApiCallbackLogVO toVo(SysApiCallbackLog callbackLog) {
        ApiCallbackLogVO vo = new ApiCallbackLogVO();
        vo.setId(callbackLog.getId());
        vo.setAccountId(callbackLog.getAccountId());
        vo.setAccountName(callbackLog.getAccountName());
        vo.setSystemName(callbackLog.getSystemName());
        vo.setSystemCode(callbackLog.getSystemCode());
        vo.setBusinessType(callbackLog.getBusinessType());
        vo.setEventType(callbackLog.getEventType());
        vo.setCallbackUrl(callbackLog.getCallbackUrl());
        vo.setHttpMethod(callbackLog.getHttpMethod());
        vo.setRequestHeaders(callbackLog.getRequestHeaders());
        vo.setRequestBody(callbackLog.getRequestBody());
        vo.setResponseStatusCode(callbackLog.getResponseStatusCode());
        vo.setResponseBody(callbackLog.getResponseBody());
        vo.setCallbackStatus(callbackLog.getCallbackStatus());
        vo.setRetryCount(callbackLog.getRetryCount());
        vo.setMaxRetryCount(callbackLog.getMaxRetryCount());
        vo.setNextRetryTime(callbackLog.getNextRetryTime());
        vo.setLastCallbackTime(callbackLog.getLastCallbackTime());
        vo.setSuccessTime(callbackLog.getSuccessTime());
        vo.setLastErrorMessage(callbackLog.getLastErrorMessage());
        vo.setCreateTime(callbackLog.getCreateTime());
        vo.setUpdateTime(callbackLog.getUpdateTime());
        return vo;
    }

    private String truncateText(String text, int maxLength) {
        String normalized = StrUtil.trimToNull(text);
        if (normalized == null || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String resolveErrorMessage(Exception exception) {
        String message = StrUtil.blankToDefault(exception.getMessage(), exception.getClass().getSimpleName());
        return truncateText(message, MAX_ERROR_MESSAGE_LENGTH);
    }

    private long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum.longValue();
    }

    private long normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize.longValue(), MAX_PAGE_SIZE);
    }
}
