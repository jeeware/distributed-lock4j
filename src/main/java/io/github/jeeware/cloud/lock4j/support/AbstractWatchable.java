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
