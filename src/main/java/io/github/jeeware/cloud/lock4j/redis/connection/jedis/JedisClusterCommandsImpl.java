/*
 * Copyright 2020-2020-2024 Hichem BOURADA and other authors.
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

package io.github.jeeware.cloud.lock4j.redis.connection.jedis;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPubSub;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@RequiredArgsConstructor
final class JedisClusterCommandsImpl implements JedisCommands {

    @NonNull
    final JedisCluster jedisCluster;

    public List<String> configGet(String pattern) {
        return jedisCluster.getClusterNodes()
                .values()
                .stream()
                .flatMap(pool -> {
                    try (Jedis jedis = pool.getResource()) {
                        return jedis.configGet(pattern).stream();
                    }
                })
                .distinct()
                .collect(Collectors.toList());

    }

    @Override
    public String configSet(String parameter, String value) {
        jedisCluster.getClusterNodes()
                .values()
                .forEach(pool -> {
                    try (Jedis jedis = pool.getResource()) {
                        jedis.configSet(parameter, value);
                    }
                });
        return "OK";
    }

    @Override
    public Object eval(String script, List<String> keys, List<String> args) {
        return jedisCluster.eval(script, keys, args);
    }

    @Override
    public Object evalsha(String sha1, List<String> keys, List<String> args) {
        return jedisCluster.evalsha(sha1, keys, args);
    }

    @Override
    public String scriptLoad(String script) {
        final AtomicReference<String> firstResult = new AtomicReference<>();
        jedisCluster.getClusterNodes()
                .values()
                .forEach(pool -> {
                    try (Jedis jedis = pool.getResource()) {
                        firstResult.compareAndSet(null, jedis.scriptLoad(script));
                    }
                });
        return firstResult.get();
    }

    @Override
    public int getDB() {
        return jedisCluster.getClusterNodes().values()
                .stream()
                .findFirst()
                .map(pool -> {
                    try (Jedis jedis = pool.getResource()) {
                        return jedis.getDB();
                    }
                }).orElse(0);
    }

    @Override
    public void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
        jedisCluster.psubscribe(jedisPubSub, patterns);
    }

    @Override
    public void close() {
        jedisCluster.close();
    }

}
