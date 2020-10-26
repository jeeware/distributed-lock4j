package com.jeeware.cloud.lock.spring.annotation;

import static com.jeeware.cloud.lock.spring.annotation.DistributedLockInterceptor.ANNOTATION_TYPE;

import com.jeeware.cloud.lock.DistributedLockRegistry;
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
