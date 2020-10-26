package com.jeeware.cloud.lock.function;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.jeeware.cloud.lock.Watchable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@FunctionalInterface
public interface WatchableThreadFactory extends ThreadFactory {

    Thread newThread(Watchable w);

    default Thread newThread(Runnable r) {
        return newThread((Watchable) r);
    }

    static WatchableThreadFactory of(@NonNull ThreadFactory threadFactory) {
        return threadFactory instanceof WatchableThreadFactory ? (WatchableThreadFactory) threadFactory
                : threadFactory::newThread;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    final class Default implements WatchableThreadFactory {

        public static final Default INSTANCE = new Default();

        private final ThreadFactory delegate = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(Watchable w) {
            Thread thread = delegate.newThread(w);
            thread.setName(w.name());
            return thread;
        }
    }
}
