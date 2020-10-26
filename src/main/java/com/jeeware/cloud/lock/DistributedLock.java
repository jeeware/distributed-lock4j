package com.jeeware.cloud.lock;

import java.util.concurrent.locks.Lock;

/**
 * {@link Lock} specialization for concurrent processes.
 *
 * @author hbourada
 * @version 1.0
 */
public interface DistributedLock extends Lock {

    boolean isHeldByCurrentProcess();

}
