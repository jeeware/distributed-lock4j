package com.jeeware.cloud.lock.redis.connection;

import java.util.List;
import java.util.Map;

import lombok.NonNull;

public interface RedisConnection extends AutoCloseable {

    @NonNull
    Map<String, String> configGet(String parameter);

    String configSet(String parameter, String value);

    <T> T evalSha(String sha1, List<String> keys, List<?> args, Class<T> returnType);

    <T> T  eval(String script, List<String> keys, List<?> args, Class<T> returnType);

    String scriptLoad(String script);

    void pSubscribe(MessageListener listener, String... patterns);

    void pUnsubscribe();

    boolean isSubscribed();

    int getDatabase();

    @Override
    void close();

}
