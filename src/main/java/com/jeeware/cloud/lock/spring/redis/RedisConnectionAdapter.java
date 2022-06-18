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

package com.jeeware.cloud.lock.spring.redis;

import com.jeeware.cloud.lock.redis.connection.AbstractRedisConnection;
import com.jeeware.cloud.lock.util.Utils;
import com.jeeware.cloud.lock.redis.connection.MessageListener;
import com.jeeware.cloud.lock.redis.connection.Subscription;
import org.springframework.data.redis.connection.DecoratedRedisConnection;
import org.springframework.data.redis.connection.DefaultStringRedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class RedisConnectionAdapter extends AbstractRedisConnection {

    private final StringRedisConnection redisConnection;

    private final int database;

    public RedisConnectionAdapter(org.springframework.data.redis.connection.RedisConnection redisConnection, int database) {
        this.redisConnection = new DefaultStringRedisConnection(redisConnection);
        this.database = database;
    }

    @Override
    public Map<String, String> configGet(String parameter) {
        Properties properties = redisConnection.getConfig(parameter);
        if (properties != null) {
            Map<String, String> config = new HashMap<>(properties.size());
            properties.forEach((k, v) -> config.put((String) k, (String) v));
            return config;
        }
        return Collections.emptyMap();
    }

    @Override
    public String configSet(String parameter, String value) {
        // use RedisConnection.execute as RedisConnection.setConfig return void
        byte[] raw = (byte[]) redisConnection.execute("CONFIG", "SET", parameter, value);
        return RedisSerializer.string().deserialize(raw);
    }

    @Override
    public <T> T evalSha(String sha1, List<String> keys, List<?> args, Class<T> returnType) {
        return redisConnection.evalSha(sha1, ReturnType.fromJavaType(returnType), keys.size(), keysAndArgs(keys, args));
    }

    @Override
    public <T> T eval(String script, List<String> keys, List<?> args, Class<T> returnType) {
        return redisConnection.eval(script, ReturnType.fromJavaType(returnType), keys.size(), keysAndArgs(keys, args));
    }

    private static String[] keysAndArgs(List<String> keys, List<?> args) {
        List<String> stringArgs = Utils.toStringList(args);
        List<String> keysAndArgs = new ArrayList<>(keys.size() + stringArgs.size());
        keysAndArgs.addAll(keys);
        keysAndArgs.addAll(stringArgs);
        return keysAndArgs.toArray(new String[0]);
    }

    @Override
    public String scriptLoad(String script) {
        return redisConnection.scriptLoad(script);
    }

    @Override
    protected Subscription createSubscription(MessageListener listener) {
        return new SubscriptionAdapter(listener, ((DecoratedRedisConnection) redisConnection).getDelegate());
    }

    @Override
    public int getDatabase() {
        return database;
    }

    @Override
    public void close() {
        super.close();
        redisConnection.close();
    }

}
