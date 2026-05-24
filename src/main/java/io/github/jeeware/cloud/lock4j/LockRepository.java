/*
 * Copyright 2020-2026 Hichem BOURADA and other authors.
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

package io.github.jeeware.cloud.lock4j;

/**
 * @author hbourada
 * @version 1.0
 */
public interface LockRepository {

    boolean acquireLock(String lockId, String instanceId);

    /**
     *
     * @param lockId          the lock identifier
     * @param instanceId      the instance id
     * @param clockSkewMillis the clock skew or tolerated time difference.
     */
    boolean acquireLockWithClockSkew(String lockId, String instanceId, long clockSkewMillis);

    /**
     *
     * @param instanceId instance unique identifier to monitor which instance get the lock.
     * @deprecated Use {@link #refreshActiveLock(String, String)} instead, this method will be removed,
     * here only for retro-compatibility.
     */
    @Deprecated
    default void refreshActiveLocks(String instanceId) {
        refreshActiveLock(null, instanceId);
    }

    /**
     * Update heartbeat time of the active lock in the instance id to detect deadlock.
     *
     * @param lockId     the lock identifier.
     * @param instanceId instance identifier to monitor which instance get the lock.
     */
    void refreshActiveLock(String lockId, String instanceId);

    void releaseLock(String lockId, String instanceId);

    void releaseDeadLocks(long timeoutInterval);

    /**
     * Await and block until the distributed lock is released by another
     * process. Default implementation is: {@code Thread.sleep(100)}.
     *
     * @param lockId lock identifier
     * @throws InterruptedException if current thread was interrupted
     */
    default void awaitReleaseLock(String lockId) throws InterruptedException {
        Thread.sleep(100);
    }

    /**
     * Await and block until the distributed lock is released by another process
     * or timeout reached. Default implementation is: {@code Thread.sleep(100)}.
     *
     * @param lockId        lock identifier
     * @param timeoutMillis maximum time to wait in milliseconds
     * @throws InterruptedException if current thread was interrupted
     */
    default void awaitReleaseLock(String lockId, long timeoutMillis) throws InterruptedException {
        Thread.sleep(100);
    }

    /**
     * @return true iff this repository can watch lock event changes. Default
     * implementation return {@code false}.
     */
    default boolean isWatchable() {
        return false;
    }
}
