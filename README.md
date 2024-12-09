### Presentation
A distributed lock implementation based on a shared SQL or NoSQL database.

This library can be used in a Spring Boot or vanilla applications (i.e. without Spring)

A Spring Boot Auto-configurer is provided to ease its use.

We can customize default behavior by overriding configuration properties in your `application.properties` 
or `application.yml` or defining your custom `LockRepository` bean for a particular database. 

#### Spring Boot sample for scheduled job using SQL database:

``` java
import io.github.jeeware.cloud.lock4j.spring.annotation.DistributedLock;
import io.github.jeeware.cloud.lock4j.spring.annotation.DistributedLock.Mode;
import io.github.jeeware.cloud.lock4j.spring.annotation.EnableDistributedLock;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@EnableDistributedLock
@Configuration(proxyBeanMethods = false)
class MyConfig {
  // ....
}

@Component
class MyJobs {
  // ....

  @Scheduled(fixedRate = 60_000)
  @DistributedLock(mode = Mode.TRY_LOCK)
  public void doEachOneMinute() {
    // Only one instance executes this task at a given period
    // ....
  }
}
```
``` yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydatabase
    username: myuser
    password: mypass
    initialization-mode: always
    schema: classpath:schema.sql
  task:
    scheduling:
      pool:
        size: 10
cloud:
  lock4j:
    jdbc:
      table-name: LOCKS
    instance-id: ${spring.application.name}-${random.uuid}
    type: jdbc
```
#### Vanilla sample for scheduled job using SQL database:

``` java
import io.github.jeeware.cloud.lock4j.DistributedLockRegistry;
import io.github.jeeware.cloud.lock4j.LockRepository;
import io.github.jeeware.cloud.lock4j.Retryer;
import io.github.jeeware.cloud.lock4j.jdbc.JdbcInitializingLockRepository;
import io.github.jeeware.cloud.lock4j.jdbc.SQLDialects;
import io.github.jeeware.cloud.lock4j.support.SimpleRetryer;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

public class MyApplication {

  public static void main(String[] args) {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2); // minimum 2 threads!
    LockRepository lockRepository = new JdbcInitializingLockRepository(dataSource(), SQLDialects.POSTGRESQL, "locks", null);
    lockRepository.initialize(); // create necessary lock tables and functions
    SimpleRetryer retryer = new SimpleRetryer(3, IOException.class);
    try (DistributedLockRegistry lockRegistry = new DistributedLockRegistry(lockRepository, scheduler, () -> retryer)) {
      scheduler.scheduleAtFixedRate(() -> doEachOneMinute(lockRegistry), 0, 1, TimeUnit.MINUTES);
      // do whatever you want
    } finally {
      scheduler.shutdown();
    }
  }

  private static DataSource dataSource() {
    PGSimpleDataSource dataSource = new PGSimpleDataSource();
    dataSource.setUrl("jdbc:postgresql://localhost:5432/mydatabase");
    // ....
    return dataSource;
  }

  private static void doEachOneMinute(DistributedLockRegistry lockRegistry) {
    if (lockRegistry.getLock("MyLockIdentifierName").tryLock()) {
      try {
        // Repeated task executed by only one application instance
        // ....
      } finally {
        lock.unlock();
      }
    }
  }

}
```

Actually accepted databases are:
- NoSQL Databases:
  1. MongoDB
  2. Redis
- SQL Databases:
  1. PostgresSQL
  2. Oracle
  3. MySQL
  4. HSQLDB
  5. H2

### Minimum Requirement
* JDK: 8
* mongodb java driver: 3.7.0
* Spring Boot: 1.5
