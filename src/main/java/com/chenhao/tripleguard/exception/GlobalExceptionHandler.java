package com.chenhao.tripleguard.exception;

import com.chenhao.tripleguard.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一处理所有Controller层抛出的异常，返回标准化的API响应格式
 * </p>
 *
 * @author chenhao
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     * <p>
     * 当 @Valid 注解校验失败时抛出此异常
     * </p>
     *
     * @param ex MethodArgumentNotValidException
     * @return 包含校验错误信息的ApiResponse
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("参数校验失败: {}", errorMessage);
        return ApiResponse.badRequest(errorMessage);
    }

    /**
     * 处理通用异常
     * <p>
     * 捕获所有未被特定处理器处理的异常，防止敏感信息泄露
     * </p>
     *
     * @param ex Exception
     * @return 通用错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("服务器内部错误: ", ex);
        return ApiResponse.serverError("服务器内部错误，请稍后重试");
    }
}
