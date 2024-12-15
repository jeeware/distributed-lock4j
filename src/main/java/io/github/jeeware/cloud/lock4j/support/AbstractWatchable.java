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

package io.github.jeeware.cloud.lock4j.support;

import io.github.jeeware.cloud.lock4j.Watchable;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Base implementation for a watchable lock repository
 *
 * @author hbourada
 */
@Slf4j
public abstract class AbstractWatchable implements Watchable {

    private final Map<String, Semaphore> locks = new ConcurrentHashMap<>();

    protected volatile boolean active;

    @Override
    public void await(String lockId) throws InterruptedException {
        Semaphore semaphore = getSemaphore(lockId);
        semaphore.acquire();
        log.trace("Semaphore [lockId={}, {}] acquired", lockId, semaphore);
    }

    @Override
    public void await(String lockId, long timeoutMillis) throws InterruptedException {
        Semaphore semaphore = getSemaphore(lockId);
        boolean acquired = semaphore.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
        log.trace("Try acquire semaphore [lockId={}, {}] with timeout {}ms => {} ", lockId, semaphore, timeoutMillis, acquired);
    }

    @Override
    public void signal(String lockId) {
        Semaphore semaphore = getSemaphore(lockId);
        if (semaphore.availablePermits() == 0) {
            semaphore.release();
            log.trace("Semaphore [lockId={}, {}] released", lockId, semaphore);
        }
    }

    private Semaphore getSemaphore(String lock) {
        return locks.computeIfAbsent(lock, l -> new Semaphore(0));
    }

    @Override
    public boolean isActive() {
        return active;
    }

}
