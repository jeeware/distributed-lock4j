/*
 * Copyright 2020-2025 Hichem BOURADA and other authors.
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

import lombok.Getter;

/**
 * Distributed lock exception base class.
 *
 * @author hbourada
 */
@Getter
public abstract class DistributedLockException extends RuntimeException {

    private final String lockId;
    private final String instanceId;

    DistributedLockException(Throwable cause, String lockId, String instanceId) {
        super(cause);
        this.lockId = lockId;
        this.instanceId = instanceId;
    }

    @Override
    public final String getMessage() {
        return String.format("Can not %s lock id: %s for instance id: %s", this.getClass() == CannotAcquire.class ?
                "acquire" : "release", lockId, instanceId);
    }

    public static DistributedLockException create(boolean acquire, String lockId, String instanceId, Throwable cause) {
        return acquire ? new CannotAcquire(lockId, instanceId, cause) : new CannotRelease(lockId, instanceId, cause);
    }

    /**
     * {@link DistributedLockException} raised when a distributed lock can not be acquired for some reason.
     */
    private static final class CannotAcquire extends DistributedLockException {

        public CannotAcquire(String lockId, String instanceId, Throwable cause) {
            super(cause, lockId, instanceId);
        }
    }

    /**
     * {@link DistributedLockException} raised when a distributed lock can not be released for some reason
     */
    private static final class CannotRelease extends DistributedLockException {

        public CannotRelease(String lockId, String instanceId, Throwable cause) {
            super(cause, lockId, instanceId);
        }
    }
}
