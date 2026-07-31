package com.lumina.pool;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * 互助池分配记录：接收者领取 → 用后释放，防止单个 Key 被多人并发占用。
 */
@Entity
@Table(name = "pool_allocations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiver_user_id", nullable = false)
    private Long receiverUserId;

    @Column(name = "key_id", nullable = false)
    private Long keyId;

    @Column(name = "allocated_at", nullable = false, updatable = false)
    private Instant allocatedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "release_reason", length = 32)
    private String releaseReason;

    @jakarta.persistence.PrePersist
    void prePersist() {
        if (allocatedAt == null) {
            allocatedAt = Instant.now();
        }
    }
}
