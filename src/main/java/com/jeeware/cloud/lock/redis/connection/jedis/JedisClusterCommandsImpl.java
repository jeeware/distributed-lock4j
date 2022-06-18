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

package com.jeeware.cloud.lock.redis.connection.jedis;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.util.List;

//TODO
@RequiredArgsConstructor
final class JedisClusterCommandsImpl implements JedisCommands {

    @Delegate(types = JedisCommands.class, excludes = ExcludedCommands.class)
    @NonNull
    final JedisCluster jedisCluster;

    public List<String> configGet(String pattern) {
//        jedisCluster.getConnectionFromSlot(0);
        return jedisCluster.getClusterNodes()
                .values()
                .stream()
                .findFirst()
                .map(pool -> {
                    try (Jedis jedis = pool.getResource()) {
                        return jedis.configGet(pattern);
                    }
                }).orElseThrow(() ->new IllegalStateException(""));

    }

    @Override
    public String configSet(String parameter, String value) {
        return null;
    }

    @Override
    public String scriptLoad(String script) {
        return null;
    }

    @Override
    public int getDB() {
        return 0;
    }

    @Override
    public void close() {
        // Do nothing as resource is not to be returned to a Pool
    }

    private interface ExcludedCommands {

        List<String> configGet(String pattern);

        String configSet(String parameter, String value);

        String scriptLoad(String script);

        int getDB();

        void close();

    }
}
