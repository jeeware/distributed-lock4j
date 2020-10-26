package com.jeeware.cloud.lock.spring.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author hbourada
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class DistributedLockConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DistributedLockPostProcessor distributedLockPostProcessor() {
        return new DistributedLockPostProcessor();
    }
}
