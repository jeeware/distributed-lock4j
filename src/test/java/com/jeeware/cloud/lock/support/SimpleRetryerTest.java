package com.jeeware.cloud.lock.support;

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
