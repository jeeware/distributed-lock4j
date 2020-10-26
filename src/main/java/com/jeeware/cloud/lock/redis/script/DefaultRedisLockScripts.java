package com.jeeware.cloud.lock.redis.script;

import com.jeeware.cloud.lock.util.Utils;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * {@link RedisLockScripts} default implementation which uses a set of provided
 * classpath lua files.
 *
 * @author hbourada
 */
public class DefaultRedisLockScripts implements RedisLockScripts {

    private static final String ROOT_PATH = "com/jeeware/cloud/lock/redis/";

    private static final String ACQUIRE_LOCK_PATH = ROOT_PATH + "acquire_lock.lua";

    private static final String RELEASE_LOCK_PATH = ROOT_PATH + "release_lock.lua";

    private static final String REFRESH_ACTIVE_LOCKS_PATH = ROOT_PATH + "refresh_active_locks.lua";

    private final ClassLoader classLoader;

    public DefaultRedisLockScripts(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader is null");
    }

    public DefaultRedisLockScripts() {
        this(Utils.defaultClassLoader());
    }

    @Override
    public RedisScript<Long> acquireLock() {
        return fromPath(ACQUIRE_LOCK_PATH);
    }

    @Override
    public RedisScript<Long> releaseLock() {
        return fromPath(RELEASE_LOCK_PATH);
    }

    @Override
    public RedisScript<Long> refreshActiveLocks() {
        return fromPath(REFRESH_ACTIVE_LOCKS_PATH);
    }

    @SneakyThrows
    protected RedisScript<Long> fromPath(String path) {
        try (InputStream resource = classLoader.getResourceAsStream(path)) {
            final String scriptName = path.substring(ROOT_PATH.length());
            return new DefaultRedisScript<>(scriptName, resource, UTF_8, Long.class);
        }
    }

}
