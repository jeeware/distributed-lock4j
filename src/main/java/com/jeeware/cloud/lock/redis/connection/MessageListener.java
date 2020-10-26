package com.jeeware.cloud.lock.redis.connection;

/**
 * Callback interface for Redis pub/sub events
 *
 * @author hbourada
 */
public interface MessageListener {

    /**
     * Message received from a pattern subscription.
     *
     * @param pattern Key pattern
     * @param channel Channel
     * @param message Message
     */
    void onMessage(String pattern, String channel, String message);

    /**
     * Subscribed to a pattern.
     *
     * @param pattern Key pattern
     * @param count   Subscription count.
     */
    default void onPSubscribe(String pattern, long count) {
    }

    /**
     * Unsubscribed to a pattern.
     *
     * @param pattern Key pattern
     * @param count   Subscription count.
     */
    default void onPUnsubscribe(String pattern, long count) {
    }

}
