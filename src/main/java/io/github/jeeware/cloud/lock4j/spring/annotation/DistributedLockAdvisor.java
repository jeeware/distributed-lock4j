/*
 * Copyright 2020-2020-2024 Hichem BOURADA and other authors.
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

import static io.github.jeeware.cloud.lock4j.spring.annotation.DistributedLockInterceptor.ANNOTATION_TYPE;

import io.github.jeeware.cloud.lock4j.DistributedLockRegistry;
import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author hbourada
 * @version 1.0
 */
@EqualsAndHashCode(callSuper = true)
@ToString
public class DistributedLockAdvisor extends AbstractPointcutAdvisor {

    private final transient DistributedLockInterceptor advice;

    private final transient AnnotationMatchingPointcut pointcut;

    public DistributedLockAdvisor(ConfigurableBeanFactory beanFactory, DistributedLockRegistry registry) {
        this.advice = new DistributedLockInterceptor(beanFactory, registry);
        this.pointcut = AnnotationMatchingPointcut.forMethodAnnotation(ANNOTATION_TYPE);
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public Advice getAdvice() {
        return advice;
    }
}
