/*
 * Copyright 2020-2026 Hichem BOURADA and other authors.
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

package io.github.jeeware.cloud.lock4j.redis;

import io.github.jeeware.cloud.lock4j.util.Utils;
import lombok.Getter;

@Getter
public class RedisLockKey {

    private static final char SEPARATOR = ':';

    private static final String CLOCK_SKEW_KEY = "clock_skew";

    private final String id;

    private final String clockSkew;

    public RedisLockKey(String prefix, String id, boolean redisCluster) {
        final String prefixSeparator = Utils.isNullOrEmpty(prefix) ? "" : prefix + SEPARATOR;
        this.id = prefixSeparator + id;
        this.clockSkew = hashtag(this.id, redisCluster) + SEPARATOR + CLOCK_SKEW_KEY;
    }

    private static String hashtag(String key, boolean redisCluster) {
        return redisCluster ? '{' + key + '}' : key;
    }

}
