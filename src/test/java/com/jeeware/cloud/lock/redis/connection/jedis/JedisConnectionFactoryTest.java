package com.jeeware.cloud.lock.redis.connection.jedis;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.jeeware.cloud.lock.redis.connection.RedisConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.util.SocketUtils;

import com.jeeware.cloud.lock.redis.connection.MessageListener;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.embedded.RedisServer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
class JedisConnectionFactoryTest {

    static final String CONFIG_NAME = "notify-keyspace-events";

    final int port = SocketUtils.findAvailableTcpPort();
    final RedisServer redisServer = new RedisServer(port);
    final JedisPool jedisPool = new JedisPool("localhost", port);
    final JedisConnectionFactory connectionFactory = JedisConnectionFactory.of(jedisPool);
    final ExecutorService executorService = Executors.newFixedThreadPool(2);
    List<String> keys;
    CountDownLatch latch;

    @BeforeAll
    void beforeAll() {
        redisServer.start();
    }

    @AfterAll
    void afterAll() {
        redisServer.stop();
        jedisPool.close();
        executorService.shutdownNow();
    }

    @BeforeEach
    void beforeEach() {
        keys = IntStream.range(0, nextInt(1, 50))
                .mapToObj(i -> randomAlphabetic(10))
                .collect(Collectors.toList());

        try (Jedis jedis = jedisPool.getResource()) {
            keys.forEach(k -> jedis.set(k, "test"));
        }
        latch = new CountDownLatch(1);
    }

    @Test
    void pSubscribeSuccess() throws InterruptedException {

        try (Jedis jedis = jedisPool.getResource()) {
            log.info("keys={}", jedis.keys("*"));
        }

        try (RedisConnection connection = connectionFactory.getConnection()) {
            final Map<String, String> configs = connection.configGet(CONFIG_NAME);

            assertThat(configs.values()).first().asString().isEmpty();

            final String reply = connection.configSet(CONFIG_NAME, "Egx");

            assertThat(reply).isEqualTo("OK");

            final MyMessageListener listener = new MyMessageListener();
            executorService.submit(() -> addListener(connection, listener));
            executorService.submit(() -> deleteKeysAndUnsubscribe(connection));
            latch.await(5, TimeUnit.SECONDS);
//            assertThat(listener.keys).isEqualTo(keys);
            log.info("listener.keys[{}]={}", listener.keys.size(), listener.keys);
            log.info("this.keys[{}]={}", keys.size(),  keys);

        }

    }

    void deleteKeysAndUnsubscribe(RedisConnection connection) {
        sleep();
        try (Jedis jedis = jedisPool.getResource()) {
            keys.forEach(k -> {
                final Long r = jedis.del(k);
                log.debug("del key {}={}:", k, r);
                sleep();
            });
        } finally {
            connection.pUnsubscribe();
        }
    }

    private void sleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException ignore) {
        }
    }

    void addListener(RedisConnection connection, MessageListener listener) {
        connection.pSubscribe(listener, "__keyevent@0__:*");
        latch.countDown();
    }

    @Slf4j
    static class MyMessageListener implements MessageListener {

        final List<String> keys = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void onMessage(String pattern, String channel, String message) {
            log.info("channel={}, message={}", channel, message);
            keys.add(message);
        }
    }
}