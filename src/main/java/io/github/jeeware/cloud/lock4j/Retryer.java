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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.Callable;

/**
 * Strategy for retrying an operation for a defined exception hierarchy.
 *
 * @author hbourada
 */
public interface Retryer {

    Retryer NEVER = new NeverRetryer();

    /**
     * @deprecated use {@link #shouldRetryFor(Exception)} instead
     */
    @Deprecated
    default boolean retryFor(Exception e) {
        return shouldRetryFor(e);
    }

    /**
     * @param e an exception raised by a task execution
     * @return true iff we can retry for the given exception, otherwise false
     * @since 1.0.2
     */
    boolean shouldRetryFor(Exception e);

    default <T> T apply(Callable<T> callable) throws Exception {
        do {
            try {
                return callable.call();
            } catch (Exception e) {
                if (!shouldRetryFor(e)) {
                    throw e;
                }
                sleep();

            }
        } while (true);
    }

    void sleep() throws InterruptedException;

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    final class NeverRetryer implements Retryer {

        @Override
        public boolean shouldRetryFor(Exception e) {
            return false;
        }

        @Override
        public void sleep() {
            // do nothing
        }

        @Override
        public <T> T apply(Callable<T> callable) throws Exception {
            return callable.call();
        }
    }
}
