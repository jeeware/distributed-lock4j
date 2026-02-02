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

import com.mongodb.MongoException;
import io.github.jeeware.cloud.lock4j.Retryer;
import io.github.jeeware.cloud.lock4j.Retryer.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.sql.SQLTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.from;
import static org.springframework.beans.BeanUtils.instantiateClass;

class SimpleRetryerTest {

    @ParameterizedTest
    @ValueSource(classes = {SocketTimeoutException.class, SQLTimeoutException.class, FileNotFoundException.class})
    void retryForExactRetryableExceptionShouldReturnTrue(Class<? extends Exception> retryableException) {
        Retryer retryer = retryer(retryableException);
        Context context = retryer.createContext();
        Exception exception = instantiateClass(retryableException);

        assertThat(retryer.shouldRetryFor(exception, context)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(classes = {SocketTimeoutException.class, ConnectException.class, FileNotFoundException.class})
    void retryForDescendentRetryableExceptionShouldReturnTrue(Class<? extends Exception> retryableException) {
        Retryer retryer = retryer(IOException.class);
        Context context = retryer.createContext();
        Exception exception = instantiateClass(retryableException);

        assertThat(retryer.shouldRetryFor(exception, context)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(classes = {InterruptedException.class, BindException.class, NullPointerException.class})
    void retryForNonRetryableExceptionShouldReturnFalse(Class<? extends Exception> nonRetryableException) {
        Retryer retryer = SimpleRetryer.builder().maxRetry(1).nonRetryableException(nonRetryableException).build();
        Context context = retryer.createContext();
        Exception exception = instantiateClass(nonRetryableException);

        assertThat(retryer.shouldRetryFor(exception, context)).isFalse();
    }

    @Test
    void incrementRetryCountForSimpleRetryerContextWhenMaxRetryIsOneShouldReturnTerminatedTrue() {
        Context context = SimpleRetryer.builder().maxRetry(1).build().createContext();

        context.incrementRetryCount();

        assertThat(context)
                .returns(1, from(Context::getRetryCount))
                .returns(true, from(Context::isTerminated));
    }

    @Test
    void retryForRetryableCauseShouldReturnTrue() {
        Retryer retryer = retryer(IOException.class);
        Context context = retryer.createContext();
        Exception exception = new DataAccessResourceFailureException("",
                new MongoException("", new SocketTimeoutException("timeout")));

        assertThat(retryer.shouldRetryFor(exception, context)).isTrue();
    }

    @Test
    void applyWithCallableShouldSuccess() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Retryer retryer = retryer(ArithmeticException.class);

        int result = retryer.apply(() -> 1 / counter.getAndIncrement(), null);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void applyWithCallableShouldThrow() {
        AtomicInteger counter = new AtomicInteger(0);
        Retryer retryer = retryer(ArithmeticException.class);

        assertThatThrownBy(() -> retryer.apply(() -> 1 / (counter.get() * (counter.getAndIncrement() - 1)), null))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("by zero");
    }

    @Test
    void applyWithCallableAndRecoveryShouldSuccess() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Retryer retryer = retryer(ArithmeticException.class);

        int result = retryer.apply(() -> 1 / (counter.get() * (counter.getAndIncrement() - 1)), (ex, context) -> 5);

        assertThat(result).isEqualTo(5);
    }

    private static SimpleRetryer retryer(Class<? extends Exception> retryableException) {
        return SimpleRetryer.builder().maxRetry(1).retryableException(retryableException).build();
    }

}
