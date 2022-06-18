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

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPubSub;

import java.util.List;

public interface JedisCommands extends AutoCloseable {

    List<String> configGet(String pattern);

    String configSet(String parameter, String value);

    Object eval(String script, List<String> keys, List<String> args);

    Object evalsha(String sha1, List<String> keys, List<String> args);

    String scriptLoad(String script);

    int getDB();

    void psubscribe(JedisPubSub jedisPubSub, final String... patterns);

    @Override
    void close();

    static JedisCommands of(Jedis jedis) {
        return new JedisCommandsImpl(jedis);
    }

    static JedisCommands of(JedisCluster jedisCluster) {
        return new JedisClusterCommandsImpl(jedisCluster);
    }

}
