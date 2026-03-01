package com.chenhao.tripleguard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用API响应封装类
 * <p>
 * 用于统一封装所有API接口的响应格式，包含状态码、消息和数据
 * </p>
 *
 * @param <T> 响应数据类型
 * @author chenhao
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * 响应状态码
     * <p>
     * 200: 成功
     * 400: 客户端请求错误
     * 401: 未授权
     * 403: 禁止访问
     * 404: 资源不存在
     * 500: 服务器内部错误
     * </p>
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳
     */
    @Builder.Default
    private long timestamp = System.currentTimeMillis();

    /**
     * 创建成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功的API响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .build();
    }

    /**
     * 创建成功响应（带自定义消息）
     *
     * @param data    响应数据
     * @param message 响应消息
     * @param <T>     数据类型
     * @return 成功的API响应
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 创建成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return 成功的API响应
     */
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .code(200)
                .message("success")
                .build();
    }

    /**
     * 创建错误响应
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 错误的API响应
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 创建错误响应（带数据）
     *
     * @param code    错误码
     * @param message 错误消息
     * @param data    附加数据
     * @param <T>     数据类型
     * @return 错误的API响应
     */
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 创建400错误响应
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 400错误的API响应
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return error(400, message);
    }

    /**
     * 创建404错误响应
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 404错误的API响应
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return error(404, message);
    }

    /**
     * 创建500错误响应
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 500错误的API响应
     */
    public static <T> ApiResponse<T> serverError(String message) {
        return error(500, message);
    }
}
