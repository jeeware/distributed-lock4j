/*
 * Copyright 2020-2024 Hichem BOURADA and other authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.jeeware.cloud.lock4j.redis.connection.AbstractRedisConnection;
import io.github.jeeware.cloud.lock4j.util.Utils;
import io.github.jeeware.cloud.lock4j.redis.connection.MessageListener;
import io.github.jeeware.cloud.lock4j.redis.connection.Subscription;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JedisConnection extends AbstractRedisConnection {

    @NonNull
    private final JedisCommands jedisCommands;

    @Override
    public Map<String, String> configGet(String parameter) {
        final List<String> result = jedisCommands.configGet(parameter);

        if (result == null || result.isEmpty()) {
            return Collections.emptyMap();
        }

        final int size = result.size();

        if (size % 2 != 0) {
            throw new IllegalStateException("config get for " + parameter + " returns an odd number: " + size);
        }

        final Map<String, String> parameters = new HashMap<>(size / 2);

        for (int i = 0; i < size; i += 2) {
            parameters.put(result.get(i), result.get(i + 1));
        }

        return parameters;
    }

    @Override
    public String configSet(String parameter, String value) {
        return jedisCommands.configSet(parameter, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T evalSha(String sha1, List<String> keys, List<?> args, Class<T> returnType) {
        if (returnType != null) {
            returnType.cast(jedisCommands.evalsha(sha1, keys, Utils.toStringList(args)));
        }
        return (T) jedisCommands.evalsha(sha1, keys, Utils.toStringList(args));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T eval(String script, List<String> keys, List<?> args, Class<T> returnType) {
        if (returnType != null) {
            return returnType.cast(jedisCommands.eval(script, keys, Utils.toStringList(args)));
        }
        return (T) jedisCommands.eval(script, keys, Utils.toStringList(args));
    }

    @Override
    public String scriptLoad(String script) {
        return jedisCommands.scriptLoad(script);
    }

    @Override
    public int getDatabase() {
        return jedisCommands.getDB();
    }

    @Override
    protected Subscription createSubscription(MessageListener listener) {
        return new JedisSubscription(listener, jedisCommands);
    }

    @Override
    public void close() {
        super.close();
        jedisCommands.close();
    }

}
