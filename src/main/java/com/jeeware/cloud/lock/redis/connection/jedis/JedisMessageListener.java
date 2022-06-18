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

import com.jeeware.cloud.lock.redis.connection.MessageListener;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.JedisPubSub;

@RequiredArgsConstructor
public final class JedisMessageListener extends JedisPubSub {

    @NonNull
    private final MessageListener listener;

    @Override
    public void onPMessage(String pattern, String channel, String message) {
        listener.onMessage(pattern, channel, message);
    }

    @Override
    public void onPSubscribe(String pattern, int count) {
        listener.onPSubscribe(pattern, count);
    }

    @Override
    public void onPUnsubscribe(String pattern, int count) {
        listener.onPUnsubscribe(pattern, count);
    }
}
