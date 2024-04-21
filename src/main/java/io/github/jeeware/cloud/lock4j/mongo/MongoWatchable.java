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

package io.github.jeeware.cloud.lock4j.mongo;

import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.github.jeeware.cloud.lock4j.support.AbstractWatchable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@RequiredArgsConstructor
final class MongoWatchable extends AbstractWatchable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoWatchable.class);

    @NonNull
    private final MongoCollection<LockEntity> collection;

    private MongoCursor<ChangeStreamDocument<LockEntity>> iterator;

    @Override
    public void run() {
        try {
            MongoCursor<ChangeStreamDocument<LockEntity>> it = (iterator = changeStreamIterator());
            this.active = true;
            String collectionName = collection.getNamespace().getCollectionName();
            LOGGER.info("Start watching collection {}", collectionName);

            while (it.hasNext()) {
                final BsonDocument key = it.next().getDocumentKey();
                if (key != null) {
                    this.signal(key.getString("_id").getValue());
                }
            }

            LOGGER.info("End watching collection {}", collectionName);
        } catch (MongoCommandException e) {
            LOGGER.warn("Mongo watch collection is not available: {}", e.getMessage());
        } catch (MongoException e) {
            LOGGER.error("Unexpected error occurred during watch: {}", e.getMessage(), e);
        } finally {
            this.active = false;
        }
    }

    private MongoCursor<ChangeStreamDocument<LockEntity>> changeStreamIterator() {
        final List<Bson> pipeline = Collections.singletonList(match(and(
                eq("operationType", "update"),
                eq("updateDescription.updatedFields." + LockEntity.STATE_FIELD, MongoLockRepository.UNLOCKED))));
        return collection.watch(pipeline).iterator();
    }

    @Override
    public void close() {
        if (iterator != null) {
            iterator.close();
        }
    }

}
