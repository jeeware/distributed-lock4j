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

package io.github.jeeware.cloud.lock4j.redis.script;

import io.github.jeeware.cloud.lock4j.redis.connection.RedisConnection;
import io.github.jeeware.cloud.lock4j.redis.connection.RedisConnectionFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class DefaultScriptExecutor implements ScriptExecutor {

    @NonNull
    private final RedisConnectionFactory connectionFactory;

    @Override
    public <T> T execute(RedisScript<T> script, List<String> keys, List<?> args) {
        final RedisConnection connection = connectionFactory.getConnection();
        try {
            if (script.isLoaded()) {
                return connection.evalSha(script.getSha1(), keys, args, script.getReturnType());
            }
            return connection.eval(script.getScriptAsString(), keys, args, script.getReturnType());
        } catch (Exception e) {
            if (containsNoScript(e)) {
                return connection.eval(script.getScriptAsString(), keys, args, script.getReturnType());
            }
            throw e;
        } finally {
            connection.close();
        }
    }
}
