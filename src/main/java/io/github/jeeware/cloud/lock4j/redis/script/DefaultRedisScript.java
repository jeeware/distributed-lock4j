/*
 * Copyright 2020-2024 Hichem BOURADA and other authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.jeeware.cloud.lock4j.redis.script;

import io.github.jeeware.cloud.lock4j.util.Utils;
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
