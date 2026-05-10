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


import io.github.jeeware.cloud.lock4j.DistributedLockException.CannotAcquire;
import io.github.jeeware.cloud.lock4j.DistributedLockException.CannotRelease;
import io.github.jeeware.cloud.lock4j.Retryer.Context;
import io.github.jeeware.cloud.lock4j.support.LoggingErrorTask;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static io.github.jeeware.cloud.lock4j.util.Utils.defaultIfNull;
import static io.github.jeeware.cloud.lock4j.util.Utils.getIfNull;
import static io.github.jeeware.cloud.lock4j.util.Utils.validateNullOrPositive;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Registry for {@link DistributedLock}
 *
 * @author hbourada
 * @since 1.0
 */
public class DistributedLockRegistry implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLockRegistry.class);

    private static final Duration DEFAULT_REFRESH_INTERVAL = Duration.ofMillis(5000);

    private static final Duration DEFAULT_DEADLOCK_TIMEOUT = Duration.ofMillis(30000);

    private final Map<String, DistributedLockImpl> locks;

    private final LockRepository repository;

    private final ScheduledExecutorService scheduler;

    private final DistributedLockRetryer retryer;

    private final AtomicBoolean started;

    private String instanceId;

    private long refreshLockInterval;

    private long deadLockTimeout;

    private ScheduledFuture<?> unlockDeadLocksFuture;

    @Deprecated
    public DistributedLockRegistry(LockRepository repository, ScheduledExecutorService scheduler, Retryer retryer) {
        this(repository, scheduler, retryer, null, null, null);
    }

    @Builder
    protected DistributedLockRegistry(LockRepository repository, ScheduledExecutorService scheduler,
                                      Retryer retryer, String instanceId,
                                      Duration refreshLockInterval, Duration deadLockTimeout) {
        this.repository = Objects.requireNonNull(repository, "repository is null");
        this.scheduler = getIfNull(DisabledShutdownScheduler.of(scheduler), () -> Executors.newScheduledThreadPool(1));
        this.retryer = new DistributedLockRetryer(retryer);
        this.instanceId = getIfNull(instanceId, () -> UUID.randomUUID().toString());
        this.refreshLockInterval = defaultIfNull(validateNullOrPositive(refreshLockInterval, "refreshLockInterval"),
                DEFAULT_REFRESH_INTERVAL).toMillis();
        this.deadLockTimeout = defaultIfNull(validateNullOrPositive(deadLockTimeout, "deadLockTimeout"),
                DEFAULT_DEADLOCK_TIMEOUT).toMillis();
        this.locks = new ConcurrentHashMap<>();
        this.started = new AtomicBoolean();
    }

    public DistributedLock getLock(String id) {
        if (started.compareAndSet(false, true)) {
            unlockDeadLocksFuture = schedulePeriodically(this::releaseDeadLocks, 0, deadLockTimeout);
            LOGGER.info("Scheduled tasks for registry {} created.", this);
        }

        return locks.computeIfAbsent(id, DistributedLockImpl::new);
    }

    private ScheduledFuture<?> schedulePeriodically(Runnable task, long initialDelayMillis, long delayMillis) {
        return scheduler.scheduleWithFixedDelay(new LoggingErrorTask(task), initialDelayMillis, delayMillis, MILLISECONDS);
    }

    private void releaseDeadLocks() {
        repository.releaseDeadLocks(deadLockTimeout);
    }

    @Override
    public void close() {
        if (started.compareAndSet(true, false)) {
            boolean unlockCanceled = unlockDeadLocksFuture.cancel(true);
            LOGGER.info("Closing registry instanceId: {}. " +
                    "Cancel scheduled unlock deadlocks: {}", instanceId, unlockCanceled);
            locks.forEach((id, lock) -> {
                if (lock.tryUnlock()) {
                    LOGGER.info("Successfully unlocked lock id={} when closing registry instanceId: {}", id, instanceId);
                }
            });
            scheduler.shutdown();
        }
    }

    /**
     * @since 1.0.3
     * @deprecated use {@link DistributedLockRegistryBuilder#instanceId(String)} instead
     */
    @Deprecated
    public void setInstanceId(String instanceId) {
        this.instanceId = Validate.notEmpty(instanceId, "instanceId is empty");
    }

    /**
     * @since 1.0.3
     * @deprecated {@link DistributedLockRegistryBuilder#refreshLockInterval(Duration)} instead
     */
    @Deprecated
    public void setRefreshLockInterval(long refreshLockIntervalMillis) {
        Validate.isTrue(refreshLockIntervalMillis > 0, "refreshLockIntervalMillis > 0");
        this.refreshLockInterval = refreshLockIntervalMillis;
    }

    /**
     * @since 1.0.3
     * @deprecated {@link DistributedLockRegistryBuilder#deadLockTimeout(Duration)} instead
     */
    @Deprecated
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

        volatile ScheduledFuture<?> refreshLockFuture;

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
                        onAcquiredLock();
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
                    onAcquiredLock();
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
                        onAcquiredLock();
                        return true;
                    }
                    repository.awaitReleaseLock(id, until - System.currentTimeMillis());
                } while (System.currentTimeMillis() <= until);
                // cannot acquire remote lock after timeout => release local lock
                jvmLock.unlock();
                return false;
            }, new AcquireLockRecovery<>(true));
        }

        private void onAcquiredLock() {
            heldByCurrentProcess = true;
            refreshLockFuture = schedulePeriodically(this::refreshActiveLock, refreshLockInterval, refreshLockInterval);
        }

        private void refreshActiveLock() {
            repository.refreshActiveLock(id, instanceId);
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
                    onReleasedLock();
                    return null;
                }, (exception, context) -> {
                    onReleasedLock();
                    throw new CannotRelease(id, instanceId, exception);
                });
            }
            jvmLock.unlock();
        }

        private void onReleasedLock() {
            refreshLockFuture.cancel(true);
            heldByCurrentProcess = false;
            locks.remove(id); // lock is no more used => remove it
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

    @RequiredArgsConstructor
    static final class DisabledShutdownScheduler implements ScheduledExecutorService {

        @Delegate
        private final ScheduledExecutorService delegate;

        static DisabledShutdownScheduler of(ScheduledExecutorService delegate) {
            return delegate == null ? null : new DisabledShutdownScheduler(delegate);
        }

        @Override
        public void shutdown() {
            // Do nothing
        }

        @Override
        public List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }
    }
}
