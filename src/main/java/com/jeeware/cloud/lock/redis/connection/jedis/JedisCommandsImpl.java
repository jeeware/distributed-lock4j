package com.jeeware.cloud.lock.redis.connection.jedis;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import redis.clients.jedis.Jedis;

@RequiredArgsConstructor
final class JedisCommandsImpl implements JedisCommands {

    @Delegate(types = JedisCommands.class)
    @NonNull
    final Jedis jedis;

}
