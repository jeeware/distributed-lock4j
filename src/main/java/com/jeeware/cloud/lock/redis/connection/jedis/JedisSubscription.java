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

package com.jeeware.cloud.lock.redis.connection.jedis;

import java.util.Objects;

import com.jeeware.cloud.lock.redis.connection.MessageListener;
import com.jeeware.cloud.lock.redis.connection.Subscription;

public final class JedisSubscription implements Subscription {

    private final JedisMessageListener listener;

    private final JedisCommands jedisCommands;

    private String[] patterns;

    public JedisSubscription(MessageListener listener, JedisCommands jedisCommands) {
        this.listener = new JedisMessageListener(listener);
        this.jedisCommands = Objects.requireNonNull(jedisCommands, "jedisCommands is null");
    }

    @Override
    public void pSubscribe(String... patterns) {
        this.patterns = patterns;
        jedisCommands.psubscribe(listener, patterns);
    }

    @Override
    public void pUnsubscribe() {
        listener.punsubscribe();
    }

    @Override
    public String[] getPatterns() {
        return patterns;
    }

}
