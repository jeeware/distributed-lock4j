local ttl = ARGV[2]
local status = redis.call('set', KEYS[1], ARGV[1], 'PX', ttl, 'NX')
if status ~= false then
    local clockSkewKey = KEYS[2]
    if clockSkewKey ~= nil then
        redis.call('set', clockSkewKey, ARGV[3], 'PX', ttl)
    end
    return 1
end
return 0
