-- lua逻辑：首先判断传入的参数是否大于0，如果小于或者等于0，则返回-2，初始化库存成功，返回1
-- 两个入参：
-- KEYS[1] : 活动库存的key
-- ARGV[1] : 活动库存的初始化数量

local paramStock = tonumber(ARGV[1])

-- 参数不能小于或者等于0
if paramStock <= 0 then
    return -2
end

redis.call('set',KEYS[1], paramStock)
return 1