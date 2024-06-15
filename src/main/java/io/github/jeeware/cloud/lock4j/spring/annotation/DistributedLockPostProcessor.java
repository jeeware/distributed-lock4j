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

package io.github.jeeware.cloud.lock4j.spring.annotation;

import io.github.jeeware.cloud.lock4j.DistributedLockRegistry;
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
