package com.jeeware.cloud.lock.redis.connection;

public interface Subscription extends AutoCloseable {

    void pSubscribe(String... patterns);

    void pUnsubscribe();

    String[] getPatterns();

    @Override
    default void close() {
        pUnsubscribe();
    }
}
