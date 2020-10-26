package com.jeeware.cloud.lock;

/**
 * @author hbourada
 * @version 1.0
 */
public interface LockRepository {

    boolean acquireLock(String lockId, String instanceId);

    void refreshActiveLocks(String instanceId);

    void releaseLock(String lockId, String instanceId);

    void releaseDeadLocks(long timeoutInterval);

    /**
     * Await and block until the distributed lock is released by another
     * process. Default implementation is: {@code Thread.sleep(100)}.
     * 
     * @param lockId
     *            lock identifier
     * @throws InterruptedException
     *             if current thread was interrupted
     */
    default void awaitReleaseLock(String lockId) throws InterruptedException {
        Thread.sleep(100);
    }

    /**
     * Await and block until the distributed lock is released by another process
     * or timeout reached. Default implementation is: {@code Thread.sleep(100)}.
     *
     * @param lockId
     *            lock identifier
     * @param timeoutMillis
     *            maximum time to wait in milliseconds
     * @throws InterruptedException
     *             if current thread was interrupted
     */
    default void awaitReleaseLock(String lockId, long timeoutMillis) throws InterruptedException {
        Thread.sleep(100);
    }

    /**
     * @return true iff this repository can watch lock event changes. Default
     *         implementation return {@code false}.
     */
    default boolean isWatchable() {
        return false;
    }

}
