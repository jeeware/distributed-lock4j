package com.jeeware.cloud.lock.spring.annotation;

import com.jeeware.cloud.lock.DistributedLockRegistry;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hbourada
 * @version 1.0
 */
@Slf4j
public class DistributedLockPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {

    @Setter
    @NonNull
    private transient DistributedLockRegistry registry;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory);

        if (!(beanFactory instanceof ConfigurableBeanFactory)) {
            throw new IllegalArgumentException("Cannot resolve expressions in " + DistributedLockInterceptor.ANNOTATION_TYPE.getName()
                    + " without ConfigurableBeanFactory");
        }

        if (this.registry == null) {
            this.registry = beanFactory.getBean(DistributedLockRegistry.class);
        }

        this.advisor = new DistributedLockAdvisor((ConfigurableBeanFactory) beanFactory, registry);
        log.info("@{} with {}", EnableDistributedLock.class.getSimpleName(), this);
    }

    @Override
    public String toString() {
        return getClass().getName() +
                "(registry=" + registry +
                ", advisor=" + advisor +
                ')';
    }
}
