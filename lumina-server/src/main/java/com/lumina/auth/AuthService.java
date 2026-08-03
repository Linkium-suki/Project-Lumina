package com.lumina.auth;

import com.lumina.user.User;
import com.lumina.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenManager tokenManager;

    /**
     * 设备注册：已存在则直接返回既有 token，保证幂等。
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        return userRepository.findByDeviceId(request.deviceId())
                .map(this::toResponse)
                .orElseGet(() -> createUser(request));
    }

    private RegisterResponse createUser(RegisterRequest request) {
        String token = tokenManager.generateToken();
        User user = User.builder()
                .deviceId(request.deviceId())
                .nickname(request.nickname())
                .tokenHash(tokenManager.hash(token))
                .build();
        userRepository.save(user);
        return new RegisterResponse(user.getId(), token);
    }

    private RegisterResponse toResponse(User user) {
        return new RegisterResponse(user.getId(), null);
    }
}
