package com.jeeware.cloud.lock;

/**
 * Strategy interface to transform and eventually wrap a checked exception into
 * a runtime exception
 *
 * @author hbourada
 */
@FunctionalInterface
public interface ExceptionTranslator<E extends Exception, R extends RuntimeException> {

    R translate(E exception, Object... args);
}
