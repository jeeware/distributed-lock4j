package com.jeeware.cloud.lock.redis.connection.lettuce;

import com.jeeware.cloud.lock.redis.connection.MessageListener;
import com.jeeware.cloud.lock.redis.connection.Subscription;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

final class LettuceSubscription implements Subscription {

    final LettuceMessageListener listener;

    final StatefulRedisPubSubConnection<String, String> redisPubSubConnection;

    String[] patterns;

    LettuceSubscription(MessageListener listener, LettuceConnectionFactory factory) {
        this.listener = new LettuceMessageListener(listener);
        this.redisPubSubConnection = factory.getPubSubConnection();
    }

    @Override
    public void pSubscribe(String... patterns) {
        this.patterns = patterns;
        final StatefulRedisPubSubConnection<String, String> connection = redisPubSubConnection;
        if (connection instanceof StatefulRedisClusterPubSubConnection) {
            ((StatefulRedisClusterPubSubConnection<?, ?>) connection).setNodeMessagePropagation(true);
        }
        connection.addListener(listener);
        redisPubSubConnection.sync().psubscribe(patterns);
    }

    @Override
    public void pUnsubscribe() {
        redisPubSubConnection.removeListener(listener);
        redisPubSubConnection.sync().punsubscribe();
    }

    @Override
    public String[] getPatterns() {
        return patterns;
    }

    @Override
    public void close() {
        Subscription.super.close();
        redisPubSubConnection.close();
    }

}
