package com.bml.core.framework.exception;

import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.common.exception.BusinessException;
import com.bml.core.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常处理器
 * <p>
 * 统一捕获并处理 Controller 层抛出的各类异常，
 * 确保所有 API 响应都遵循 {@link Result} 统一格式。
 * </p>
 *
 * <h3>异常处理优先级（从高到低）：</h3>
 * <ol>
 * <li>{@link BusinessException} — 业务异常，已知的业务逻辑错误</li>
 * <li>{@link AccessDeniedException} — 权限不足（403）</li>
 * <li>{@link MethodArgumentNotValidException} — 参数校验失败（{@code @Valid}）</li>
 * <li>{@link BindException} — 参数绑定失败</li>
 * <li>{@link NoHandlerFoundException} — 404 资源不存在</li>
 * <li>{@link Exception} — 未知系统异常（兜底）</li>
 * </ol>
 *
 * @author BML Team
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常处理
     * <p>
     * 捕获所有 {@link BusinessException}，返回业务错误码和消息。
     * 这是最常用的异常类型，用于表示可预期的业务逻辑错误，
     * 如"用户名已存在"、"库存不足"等。
     * </p>
     *
     * @param e       业务异常
     * @param request HTTP 请求
     * @return 统一错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {} {}", request.getRequestURI(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 权限不足异常处理（403）
     * <p>
     * 当 {@code @PreAuthorize} 注解校验不通过时抛出。
     * 注意：此处理器作为兜底，主要场景由 {@code AccessDeniedHandlerImpl} 在
     * Security Filter 层处理。此处捕获的是业务代码中手动抛出或AOP中抛出的情况。
     * </p>
     *
     * @param e       权限不足异常
     * @param request HTTP 请求
     * @return 统一错误响应（403）
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("权限不足: {} {}", request.getRequestURI(), e.getMessage());
        return Result.fail(GlobalErrorCode.FORBIDDEN);
    }

    /**
     * 参数校验异常处理
     * <p>
     * 当使用 {@code @Valid} 或 {@code @Validated} 注解校验请求体参数失败时触发。
     * 返回第一个校验失败字段的错误消息。
     * </p>
     *
     * @param e 参数校验异常
     * @return 统一错误响应（400）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数校验失败";
        log.warn("参数校验异常: {}", message);
        return Result.fail(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 绑定异常处理
     * <p>
     * 当 Query 参数或 Form 表单绑定到对象失败时触发
     * （与 {@code @RequestBody} 的 {@link MethodArgumentNotValidException} 不同）。
     * </p>
     *
     * @param e 绑定异常
     * @return 统一错误响应（400）
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getAllErrors().get(0).getDefaultMessage();
        log.warn("绑定异常: {}", message);
        return Result.fail(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 404 异常处理
     * <p>
     * 当请求的 URL 无对应 Controller 映射时触发。
     * 需要在 {@code application.yml} 中配置：
     * 
     * <pre>
     * spring.mvc.throw-exception-if-no-handler-found: true
     * spring.web.resources.add-mappings: false
     * </pre>
     * </p>
     *
     * @param e 404 异常
     * @return 统一错误响应（404）
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("404异常: {}", e.getRequestURL());
        return Result.fail(GlobalErrorCode.NOT_FOUND);
    }

    /**
     * 系统异常兜底处理
     * <p>
     * 捕获所有未被上述处理器匹配的异常。
     * 这类异常通常是程序 Bug 或第三方服务异常，
     * 需要记录完整异常堆栈用于排查。
     * </p>
     *
     * @param e       系统异常
     * @param request HTTP 请求
     * @return 统一错误响应（500）
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: {}", request.getRequestURI(), e);
        return Result.fail(GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }
}
