-- lua逻辑：首先判断活动库存是否存在，如果库存不存在则返回-1，接下来判断传入的要扣减的库存是否大于0，如果要扣减的库存不大于0，则返回-2，库存不足返回-3，扣减库存成功返回1
-- 两个入参：
-- KEYS[1] : 活动库存的key
-- ARGV[1] : 活动库存的扣减数量
local stock = redis.call('get', KEYS[1])

-- 商品库存不存在
if not stock then
    return -1
end

-- 获取Redis库存和扣减的库存数量
local intStock = tonumber(stock)
local paramStock = tonumber(ARGV[1])

-- 参数不能小于或者等于0
if paramStock <= 0 then
    return -2
end

-- 库存不足
if intStock < paramStock then
    return -3
end

-- 正常扣减库存
redis.call('decrby',KEYS[1], paramStock)

return 1