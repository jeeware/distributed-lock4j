local ttl = ARGV[1]
redis.call('pexpire', KEYS[2], ttl) -- do nothing if clockSkew key does not exist
return redis.call('pexpire', KEYS[1], ttl);
