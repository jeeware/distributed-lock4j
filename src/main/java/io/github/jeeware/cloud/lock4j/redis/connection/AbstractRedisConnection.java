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

package io.github.jeeware.cloud.lock4j.redis.connection;

import java.util.Arrays;

public abstract class AbstractRedisConnection implements RedisConnection {

    protected Subscription subscription;

    @Override
    public void pSubscribe(MessageListener listener, String... patterns) {
        if (subscription != null) {
            throw new IllegalStateException("Connection already subscribed with pattern: " +
                    Arrays.toString(subscription.getPatterns()));
        }
        subscription = createSubscription(listener);
        subscription.pSubscribe(patterns);
    }

    protected abstract Subscription createSubscription(MessageListener listener);

    @Override
    public void pUnsubscribe() {
        if (subscription == null) {
            throw new IllegalStateException("Connection has not a subscribed listener");
        }
        subscription.pUnsubscribe();
        subscription = null;
    }

    @Override
    public boolean isSubscribed() {
        return subscription != null;
    }

    @Override
    public void close() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }
}
