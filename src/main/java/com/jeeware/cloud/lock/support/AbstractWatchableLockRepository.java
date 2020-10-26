package com.jeeware.cloud.lock.support;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jeeware.cloud.lock.Watchable;
import com.jeeware.cloud.lock.WatchableLockRepository;
import com.jeeware.cloud.lock.function.WatchableThreadFactory;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watchable lock repository base implementation.
 * 
 * @author hbourada
 */
public abstract class AbstractWatchableLockRepository implements WatchableLockRepository {

    private final AtomicBoolean closed = new AtomicBoolean();

    private WatchableThreadFactory threadFactory = WatchableThreadFactory.Default.INSTANCE;

    private Watchable watchable;

    private Thread watcher;

    public void start() {
        watchable = createWatchable();
        watcher = threadFactory.newThread(watchable);
        watcher.start();
    }

    public void setThreadFactory(@NonNull ThreadFactory threadFactory) {
        this.threadFactory = WatchableThreadFactory.of(threadFactory);
    }

    @NonNull
    protected abstract Watchable createWatchable();

    @Override
    public final void awaitReleaseLock(String lockId) throws InterruptedException {
        if (watchable.isActive()) {
            watchable.await(lockId);
        } else {
            WatchableLockRepository.super.awaitReleaseLock(lockId);
        }
    }

    @Override
    public void awaitReleaseLock(String lockId, long timeoutMillis) throws InterruptedException {
        if (watchable.isActive()) {
            watchable.await(lockId, timeoutMillis);
        } else {
            WatchableLockRepository.super.awaitReleaseLock(lockId, timeoutMillis);
        }
    }

    @Override
    public void close() {
        if (watchable != null && closed.compareAndSet(false, true)) {
            try {
                watchable.close();
            } catch (Exception e) {
                Logger logger = LoggerFactory.getLogger(getClass());
                logger.error("Error on close: {} => Interrupting watcher thread {}", e.getMessage(),
                        Thread.currentThread().getName(), e);
                watcher.interrupt();
            }
        }
    }

}
