if (redis.call('exists', KEYS[1]) == 0) then
    return -1;
end
if (redis.call('exists', KEYS[2]) == 1) then
    return -2;
end
if (redis.call('exists', KEYS[3]) == 1) then
    return -3;
end
if (redis.call('exists', KEYS[1]) == 1) then
    local stocksAmount = tonumber(redis.call('get', KEYS[1]));
    local quantity = tonumber(ARGV[1]);
    if (stocksAmount < quantity) then
        return -4;
    end
    if (stocksAmount >= quantity) then
        redis.call('incrby', KEYS[1], 0 - quantity);
        return 1;
    end
end
return -10;
