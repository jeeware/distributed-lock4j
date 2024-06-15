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

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleRetryerTest {

    @Test
    void retryForExactDeclaredException() {
        final int maxRetry = 3;
        final SimpleRetryer retryer = new SimpleRetryer(maxRetry, IOException.class);

        boolean allRetry = IntStream.range(0, maxRetry)
                .mapToObj(i -> new IOException(String.valueOf(i)))
                .allMatch(retryer::retryFor);

        Assertions.assertThat(allRetry).isTrue();
    }

    @Test
    void retryForDescendentException() {
        final int maxRetry = 4;
        final SimpleRetryer retryer = new SimpleRetryer(maxRetry, IOException.class);

        boolean allRetry = Stream.of(new SocketTimeoutException(), new ConnectException(), new SocketException(),
                new ConnectException())
                .allMatch(retryer::retryFor);

        Assertions.assertThat(allRetry).isTrue();
    }

    @Test
    void retryForNonRetryableException() {
        final int maxRetry = 2;
        final SimpleRetryer retryer = new SimpleRetryer(maxRetry, IOException.class);

        boolean allRetry = Stream.of(new IllegalStateException(), new IOException())
                .allMatch(retryer::retryFor);

        Assertions.assertThat(allRetry).isFalse();
    }

}
