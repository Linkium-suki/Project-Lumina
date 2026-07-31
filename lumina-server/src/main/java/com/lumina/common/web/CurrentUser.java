package com.lumina.common.web;

import com.lumina.common.exception.BizException;
import com.lumina.common.ErrorCode;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 从当前请求上下文读取由 AuthInterceptor 写入的登录用户 ID。
 */
public final class CurrentUser {

    public static final String ATTR_USER_ID = "lumina.userId";

    private CurrentUser() {
    }

    public static Long userId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        Object value = attrs.getRequest().getAttribute(ATTR_USER_ID);
        if (value instanceof Long userId) {
            return userId;
        }
        throw new BizException(ErrorCode.UNAUTHORIZED);
    }
}
