package com.jeeware.cloud.lock.support;

import com.jeeware.cloud.lock.Retryer;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple {@link Retryer} with a max retry count and a set of retryable
 * exception types base.
 * 
 * @author hbourada
 */
public class SimpleRetryer implements Retryer {

    private final Map<Class<?>, Boolean> exceptionTypes;

    private final int maxRetry;

    private int retryCount;

    @SafeVarargs
    public SimpleRetryer(int maxRetry, Class<? extends Exception>... exceptionTypes) {
        this.maxRetry = maxRetry;
        this.retryCount = 0;
        this.exceptionTypes = new IdentityHashMap<>(exceptionTypes.length);

        for (Class<?> exceptionType : exceptionTypes) {
            this.exceptionTypes.put(exceptionType, true);
        }
    }

    @Override
    public boolean retryFor(Exception e) {
        if (retryCount++ < maxRetry) {
            final List<Class<?>> parentTypes = new ArrayList<>();
            Class<?> currentType = e.getClass();
            boolean retry = false;

            do {
                final Boolean currentRetry = exceptionTypes.get(currentType);
                if (currentRetry != null) {
                    retry = currentRetry;
                    break;
                }
                parentTypes.add(currentType);
                currentType = currentType.getSuperclass();
            } while (currentType != Exception.class);

            if (!parentTypes.isEmpty()) {
                final boolean r = retry;
                parentTypes.forEach(type -> exceptionTypes.put(type, r));
            }

            return retry;
        }

        return false;
    }

}
