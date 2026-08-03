package com.lumina.chat;

import com.lumina.orchestration.failover.RouteSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {

    long countByUserIdAndSourceAndCreatedAtGreaterThanEqual(
            Long userId, RouteSource source, Instant since);
}
