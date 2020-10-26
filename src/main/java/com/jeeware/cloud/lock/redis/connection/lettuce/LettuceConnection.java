package com.jeeware.cloud.lock.redis.connection.lettuce;

import java.util.List;
import java.util.Map;

import com.jeeware.cloud.lock.redis.connection.AbstractRedisConnection;
import com.jeeware.cloud.lock.util.Utils;
import com.jeeware.cloud.lock.redis.connection.MessageListener;
import com.jeeware.cloud.lock.redis.connection.Subscription;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisScriptingCommands;
import io.lettuce.core.api.sync.RedisServerCommands;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LettuceConnection extends AbstractRedisConnection {

    @NonNull
    private final LettuceConnectionFactory connectionFactory;

    @Override
    public Map<String, String> configGet(String parameter) {
        return redisCommands().configGet(parameter);
    }

    @Override
    public String configSet(String parameter, String value) {
        return redisCommands().configSet(parameter, value);
    }

    private <C extends RedisServerCommands<String, String> & RedisScriptingCommands<String, String>> C redisCommands() {
        return (C) connectionFactory.getSharedConnection().redisCommands();
    }

    @Override
    public <T> T evalSha(String sha1, List<String> keys, List<?> args, Class<T> returnType) {
        return redisCommands().evalsha(sha1, toScriptOutputType(returnType),
                keys.toArray(new String[0]), Utils.toStringArray(args));
    }

    @Override
    public <T> T eval(String script, List<String> keys, List<?> args, Class<T> returnType) {
        return redisCommands().eval(script, toScriptOutputType(returnType),
                keys.toArray(new String[0]), Utils.toStringArray(args));
    }

    @Override
    public String scriptLoad(String script) {
        return redisCommands().scriptLoad(script);
    }

    @Override
    public int getDatabase() {
        return connectionFactory.getDatabase();
    }

    @Override
    protected Subscription createSubscription(MessageListener listener) {
        return new LettuceSubscription(listener, connectionFactory);
    }

    private static ScriptOutputType toScriptOutputType(Class<?> returnType) {
        if (returnType == null) {
            return ScriptOutputType.STATUS;
        }
        if (returnType == Long.class) {
            return ScriptOutputType.INTEGER;
        }
        if (returnType == Boolean.class) {
            return ScriptOutputType.BOOLEAN;
        }
        if (List.class.isAssignableFrom(returnType)) {
            return ScriptOutputType.MULTI;
        }

        return ScriptOutputType.VALUE;
    }
}
