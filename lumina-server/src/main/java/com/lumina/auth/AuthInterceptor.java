package com.lumina.auth;

import com.lumina.common.ErrorCode;
import com.lumina.common.exception.BizException;
import com.lumina.common.web.RateLimiter;
import com.lumina.user.User;
import com.lumina.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Bearer token 鉴权拦截器：校验 token 哈希并写入 userId 请求属性，同时做全局限流。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;
    private final TokenManager tokenManager;
    private final RateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = extractToken(request);
        if (token == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "缺少 Authorization: Bearer token");
        }
        if (!rateLimiter.tryAcquire(token)) {
            throw new BizException(ErrorCode.RATE_LIMITED);
        }
        String hash = tokenManager.hash(token);
        User user = userRepository.findByTokenHash(hash)
                .filter(u -> tokenManager.matches(token, u.getTokenHash()))
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        request.setAttribute(com.lumina.common.web.CurrentUser.ATTR_USER_ID, user.getId());
        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length());
    }
}
