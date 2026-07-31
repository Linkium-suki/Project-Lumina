package com.lumina.key;

import com.lumina.common.ErrorCode;
import com.lumina.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KeyService {

    private final AiKeyRepository keyRepository;
    private final KeyEncryptor keyEncryptor;

    @Transactional
    public KeyResponse addKey(Long userId, KeyRequest request) {
        AiKey key = AiKey.builder()
                .ownerUserId(userId)
                .provider(request.provider())
                .model(request.model())
                .encryptedKey(keyEncryptor.encrypt(request.apiKey()))
                .status(KeyStatus.PRIVATE)
                .build();
        keyRepository.save(key);
        return toResponse(key);
    }

    @Transactional(readOnly = true)
    public List<KeyResponse> listKeys(Long userId) {
        return keyRepository.findByOwnerUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteKey(Long userId, Long keyId) {
        AiKey key = keyRepository.findByIdAndOwnerUserId(keyId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.KEY_NOT_FOUND));
        if (key.getStatus() == KeyStatus.POOL) {
            throw new BizException(ErrorCode.CONFLICT, "该 Key 已捐赠进互助池，请先撤回捐赠");
        }
        keyRepository.delete(key);
    }

    /**
     * 取回用户指定 Key 的明文（仅供内部调度使用）。
     */
    public String decryptKey(AiKey key) {
        return keyEncryptor.decrypt(key.getEncryptedKey());
    }

    /**
     * 校验自填 Key 归属并返回密文实体。
     */
    @Transactional(readOnly = true)
    public AiKey requireOwnKey(Long userId, Long keyId) {
        return keyRepository.findByIdAndOwnerUserId(keyId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.KEY_NOT_FOUND));
    }

    public KeyResponse toResponse(AiKey key) {
        return new KeyResponse(
                key.getId(),
                key.getProvider(),
                key.getModel(),
                key.getStatus(),
                key.getUsedQuota(),
                key.getExpiresAt());
    }
}
