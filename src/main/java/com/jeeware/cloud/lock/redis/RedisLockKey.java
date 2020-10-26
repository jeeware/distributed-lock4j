package com.jeeware.cloud.lock.redis;

import java.time.Instant;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

@Getter
public final class RedisLockKey {

    private static final char SEPARATOR = ':';

    private static final String LOCKED_BY_FIELD = "locked_by";

    private final String id;

    private final String lockedBy;

    private final Instant lockedAt;

    public RedisLockKey(String prefix, String id, String instanceId) {
        final String prefixSeparator = StringUtils.isEmpty(prefix) ? "" : prefix + SEPARATOR;
        this.id = prefixSeparator + id;
        this.lockedBy = prefixSeparator + LOCKED_BY_FIELD + SEPARATOR + instanceId;
        this.lockedAt = Instant.now();
    }

}
