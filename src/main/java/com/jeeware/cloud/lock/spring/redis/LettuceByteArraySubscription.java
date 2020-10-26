package com.jeeware.cloud.lock.spring.redis;

import java.util.Objects;

import org.springframework.data.redis.serializer.RedisSerializer;

import com.jeeware.cloud.lock.redis.connection.MessageListener;
import com.jeeware.cloud.lock.redis.connection.Subscription;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

final class LettuceByteArraySubscription implements Subscription {

    final LettuceByteArrayMessageListener listener;

    final StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection;

    String[] patterns;

    LettuceByteArraySubscription(MessageListener listener, StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection) {
        this.listener = new LettuceByteArrayMessageListener(listener);
        this.pubSubConnection = Objects.requireNonNull(pubSubConnection, "pubSubConnection is null");
    }

    @Override
    public void pSubscribe(String... patterns) {
        this.patterns = patterns;
        pubSubConnection.addListener(listener);
        pubSubConnection.sync().psubscribe(serializeArray(patterns));
    }

    @Override
    public void pUnsubscribe() {
        pubSubConnection.removeListener(listener);
        pubSubConnection.sync().punsubscribe();
    }

    @Override
    public String[] getPatterns() {
        return this.patterns;
    }

    @Override
    public void close() {
        Subscription.super.close();
        pubSubConnection.close();
    }

    static byte[][] serializeArray(String[] array) {
        RedisSerializer<String> serializer = RedisSerializer.string();
        byte[][] bytes = new byte[array.length][];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = serializer.serialize(array[i]);
        }

        return bytes;
    }
}
