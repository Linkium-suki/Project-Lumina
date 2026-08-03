package com.lumina.orchestration.provider;

import com.lumina.common.ErrorCode;
import com.lumina.common.exception.BizException;

/**
 * Provider 调用失败。Router 会据此触发故障转移。
 */
public class ProviderException extends RuntimeException {

    public ProviderException(String message) {
        super(message);
        this.errorCode = ErrorCode.PROVIDER_RESPONSE;
    }

    public ProviderException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.PROVIDER_RESPONSE;
    }

    private final ErrorCode errorCode;

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static ProviderException unavailable(String message, Throwable cause) {
        return new ProviderException(message, cause);
    }
}
