package com.jeeware.cloud.lock.redis.connection.lettuce;

import com.jeeware.cloud.lock.redis.connection.MessageListener;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class LettuceMessageListener extends RedisPubSubAdapter<String, String> {

    @NonNull
    private final MessageListener listener;

    @Override
    public void message(String pattern, String channel, String message) {
        listener.onMessage(pattern, channel, message);
    }

    @Override
    public void psubscribed(String pattern, long count) {
        listener.onPSubscribe(pattern, count);
    }

    @Override
    public void punsubscribed(String pattern, long count) {
        listener.onPUnsubscribe(pattern, count);
    }
}
