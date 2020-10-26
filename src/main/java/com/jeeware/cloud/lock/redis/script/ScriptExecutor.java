package com.jeeware.cloud.lock.redis.script;

import java.util.List;

public interface ScriptExecutor {

    <T> T execute(RedisScript<T> script, List<String> keys, List<?> args);

    default boolean containsNoScript(Exception e) {
        Throwable current = e;
        do {
            final String message = current.getMessage();
            if (message != null && message.contains("NOSCRIPT")) {
                return true;
            }
            current = current.getCause();
        } while (current != null);

        return false;
    }

}
