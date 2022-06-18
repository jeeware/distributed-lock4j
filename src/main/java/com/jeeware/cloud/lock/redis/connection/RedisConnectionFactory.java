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

package com.jeeware.cloud.lock.redis.connection;

import com.jeeware.cloud.lock.redis.script.DefaultScriptExecutor;
import com.jeeware.cloud.lock.redis.script.ScriptExecutor;
import lombok.NonNull;

/**
 * Redis connection abstract factory allowing use for various Redis java drivers
 * as Jedis or Lettuce.
 * 
 * @author hbourada
 */
public interface RedisConnectionFactory {

    /**
     * @return a Redis connection, never null
     */
    @NonNull
    RedisConnection getConnection();

    /**
     * @return a Redis script executor to evaluate a script, default to
     *         {@link DefaultScriptExecutor}
     */
    @NonNull
    default ScriptExecutor getScriptExecutor() {
        return new DefaultScriptExecutor(this);
    }
}
