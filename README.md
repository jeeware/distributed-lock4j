### Presentation
A distributed _reentrant lock_ implementation based on a shared SQL or NoSQL database.

This library can be used in a Spring Boot or vanilla applications (i.e., without Spring)

A Spring Boot Auto-configurer is provided to ease its use.

This distributed lock can be used to implement _distributed scheduled tasks_ on a multi-instances microservice.

You can customize default behavior by overriding configuration in your `application.properties` 
or `application.yml` or defining your custom `LockRepository` bean for a particular database. 

#### Spring Boot sample for a scheduled job using SQL database:

```java
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
class MyScheduledJobs {
  // ....

  @Scheduled(fixedRate = 60_000)
  @DistributedLock(mode = Mode.TRY_LOCK)
  public void doEachOneMinute() {
    // Only one instance executes this task at a given period
    // ....
  }
}
```
```yaml
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
        size: 1
cloud:
  lock4j:
    jdbc:
      table-name: LOCKS
    instance-id: ${spring.application.name}-${random.uuid}
    type: jdbc
```
#### Vanilla sample for a scheduled job using SQL database:

```java
package io.github.jeeware.demo;

import io.github.jeeware.cloud.lock4j.jdbc.JdbcInitializingLockRepository;
import io.github.jeeware.cloud.lock4j.jdbc.SQLDialects;
import io.github.jeeware.cloud.lock4j.support.RandomBackoffStrategy;
import io.github.jeeware.cloud.lock4j.support.SimpleRetryer;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledJobApplication {

  public static void main(String[] args) {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    JdbcInitializingLockRepository lockRepository = new JdbcInitializingLockRepository(dataSource(), SQLDialects.POSTGRESQL, "locks", null);
    lockRepository.initialize(); // create necessary lock tables and functions
    BackoffStrategy backoffStrategy = RandomBackoffStrategy.builder().maxSleepDuration(Duration.ofSeconds(5)).build();
    Retryer retryer = SimpleRetryer.builder().maxRetry(3).retryableException(IOException.class).backoffStrategy(backoffStrategy).build();
    try (DistributedLockRegistry lockRegistry = new DistributedLockRegistry(lockRepository, scheduler, retryer)) {
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
    DistributedLock lock = lockRegistry.getLock("MyLockIdentifierName");
    if (lock.tryLock()) {
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
#### Spring sample for an api which can be called once a time

```java
import io.github.jeeware.cloud.lock4j.spring.annotation.DistributedLock;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class MyUserController {
    
  private final UserService userService;
  
  public MyUserController(UserService userService) {
      this.userService = userService;
  }
    
  @GetMapping("/me")
  @DistributedLock
  public User me() {
      return userService.getMe();
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

### Who use this library
![MAIF](images/logo-maif.png "https://connect.maif.fr")

![EDF](images/logo-edf.jpeg "EDF/DIPNN")




