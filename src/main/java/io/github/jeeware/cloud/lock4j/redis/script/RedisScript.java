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
     * @param  sha1 the SHA1 value
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
