package com.lumina.key;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "ai_keys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AiProvider provider;

    @Column(length = 64)
    private String model;

    @Column(name = "encrypted_key", nullable = false)
    private String encryptedKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private KeyStatus status = KeyStatus.PRIVATE;

    @Column(name = "used_quota", nullable = false)
    @Builder.Default
    private long usedQuota = 0;

    @Column(name = "donated_at")
    private Instant donatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @jakarta.persistence.PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
