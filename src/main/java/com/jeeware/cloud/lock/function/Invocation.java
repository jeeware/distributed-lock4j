package com.jeeware.cloud.lock.function;

import java.util.function.Consumer;

import com.jeeware.cloud.lock.DistributedLock;

@FunctionalInterface
public interface Invocation {

    Object apply(DistributedLock lock) throws InterruptedException;

    static Invocation of(Consumer<DistributedLock> consumer) {
        return l -> {
            consumer.accept(l);
            return null;
        };
    }
}
