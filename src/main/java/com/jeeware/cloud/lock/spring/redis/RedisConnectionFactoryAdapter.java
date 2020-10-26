package com.jeeware.cloud.lock.spring.redis;

import com.jeeware.cloud.lock.redis.connection.RedisConnection;
import com.jeeware.cloud.lock.redis.connection.RedisConnectionFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RedisConnectionFactoryAdapter implements RedisConnectionFactory {

    @NonNull
    private final org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory;

    private final int database;

    @Override
    public RedisConnection getConnection() {
        return new RedisConnectionAdapter(redisConnectionFactory.getConnection(), database);
    }

}
