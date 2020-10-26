package com.jeeware.cloud.lock.redis.connection;

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
