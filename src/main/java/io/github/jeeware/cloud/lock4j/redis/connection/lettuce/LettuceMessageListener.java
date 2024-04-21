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

package io.github.jeeware.cloud.lock4j.redis.connection.lettuce;

import io.github.jeeware.cloud.lock4j.redis.connection.MessageListener;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class LettuceMessageListener extends RedisPubSubAdapter<String, String> {

    @NonNull
    private final MessageListener listener;

    @Override
    public void message(String pattern, String channel, String message) {
        listener.onMessage(pattern, channel, message);
    }

    @Override
    public void psubscribed(String pattern, long count) {
        listener.onPSubscribe(pattern, count);
    }

    @Override
    public void punsubscribed(String pattern, long count) {
        listener.onPUnsubscribe(pattern, count);
    }
}
