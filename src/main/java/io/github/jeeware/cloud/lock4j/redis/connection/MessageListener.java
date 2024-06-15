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

package io.github.jeeware.cloud.lock4j.redis.connection;

/**
 * Callback interface for Redis pub/sub events
 *
 * @author hbourada
 */
public interface MessageListener {

    /**
     * Message received from a pattern subscription.
     *
     * @param pattern Key pattern
     * @param channel Channel
     * @param message Message
     */
    void onMessage(String pattern, String channel, String message);

    /**
     * Subscribed to a pattern.
     *
     * @param pattern Key pattern
     * @param count   Subscription count.
     */
    default void onPSubscribe(String pattern, long count) {
    }

    /**
     * Unsubscribed to a pattern.
     *
     * @param pattern Key pattern
     * @param count   Subscription count.
     */
    default void onPUnsubscribe(String pattern, long count) {
    }

}
