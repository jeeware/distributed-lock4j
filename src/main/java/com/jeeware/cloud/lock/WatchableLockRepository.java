package com.jeeware.cloud.lock;

/**
 * Watchable lock repository interface.
 * 
 * @author hbourada
 */
public interface WatchableLockRepository extends LockRepository, AutoCloseable {

    /**
     * Starts eventually a background thread to listen repository events
     */
    void start();

    /**
     * Stop background thread and clean resources.
     */
    void close();

    /**
     * @return always return {@code true}
     */
    @Override
    default boolean isWatchable() {
        return true;
    }

}
