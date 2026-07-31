CREATE TABLE pool_allocations (
    id                BIGSERIAL PRIMARY KEY,
    receiver_user_id  BIGINT      NOT NULL REFERENCES users (id),
    key_id            BIGINT      NOT NULL REFERENCES ai_keys (id),
    allocated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    released_at       TIMESTAMPTZ,
    release_reason    VARCHAR(32)
);

COMMENT ON TABLE pool_allocations IS '互助池分配记录：领取后获得该 Key 的使用权，用后释放';

CREATE INDEX idx_pool_alloc_active
    ON pool_allocations (receiver_user_id)
    WHERE released_at IS NULL;

-- 同一时刻一个 Key 只允许被一人领取，并发领取冲突由唯一索引兜底
CREATE UNIQUE INDEX uk_pool_alloc_key_active
    ON pool_allocations (key_id)
    WHERE released_at IS NULL;

CREATE TABLE chat_logs (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT      NOT NULL REFERENCES users (id),
    provider           VARCHAR(32) NOT NULL,
    model              VARCHAR(64),
    source             VARCHAR(16) NOT NULL,
    prompt_tokens      INT         NOT NULL DEFAULT 0,
    completion_tokens  INT         NOT NULL DEFAULT 0,
    voice_requested    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN chat_logs.source IS 'OWN=自有Key / POOL=互助池 / WATCHMAN=守夜人兜底';
COMMENT ON TABLE chat_logs IS '每次对话调用的用量审计记录，用于互助池配额与成本统计';

CREATE INDEX idx_chat_logs_user_time ON chat_logs (user_id, created_at);
CREATE INDEX idx_chat_logs_created ON chat_logs (created_at);
