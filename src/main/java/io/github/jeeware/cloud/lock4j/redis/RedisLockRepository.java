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

package io.github.jeeware.cloud.lock4j.redis;

import io.github.jeeware.cloud.lock4j.LockRepository;
import io.github.jeeware.cloud.lock4j.Watchable;
import io.github.jeeware.cloud.lock4j.redis.connection.MessageListener;
import io.github.jeeware.cloud.lock4j.redis.connection.RedisConnection;
import io.github.jeeware.cloud.lock4j.redis.connection.RedisConnectionFactory;
import io.github.jeeware.cloud.lock4j.redis.script.RedisLockScripts;
import io.github.jeeware.cloud.lock4j.redis.script.RedisScript;
import io.github.jeeware.cloud.lock4j.redis.script.ScriptExecutor;
import io.github.jeeware.cloud.lock4j.support.AbstractWatchable;
import io.github.jeeware.cloud.lock4j.support.AbstractWatchableLockRepository;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * {@link LockRepository} Redis implementation.
 *
 * @author hbourada
 */
public class RedisLockRepository extends AbstractWatchableLockRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisLockRepository.class);

    private static final long ACQUIRED = 1;

    private final RedisScript<Long> acquireLock;

    private final RedisScript<Long> refreshActiveLock;

    private final RedisScript<Long> releaseLock;

    private final RedisConnectionFactory connectionFactory;

    private final ScriptExecutor scriptExecutor;

    private final long expirationMillis;

    private final String lockPrefix;

    public RedisLockRepository(RedisLockScripts redisLockScripts, RedisConnectionFactory connectionFactory,
                               Duration expiration, String lockPrefix) {
        requireNonNull(redisLockScripts, "redisLockScripts is null");
        this.acquireLock = redisLockScripts.acquireLock();
        this.refreshActiveLock = redisLockScripts.refreshActiveLock();
        this.releaseLock = redisLockScripts.releaseLock();
        this.connectionFactory = requireNonNull(connectionFactory, "connectionFactory is null");
        this.scriptExecutor = connectionFactory.getScriptExecutor();
        this.expirationMillis = requireNonNull(expiration, "expiration is null").toMillis();
        this.lockPrefix = lockPrefix;
    }

    @Override
    public void refreshActiveLock(String lockId, String instanceId) {
        final RedisLockKey lockKey = newRedisLockKey(lockId);
        final List<String> keys = asList(lockKey.getId(), lockKey.getClockSkew());
        final List<Long> args = singletonList(expirationMillis);
        final Long count = scriptExecutor.execute(refreshActiveLock, keys, args);

        if (count > 0) {
            LOGGER.debug("Lock {} was refreshed for instanceId: {}", lockId, instanceId);
        }
    }

    protected RedisLockKey newRedisLockKey(String id) {
        return new RedisLockKey(lockPrefix, id, connectionFactory.isRedisCluster());
    }

    @Override
    public boolean acquireLock(String lockId, String instanceId) {
        final RedisLockKey lockKey = newRedisLockKey(lockId);
        final List<String> keys = singletonList(lockKey.getId());
        final List<Object> args = asList(instanceId, expirationMillis);
        final Long result = scriptExecutor.execute(acquireLock, keys, args);

        return ACQUIRED == result;
    }

    @Override
    public boolean acquireLockWithClockSkew(String lockId, String instanceId, long clockSkewMillis) {
        final RedisLockKey lockKey = newRedisLockKey(lockId);
        final List<String> keys = asList(lockKey.getId(), lockKey.getClockSkew());
        final List<Object> args = asList(instanceId, expirationMillis, clockSkewMillis);
        final Long result = scriptExecutor.execute(acquireLock, keys, args);

        return ACQUIRED == result;
    }

    @Override
    public void releaseLock(String lockId, String instanceId) {
        final RedisLockKey lockKey = newRedisLockKey(lockId);
        final List<String> keys = asList(lockKey.getId(), lockKey.getClockSkew());
        final Long count = scriptExecutor.execute(releaseLock, keys, null);
        LOGGER.debug("{} lock id: {} was released for instanceId: {}", count, lockId, instanceId);
    }

    @Override
    public void releaseDeadLocks(long timeoutInterval) {
        // Do nothing as Redis server removes expired lock keys automatically,
        // and we are listening `del` and `expired` events to remove lock from
        // *:lock:* keys
    }

    @Override
    protected Watchable createWatchable() {
        return new RedisWatchable();
    }

    @Override
    public void close() {
        super.close();
        LOGGER.info("Successfully closed");
    }

    final class RedisWatchable extends AbstractWatchable implements MessageListener {

        static final String NOTIFY_KEYSPACE_EVENTS = "notify-keyspace-events";

        static final String NOTIFY_KEYSPACE_FEATURES = "Egx";

        final Logger logger = LoggerFactory.getLogger(RedisWatchable.class);

        final CountDownLatch latch = new CountDownLatch(1);

        final String idPrefix;

        final String clockSkewPrefix;

        RedisConnection connection;

        RedisWatchable() {
            RedisLockKey lockKey = newRedisLockKey("");
            this.idPrefix = lockKey.getId();
            this.clockSkewPrefix = lockKey.getClockSkew();
        }

        @Override
        public void run() {
            connection = connectionFactory.getConnection();
            // load all scripts to Redis server to get theirs sha1
            try {
                final List<RedisScript<?>> scripts = asList(acquireLock, refreshActiveLock, releaseLock);
                scripts.forEach(script -> {
                    final String sha = connection.scriptLoad(script.getScriptAsString());
                    script.setSha1(sha);
                    logger.info("Successfully set sha1={} for script {}", sha, script.getName());
                });
                // add notify keyspace config if necessary
                final Map<String, String> notifyConfig = connection.configGet(NOTIFY_KEYSPACE_EVENTS);
                String features = notifyConfig.values().stream().findFirst().orElse("");
                final String missingNotifyFeatures = getMissingNotifyFeatures(features);
                if (!missingNotifyFeatures.isEmpty()) {
                    features += missingNotifyFeatures;
                    final String reply = connection.configSet(NOTIFY_KEYSPACE_EVENTS, features);
                    if ("OK".equals(reply)) {
                        logger.info("Successfully set configuration {} with {}", NOTIFY_KEYSPACE_EVENTS, features);
                    } else {
                        logger.warn("Error when set configuration {} to {} => reply={}", NOTIFY_KEYSPACE_EVENTS, features, reply);
                    }
                }

                final String pattern = "__keyevent@" + connection.getDatabase() + "__:*";
                logger.debug("Subscribing to pattern: {}", pattern);
                connection.pSubscribe(this, pattern);
                // block current thread until close
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Unexpected error: {}", e.getMessage(), e);
                close();
            }
        }

        String getMissingNotifyFeatures(String features) {
            if (features.isEmpty()) {
                return NOTIFY_KEYSPACE_FEATURES;
            }
            char[] notifyFeaturesChars = NOTIFY_KEYSPACE_FEATURES.toCharArray();
            StringBuilder missingChars = new StringBuilder(notifyFeaturesChars.length);
            for (char ch : features.toCharArray()) {
                if (!ArrayUtils.contains(notifyFeaturesChars, ch)) {
                    missingChars.append(ch);
                }
            }
            return missingChars.toString();
        }

        @Override
        public void close() {
            if (connection != null) {
                connection.close();
                connection = null;
                active = false;
                latch.countDown();
                logger.debug("Successfully closed");
            }
        }

        @Override
        public void onMessage(String pattern, String channel, String message) {
            if (message.startsWith(idPrefix) && (channel.endsWith("expired")
                    || channel.endsWith("del") && !message.startsWith(clockSkewPrefix))) {
                final String lockId = message.substring(idPrefix.length());
                this.signal(lockId);
            }
        }

        @Override
        public void onPSubscribe(String pattern, long count) {
            active = true;
        }

        @Override
        public void onPUnsubscribe(String pattern, long count) {
            active = false;
        }

    }

}
