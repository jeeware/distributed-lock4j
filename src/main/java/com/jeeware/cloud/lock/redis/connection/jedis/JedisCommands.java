package com.jeeware.cloud.lock.redis.connection.jedis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPubSub;

import java.util.List;

public interface JedisCommands extends AutoCloseable {

    List<String> configGet(String pattern);

    String configSet(String parameter, String value);

    Object eval(String script, List<String> keys, List<String> args);

    Object evalsha(String sha1, List<String> keys, List<String> args);

    String scriptLoad(String script);

    int getDB();

    void psubscribe(JedisPubSub jedisPubSub, final String... patterns);

    @Override
    void close();

    static JedisCommands of(Jedis jedis) {
        return new JedisCommandsImpl(jedis);
    }

    static JedisCommands of(JedisCluster jedisCluster) {
        return new JedisClusterCommandsImpl(jedisCluster);
    }

}
