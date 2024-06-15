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

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import io.github.jeeware.cloud.lock4j.Watchable;
import io.github.jeeware.cloud.lock4j.support.AbstractWatchableLockRepository;
import org.apache.commons.lang3.Validate;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;

import io.github.jeeware.cloud.lock4j.ExceptionTranslator;
import io.github.jeeware.cloud.lock4j.LockRepository;

/**
 * {@link LockRepository} implementation using a MongoDB collection.
 * 
 * @author hbourada
 * @version 1.0
 */
public class MongoLockRepository extends AbstractWatchableLockRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoLockRepository.class);

    static final int UNLOCKED = 0;

    static final int LOCKED = 1;

    private final MongoCollection<LockEntity> collection;

    private final ExceptionTranslator<MongoException, ? extends RuntimeException> translator;

    private final UpdateOptions updateOptions;

    private final UpdateOptions upsertOptions;

    public MongoLockRepository(MongoDatabase database,
            String collectionName,
            ExceptionTranslator<MongoException, ? extends RuntimeException> translator) {
        Objects.requireNonNull(database, "database is null");
        Validate.notBlank(collectionName, "collectionName is blank");
        this.collection = getMongoCollection(database, collectionName);
        this.translator = Objects.requireNonNull(translator, "translator is null");
        this.updateOptions = new UpdateOptions();
        this.upsertOptions = new UpdateOptions().upsert(true);
    }

    private static MongoCollection<LockEntity> getMongoCollection(MongoDatabase database, String collectionName) {
        CodecRegistry codecRegistry = fromRegistries(database.getCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().register(LockEntity.class).build()));

        return database.withCodecRegistry(codecRegistry).getCollection(collectionName, LockEntity.class);
    }

    @Override
    public boolean acquireLock(String lockId, String instanceId) {
        final Instant now = Instant.now();
        final LockEntity lock = new LockEntity(lockId, LOCKED, now, null, instanceId, now);
        final Bson filter = and(eq(lockId), eq(LockEntity.STATE_FIELD, UNLOCKED));
        final Bson update = new Document("$set", lock);

        return execute(() -> {
            try {
                final UpdateResult result = collection.updateOne(filter, update, upsertOptions);
                return result.getModifiedCount() > 0 || result.getUpsertedId() != null;
            } catch (MongoWriteException e) {
                if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                    return false;
                }
                throw e;
            }
        });
    }

    @Override
    public void refreshActiveLocks(String instanceId) {
        final Instant now = Instant.now();
        final Bson filter = and(eq(LockEntity.STATE_FIELD, LOCKED), eq(LockEntity.LOCKED_BY_FIELD, instanceId));
        final Bson update = set(LockEntity.LOCK_HEARTBEAT_AT_FIELD, now);
        final UpdateResult result = execute(() -> collection.updateMany(filter, update, updateOptions));

        if (result.getModifiedCount() > 0) {
            LOGGER.debug("{} locks was refreshed for instanceId: {}", result.getModifiedCount(), instanceId);
        }
    }

    @Override
    public void releaseLock(String lockId, String instanceId) {
        final Bson filter = eq(lockId);
        final Bson update = combine(set(LockEntity.STATE_FIELD, UNLOCKED), set(LockEntity.UNLOCKED_AT_FIELD, Instant.now()));
        final UpdateResult result = execute(() -> collection.updateOne(filter, update, updateOptions));

        if (result.getModifiedCount() > 0) {
            LOGGER.debug("{} lock id: {} was released for instanceId: {}", result.getModifiedCount(),
                    lockId, instanceId);
        }
    }

    @Override
    public void releaseDeadLocks(long timeoutMillis) {
        final Instant now = Instant.now();
        final Instant timeout = now.minusMillis(timeoutMillis);
        final Bson filter = and(eq(LockEntity.STATE_FIELD, LOCKED), lt(LockEntity.LOCK_HEARTBEAT_AT_FIELD, timeout));
        final Bson update = combine(set(LockEntity.STATE_FIELD, UNLOCKED), set(LockEntity.UNLOCKED_AT_FIELD, now));

        if (LOGGER.isDebugEnabled()) {
            final List<LockEntity> locks = execute(() -> collection.find(filter).into(new ArrayList<>()));
            if (!locks.isEmpty()) {
                LOGGER.debug("{} dead locks will be released => {}", locks.size(), locks);
            }
        }

        final UpdateResult result = execute(() -> collection.updateMany(filter, update, updateOptions));

        if (result.getModifiedCount() > 0) {
            LOGGER.info("{} locks was released after timeout: {}ms", result.getModifiedCount(), timeoutMillis);
        }
    }

    private <T> T execute(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (MongoException e) {
            throw translator.translate(e);
        }
    }

    @Override
    protected Watchable createWatchable() {
        return new MongoWatchable(collection);
    }

    @Override
    public void close() {
        super.close();
        LOGGER.info("Close successful");
    }

}
