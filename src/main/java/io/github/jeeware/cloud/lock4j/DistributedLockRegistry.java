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
import java.util.function.Function;
import java.util.function.Supplier;

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

    private final Supplier<Retryer> retryerSupplier;

    private final AtomicBoolean scheduledTasksStarted;

    private String instanceId;

    private long refreshLockInterval;

    private long deadLockTimeout;

    private ScheduledFuture<?> refreshLockFuture;

    private ScheduledFuture<?> unlockDeadLocksFuture;

    public DistributedLockRegistry(LockRepository repository, ScheduledExecutorService scheduler, Supplier<Retryer> retryerSupplier) {
        this.repository = Objects.requireNonNull(repository, "repository is null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler is null");
        this.retryerSupplier = Objects.requireNonNull(retryerSupplier, "retryerSupplier is null");
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

        return locks.computeIfAbsent(id, k -> new DistributedLockImpl(id));
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
        Validate.notEmpty(instanceId, "instanceId is empty");
        this.instanceId = instanceId;
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
    final class DistributedLockImpl implements DistributedLock, Supplier<Retryer> {

        final String id;

        final ReentrantLock jvmLock = new ReentrantLock();

        volatile boolean heldByCurrentProcess;

        @Override
        public void lock() {
//            jvmLock.lock();
//            Retryer retryer = null;
//
//            do {
//                try {
//                    if (repository.acquireLock(id, instanceId)) {
//                        heldByCurrentProcess = true;
//                        break;
//                    }
//                    repository.awaitReleaseLock(id);
//                } catch (InterruptedException ie) {
//                    jvmLock.unlock();
//                    handleInterruptedException(ie, false);
//                } catch (Exception e) {
//                    if (retryer == null) {
//                        retryer = retryerSupplier.get();
//                    }
//                    if (!retryer.retryFor(e)) {
//                        handleException(e, true);
//                    }
//                }
//            } while (true);
            jvmLock.lock();
//            doLock(() -> {
//                do {
//                    if (repository.acquireLock(id, instanceId)) {
//                        heldByCurrentProcess = true;
//                        return null;
//                    }
//                    repository.awaitReleaseLock(id);
//                } while (true);
//            });
        }

        private void handleException(Exception cause, boolean acquire) {
            if (acquire) {
                jvmLock.unlock();
            }
            throw DistributedLockException.create(acquire, id, instanceId, cause);
        }

        @SneakyThrows
        private void handleInterruptedException(InterruptedException ie, boolean rethrow) {
            Thread.currentThread().interrupt();
            if (rethrow) {
                throw ie;
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            jvmLock.lockInterruptibly();
            Retryer retryer = null;

            do {
                try {
                    if (!repository.acquireLock(id, instanceId)) {
                        repository.awaitReleaseLock(id);
                    } else if (Thread.interrupted()) {
                        throw new InterruptedException();
                    } else {
                        heldByCurrentProcess = true;
                        break;
                    }
                } catch (InterruptedException ie) {
                    jvmLock.unlock();
                    handleInterruptedException(ie, true);
                } catch (Exception e) {
                    if (retryer == null) {
                        retryer = retryerSupplier.get();
                    }
                    if (!retryer.shouldRetryFor(e)) {
                        handleException(e, true);
                    }
                }
            } while (true);
        }

        @Override
        public boolean tryLock() {
            if (!jvmLock.tryLock()) {
                return false;
            }

            try {
                if (repository.acquireLock(id, instanceId)) {
                    heldByCurrentProcess = true;
                    return true;
                }
            } catch (Exception e) {
                handleException(e, true);
            }

            jvmLock.unlock();

            return false;
        }

        @Override
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            final long until = System.currentTimeMillis() + unit.toMillis(timeout);

            if (!jvmLock.tryLock(timeout, unit)) {
                return false;
            }

            Retryer retryer = null;

            do {
                try {
                    if (repository.acquireLock(id, instanceId)) {
                        heldByCurrentProcess = true;
                        return true;
                    }
                    repository.awaitReleaseLock(id, until - System.currentTimeMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    jvmLock.unlock();
                    throw ie;
                } catch (Exception e) {
                    if (retryer == null) {
                        retryer = retryerSupplier.get();
                    }
                    if (!retryer.shouldRetryFor(e)) {
                        handleException(e, true);
                    }
                }
            } while (System.currentTimeMillis() <= until);

            // can not acquire remote lock after timeout => release local lock
            jvmLock.unlock();

            return false;
        }

        @Override
        public void unlock() {
            Retryer retryer = null;
            do {
                try {
                    repository.releaseLock(id, instanceId);
                    heldByCurrentProcess = false;
                    break;
                } catch (Exception e) {
                    if (retryer == null) {
                        retryer = retryerSupplier.get();
                    }
                    if (!retryer.shouldRetryFor(e)) {
                        handleException(e, false);
                    }
                }
            } while (true);
            locks.remove(id); // lock is no more used => remove it
            jvmLock.unlock();
        }

        @SneakyThrows
        private <T> T doLock(Function<Lock, Object> lockFn, Callable<T> callable) {
            if (Boolean.FALSE.equals(lockFn.apply(jvmLock))) {
                //noinspection unchecked
                return (T) Boolean.FALSE;
            }

            return get().apply(() -> {
                do {
                    return callable.call();
                } while (true);
            }, null);
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

        @Override
        public Retryer get() {
            return new Retryer.Delegate(retryerSupplier.get()) {
                @Override
                public boolean shouldRetryFor(Exception e) {
                    return !(e instanceof InterruptedException) && super.shouldRetryFor(e);
                }
            };
        }
    }
}
