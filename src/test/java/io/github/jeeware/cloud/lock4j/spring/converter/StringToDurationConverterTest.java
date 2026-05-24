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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class StringToDurationConverterTest {

    static final StringToDurationConverter converter = new StringToDurationConverter();

    @Test
    void convert_empty_should_returns_null() {
        assertThat(converter.convert("")).isNull();
    }

    @Test
    void convert_null_should_throws_exception() {
        assertThatThrownBy(() -> converter.convert(null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"86400000000000ns", "+86400000000Us", "86400000MS", "+86400s", "1440m", "+24H", "1d"})
    void convert_1_day_with_different_suffixes_should_returns_1_day_duration(String input) {
        assertThat(converter.convert(input)).isEqualTo(Duration.ofDays(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"100000", "-2580000", "+123456789", "0"})
    void convert_duration_without_suffix_should_returns_duration_of_millis(String input) {
        assertThat(converter.convert(input)).isEqualTo(Duration.ofMillis(Long.parseLong(input)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-172800000000000ns", "-172800000000us", "-172800000ms", "-172800s", "-2880m", "-48h", "-2d"})
    void convert_minus_2_days_with_different_suffixes_should_returns_minus_2_days_duration(String input) {
        assertThat(converter.convert(input)).isEqualTo(Duration.ofDays(-2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT47526H30M10.001S", "-P10DT23H15M10.12S", "+Pt7H1.015s"})
    void convert_iso_duration_should_returns_duration(String input) {
        assertThat(converter.convert(input)).isEqualTo(Duration.parse(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1000xs", "-+123456ns", "-TP15h10S"})
    void convert_incorrect_duration_should_throws_exception(String input) {
        assertThatThrownBy(() -> converter.convert(input))
                .isInstanceOfAny(IllegalArgumentException.class, DateTimeParseException.class);
    }


}