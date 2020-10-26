package com.jeeware.cloud.lock.spring.redis;

import lombok.NonNull;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.jeeware.cloud.lock.redis.connection.MessageListener;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class LettuceByteArrayMessageListener extends RedisPubSubAdapter<byte[], byte[]> {

    @NonNull
    final MessageListener listener;

    @Override
    public void message(byte[] pattern, byte[] channel, byte[] message) {
        final RedisSerializer<String> serializer = RedisSerializer.string();
        listener.onMessage(serializer.deserialize(pattern), serializer.deserialize(channel), serializer.deserialize(message));
    }

    @Override
    public void psubscribed(byte[] pattern, long count) {
        listener.onPSubscribe(RedisSerializer.string().deserialize(pattern), count);
    }

    @Override
    public void punsubscribed(byte[] pattern, long count) {
        listener.onPUnsubscribe(RedisSerializer.string().deserialize(pattern), count);
    }
}
