package com.jeeware.cloud.lock;

import com.jeeware.cloud.lock.mongo.MongoLockRepository;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

/**
 * Tests for {@link DistributedLockRegistry} with
 * {@link MongoLockRepository}
 *
 * @author hbourada
 * @version 1.0
 */
@DataMongoTest(properties = "cloud.lock.type=mongo")
@AutoConfigurationPackage
class MongoDistributedLockRegistryTest extends DistributedLockRegistryTest {
}
