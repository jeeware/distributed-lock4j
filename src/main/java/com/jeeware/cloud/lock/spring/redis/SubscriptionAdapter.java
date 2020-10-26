package com.jeeware.cloud.lock.spring.redis;

import static lombok.AccessLevel.PRIVATE;
import static org.springframework.util.ReflectionUtils.invokeMethod;

import java.lang.reflect.Method;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisClusterConnection;
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;

import com.jeeware.cloud.lock.redis.connection.MessageListener;
import com.jeeware.cloud.lock.redis.connection.Subscription;
import com.jeeware.cloud.lock.redis.connection.jedis.JedisCommands;
import com.jeeware.cloud.lock.redis.connection.jedis.JedisSubscription;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.Getter;
import lombok.SneakyThrows;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

/**
 * Spring Data Redis Subscription adapter
 *
 * @author hbourada
 */
public class SubscriptionAdapter implements Subscription {

    @Getter(lazy = true, value = PRIVATE)
    private static final Method switchToPubSub = findSwitchToPubSubMethod();

    private final Subscription delegate;

    public SubscriptionAdapter(MessageListener listener, RedisConnection connection) {
        if (connection instanceof LettuceConnection) {
            @SuppressWarnings("unchecked")
            StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection = (StatefulRedisPubSubConnection<byte[], byte[]>) invokeMethod(
                    getSwitchToPubSub(), connection);
            delegate = new LettuceByteArraySubscription(listener, pubSubConnection);
        } else if (connection instanceof JedisConnection) {
            Jedis jedis = ((JedisConnection) connection).getNativeConnection();
            delegate = new JedisSubscription(listener, JedisCommands.of(jedis));
        } else if (connection instanceof JedisClusterConnection) {
            JedisCluster jedisCluster = ((JedisClusterConnection) connection).getNativeConnection();
            delegate = new JedisSubscription(listener, JedisCommands.of(jedisCluster));
        } else {
            throw new IllegalArgumentException("Unsupported connection type:" + connection.getClass().getName());
        }
    }

    @SneakyThrows
    private static Method findSwitchToPubSubMethod() {
        Method switchToPubSub = LettuceConnection.class.getDeclaredMethod("switchToPubSub");
        if (!switchToPubSub.isAccessible()) {
            switchToPubSub.setAccessible(true);
        }
        return switchToPubSub;
    }

    @Override
    public void pSubscribe(String... patterns) {
        delegate.pSubscribe(patterns);
    }

    @Override
    public void pUnsubscribe() {
        delegate.pUnsubscribe();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public String[] getPatterns() {
        return delegate.getPatterns();
    }

}
