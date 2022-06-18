/*
 * Copyright 2020-2022 Hichem BOURADA and other authors.
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
