redis.call('srem', KEYS[2], KEYS[1])
return redis.call('del', KEYS[1])
