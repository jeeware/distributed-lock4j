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

import com.mongodb.MongoException;
import io.github.jeeware.cloud.lock4j.Retryer;
import io.github.jeeware.cloud.lock4j.Retryer.Context;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.AccessDeniedException;
import java.nio.file.ClosedFileSystemException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleRetryerTest {

    @Test
    void retryForExactDeclaredRetryableExceptionsShouldReturnTrue() {
        Retryer retryer = retryer(3, IOException.class);
        Context context = retryer.createContext();

        boolean allRetry = IntStream.range(0, 3).mapToObj(i -> new IOException(String.valueOf(i)))
                .allMatch(e -> retryer.shouldRetryFor(e, context));

        assertThat(allRetry).isTrue();
        assertThat(context.getRetryCount()).isZero();
    }

    @Test
    void retryForDescendentRetryableExceptionsShouldReturnTrue() {
        Retryer retryer = retryer(3, IOException.class);
        Context context = retryer.createContext();

        boolean allRetry = Stream.of(new SocketTimeoutException(), new SocketException(), new ConnectException())
                .allMatch(e -> retryer.shouldRetryFor(e, context));

        assertThat(allRetry).isTrue();
    }

    @Test
    void retryForNonRetryableException() {
        Retryer retryer = SimpleRetryer.builder().maxRetry(1).nonRetryableException(IOException.class)
                .nonRetryableException(IllegalStateException.class).build();
        Context context = retryer.createContext();

        boolean noneMatch = Stream.of(new ClosedFileSystemException(), new AccessDeniedException(""))
                .noneMatch(e -> retryer.shouldRetryFor(e, context));

        assertThat(noneMatch).isTrue();
    }

    @Test
    void retryForRetryableCauseShouldReturnTrue() {
        Retryer retryer = retryer(1, IOException.class);
        Context context = retryer.createContext();
        Exception exception = new DataAccessResourceFailureException("",
                new MongoException("", new SocketTimeoutException("timeout")));

        boolean retry = retryer.shouldRetryFor(exception, context);

        assertThat(retry).isTrue();
    }

    @Test
    void applyWithCallableShouldSuccess() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Retryer retryer = retryer(1, ArithmeticException.class);

        int result = retryer.apply(() -> 1 / counter.getAndIncrement(), null);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void applyWithCallableShouldThrow() {
        AtomicInteger counter = new AtomicInteger(0);
        Retryer retryer = retryer(1, ArithmeticException.class);

        assertThatThrownBy(() -> retryer.apply(() -> 1 / (counter.get() * (counter.getAndIncrement() - 1)), null))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("by zero");
    }

    @Test
    void applyWithCallableAndRecoveryShouldSuccess() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Retryer retryer = retryer(1, ArithmeticException.class);

        int result = retryer.apply(() -> 1 / (counter.get() * (counter.getAndIncrement() - 1)), (ex, context) -> 5);

        assertThat(result).isEqualTo(5);
    }

    @SafeVarargs
    private static SimpleRetryer retryer(int maxRetry, Class<? extends Exception>... retryableExceptions) {
        return SimpleRetryer.builder().maxRetry(maxRetry).retryableExceptions(Arrays.asList(retryableExceptions)).build();
    }

}
