package com.jeeware.cloud.lock;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
final class InterruptibleLockRepository implements LockRepository {

    @Delegate(excludes = AwaitReleaseLock.class)
    private final LockRepository delegate;

    private final BlockingQueue<Thread> currentlyAwaitingThreads = new LinkedBlockingQueue<>();

    public void awaitReleaseLock(String lockId) throws InterruptedException {
        try {
            currentlyAwaitingThreads.add(Thread.currentThread());
            delegate.awaitReleaseLock(lockId);
        } finally {
            currentlyAwaitingThreads.remove(Thread.currentThread());
        }
    }

    @Override
    public void awaitReleaseLock(String lockId, long timeoutMillis) throws InterruptedException {
        try {
            currentlyAwaitingThreads.add(Thread.currentThread());
            delegate.awaitReleaseLock(lockId, timeoutMillis);
        } finally {
            currentlyAwaitingThreads.remove(Thread.currentThread());

        }
    }

    public Thread interruptAnyAwaitingThread() throws InterruptedException {
        Thread thread = currentlyAwaitingThreads.take();
        thread.interrupt();
        return thread;
    }

    private interface AwaitReleaseLock {

        void awaitReleaseLock(String lockId) throws InterruptedException;

        void awaitReleaseLock(String lockId, long timeoutMillis) throws InterruptedException;
    }

}
