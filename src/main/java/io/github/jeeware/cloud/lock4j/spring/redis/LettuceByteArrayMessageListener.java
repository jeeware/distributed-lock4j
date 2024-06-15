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

package io.github.jeeware.cloud.lock4j.spring.redis;

import lombok.NonNull;
import org.springframework.data.redis.serializer.RedisSerializer;

import io.github.jeeware.cloud.lock4j.redis.connection.MessageListener;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class LettuceByteArrayMessageListener extends RedisPubSubAdapter<byte[], byte[]> {

    @NonNull
    final MessageListener listener;

    @Override
    public void message(byte[] pattern, byte[] channel, byte[] message) {
        final RedisSerializer<String> serializer = RedisSerializer.string();
        listener.onMessage(serializer.deserialize(pattern), serializer.deserialize(channel), serializer.deserialize(message));
    }

    @Override
    public void psubscribed(byte[] pattern, long count) {
        listener.onPSubscribe(RedisSerializer.string().deserialize(pattern), count);
    }

    @Override
    public void punsubscribed(byte[] pattern, long count) {
        listener.onPUnsubscribe(RedisSerializer.string().deserialize(pattern), count);
    }
}
