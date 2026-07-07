/*
 * Copyright 2020-2026 Hichem BOURADA and other authors.
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

package io.github.jeeware.cloud.lock4j.spring.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class StringToDurationConverter implements Converter<String, Duration> {

    private static final Pattern PATTERN = Pattern.compile("([+-])?(?:((\\d+)([num]s|s|m|h|d)?)|(p.+))",
            Pattern.CASE_INSENSITIVE);

    @Nullable
    @Override
    public Duration convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        Matcher matcher = PATTERN.matcher(source);
        if (matcher.matches()) {
            String suffixed = matcher.group(2);
            if (suffixed != null) {
                String sign = matcher.group(1);
                String duration = matcher.group(3);
                if (sign != null) {
                    duration = sign + duration;
                }
                return DurationUnit.fromSuffix(matcher.group(4)).apply(duration);
            }
            return Duration.parse(source);
        }
        throw new IllegalArgumentException("Invalid duration: " + source);
    }


    @RequiredArgsConstructor
    public enum DurationUnit {
        NANOSECONDS("ns", Duration::ofNanos),
        MICROSECONDS("us", v -> Duration.of(v, ChronoUnit.MICROS)),
        MILLISECONDS("ms", Duration::ofMillis),
        SECONDS("s", Duration::ofSeconds),
        MINUTES("m", Duration::ofMinutes),
        HOURS("h", Duration::ofHours),
        DAYS("d", Duration::ofDays),
        DEFAULT(null, Duration::ofMillis);

        private static final Map<String, DurationUnit> suffixToDurationUnits = createMappings();

        private static Map<String, DurationUnit> createMappings() {
            return Arrays.stream(values()).collect(Collectors.toMap(v -> v.suffix, Function.identity()));
        }

        private final String suffix;
        private final LongFunction<Duration> creatorFn;

        public static DurationUnit fromSuffix(String suffix) {
            return suffixToDurationUnits.get(suffix != null ? suffix.toLowerCase() : null);
        }

        public Duration apply(String durationAsString) {
            return creatorFn.apply(Long.parseLong(durationAsString));
        }
    }
}
