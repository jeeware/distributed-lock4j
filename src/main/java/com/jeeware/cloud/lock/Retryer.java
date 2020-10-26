package com.jeeware.cloud.lock;

/**
 * Strategy for retrying an operation for a defined exception hierarchy.
 * 
 * @author hbourada
 */
public interface Retryer {

    Retryer NEVER = e -> false;

    boolean retryFor(Exception e);

}
