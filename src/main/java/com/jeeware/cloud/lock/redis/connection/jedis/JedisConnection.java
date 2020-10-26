package com.jeeware.cloud.lock.redis.connection.jedis;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jeeware.cloud.lock.redis.connection.AbstractRedisConnection;
import com.jeeware.cloud.lock.util.Utils;
import com.jeeware.cloud.lock.redis.connection.MessageListener;
import com.jeeware.cloud.lock.redis.connection.Subscription;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JedisConnection extends AbstractRedisConnection {

    @NonNull
    private final JedisCommands jedisCommands;

    @Override
    public Map<String, String> configGet(String parameter) {
        final List<String> result = jedisCommands.configGet(parameter);

        if (result == null || result.isEmpty()) {
            return Collections.emptyMap();
        }

        final int size = result.size();

        if (size % 2 != 0) {
            throw new IllegalStateException("config get for " + parameter + " returns an odd number: " + size);
        }

        final Map<String, String> parameters = new HashMap<>(size / 2);

        for (int i = 0; i < size; i += 2) {
            parameters.put(result.get(i), result.get(i + 1));
        }

        return parameters;
    }

    @Override
    public String configSet(String parameter, String value) {
        return jedisCommands.configSet(parameter, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T evalSha(String sha1, List<String> keys, List<?> args, Class<T> returnType) {
        if (returnType != null) {
            returnType.cast(jedisCommands.evalsha(sha1, keys, Utils.toStringList(args)));
        }
        return (T) jedisCommands.evalsha(sha1, keys, Utils.toStringList(args));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T eval(String script, List<String> keys, List<?> args, Class<T> returnType) {
        if (returnType != null) {
            return returnType.cast(jedisCommands.eval(script, keys, Utils.toStringList(args)));
        }
        return (T) jedisCommands.eval(script, keys, Utils.toStringList(args));
    }

    @Override
    public String scriptLoad(String script) {
        return jedisCommands.scriptLoad(script);
    }

    @Override
    public int getDatabase() {
        return jedisCommands.getDB();
    }

    @Override
    protected Subscription createSubscription(MessageListener listener) {
        return new JedisSubscription(listener, jedisCommands);
    }

    @Override
    public void close() {
        super.close();
        jedisCommands.close();
    }

}
