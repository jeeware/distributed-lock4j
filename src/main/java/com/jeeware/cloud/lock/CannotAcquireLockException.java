package com.jeeware.cloud.lock;

/**
 * Specific unchecked exception raised when a distributed lock can not be
 * acquired for some reason.
 * 
 * @author hbourada
 */
public class CannotAcquireLockException extends IllegalStateException {

    public CannotAcquireLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
