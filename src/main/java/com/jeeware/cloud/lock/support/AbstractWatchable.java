package com.jeeware.cloud.lock.support;

import com.jeeware.cloud.lock.Watchable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Base implementation for a watchable lock repository
 *
 * @author hbourada
 */
public abstract class AbstractWatchable implements Watchable {

    private final Map<String, Semaphore> locks = new ConcurrentHashMap<>();

    protected volatile boolean active;

    @Override
    public void await(String lockId) throws InterruptedException {
        getSemaphore(lockId).acquire();
    }

    @Override
    public void await(String lockId, long timeoutMillis) throws InterruptedException {
        getSemaphore(lockId).tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void signal(String lockId) {
        getSemaphore(lockId).release();
    }

    private Semaphore getSemaphore(String lock) {
        return locks.computeIfAbsent(lock, l -> new Semaphore(0));
    }

    @Override
    public boolean isActive() {
        return active;
    }

}
