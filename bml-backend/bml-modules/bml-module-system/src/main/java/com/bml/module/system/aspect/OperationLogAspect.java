package com.bml.module.system.aspect;

import cn.hutool.core.util.StrUtil;
import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.common.result.Result;
import com.bml.core.framework.operlog.OperationLog;
import com.bml.core.framework.operlog.OperatorType;
import com.bml.core.framework.security.model.LoginUser;
import com.bml.core.framework.security.model.OpenApiLoginUser;
import com.bml.module.system.entity.SysDept;
import com.bml.module.system.entity.SysOperationLog;
import com.bml.module.system.service.SysDeptService;
import com.bml.module.system.service.SysOperationLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 系统操作日志切面。
 * <p>
 * 该切面统一拦截带有 {@link OperationLog} 注解的 Controller 方法，将关键审计信息写入 sys_operation_log。
 * 设计原则：日志记录失败绝不影响主业务执行，避免审计能力成为业务接口的单点风险。
 * </p>
 *
 * @author BML Team
 */
@Slf4j
@Aspect
@Order(10)
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int SUCCESS_STATUS = 0;

    private static final int ERROR_STATUS = 1;

    private static final int MAX_PARAM_LENGTH = 2000;

    private static final int MAX_RESULT_LENGTH = 2000;

    private static final int MAX_ERROR_LENGTH = 2000;

    private static final String UNKNOWN = "unknown";

    private static final Pattern JSON_SENSITIVE_FIELD_PATTERN = Pattern.compile(
            "(?i)(\\\"(?:password|oldPassword|newPassword|secret|appSecret|accessSecret|token|authorization)\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")");

    private static final Pattern TEXT_SENSITIVE_FIELD_PATTERN = Pattern.compile(
            "(?i)((?:password|oldPassword|newPassword|secret|appSecret|accessSecret|token|authorization)\\s*[=:]\\s*)[^,\\s}&]+");

    private final SysOperationLogService operationLogService;

    private final SysDeptService deptService;

    private final ObjectMapper objectMapper;

    /**
     * 环绕记录操作日志。
     *
     * @param joinPoint    切点
     * @param operationLog 日志注解
     * @return 原业务方法返回值
     * @throws Throwable 原业务异常
     */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable throwable = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            throwable = ex;
            throw ex;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            recordOperationLog(joinPoint, operationLog, result, throwable, costTime);
        }
    }

    private void recordOperationLog(ProceedingJoinPoint joinPoint, OperationLog annotation, Object result,
            Throwable throwable, long costTime) {
        try {
            HttpServletRequest request = getRequest();
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            SysOperationLog logRecord = new SysOperationLog();
            logRecord.setTitle(annotation.title());
            logRecord.setBusinessType(annotation.businessType().getCode());
            logRecord.setMethod(joinPoint.getTarget().getClass().getName() + "." + method.getName() + "()");
            logRecord.setRequestMethod(request == null ? null : request.getMethod());
            logRecord.setOperatorType(resolveOperatorType(annotation.operatorType()).getCode());
            applyCurrentUser(logRecord);
            logRecord.setOperUrl(request == null ? null : request.getRequestURI());
            logRecord.setOperIp(request == null ? null : getClientIp(request));
            logRecord.setOperTime(LocalDateTime.now());
            logRecord.setCostTime(costTime);
            if (annotation.saveRequestData()) {
                logRecord.setOperParam(truncate(serializeArguments(joinPoint), MAX_PARAM_LENGTH));
            }
            if (throwable == null) {
                applySuccessResult(logRecord, annotation, result);
            } else {
                logRecord.setStatus(ERROR_STATUS);
                logRecord.setErrorMsg(truncate(resolveErrorMessage(throwable), MAX_ERROR_LENGTH));
            }
            operationLogService.save(logRecord);
        } catch (Exception ex) {
            log.warn("系统操作日志记录失败，不影响主业务执行", ex);
        }
    }

    private void applySuccessResult(SysOperationLog logRecord, OperationLog annotation, Object result) {
        logRecord.setStatus(SUCCESS_STATUS);
        if (result instanceof Result<?> apiResult && apiResult.getCode() != GlobalErrorCode.SUCCESS.getCode()) {
            logRecord.setStatus(ERROR_STATUS);
            logRecord.setErrorMsg(truncate(apiResult.getMessage(), MAX_ERROR_LENGTH));
        }
        if (annotation.saveResponseData()) {
            logRecord.setJsonResult(truncate(serializeObject(result), MAX_RESULT_LENGTH));
        }
    }

    private void applyCurrentUser(SysOperationLog logRecord) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            logRecord.setOperName(loginUser.getUsername());
            if (loginUser.getDeptId() != null) {
                SysDept dept = deptService.getById(loginUser.getDeptId());
                if (dept != null) {
                    logRecord.setDeptName(dept.getDeptName());
                }
            }
        } else if (principal instanceof String principalName && StrUtil.isNotBlank(principalName)) {
            logRecord.setOperName(principalName);
        }
    }

    private OperatorType resolveOperatorType(OperatorType annotationOperatorType) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof OpenApiLoginUser) {
            return OperatorType.OPEN_API;
        }
        return annotationOperatorType == null ? OperatorType.OTHER : annotationOperatorType;
    }

    private String serializeArguments(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (shouldSkipArgument(arg)) {
                continue;
            }
            String name = parameterNames != null && i < parameterNames.length ? parameterNames[i] : "arg" + i;
            values.put(name, arg);
        }
        return serializeObject(values);
    }

    private boolean shouldSkipArgument(Object arg) {
        if (arg == null) {
            return false;
        }
        return arg instanceof ServletRequest
                || arg instanceof ServletResponse
                || arg instanceof MultipartFile
                || arg instanceof MultipartFile[]
                || arg instanceof BindingResult;
    }

    private String serializeObject(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return maskSensitiveText(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException ex) {
            return maskSensitiveText(String.valueOf(value));
        }
    }

    private String maskSensitiveText(String text) {
        if (text == null) {
            return null;
        }
        String maskedJson = JSON_SENSITIVE_FIELD_PATTERN.matcher(text).replaceAll("$1******$2");
        return TEXT_SENSITIVE_FIELD_PATTERN.matcher(maskedJson).replaceAll("$1******");
    }

    private HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        return Arrays.stream(new String[] {
                request.getHeader("X-Forwarded-For"),
                request.getHeader("X-Real-IP"),
                request.getHeader("Proxy-Client-IP"),
                request.getHeader("WL-Proxy-Client-IP"),
                request.getRemoteAddr()
        })
                .filter(StrUtil::isNotBlank)
                .filter(ip -> !UNKNOWN.equalsIgnoreCase(ip))
                .map(ip -> ip.split(",")[0].trim())
                .findFirst()
                .orElse(null);
    }

    private String resolveErrorMessage(Throwable throwable) {
        String message = StrUtil.blankToDefault(throwable.getMessage(), throwable.getClass().getSimpleName());
        return throwable.getClass().getName() + ": " + message;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
