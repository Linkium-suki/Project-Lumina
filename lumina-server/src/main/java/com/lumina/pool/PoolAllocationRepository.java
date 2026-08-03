package com.lumina.pool;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PoolAllocationRepository extends JpaRepository<PoolAllocation, Long> {

    List<PoolAllocation> findByReceiverUserIdAndReleasedAtIsNull(Long receiverUserId);

    Optional<PoolAllocation> findByKeyIdAndReleasedAtIsNull(Long keyId);

    /** 释放所有超出保活期限仍未归还的过期分配 */
    List<PoolAllocation> findByReleasedAtIsNullAndAllocatedAtBefore(Instant before);
}
