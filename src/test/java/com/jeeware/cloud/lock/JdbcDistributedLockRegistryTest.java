package com.jeeware.cloud.lock;

import com.jeeware.cloud.lock.jdbc.JdbcLockRepository;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;

/**
 * Tests for {@link DistributedLockRegistry} with
 * {@link JdbcLockRepository}
 *
 * @author hbourada
 * @version 1.0
 */
@JdbcTest(properties = "cloud.lock.type=jdbc")
class JdbcDistributedLockRegistryTest extends DistributedLockRegistryTest {
}
