package com.bml.core.framework.exception;

import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.common.exception.BusinessException;
import com.bml.core.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器。
 * <p>
 * 所有进入 Controller 层的异常，统一在这里收敛为标准 {@link Result} 结构，
 * 从而保证业务接口、异常接口和安全接口对前端暴露一致的响应协议。
 * </p>
 *
 * @author BML Team
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常。
     * <p>
     * 当异常携带附加载荷（{@link BusinessException#getData()}）时，
     * 将其作为 {@code Result.data} 一并下发，便于前端按字段定位非法输入
     * （例如主题模块的 {@code List<FieldError>} 全量校验场景）。
     * </p>
     *
     * @param exception 业务异常
     * @param request   当前请求
     * @return 统一失败响应
     */
    @ExceptionHandler(BusinessException.class)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Result<Void> handleBusinessException(BusinessException exception, HttpServletRequest request) {
        log.warn("业务异常: {} {}", request.getRequestURI(), exception.getMessage());
        if (exception.getData() != null) {
            // 通过原始类型回退实现把附加载荷塞入泛型 Result<Void>，前端按 data 字段消费即可
            Result raw = Result.fail(exception.getCode(), exception.getMessage(), exception.getData());
            return raw;
        }
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理权限不足异常。
     *
     * @param exception 权限异常
     * @param request   当前请求
     * @return 统一失败响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(AccessDeniedException exception, HttpServletRequest request) {
        log.warn("权限不足: {} {}", request.getRequestURI(), exception.getMessage());
        return Result.fail(GlobalErrorCode.FORBIDDEN);
    }

    /**
     * 处理请求体参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldError() != null
                ? exception.getBindingResult().getFieldError().getDefaultMessage()
                : "请求参数校验失败";
        log.warn("请求体参数校验失败: {}", message);
        return Result.badRequest(message);
    }

    /**
     * 处理表单与查询参数绑定异常。
     *
     * @param exception 参数绑定异常
     * @return 统一失败响应
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException exception) {
        String message = exception.getAllErrors().isEmpty()
                ? "请求参数绑定失败"
                : exception.getAllErrors().get(0).getDefaultMessage();
        log.warn("请求参数绑定失败: {}", message);
        return Result.badRequest(message);
    }

    /**
     * 处理简单参数约束校验异常。
     *
     * @param exception 约束异常
     * @return 统一失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("请求参数校验失败");
        log.warn("简单参数校验失败: {}", message);
        return Result.badRequest(message);
    }

    /**
     * 处理缺少必填参数异常。
     *
     * @param exception 参数缺失异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception) {
        String message = "缺少必要请求参数: " + exception.getParameterName();
        log.warn("请求参数缺失: {}", message);
        return Result.badRequest(message);
    }

    /**
     * 处理请求体无法反序列化异常。
     *
     * @param exception 反序列化异常
     * @return 统一失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        log.warn("请求体解析失败: {}", exception.getMessage());
        return Result.badRequest("请求体格式错误或字段类型不正确");
    }

    /**
     * 处理无匹配处理器的 404 异常。
     *
     * @param exception 404 异常
     * @return 统一失败响应
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException exception) {
        log.warn("接口不存在: {}", exception.getRequestURL());
        return Result.fail(GlobalErrorCode.NOT_FOUND);
    }

    /**
     * 处理请求方法不允许异常。
     *
     * @param exception 方法异常
     * @return 统一失败响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException exception) {
        log.warn("请求方法不允许: {}", exception.getMethod());
        return Result.fail(GlobalErrorCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 处理媒体类型不支持异常。
     *
     * @param exception 媒体类型异常
     * @return 统一失败响应
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException exception) {
        log.warn("媒体类型不支持: {}", exception.getContentType());
        return Result.fail(GlobalErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * 处理静态资源未找到异常。
     *
     * @param exception 404 异常
     * @return 统一失败响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException exception) {
        log.warn("资源不存在: {} / {}", exception.getHttpMethod(), exception.getResourcePath());
        return Result.fail(GlobalErrorCode.NOT_FOUND);
    }

    /**
     * 处理所有未被显式捕获的系统异常。
     *
     * @param exception 系统异常
     * @param request   当前请求
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception, HttpServletRequest request) {
        log.error("系统异常: {}", request.getRequestURI(), exception);
        return Result.fail(GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }
}
