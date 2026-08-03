package com.lumina.pool;

import com.lumina.chat.ChatLogRepository;
import com.lumina.common.ErrorCode;
import com.lumina.common.exception.BizException;
import com.lumina.key.AiKey;
import com.lumina.key.AiKeyRepository;
import com.lumina.key.AiProvider;
import com.lumina.key.KeyResponse;
import com.lumina.key.KeyService;
import com.lumina.key.KeyStatus;
import com.lumina.orchestration.failover.RouteSource;
import com.lumina.user.User;
import com.lumina.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 互助池：捐赠、领取、配额与守夜人兜底的衔接。
 */
@Service
public class PoolService {

    private final AiKeyRepository keyRepository;
    private final PoolAllocationRepository allocationRepository;
    private final UserRepository userRepository;
    private final ChatLogRepository chatLogRepository;
    private final KeyService keyService;
    private final PoolAllocator poolAllocator;
    private final int dailyLimit;

    public PoolService(
            AiKeyRepository keyRepository,
            PoolAllocationRepository allocationRepository,
            UserRepository userRepository,
            ChatLogRepository chatLogRepository,
            KeyService keyService,
            PoolAllocator poolAllocator,
            @Value("${lumina.pool.daily-request-limit:100}") int dailyLimit) {
        this.keyRepository = keyRepository;
        this.allocationRepository = allocationRepository;
        this.userRepository = userRepository;
        this.chatLogRepository = chatLogRepository;
        this.keyService = keyService;
        this.poolAllocator = poolAllocator;
        this.dailyLimit = dailyLimit;
    }

    /** 捐赠：自有 Key → 互助池。 */
    @Transactional
    public KeyResponse donate(Long userId, Long keyId) {
        AiKey key = keyRepository.findByIdAndOwnerUserId(keyId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.KEY_NOT_FOUND));
        if (key.getStatus() == KeyStatus.POOL) {
            throw new BizException(ErrorCode.CONFLICT, "该 Key 已在互助池中");
        }
        key.setStatus(KeyStatus.POOL);
        key.setDonatedAt(Instant.now());
        return keyService.toResponse(key);
    }

    /** 撤回捐赠：互助池 → 自有。 */
    @Transactional
    public void withdraw(Long userId, Long keyId) {
        AiKey key = keyRepository.findByIdAndOwnerUserId(keyId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.KEY_NOT_FOUND));
        if (key.getStatus() != KeyStatus.POOL) {
            throw new BizException(ErrorCode.CONFLICT, "该 Key 不在互助池中");
        }
        allocationRepository.findByKeyIdAndReleasedAtIsNull(keyId)
                .ifPresent(a -> {
                    throw new BizException(ErrorCode.CONFLICT, "该 Key 正被受助者使用，请稍后再撤回");
                });
        key.setStatus(KeyStatus.PRIVATE);
        key.setDonatedAt(null);
    }

    /** 开启互助模式。 */
    @Transactional
    public void join(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        user.setAidMode(true);
    }

    /** 关闭互助模式并释放名下所有领取。 */
    @Transactional
    public void leave(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        user.setAidMode(false);
        releaseActive(userId, "aid_mode_off");
    }

    /** 当前互助状态。 */
    @Transactional(readOnly = true)
    public PoolStatusResponse status(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND));
        long todayPoolUsage = todayPoolUsage(userId);
        long availableKeys = keyRepository.findByStatus(KeyStatus.POOL).stream()
                .filter(k -> allocationRepository.findByKeyIdAndReleasedAtIsNull(k.getId()).isEmpty())
                .count();
        return new PoolStatusResponse(
                user.isAidMode(),
                dailyLimit,
                todayPoolUsage,
                availableKeys);
    }

    /**
     * 为互助模式用户领取一个指定提供商的池内 Key。
     * 无可用 Key 时返回 null；超配额抛 POOL_LIMIT_EXCEEDED。
     */
    @Transactional
    public AiKey acquireKey(Long userId, AiProvider provider) {
        if (!isAidMode(userId)) {
            throw new BizException(ErrorCode.AID_MODE_REQUIRED);
        }
        if (todayPoolUsage(userId) >= dailyLimit) {
            throw new BizException(ErrorCode.POOL_LIMIT_EXCEEDED);
        }

        List<AiKey> poolKeys = keyRepository.findByStatusAndProvider(KeyStatus.POOL, provider);
        for (AiKey key : poolKeys) {
            if (poolAllocator.tryAllocate(userId, key.getId())) {
                return key;
            }
        }
        return null;
    }

    /** 使用完毕释放领取。 */
    @Transactional
    public void releaseKey(Long keyId) {
        allocationRepository.findByKeyIdAndReleasedAtIsNull(keyId)
                .ifPresent(a -> {
                    a.setReleasedAt(Instant.now());
                    a.setReleaseReason("completed");
                });
    }

    private boolean isAidMode(Long userId) {
        return userRepository.findById(userId)
                .map(User::isAidMode)
                .orElse(false);
    }

    private long todayPoolUsage(Long userId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant since = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        return chatLogRepository.countByUserIdAndSourceAndCreatedAtGreaterThanEqual(
                userId, RouteSource.POOL, since);
    }

    private void releaseActive(Long userId, String reason) {
        allocationRepository.findByReceiverUserIdAndReleasedAtIsNull(userId)
                .forEach(a -> {
                    a.setReleasedAt(Instant.now());
                    a.setReleaseReason(reason);
                });
    }
}
