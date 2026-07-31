package com.lumina.key;

import com.lumina.common.ErrorCode;
import com.lumina.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM 密钥加解密。
 * 密文格式：base64( nonce(12B) || ciphertext )。
 * 主密钥来自配置（环境变量/K8s Secret），运行时通过 SHA-256 派生为 256 位，绝不落库。
 */
@Component
public class KeyEncryptor {

    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public KeyEncryptor(@Value("${lumina.security.master-key}") String masterKey) {
        byte[] derived = sha256(masterKey);
        this.keySpec = new SecretKeySpec(derived, "AES");
        Arrays.fill(derived, (byte) 0);
    }

    public String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[nonce.length + ciphertext.length];
            System.arraycopy(nonce, 0, out, 0, nonce.length);
            System.arraycopy(ciphertext, 0, out, nonce.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "密钥加密失败", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] in = Base64.getDecoder().decode(encoded);
            if (in.length < NONCE_LENGTH + TAG_LENGTH_BITS / 8) {
                throw new IllegalArgumentException("密文格式非法");
            }
            byte[] nonce = Arrays.copyOfRange(in, 0, NONCE_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(in, NONCE_LENGTH, in.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "密钥解密失败（主密钥可能已更换）", e);
        }
    }

    private static byte[] sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
