package com.jeeware.cloud.lock.redis.connection.jedis;

import java.util.Objects;

import com.jeeware.cloud.lock.redis.connection.MessageListener;
import com.jeeware.cloud.lock.redis.connection.Subscription;

public final class JedisSubscription implements Subscription {

    private final JedisMessageListener listener;

    private final JedisCommands jedisCommands;

    private String[] patterns;

    public JedisSubscription(MessageListener listener, JedisCommands jedisCommands) {
        this.listener = new JedisMessageListener(listener);
        this.jedisCommands = Objects.requireNonNull(jedisCommands, "jedisCommands is null");
    }

    @Override
    public void pSubscribe(String... patterns) {
        this.patterns = patterns;
        jedisCommands.psubscribe(listener, patterns);
    }

    @Override
    public void pUnsubscribe() {
        listener.punsubscribe();
    }

    @Override
    public String[] getPatterns() {
        return patterns;
    }

}
