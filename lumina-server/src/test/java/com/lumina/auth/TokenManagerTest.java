package com.lumina.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenManagerTest {

    private final TokenManager tokenManager = new TokenManager();

    @Test
    void hash_isStableAndDoesNotLeakPlaintext() {
        String token = tokenManager.generateToken();

        String hash = tokenManager.hash(token);

        assertThat(hash).isNotEqualTo(token);
        assertThat(tokenManager.matches(token, hash)).isTrue();
        assertThat(tokenManager.matches("wrong-token", hash)).isFalse();
    }

    @Test
    void generateToken_isUnique() {
        assertThat(tokenManager.generateToken()).isNotEqualTo(tokenManager.generateToken());
    }
}
