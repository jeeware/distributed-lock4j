package com.jeeware.cloud.lock;

/**
 * Asynchronous command which detects modification events on lock repository
 *
 * @author hbourada
 */
public interface Watchable extends Runnable, AutoCloseable {

    void await(String lockId) throws InterruptedException;

    void await(String lockId, long timeoutMillis) throws InterruptedException;

    void signal(String lockId);

    boolean isActive();

    @Override
    void close();

    default String name() {
        return this.getClass().getSimpleName();
    }

}
