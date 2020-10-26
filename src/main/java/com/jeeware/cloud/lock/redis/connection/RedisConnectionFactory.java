package com.jeeware.cloud.lock.redis.connection;

import com.jeeware.cloud.lock.redis.script.DefaultScriptExecutor;
import com.jeeware.cloud.lock.redis.script.ScriptExecutor;
import lombok.NonNull;

/**
 * Redis connection abstract factory allowing use for various Redis java drivers
 * as Jedis or Lettuce.
 * 
 * @author hbourada
 */
public interface RedisConnectionFactory {

    /**
     * @return a Redis connection, never null
     */
    @NonNull
    RedisConnection getConnection();

    /**
     * @return a Redis script executor to evaluate a script, default to
     *         {@link DefaultScriptExecutor}
     */
    @NonNull
    default ScriptExecutor getScriptExecutor() {
        return new DefaultScriptExecutor(this);
    }
}
