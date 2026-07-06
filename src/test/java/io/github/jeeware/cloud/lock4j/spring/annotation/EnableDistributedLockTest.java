/*
 * Copyright 2020-2026 Hichem BOURADA and other authors.
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

package io.github.jeeware.cloud.lock4j.spring.annotation;

import io.github.jeeware.cloud.lock4j.DistributedLockRegistry;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.TimeUnit;

import static io.github.jeeware.cloud.lock4j.spring.annotation.DistributedLock.Mode.LOCK;
import static io.github.jeeware.cloud.lock4j.spring.annotation.DistributedLock.Mode.LOCK_INTERRUPTIBLE;
import static io.github.jeeware.cloud.lock4j.spring.annotation.DistributedLock.Mode.TRY_LOCK;
import static io.github.jeeware.cloud.lock4j.spring.annotation.DistributedLock.Mode.TRY_LOCK_WITH_CLOCK_SKEW;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link EnableDistributedLock}
 *
 * @author hbourada
 * @version 1.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"reservation.timeout=1000", "reservation.clock-skew=1s"})
class EnableDistributedLockTest {

    @Autowired
    ReservationService service;

    @MockBean
    ReservationRepository repository;

    @MockBean
    DistributedLockRegistry registry;

    @MockBean
    io.github.jeeware.cloud.lock4j.DistributedLock lock;

    @BeforeEach
    void setUp() {
        given(registry.getLock(anyString())).willReturn(lock);
    }

    @Test
    void testLock() {
        service.lockReservation();

        verify(repository).lockReservation("lock");
        verify(lock).lock();
        verify(lock).unlock();
    }

    @Test
    void testLockInterruptible() throws InterruptedException {
        service.lockInterruptibleReservation();

        verify(repository).lockReservation("lockInterruptible");
        verify(lock).lockInterruptibly();
        verify(lock).unlock();
    }

    @Test
    void testTryLockReturningTrue() {
        given(lock.tryLock()).willReturn(Boolean.TRUE);

        service.tryLockReservation();

        verify(repository).lockReservation("tryLock");
        verify(lock).tryLock();
        verify(lock).unlock();
    }

    @Test
    void testTryLockReturningFalse() {
        given(lock.tryLock()).willReturn(Boolean.FALSE);

        service.tryLockReservation();

        verify(repository, never()).lockReservation("tryLock");
        verify(lock).tryLock();
        verify(lock, never()).unlock();
    }

    @Test
    void testTryLockTimeoutExpression() throws InterruptedException {
        given(lock.tryLock(anyLong(), any())).willReturn(Boolean.TRUE);

        service.tryLockTimeoutExpressionReservation();

        verify(repository).lockReservation("tryLockTimeoutExpression");
        verify(lock).tryLock(1000, TimeUnit.MILLISECONDS);
        verify(lock).unlock();
    }

    @Test
    void testTryLockTimeout() throws InterruptedException {
        given(lock.tryLock(anyLong(), any())).willReturn(Boolean.TRUE);

        service.tryLockTimeoutReservation();

        verify(repository).lockReservation("tryLockTimeout");
        verify(lock).tryLock(2000, TimeUnit.MILLISECONDS);
        verify(lock).unlock();
    }

    @Test
    void testTryLockWithClockSkew() {
        given(lock.tryLockWithClockSkew(anyLong(), any())).willReturn(Boolean.TRUE);

        service.tryLockWithClockSkewReservation();

        verify(repository).lockReservation("tryLockWithClockSkew");
        verify(lock).tryLockWithClockSkew(2000, TimeUnit.MILLISECONDS);
        verify(lock).unlock();
    }

    @Test
    void testTryLockWithClockSkewExpression() {
        given(lock.tryLockWithClockSkew(anyLong(), any())).willReturn(Boolean.TRUE);

        service.tryLockWithClockSkewExpressionReservation();

        verify(repository).lockReservation("tryLockWithClockSkewExpression");
        verify(lock).tryLockWithClockSkew(1000, TimeUnit.MILLISECONDS);
        verify(lock).unlock();
    }

    @Test
    void testDefaultModeWithTimeout() throws InterruptedException {
        given(lock.tryLock(anyLong(), any())).willReturn(Boolean.TRUE);

        service.lockReservationWithDefaultModeWithTimeout();

        verify(repository).lockReservation("defaultModeWithTimeout");
        verify(lock).tryLock(500, TimeUnit.MILLISECONDS);
        verify(lock).unlock();
    }

    @Test
    void testDefaultModeWithClockSkew() {
        given(lock.tryLockWithClockSkew(anyLong(), any())).willReturn(Boolean.TRUE);

        service.lockReservationWithDefaultModeWithClockSkew();

        verify(repository).lockReservation("defaultModeWithClockSkew");
        verify(lock).tryLockWithClockSkew(300_000, TimeUnit.MILLISECONDS);
        verify(lock).unlock();
    }

    @Test
    void testDefaultModeWithScheduled() {
        given(lock.tryLock()).willReturn(Boolean.TRUE);

        service.lockReservationWithDefaultModeWithScheduled();

        verify(repository).lockReservation("defaultModeWithScheduled");
        verify(lock).tryLock();
        verify(lock).unlock();
    }

    @Test
    void testDefaultModeWithInterruptedException() throws InterruptedException {
        service.lockReservationWithDefaultModeWithInterruptedException();

        verify(repository).lockReservationInterruptible("defaultModeWithInterruptedException");
        verify(lock).lockInterruptibly();
        verify(lock).unlock();
    }

    @Test
    void testDefaultModeWithoutAnything() {
        service.lockReservationWithDefaultModeWithoutAnything();

        verify(repository).lockReservation("defaultModeWithoutAnything");
        verify(lock).lock();
        verify(lock).unlock();
    }

    interface ReservationRepository {

        void lockReservation(String type);

        void lockReservationInterruptible(String type) throws InterruptedException;
    }

    @TestConfiguration
    @EnableDistributedLock
    static class Config {

        @Bean
        public ReservationService reservationService(ReservationRepository repository) {
            return new ReservationService(repository);
        }

    }

    @RequiredArgsConstructor
    static class ReservationService {

        final ReservationRepository repository;

        @DistributedLock(value = "lock", mode = LOCK)
        void lockReservation() {
            repository.lockReservation("lock");
        }

        @DistributedLock(id = "lockInterruptible", mode = LOCK_INTERRUPTIBLE)
        void lockInterruptibleReservation() {
            repository.lockReservation("lockInterruptible");
        }

        @DistributedLock(id = "tryLock", mode = TRY_LOCK)
        void tryLockReservation() {
            repository.lockReservation("tryLock");
        }

        @DistributedLock(id = "tryLockTimeout", mode = TRY_LOCK, timeout = "2000")
        void tryLockTimeoutReservation() {
            repository.lockReservation("tryLockTimeout");
        }

        @DistributedLock(id = "tryLockTimeoutExpression", mode = TRY_LOCK, timeout = "${reservation.timeout}")
        void tryLockTimeoutExpressionReservation() {
            repository.lockReservation("tryLockTimeoutExpression");
        }

        @DistributedLock(id = "tryLockWithClockSkew", mode = TRY_LOCK_WITH_CLOCK_SKEW, clockSkew = "2000ms")
        void tryLockWithClockSkewReservation() {
            repository.lockReservation("tryLockWithClockSkew");
        }

        @DistributedLock(id = "tryLockWithClockSkewExpression", mode = TRY_LOCK_WITH_CLOCK_SKEW, clockSkew = "${reservation.clock-skew}")
        void tryLockWithClockSkewExpressionReservation() {
            repository.lockReservation("tryLockWithClockSkewExpression");
        }

        @DistributedLock(id = "defaultModeWithTimeout", timeout = "500ms")
        void lockReservationWithDefaultModeWithTimeout() {
            repository.lockReservation("defaultModeWithTimeout");
        }

        @DistributedLock(id = "defaultModeWithClockSkew", clockSkew = "5m")
        void lockReservationWithDefaultModeWithClockSkew() {
            repository.lockReservation("defaultModeWithClockSkew");
        }

        @Scheduled(fixedRate = 60)
        @DistributedLock(id = "defaultModeWithScheduled")
        void lockReservationWithDefaultModeWithScheduled() {
            repository.lockReservation("defaultModeWithScheduled");
        }

        @DistributedLock(id = "defaultModeWithInterruptedException")
        void lockReservationWithDefaultModeWithInterruptedException() throws InterruptedException {
            repository.lockReservationInterruptible("defaultModeWithInterruptedException");
        }

        @DistributedLock(id = "defaultModeWithoutAnything")
        void lockReservationWithDefaultModeWithoutAnything() {
            repository.lockReservation("defaultModeWithoutAnything");
        }

    }
}
