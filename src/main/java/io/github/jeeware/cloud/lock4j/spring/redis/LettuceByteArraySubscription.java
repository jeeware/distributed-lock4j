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

package io.github.jeeware.cloud.lock4j.spring.redis;

import java.util.Objects;

import org.springframework.data.redis.serializer.RedisSerializer;

import io.github.jeeware.cloud.lock4j.redis.connection.MessageListener;
import io.github.jeeware.cloud.lock4j.redis.connection.Subscription;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

final class LettuceByteArraySubscription implements Subscription {

    final LettuceByteArrayMessageListener listener;

    final StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection;

    String[] patterns;

    LettuceByteArraySubscription(MessageListener listener, StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection) {
        this.listener = new LettuceByteArrayMessageListener(listener);
        this.pubSubConnection = Objects.requireNonNull(pubSubConnection, "pubSubConnection is null");
    }

    @Override
    public void pSubscribe(String... patterns) {
        this.patterns = patterns;
        pubSubConnection.addListener(listener);
        pubSubConnection.sync().psubscribe(serializeArray(patterns));
    }

    @Override
    public void pUnsubscribe() {
        pubSubConnection.removeListener(listener);
        pubSubConnection.sync().punsubscribe();
    }

    @Override
    public String[] getPatterns() {
        return this.patterns;
    }

    @Override
    public void close() {
        Subscription.super.close();
        pubSubConnection.close();
    }

    static byte[][] serializeArray(String[] array) {
        RedisSerializer<String> serializer = RedisSerializer.string();
        byte[][] bytes = new byte[array.length][];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = serializer.serialize(array[i]);
        }

        return bytes;
    }
}
