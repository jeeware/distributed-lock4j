package com.jeeware.cloud.lock.redis.connection.jedis;

import com.jeeware.cloud.lock.redis.connection.MessageListener;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.JedisPubSub;

@RequiredArgsConstructor
public final class JedisMessageListener extends JedisPubSub {

    @NonNull
    private final MessageListener listener;

    @Override
    public void onPMessage(String pattern, String channel, String message) {
        listener.onMessage(pattern, channel, message);
    }

    @Override
    public void onPSubscribe(String pattern, int count) {
        listener.onPSubscribe(pattern, count);
    }

    @Override
    public void onPUnsubscribe(String pattern, int count) {
        listener.onPUnsubscribe(pattern, count);
    }
}
