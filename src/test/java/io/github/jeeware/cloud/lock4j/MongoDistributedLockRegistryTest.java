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

package io.github.jeeware.cloud.lock4j;

import io.github.jeeware.cloud.lock4j.mongo.MongoLockRepository;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

/**
 * Tests for {@link DistributedLockRegistry} with
 * {@link MongoLockRepository}
 *
 * @author hbourada
 * @version 1.0
 */
@DataMongoTest(properties = "cloud.lock4j.type=mongo")
@AutoConfigurationPackage
class MongoDistributedLockRegistryTest extends DistributedLockRegistryTest {
}
