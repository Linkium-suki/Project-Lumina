package com.lumina.orchestration.failover;

/**
 * 本次调用的算力来源。
 */
public enum RouteSource {
    /** 用户自填自有 Key */
    OWN,
    /** 互助池捐赠 Key */
    POOL,
    /** 守夜人兜底 Key */
    WATCHMAN
}
