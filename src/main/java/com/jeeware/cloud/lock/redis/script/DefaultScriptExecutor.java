package com.jeeware.cloud.lock.redis.script;

import com.jeeware.cloud.lock.redis.connection.RedisConnection;
import com.jeeware.cloud.lock.redis.connection.RedisConnectionFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class DefaultScriptExecutor implements ScriptExecutor {

    @NonNull
    private final RedisConnectionFactory connectionFactory;

    @Override
    public <T> T execute(RedisScript<T> script, List<String> keys, List<?> args) {
        final RedisConnection connection = connectionFactory.getConnection();
        try {
            if (script.isLoaded()) {
                return connection.evalSha(script.getSha1(), keys, args, script.getReturnType());
            }
            return connection.eval(script.getScriptAsString(), keys, args, script.getReturnType());
        } catch (Exception e) {
            if (containsNoScript(e)) {
                return connection.eval(script.getScriptAsString(), keys, args, script.getReturnType());
            }
            throw e;
        } finally {
            connection.close();
        }
    }
}
