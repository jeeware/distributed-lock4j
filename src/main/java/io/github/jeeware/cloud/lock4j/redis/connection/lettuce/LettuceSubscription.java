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
import io.github.jeeware.cloud.lock4j.redis.connection.Subscription;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

final class LettuceSubscription implements Subscription {

    final LettuceMessageListener listener;

    final StatefulRedisPubSubConnection<String, String> redisPubSubConnection;

    String[] patterns;

    LettuceSubscription(MessageListener listener, LettuceConnectionFactory factory) {
        this.listener = new LettuceMessageListener(listener);
        this.redisPubSubConnection = factory.getPubSubConnection();
    }

    @Override
    public void pSubscribe(String... patterns) {
        this.patterns = patterns;
        final StatefulRedisPubSubConnection<String, String> connection = redisPubSubConnection;
        if (connection instanceof StatefulRedisClusterPubSubConnection) {
            ((StatefulRedisClusterPubSubConnection<?, ?>) connection).setNodeMessagePropagation(true);
        }
        connection.addListener(listener);
        redisPubSubConnection.sync().psubscribe(patterns);
    }

    @Override
    public void pUnsubscribe() {
        redisPubSubConnection.removeListener(listener);
        redisPubSubConnection.sync().punsubscribe();
    }

    @Override
    public String[] getPatterns() {
        return patterns;
    }

    @Override
    public void close() {
        Subscription.super.close();
        redisPubSubConnection.close();
    }

}
