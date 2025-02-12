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

package io.github.jeeware.cloud.lock4j.support;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;


/**
 * Wrap a runnable to catch any potential exception permitting to
 * {@link java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)} to
 * reschedule subsequent executions of the task even if an error occurs.
 */
@Slf4j
@RequiredArgsConstructor
public final class LoggingErrorTask implements Runnable {

    @NonNull
    private final Runnable delegate;

    public void run() {
        try {
            delegate.run();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
