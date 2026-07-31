package com.lumina.pool;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 领取写入独立放在 REQUIRES_NEW 事务中：
 * 并发下唯一索引冲突只会回滚本次领取，不会污染外层（路由）事务。
 */
@Service
public class PoolAllocator {

    private final PoolAllocationRepository allocationRepository;

    public PoolAllocator(PoolAllocationRepository allocationRepository) {
        this.allocationRepository = allocationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAllocate(Long receiverUserId, Long keyId) {
        try {
            allocationRepository.save(PoolAllocation.builder()
                    .receiverUserId(receiverUserId)
                    .keyId(keyId)
                    .build());
            return true;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return false;
        }
    }
}
