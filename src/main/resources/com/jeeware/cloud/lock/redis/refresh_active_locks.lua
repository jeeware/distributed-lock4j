local count = 0
for _, name in ipairs(redis.call('smembers', KEYS[1])) do
    count = count + redis.call('pexpire', name, ARGV[1]);
end
return count
