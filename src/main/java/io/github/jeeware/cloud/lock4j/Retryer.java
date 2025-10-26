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
     * @deprecated use {@link #shouldRetryFor(Exception, Context)} instead
     */
    @Deprecated
    default boolean retryFor(Exception e) {
        return shouldRetryFor(e, Context.EMPTY);
    }

    /**
     * @param e       an exception raised by a task execution
     * @param context retry context
     * @return true iff we can retry for the given exception, otherwise false
     * @since 1.0.2
     */
    boolean shouldRetryFor(Exception e, Context context);

    /**
     * @param retryTask    task which will be repeated until {@link #shouldRetryFor(Exception, Context)} returns false
     * @param recoveryTask recovery task executed when retries exhausted
     * @param <T>          result type
     * @return result returned by the retryTask or recoverTask call
     * @throws Exception an exception if retry exhausted and recover is null
     * @since 1.0.2
     */
    default <T> T apply(Callable<T> retryTask, Recovery<T> recoveryTask) throws Exception {
        Context context = createContext();
        do {
            try {
                return retryTask.call();
            } catch (Exception e) {
                if (context.isTerminated() || !shouldRetryFor(e, context)) {
                    if (recoveryTask != null) {
                        return recoveryTask.recover(e, context);
                    }
                    throw e;
                }
                context.incrementRetryCount();
                sleep(context);
            }
        } while (true);
    }

    /**
     * creates a retry context
     *
     * @return a retry context recording infos between retries
     */
    Context createContext();

    /**
     * Mark a pause between retries
     *
     * @param context
     * @throws InterruptedException
     */
    void sleep(Context context) throws InterruptedException;

    /**
     * Recovery callback executed after exhausting all retries
     *
     * @param <T> recover returning type
     */
    @FunctionalInterface
    interface Recovery<T> {

        @SuppressWarnings("java:S112")
        T recover(Exception exception, Context context) throws Exception;
    }

    /**
     * Contextual retry object
     */
    interface Context {

        Context EMPTY = new EmptyContext();

        int getRetryCount();

        void incrementRetryCount();

        boolean isTerminated();
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    final class EmptyContext implements Context {

        @Override
        public int getRetryCount() {
            return 0;
        }

        @Override
        public void incrementRetryCount() {
            // No op
        }

        @Override
        public boolean isTerminated() {
            return true;
        }

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    final class NeverRetryer implements Retryer {

        @Override
        public boolean shouldRetryFor(Exception e, Context context) {
            return false;
        }

        @Override
        public void sleep(Context context) {
            // do nothing
        }

        @Override
        public <T> T apply(Callable<T> callable, Recovery<T> recoveryTask) throws Exception {
            if (recoveryTask == null) {
                return callable.call();
            }
            try {
                return callable.call();
            } catch (Exception e) {
                return recoveryTask.recover(e, Context.EMPTY);
            }
        }

        @Override
        public Context createContext() {
            return Context.EMPTY;
        }
    }

}
