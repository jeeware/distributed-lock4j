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

import io.github.jeeware.cloud.lock4j.function.Invocation;
import io.github.jeeware.cloud.lock4j.spring.autoconfigure.DistributedLockAutoConfiguration;
import io.github.jeeware.cloud.lock4j.spring.autoconfigure.DistributedLockProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link DistributedLockRegistry} according to a specific
 * {@link LockRepository} implementation specified by {@code cloud.lock4j.type}
 * property
 *
 * @author hbourada
 * @version 1.0
 */
@Slf4j
@ContextConfiguration(classes = DistributedLockRegistryTest.Config.class)
abstract class DistributedLockRegistryTest {

    private static final int MIN_TIME = 1000; // 1 seconds

    private static final int MAX_TIME = 2000; // 2 seconds

    @Autowired
    DistributedLockRegistry lockRegistry;

    @Autowired
    AsyncListenableTaskExecutor taskExecutor;

    @Autowired
    LockRepository repository;

    @Autowired
    ThreadPoolTaskScheduler scheduler;

    @Autowired
    DistributedLockProperties properties;

    @Autowired
    Retryer retryer;

    private final String lockName = "lock-" + randomAlphanumeric(10);

    private final int nTasks = RandomUtils.nextInt(2, 10);

    private final List<String> inputs = new ArrayList<>(nTasks);

    private final BlockingQueue<String> singletonQueue = new ArrayBlockingQueue<>(1);

    private final CountDownLatch latch = new CountDownLatch(nTasks);

    private final AtomicReference<Throwable> exceptionRef = new AtomicReference<>();

    private final Set<String> results = new CopyOnWriteArraySet<>();

    @BeforeEach
    void setUp() {
        for (int i = 0; i < nTasks; i++) {
            inputs.add(randomAlphanumeric(20));
        }
        log.info("Creating {} inputs data to be processed by concurrent tasks", nTasks);
    }

    @Test
    void lockByConcurrentThreadsShouldAcquireSequentially() throws InterruptedException {
        final DistributedLock lock = lockRegistry.getLock(lockName);

        for (int i = 0; i < nTasks; i++) {
            final String input = inputs.get(i);
            submitListenableTask(() -> runTask(lock, input, Invocation.of(DistributedLock::lock)));
        }

        waitAndAssert(false);
    }

    @Test
    void lockByConcurrentProcessesShouldAcquireSequentially() throws InterruptedException {
        /*
         * We simulate concurrent processes by creating many lock registries as
         * there is a single registry by process in a normal spring application
         * then we acquire a lock with same name.
         */
        final DistributedLockRegistry[] processLockRegistries = createLockRegistries();

        for (int i = 0; i < nTasks; i++) {
            final DistributedLock lock = processLockRegistries[i].getLock(lockName);
            final String input = inputs.get(i);
            submitListenableTask(() -> runTask(lock, input, Invocation.of(DistributedLock::lock)));
        }

        waitAndAssert(false);
    }

    @Test
    void tryLockByConcurrentProcessesShouldRunTaskOnce() throws InterruptedException {
        final DistributedLockRegistry[] processLockRegistries = createLockRegistries();

        for (int i = 0; i < nTasks; i++) {
            final DistributedLock lock = processLockRegistries[i].getLock(lockName);
            final String input = inputs.get(i);
            submitListenableTask(() -> runTask(lock, input, DistributedLock::tryLock));
        }

        waitAndAssert(true);
    }

    @Test
    void tryLockTimeoutByConcurrentProcessesShouldRunAllTasks() throws InterruptedException {
        final DistributedLockRegistry[] processLockRegistries = createLockRegistries();

        for (int i = 0; i < nTasks; i++) {
            final DistributedLock lock = processLockRegistries[i].getLock(lockName);
            final String input = inputs.get(i);
            submitListenableTask(() -> runTask(lock, input, l -> l.tryLock(nTasks * MAX_TIME, MILLISECONDS)));
        }

        waitAndAssert(false);
    }

    @Test
    void lockInterruptiblyByConcurrentProcessesShouldRunAllTasksExceptInterrupted() throws InterruptedException {
        final InterruptibleLockRepository interruptibleRepository = new InterruptibleLockRepository(repository);
        final DistributedLockRegistry[] processLockRegistries = createLockRegistries(nTasks, interruptibleRepository);

        for (int i = 0; i < nTasks; i++) {
            final DistributedLock lock = processLockRegistries[i].getLock(lockName);
            final String input = inputs.get(i);
            submitListenableTask(() -> runTask(lock, input, l -> {
                l.lockInterruptibly();
                return null;
            }));
        }

        final Thread awaitingThread = interruptibleRepository.interruptAnyAwaitingThread();
        log.info("Thread: {} was interrupted", awaitingThread.getName());

        latch.await();

        assertThat(results).hasSize(nTasks - 1); // all except interrupted
        assertThat(inputs).containsAll(results);
        assertThat(exceptionRef.get()).isInstanceOf(InterruptedException.class);
    }

    @Test
    void onDeadLockShouldAcquireAfterReleasedBySchedulerTask() throws InterruptedException {
        final DistributedLockRegistry[] processLockRegistries = createLockRegistries(nTasks + 1, repository);
        final DistributedLock deadlock = processLockRegistries[0].getLock(lockName);
        final BlockingQueue<ThreadAndLock> queue = new ArrayBlockingQueue<>(1);

        taskExecutor.execute(() -> runTaskCausingDeadlock(deadlock, queue));

        for (int i = 0; i < nTasks; i++) {
            final DistributedLock lock = processLockRegistries[i + 1].getLock(lockName);
            final String input = inputs.get(i);
            submitListenableTask(() -> runTask(lock, input, Invocation.of(DistributedLock::lock)));
        }

        // simulate process crash by stopping task and closing registry
        final ThreadAndLock tal = queue.take();
        final Thread thread = tal.thread;
        final DistributedLockRegistry registry = ((DistributedLockRegistry.DistributedLockImpl) tal.lock).getRegistry();
        log.info("Interrupting thread: {}", thread.getName());
        thread.interrupt();
        registry.close();

        waitAndAssert(false);
    }

    @SneakyThrows
    private void runTaskCausingDeadlock(DistributedLock lock, BlockingQueue<ThreadAndLock> queue) {
        lock.lock();
        log.info("Start execute task causing deadlock");
        queue.add(new ThreadAndLock(Thread.currentThread(), lock));
        MILLISECONDS.sleep(2 * properties.getDeadLockTimeout());
        log.info("End execute task causing deadlock");
        lock.unlock(); // on error this is not executed => cause deadlock
    }

    @Test
    void newConditionShouldFail() {
        final DistributedLock lock = lockRegistry.getLock(lockName);
        assertThatThrownBy(lock::newCondition).isInstanceOf(UnsupportedOperationException.class);
    }

    private DistributedLockRegistry[] createLockRegistries() {
        return createLockRegistries(nTasks, repository);
    }

    private DistributedLockRegistry[] createLockRegistries(int count, LockRepository repository) {
        final ScheduledExecutorService scheduledExecutor = scheduler.getScheduledExecutor();
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    DistributedLockRegistry registry = new DistributedLockRegistry(repository, scheduledExecutor, retryer);
                    registry.setRefreshLockInterval(properties.getRefreshLockInterval());
                    registry.setDeadLockTimeout(properties.getDeadLockTimeout());
                    return registry;
                })
                .toArray(DistributedLockRegistry[]::new);
    }

    private void submitListenableTask(Callable<String> task) {
        taskExecutor.submitListenable(task).addCallback(r -> {
            if (r != null) {
                results.add(r);
            }
//            log.debug("on success with r={}, latch={}", r, latch);
            latch.countDown();
        }, e -> {
            exceptionRef.compareAndSet(null, e);
//            log.debug("on failure with exception={}, latch={}", e, latch);
            latch.countDown();
        });
    }

    private String runTask(DistributedLock lock, String value, Invocation invocation) throws InterruptedException {
        final Object hasLock = invocation.apply(lock);
        // cancel task directly in case tryLock returning false
        if (Boolean.FALSE.equals(hasLock)) {
            log.info("task {} has not acquired lock: {}", value, lock);
            return null;
        }

        /*
         * bounded queue ensure that no other thread / process can add elements
         * while lock is acquired by current thread / process.
         */
        try {
            log.info("Start execute task {}", value);
            singletonQueue.add(value);
            assertThat(lock.isHeldByCurrentProcess()).as("lock %s, value=%s", lock, value).isTrue();
            MILLISECONDS.sleep(RandomUtils.nextInt(MIN_TIME, MAX_TIME));
            log.info("End execute task {}", singletonQueue.element());
            return singletonQueue.remove();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ie);
        } finally {
            lock.unlock();
        }
    }

    private void waitAndAssert(boolean hasSingleResult) throws InterruptedException {
        latch.await(); // wait tasks termination

        if (hasSingleResult) {
            assertThat(results).hasSize(1); // only 1 task is successful
            assertThat(inputs).containsAll(results);
        } else {
            assertThat(results).hasSize(nTasks); // all tasks are successful
            assertThat(results).containsExactlyInAnyOrderElementsOf(inputs);
        }

        assertThat(exceptionRef.get()).isNull(); // no interference
    }

    @RequiredArgsConstructor
    static final class ThreadAndLock {
        final Thread thread;
        final DistributedLock lock;
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(DistributedLockAutoConfiguration.class)
    static class Config {

        @Bean
        public ThreadPoolTaskScheduler taskScheduler() {
            return new TaskSchedulerBuilder().poolSize(2).build();
        }

        @Bean
        public ThreadPoolTaskExecutor taskExecutor() {
            return new TaskExecutorBuilder().corePoolSize(4).maxPoolSize(4).build();
        }
    }

}
