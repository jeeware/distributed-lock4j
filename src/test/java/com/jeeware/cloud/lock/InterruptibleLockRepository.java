/*
 * Copyright 2020-2022 Hichem BOURADA and other authors.
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
