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

package io.github.jeeware.cloud.lock4j.redis.connection.lettuce;

import java.util.List;
import java.util.Map;

import io.github.jeeware.cloud.lock4j.redis.connection.AbstractRedisConnection;
import io.github.jeeware.cloud.lock4j.util.Utils;
import io.github.jeeware.cloud.lock4j.redis.connection.MessageListener;
import io.github.jeeware.cloud.lock4j.redis.connection.Subscription;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisScriptingCommands;
import io.lettuce.core.api.sync.RedisServerCommands;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LettuceConnection extends AbstractRedisConnection {

    @NonNull
    private final LettuceConnectionFactory connectionFactory;

    @Override
    public Map<String, String> configGet(String parameter) {
        return redisCommands().configGet(parameter);
    }

    @Override
    public String configSet(String parameter, String value) {
        return redisCommands().configSet(parameter, value);
    }

    private <C extends RedisServerCommands<String, String> & RedisScriptingCommands<String, String>> C redisCommands() {
        return (C) connectionFactory.getSharedConnection().redisCommands();
    }

    @Override
    public <T> T evalSha(String sha1, List<String> keys, List<?> args, Class<T> returnType) {
        return redisCommands().evalsha(sha1, toScriptOutputType(returnType),
                keys.toArray(new String[0]), Utils.toStringArray(args));
    }

    @Override
    public <T> T eval(String script, List<String> keys, List<?> args, Class<T> returnType) {
        return redisCommands().eval(script, toScriptOutputType(returnType),
                keys.toArray(new String[0]), Utils.toStringArray(args));
    }

    @Override
    public String scriptLoad(String script) {
        return redisCommands().scriptLoad(script);
    }

    @Override
    public int getDatabase() {
        return connectionFactory.getDatabase();
    }

    @Override
    protected Subscription createSubscription(MessageListener listener) {
        return new LettuceSubscription(listener, connectionFactory);
    }

    private static ScriptOutputType toScriptOutputType(Class<?> returnType) {
        if (returnType == null) {
            return ScriptOutputType.STATUS;
        }
        if (returnType == Long.class) {
            return ScriptOutputType.INTEGER;
        }
        if (returnType == Boolean.class) {
            return ScriptOutputType.BOOLEAN;
        }
        if (List.class.isAssignableFrom(returnType)) {
            return ScriptOutputType.MULTI;
        }

        return ScriptOutputType.VALUE;
    }
}
