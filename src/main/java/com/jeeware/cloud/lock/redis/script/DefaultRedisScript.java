package com.jeeware.cloud.lock.redis.script;

import com.jeeware.cloud.lock.util.Utils;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class DefaultRedisScript<T> implements RedisScript<T> {

    private final String name;

    private final String script;

    private final Class<T> returnType;

    private volatile String sha1;

    public DefaultRedisScript(String name, String script, Class<T> returnType) {
        this.name = name;
        this.script = Validate.notBlank(script, "script is blank");
        this.returnType = returnType;
    }

    public DefaultRedisScript(String name, InputStream scriptStream, Charset charset,
                              Class<T> returnType) throws IOException {
        this(name, Utils.toString(scriptStream, charset), returnType);
    }

    @Override
    public String getSha1() {
        return sha1;
    }

    @Override
    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    @Override
    public String getScriptAsString() {
        return script;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<T> getReturnType() {
        return returnType;
    }

}
