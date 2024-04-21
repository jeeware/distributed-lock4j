local status = redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2], 'NX')
if status ~= false then
    redis.call('sadd', KEYS[2], KEYS[1])
    return 1
end
return 0
