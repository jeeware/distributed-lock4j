/*
 * Copyright 2020-2020-2024 Hichem BOURADA and other authors.
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

package io.github.jeeware.cloud.lock4j.jdbc.script;

import io.github.jeeware.cloud.lock4j.util.Utils;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

@EqualsAndHashCode
@ToString
public final class DefaultSqlScript implements SqlScript {

    private final String script;

    private final String path;

    public DefaultSqlScript(String path, InputStream inputStream, Charset charset) throws IOException {
        this(Utils.toString(inputStream, charset), path);
    }

    public DefaultSqlScript(String script, String path) {
        this.script = Validate.notEmpty(script, "script can not be empty");
        this.path = path;
    }

    @Override
    public String getScriptAsString() {
        return script;
    }

    @Override
    public String getPath() {
        return path;
    }
}
