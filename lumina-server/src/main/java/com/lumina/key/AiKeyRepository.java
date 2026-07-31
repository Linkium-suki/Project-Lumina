package com.lumina.key;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiKeyRepository extends JpaRepository<AiKey, Long> {

    List<AiKey> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    Optional<AiKey> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    List<AiKey> findByOwnerUserIdAndProviderAndStatus(Long ownerUserId, AiProvider provider, KeyStatus status);

    List<AiKey> findByStatus(KeyStatus status);

    /** 互助池中指定提供商可用（未过期）的 Key */
    List<AiKey> findByStatusAndProvider(KeyStatus status, AiProvider provider);
}
