package com.lumina.common.exception;

import com.lumina.common.ApiResponse;
import com.lumina.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBiz(BizException e) {
        HttpStatus status = switch (e.getErrorCode()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case NOT_FOUND, KEY_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.OK;
        };
        return ResponseEntity.status(status).body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String msg = fieldError == null
                ? ErrorCode.BAD_REQUEST.getMessage()
                : fieldError.getField() + " " + fieldError.getDefaultMessage();
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.BAD_REQUEST, msg));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR));
    }
}
