-- lua逻辑：首先判断活动库存是否存在，如果不存在，则返回-1，如果传入的要扣减的库存数量小于或者等于0，则返回-2，增加库存成功，则返回1
-- 两个入参：
-- KEYS[1] : 活动库存的key
-- ARGV[1] : 活动库存的增加数量
local stock = redis.call('get', KEYS[1])

-- 商品库存不存在
if not stock then
    return -1
end

local paramStock = tonumber(ARGV[1])

-- 参数不能小于或者等于0
if paramStock <= 0 then
    return -2
end

redis.call('incrby',KEYS[1], paramStock)
return 1