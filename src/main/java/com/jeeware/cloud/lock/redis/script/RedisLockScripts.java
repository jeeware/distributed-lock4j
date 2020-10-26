package com.jeeware.cloud.lock.redis.script;

import lombok.NonNull;

/**
 * Facade interface containing lock redis scripts. All methods must never return
 * <code>null</code> {@link RedisScript}
 * 
 * @author hbourada
 */
public interface RedisLockScripts {

    @NonNull
    RedisScript<Long> acquireLock();

    @NonNull
    RedisScript<Long> releaseLock();

    @NonNull
    RedisScript<Long> refreshActiveLocks();

}
