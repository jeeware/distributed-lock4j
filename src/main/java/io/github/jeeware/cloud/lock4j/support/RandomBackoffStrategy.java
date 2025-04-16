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

import io.github.jeeware.cloud.lock4j.BackoffStrategy;

import java.time.Duration;
import java.util.Random;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * {@link BackoffStrategy} where {@link #sleep()} causes the current thread to sleep a random duration for each call.
 *
 * @author hbourada
 */
public class RandomBackoffStrategy implements BackoffStrategy {

    private final Random random;
    private final long minSleepInMillis;
    private final long maxMinDiffInMillis;

    public RandomBackoffStrategy(Random random, long minSleepInMillis, long maxSleepInMillis) {
        notNull(random, "random must not be null");
        isTrue(minSleepInMillis >= 0, "minSleepInMillis must be positive");
        isTrue(maxSleepInMillis >= 0, "maxSleepInMillis must be positive");
        isTrue(maxSleepInMillis >= minSleepInMillis, "maxSleepInMillis must be greater than minSleepInMillis");
        this.random = random;
        this.minSleepInMillis = minSleepInMillis;
        this.maxMinDiffInMillis = maxSleepInMillis - minSleepInMillis;
    }

    public static RandomBackoffStrategyBuilder builder() {
        return new RandomBackoffStrategyBuilder();
    }

    @Override
    public void sleep() throws InterruptedException {
        Thread.sleep(randomSleepInMillis());
    }

    @SuppressWarnings("java:S2140")
    private long randomSleepInMillis() {
        return (long) (minSleepInMillis + maxMinDiffInMillis * random.nextDouble());
    }

    public static class RandomBackoffStrategyBuilder {
        private Random random;
        private long minSleepInMillis;
        private long maxSleepInMillis;

        public RandomBackoffStrategyBuilder random(Random random) {
            this.random = random;
            return this;
        }

        public RandomBackoffStrategyBuilder minSleepDuration(Duration minSleepDuration) {
            this.minSleepInMillis = minSleepDuration.toMillis();
            return this;
        }

        public RandomBackoffStrategyBuilder maxSleepDuration(Duration maxSleepDuration) {
            this.maxSleepInMillis = maxSleepDuration.toMillis();
            return this;
        }

        public RandomBackoffStrategy build() {
            return new RandomBackoffStrategy(defaultIfNull(random, new Random()), minSleepInMillis, maxSleepInMillis);
        }

    }
}
