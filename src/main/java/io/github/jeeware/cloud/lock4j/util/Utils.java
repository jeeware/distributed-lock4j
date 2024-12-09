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

package io.github.jeeware.cloud.lock4j.util;

import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@UtilityClass
public class Utils {

    private static final int BUFFER_SIZE = 8192;

    public static List<String> toStringList(List<?> args) {
        return args == null || args.isEmpty() ? Collections.emptyList()
                : args.stream()
                .map(v -> Objects.toString(v, null))
                .collect(Collectors.toList());
    }

    public static String[] toStringArray(List<?> args) {
        if (args == null || args.isEmpty()) {
            return new String[0];
        }
        final int size = args.size();
        final String[] stringArray = new String[size];
        for (int i = 0; i < size; i++) {
            stringArray[i] = Objects.toString(args.get(i), null);
        }
        return stringArray;
    }

    public static String toString(InputStream stream, Charset charset) throws IOException {
        Objects.requireNonNull(stream, "stream is null");
        Objects.requireNonNull(charset, "charset is null");
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
        final byte[] buf = new byte[BUFFER_SIZE];
        int count;

        while ((count = stream.read(buf)) != -1) {
            baos.write(buf, 0, count);
        }

        return baos.toString(charset.name());
    }

    public static ClassLoader defaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Exception ignore) {
            // Can not get thread context ClassLoader for some security reason
        }
        // Use class loader of this class.
        if (cl == null) {
            cl = Utils.class.getClassLoader();
            // Use the bootstrap ClassLoader
            if (cl == null) {
                cl = ClassLoader.getSystemClassLoader();
            }
        }
        return cl;
    }
}
