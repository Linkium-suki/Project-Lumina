package com.lumina.key;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeyEncryptorTest {

    private final KeyEncryptor encryptor = new KeyEncryptor("test-master-key-with-sufficient-length");

    @Test
    void encryptThenDecrypt_roundTrips() {
        String secret = "sk-very-secret-key-123456";

        String encrypted = encryptor.encrypt(secret);

        assertThat(encrypted).isNotEqualTo(secret);
        assertThat(encryptor.decrypt(encrypted)).isEqualTo(secret);
    }

    @Test
    void encrypt_producesUniqueCiphertextPerCall() {
        String a = encryptor.encrypt("same-plaintext");
        String b = encryptor.encrypt("same-plaintext");

        assertThat(a).isNotEqualTo(b);
        assertThat(encryptor.decrypt(a)).isEqualTo(encryptor.decrypt(b));
    }
}
