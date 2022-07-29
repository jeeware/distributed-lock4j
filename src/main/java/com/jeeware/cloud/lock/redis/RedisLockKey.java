/*
 * Copyright 2020-2022 Hichem BOURADA and other authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        this(prefix, id, instanceId, false);
    }

    public RedisLockKey(String prefix, String id, String instanceId, boolean redisCluster) {
        final String prefixSeparator = StringUtils.isEmpty(prefix) ? "" :  prefix + SEPARATOR;
        this.lockedBy = prefixSeparator + LOCKED_BY_FIELD + SEPARATOR + instanceId;
        this.id = prefixSeparator + clusterKeySlot(redisCluster, this.lockedBy) + id;
        this.lockedAt = Instant.now();
    }

    private static String clusterKeySlot(boolean redisCluster, String key) {
        return redisCluster ? '{' + key + '}' : "";
    }

}
