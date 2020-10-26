package com.jeeware.cloud.lock.redis.connection.jedis;

import com.jeeware.cloud.lock.redis.connection.RedisConnectionFactory;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.util.Pool;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class JedisConnectionFactory implements RedisConnectionFactory {

    public static JedisConnectionFactory of(Pool<Jedis> jedisPool) {
        return new JedisPoolConnectionFactory(jedisPool);
    }

    public static JedisConnectionFactory of(JedisCluster jedisCluster) {
        return new JedisClusterConnectionFactory(jedisCluster);
    }

    @RequiredArgsConstructor
    static final class JedisPoolConnectionFactory extends JedisConnectionFactory {

        @NonNull
        final Pool<Jedis> jedisPool;

        @Override
        public JedisConnection getConnection() {
            return new JedisConnection(new JedisCommandsImpl(jedisPool.getResource()));
        }

    }

    @RequiredArgsConstructor
    static final class JedisClusterConnectionFactory extends JedisConnectionFactory {

        @NonNull
        final JedisCluster jedisCluster;

        @Override
        public JedisConnection getConnection() {
            return new JedisConnection(new JedisClusterCommandsImpl(jedisCluster));
        }

    }

}
