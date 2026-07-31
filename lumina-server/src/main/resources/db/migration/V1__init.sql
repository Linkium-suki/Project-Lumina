CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    device_id   VARCHAR(128) NOT NULL UNIQUE,
    nickname    VARCHAR(64),
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    aid_mode    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN users.token_hash IS '客户端注册 token 的 SHA-256 哈希，仅存哈希不存明文';
COMMENT ON COLUMN users.aid_mode IS '是否开启互助模式（使用社区捐赠算力）';

CREATE TABLE ai_keys (
    id             BIGSERIAL PRIMARY KEY,
    owner_user_id  BIGINT       NOT NULL REFERENCES users (id),
    provider       VARCHAR(32)  NOT NULL,
    model          VARCHAR(64),
    encrypted_key  TEXT         NOT NULL,
    status         VARCHAR(16)  NOT NULL DEFAULT 'PRIVATE',
    used_quota     BIGINT       NOT NULL DEFAULT 0,
    donated_at     TIMESTAMPTZ,
    expires_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN ai_keys.encrypted_key IS 'AES-256-GCM 加密后的 API Key 密文（base64(iv+ciphertext)）';
COMMENT ON COLUMN ai_keys.status IS 'PRIVATE=自有 / POOL=已捐赠进互助池';

CREATE INDEX idx_ai_keys_owner ON ai_keys (owner_user_id);
CREATE INDEX idx_ai_keys_status ON ai_keys (status) WHERE status = 'POOL';
