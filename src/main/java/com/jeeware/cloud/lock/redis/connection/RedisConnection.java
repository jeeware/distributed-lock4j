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

import java.util.List;
import java.util.Map;

import lombok.NonNull;

public interface RedisConnection extends AutoCloseable {

    @NonNull
    Map<String, String> configGet(String parameter);

    String configSet(String parameter, String value);

    <T> T evalSha(String sha1, List<String> keys, List<?> args, Class<T> returnType);

    <T> T  eval(String script, List<String> keys, List<?> args, Class<T> returnType);

    String scriptLoad(String script);

    void pSubscribe(MessageListener listener, String... patterns);

    void pUnsubscribe();

    boolean isSubscribed();

    int getDatabase();

    @Override
    void close();

}
