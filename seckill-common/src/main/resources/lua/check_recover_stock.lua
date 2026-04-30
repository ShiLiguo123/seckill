local checkStatus = redis.call('get', KEYS[1])
-- 如果存在数据，则直接返回0
if checkStatus then
    return 0;
end
--如果不存在数据
redis.call('setex', KEYS[1], ARGV[1], '1');
-- 返回1
return 1;


