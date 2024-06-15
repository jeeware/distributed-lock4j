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

package io.github.jeeware.cloud.lock4j.mongo;

import java.time.Instant;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public final class LockEntity {

    static final String STATE_FIELD = "state";

    static final String LOCKED_AT_FIELD = "locked_at";

    static final String UNLOCKED_AT_FIELD = "unlocked_at";

    static final String LOCKED_BY_FIELD = "locked_by";

    static final String LOCK_HEARTBEAT_AT_FIELD = "lock_heartbeat_at";

    @BsonId
    private String id;

    @BsonProperty(STATE_FIELD)
    private int state;

    @BsonProperty(LOCKED_AT_FIELD)
    private Instant lockedAt;

    @BsonProperty(UNLOCKED_AT_FIELD)
    private Instant unlockedAt;

    @BsonProperty(LOCKED_BY_FIELD)
    private String lockedBy;

    @BsonProperty(LOCK_HEARTBEAT_AT_FIELD)
    private Instant lockHeartbeatAt;

}
