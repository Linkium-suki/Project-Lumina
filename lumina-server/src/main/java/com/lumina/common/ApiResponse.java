package com.lumina.common;

/**
 * 统一响应包装：success=true 时为正常数据，否则为错误信息。
 */
public record ApiResponse<T>(boolean success, String code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", null, data);
    }

    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getMessage());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode.getCode(), message, null);
    }
}
