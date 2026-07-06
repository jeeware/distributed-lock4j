local lockKey = KEYS[1]
local clockSkewKey = KEYS[2]
local clockSkew = tonumber(redis.call('get', clockSkewKey))
if clockSkew ~= nil then
    redis.call('del', clockSkewKey)
    redis.call('pexpire', lockKey, clockSkew)
    return 0
end
return redis.call('del', lockKey)
