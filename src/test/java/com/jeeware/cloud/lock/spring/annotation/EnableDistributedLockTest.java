/*
 * Copyright 2020-2022 Hichem BOURADA and other authors.
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

package com.jeeware.cloud.lock.spring.annotation;

import static com.jeeware.cloud.lock.spring.annotation.DistributedLock.Mode.LOCK_INTERRUPTIBLE;
import static com.jeeware.cloud.lock.spring.annotation.DistributedLock.Mode.TRY_LOCK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import com.jeeware.cloud.lock.DistributedLockRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import lombok.RequiredArgsConstructor;

/**
 * Tests for {@link EnableDistributedLock}
 * 
 * @author hbourada
 * @version 1.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = "reservation.timeout=1000")
class EnableDistributedLockTest {

    @Autowired
    ReservationService service;

    @MockBean
    ReservationRepository repository;

    @MockBean
    DistributedLockRegistry registry;

    @MockBean
    com.jeeware.cloud.lock.DistributedLock lock;

    @Test
    void testLock() {
        given(registry.getLock(anyString())).willReturn(lock);

        service.lockReservation();

        verify(repository).lockReservation("lock");
        verify(lock).unlock();
    }

    @Test
    void testLockInterruptible() {
        given(registry.getLock(anyString())).willReturn(lock);

        service.lockInterruptibleReservation();

        verify(repository).lockReservation("lockInterruptible");
        verify(lock).unlock();
    }

    @Test
    void testTryLockReturningTrue() {
        given(lock.tryLock()).willReturn(Boolean.TRUE);
        given(registry.getLock(anyString())).willReturn(lock);

        service.tryLockReservation();

        verify(repository).lockReservation("tryLock");
        verify(lock).unlock();
    }

    @Test
    void testTryLockReturningFalse() {
        given(lock.tryLock()).willReturn(Boolean.FALSE);
        given(registry.getLock(anyString())).willReturn(lock);

        service.tryLockReservation();

        verify(repository, never()).lockReservation("tryLock");
        verify(lock, never()).unlock();
    }

    @Test
    void testTryLockTimeoutExpression() throws InterruptedException {
        given(lock.tryLock(anyLong(), any())).willReturn(Boolean.TRUE);
        given(registry.getLock(anyString())).willReturn(lock);

        service.tryLockTimeoutExpressionReservation();

        verify(repository).lockReservation("tryLockTimeoutExpression");
        verify(lock).tryLock(1000, TimeUnit.MILLISECONDS);
        verify(lock).unlock();
    }

    @Test
    void testTryLockTimeout() throws InterruptedException {
        given(lock.tryLock(anyLong(), any())).willReturn(Boolean.TRUE);
        given(registry.getLock(anyString())).willReturn(lock);

        service.tryLockTimeoutReservation();

        verify(repository).lockReservation("tryLockTimeout");
        verify(lock).tryLock(2000, TimeUnit.MILLISECONDS);
        verify(lock).unlock();
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

        @DistributedLock(value = "lock")
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

    }

    interface ReservationRepository {

        void lockReservation(String type);
    }
}
