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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleRetryerTest {

    @Test
    void retryForExactDeclaredException() {
        final SimpleRetryer retryer = retryer(3, IOException.class);

        boolean allRetry = IntStream.range(0, 3)
                .mapToObj(i -> new IOException(String.valueOf(i)))
                .allMatch(retryer::shouldRetryFor);

        assertThat(allRetry).isTrue();
    }

    @Test
    void retryForDescendentException() {
        final SimpleRetryer retryer = retryer(4, IOException.class);

        boolean allRetry = Stream.of(new SocketTimeoutException(), new ConnectException(), new SocketException(),
                        new ConnectException())
                .allMatch(retryer::shouldRetryFor);

        assertThat(allRetry).isTrue();
    }

    @Test
    void retryForNonRetryableException() {
        final SimpleRetryer retryer = retryer(2, IOException.class);

        boolean allRetry = Stream.of(new IllegalStateException(), new IOException())
                .allMatch(retryer::shouldRetryFor);

        assertThat(allRetry).isFalse();
    }

    @SafeVarargs
    private static SimpleRetryer retryer(int maxRetry, Class<? extends Exception>... exceptionTypes) {
        return SimpleRetryer.builder()
                .maxRetry(maxRetry)
                .backoffStrategy(RandomBackoffStrategy.builder().maxSleepDuration(Duration.ofSeconds(1)).build())
                .retryableExceptions(Arrays.asList(exceptionTypes))
                .build();
    }

}
