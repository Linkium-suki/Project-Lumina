package com.lumina.key;

/**
 * Key 生命周期状态。
 */
public enum KeyStatus {
    /** 用户自有，仅本人可用 */
    PRIVATE,
    /** 已捐赠进互助池，交由调度器按需分配 */
    POOL
}
