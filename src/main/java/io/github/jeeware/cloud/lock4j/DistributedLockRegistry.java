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

package io.github.jeeware.cloud.lock4j;


import io.github.jeeware.cloud.lock4j.DistributedLockException.CannotAcquire;
import io.github.jeeware.cloud.lock4j.DistributedLockException.CannotRelease;
import io.github.jeeware.cloud.lock4j.Retryer.Context;
import io.github.jeeware.cloud.lock4j.support.LoggingErrorTask;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Registry for {@link DistributedLock}
 *
 * @author hbourada
 * @version 1.0
 */
public class DistributedLockRegistry implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLockRegistry.class);

    private static final int DEFAULT_REFRESH_INTERVAL = 5000;

    private static final int DEFAULT_DEADLOCK_TIMEOUT = 30000;

    private final Map<String, DistributedLockImpl> locks;

    private final LockRepository repository;

    private final ScheduledExecutorService scheduler;

    private final DistributedLockRetryer retryer;

    private final AtomicBoolean scheduledTasksStarted;

    private String instanceId;

    private long refreshLockInterval;

    private long deadLockTimeout;

    private ScheduledFuture<?> refreshLockFuture;

    private ScheduledFuture<?> unlockDeadLocksFuture;

    public DistributedLockRegistry(LockRepository repository, ScheduledExecutorService scheduler, Retryer retryer) {
        this.repository = Objects.requireNonNull(repository, "repository is null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler is null");
        this.retryer = new DistributedLockRetryer(retryer);
        this.instanceId = UUID.randomUUID().toString();
        this.locks = new ConcurrentHashMap<>();
        this.scheduledTasksStarted = new AtomicBoolean();
        this.refreshLockInterval = DEFAULT_REFRESH_INTERVAL;
        this.deadLockTimeout = DEFAULT_DEADLOCK_TIMEOUT;
    }

    public DistributedLock getLock(String id) {
        if (scheduledTasksStarted.compareAndSet(false, true)) {
            refreshLockFuture = schedulePeriodically(this::refreshActiveLocks, refreshLockInterval, refreshLockInterval);
            unlockDeadLocksFuture = schedulePeriodically(this::releaseDeadLocks, 0, deadLockTimeout);
            LOGGER.info("Scheduled tasks for registry {} created.", this);
        }

        return locks.computeIfAbsent(id, DistributedLockImpl::new);
    }

    private ScheduledFuture<?> schedulePeriodically(Runnable task, long initialDelayMillis, long delayMillis) {
        return scheduler.scheduleWithFixedDelay(new LoggingErrorTask(task), initialDelayMillis, delayMillis, MILLISECONDS);
    }

    private void refreshActiveLocks() {
        if (!locks.isEmpty()) {
            repository.refreshActiveLocks(instanceId);
        }
    }

    private void releaseDeadLocks() {
        repository.releaseDeadLocks(deadLockTimeout);
    }

    @Override
    public void close() {
        if (scheduledTasksStarted.compareAndSet(true, false)) {
            boolean refreshCanceled = refreshLockFuture.cancel(true);
            boolean unlockCanceled = unlockDeadLocksFuture.cancel(true);
            LOGGER.info("Closing registry instanceId: {}. Cancel scheduled refresh lock: {}, " +
                    "cancel scheduled unlock deadlocks: {}", instanceId, refreshCanceled, unlockCanceled);
            locks.forEach((id, lock) -> {
                if (lock.tryUnlock()) {
                    LOGGER.info("Successfully unlocked lock id={} when closing registry instanceId: {}", id, instanceId);
                }
            });
        }
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = Validate.notEmpty(instanceId, "instanceId is empty");
    }

    public void setRefreshLockInterval(long refreshLockIntervalMillis) {
        Validate.isTrue(refreshLockIntervalMillis > 0, "refreshLockIntervalMillis > 0");
        this.refreshLockInterval = refreshLockIntervalMillis;
    }

    public void setDeadLockTimeout(long deadLockTimeoutMillis) {
        Validate.isTrue(deadLockTimeoutMillis > 0, "deadLockTimeoutMillis > 0");
        this.deadLockTimeout = deadLockTimeoutMillis;
    }

    @Override
    public String toString() {
        return "(instanceId='" + instanceId + '\'' +
                ", repository=" + repository +
                ", refreshLockInterval=" + refreshLockInterval +
                ", deadLockTimeout=" + deadLockTimeout +
                ')';
    }

    @RequiredArgsConstructor
    final class DistributedLockImpl implements DistributedLock {

        final String id;

        final ReentrantLock jvmLock = new ReentrantLock();

        volatile boolean heldByCurrentProcess;

        @Override
        @SneakyThrows
        public void lock() {
            lockImpl(AcquireLock.LOCK);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            lockImpl(AcquireLock.LOCK_INTERRUPTIBLE);
        }

        private void lockImpl(AcquireLock acquireLock) throws InterruptedException {
            acquireLock.apply(jvmLock);
            // reentrant lock
            if (heldByCurrentProcess) {
                return;
            }
            retryer.apply(() -> {
                do {
                    if (repository.acquireLock(id, instanceId)) {
                        heldByCurrentProcess = true;
                        return null;
                    }
                    if (acquireLock.isInterruptible() && Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    repository.awaitReleaseLock(id);
                } while (true);
            }, new AcquireLockRecovery<>(acquireLock.isInterruptible()));
        }

        @Override
        @SneakyThrows
        public boolean tryLock() {
            if (!jvmLock.tryLock()) {
                return false;
            }
            // reentrant lock
            if (heldByCurrentProcess) {
                return true;
            }
            return retryer.apply(() -> {
                if (repository.acquireLock(id, instanceId)) {
                    heldByCurrentProcess = true;
                    return true;
                }
                jvmLock.unlock();
                return false;
            }, new AcquireLockRecovery<>(false));
        }

        @Override
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            final long until = System.currentTimeMillis() + unit.toMillis(timeout);
            if (!jvmLock.tryLock(timeout, unit)) {
                return false;
            }
            // reentrant lock
            if (heldByCurrentProcess) {
                return true;
            }
            return retryer.apply(() -> {
                do {
                    if (repository.acquireLock(id, instanceId)) {
                        heldByCurrentProcess = true;
                        return true;
                    }
                    repository.awaitReleaseLock(id, until - System.currentTimeMillis());
                } while (System.currentTimeMillis() <= until);
                // cannot acquire remote lock after timeout => release local lock
                jvmLock.unlock();
                return false;
            }, new AcquireLockRecovery<>(true));
        }

        @Override
        @SneakyThrows
        public void unlock() {
            if (!jvmLock.isHeldByCurrentThread()) {
                throw new IllegalMonitorStateException("Attempt to unlock lock id=" + id +
                        " not locked by the current thread " + Thread.currentThread().getName());
            }
            if (jvmLock.getHoldCount() == 1) {
                retryer.apply(() -> {
                    repository.releaseLock(id, instanceId);
                    heldByCurrentProcess = false;
                    locks.remove(id); // lock is no more used => remove it
                    return null;
                }, (exception, context) -> {
                    locks.remove(id);
                    throw new CannotRelease(id, instanceId, exception);
                });
            }
            jvmLock.unlock();
        }

        boolean tryUnlock() {
            if (isHeldByCurrentProcess()) {
                repository.releaseLock(id, instanceId);
                return true;
            }
            return false;
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("newCondition not supported");
        }

        @Override
        public boolean isHeldByCurrentProcess() {
            return heldByCurrentProcess;
        }

        // visible for test
        DistributedLockRegistry getRegistry() {
            return DistributedLockRegistry.this;
        }

        @Override
        public String toString() {
            return "DistributedLockImpl[id=" + id + ", instanceId=" + instanceId + ", jvmLock=" + jvmLock
                    + ", heldByCurrentProcess=" + heldByCurrentProcess + "]";
        }

        @RequiredArgsConstructor
        private class AcquireLockRecovery<T> implements Retryer.Recovery<T> {
            private final boolean rethrowInterrupted;

            @Override
            public T recover(Exception exception, Context context) throws InterruptedException {
                jvmLock.unlock();
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    if (rethrowInterrupted) {
                        throw (InterruptedException) exception;
                    }
                }
                throw new CannotAcquire(id, instanceId, exception);
            }
        }
    }

    static final class DistributedLockRetryer implements Retryer {

        private final Retryer retryer;

        public DistributedLockRetryer(Retryer retryer) {
            this.retryer = Objects.requireNonNull(retryer, "retryer is null");
        }

        @Override
        public boolean shouldRetryFor(Exception e, Context context) {
            return !(e instanceof InterruptedException) && retryer.shouldRetryFor(e, context);
        }

        @Override
        public Context createContext() {
            return retryer.createContext();
        }

        @Override
        public void sleep(Context context) throws InterruptedException {
            retryer.sleep(context);
        }

        @Override
        public <T> T apply(Callable<T> retryTask, Recovery<T> recoveryTask) throws InterruptedException {
            try {
                return retryer.apply(retryTask, recoveryTask);
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                return sneakyThrow(e);
            }
        }

        @SneakyThrows
        private static <T> T sneakyThrow(Exception e) {
            throw e;
        }

    }

    interface AcquireLock {

        void apply(Lock lock) throws InterruptedException;

        default boolean isInterruptible() {
            return false;
        }

        AcquireLock LOCK = Lock::lock;

        AcquireLock LOCK_INTERRUPTIBLE = new AcquireLock() {
            @Override
            public void apply(Lock lock) throws InterruptedException {
                lock.lockInterruptibly();
            }

            @Override
            public boolean isInterruptible() {
                return true;
            }
        };
    }
}
