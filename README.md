### Presentation
A distributed lock implementation based on a shared SQL or NoSQL database.

This framework can be used in a Spring Boot or vanilla applications (i.e. without Spring)

A Spring Boot Auto-configurer is provided to ease its use.

We can customize default behavior by overriding configuration properties in your `application.properties` 
or `application.yml` or defining your custom `LockRepository` bean for a particular database. 

#### Spring Boot sample for scheduled job using SQL database:
``` java
@EnableScheduling
@EnableDistributedLock
@Configuration
class MyConfig {
    ....
}

@Component
class MyJobs {
    ....
    @Scheduled(fixedDelay = 300000)
    @DistributedLock(mode = Mode.TRY_LOCK)
    public void doPeridically() throws Exception {
        ....
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
  lock:
    jdbc:
      table-name: LOCKS
    instance-id: ${spring.application.name}-${random.uuid}
    type: jdbc
```

Actually accepted databases are:
1. MongoDB
2. PostgresSQL
3. HSQLDB
4. MySQL
5. H2 
6. Oracle

### Minimum Requirement
* JDK: 8
* mongodb java driver: 3.7.0
* Spring Boot: 1.5
