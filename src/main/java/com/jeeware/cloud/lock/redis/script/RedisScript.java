package com.jeeware.cloud.lock.redis.script;

/**
 * @author hbourada
 */
public interface RedisScript<T> {

    /**
     * @return The SHA1 of the script, used for executing <code>EVALSHA</code>
     *         Redis command.
     */
    String getSha1();

    /**
     * Set SHA1 returned from <code>SCRIPT LOAD</code> Redis command.
     */
    void setSha1(String sha1);

    /**
     * @return The script contents.
     */
    String getScriptAsString();

    /**
     * @return The script name or null if not specified.
     */
    String getName();

    /**
     * @return The script return type as java class.
     */
    Class<T> getReturnType();

    /**
     * @return true iff script is already loaded in Redis script cache, false
     *         otherwise.
     */
    default boolean isLoaded() {
        return getSha1() != null;
    }

}
