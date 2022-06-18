/*
 * Copyright 2020-2022-2022 Hichem BOURADA and other authors.
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

package com.jeeware.cloud.lock.jdbc.script;

import org.apache.commons.lang3.Validate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface SqlScriptRunner {

    void runScripts(List<SqlScript> scripts);

    interface Context {

        Context IDENTITY = v -> v;

        String resolve(String value) throws MissingVariableException;

        class MissingVariableException extends RuntimeException {

            public MissingVariableException(String message) {
                super(message);
            }

        }

        class Default implements Context {
            private static final String DEFAULT_PLACEHOLDER = "@@";
            private final Map<String, String> variables;
            private final String placeholder;
            private final Pattern variablePattern;

            Default() {
                this(Collections.emptyMap());
            }

            public Default(Map<String, String> variables) {
                this(variables, DEFAULT_PLACEHOLDER);
            }

            public Default(Map<String, String> variables, String placeholder) {
                this.variables = new HashMap<>(Objects.requireNonNull(variables, "variables is null"));
                this.placeholder = Validate.notBlank(placeholder, "placeholder is blank");
                this.variablePattern = Pattern.compile(String.format("%1$s(.+?)%1$s", Pattern.quote(placeholder)));
            }

            @Override
            public String resolve(String value) {
                if (value == null || !value.contains(placeholder)) {
                    return value;
                }
                Matcher matcher = variablePattern.matcher(value);
                if (!matcher.find()) {
                    return value;
                }
                StringBuffer result = new StringBuffer(value.length());
                do {
                    String variableName = matcher.group(1);
                    matcher.appendReplacement(result, variables.computeIfAbsent(variableName, k -> {
                        throw new MissingVariableException("variable [" + k + "] is not defined in the context: " + this);
                    }));
                } while (matcher.find());
                matcher.appendTail(result);
                return result.toString();
            }

            Default with(String name, String value) {
                variables.put(name, value);
                return this;
            }

            @Override
            public String toString() {
                return "Default{variables=" + variables + '}';
            }
        }
    }
}
