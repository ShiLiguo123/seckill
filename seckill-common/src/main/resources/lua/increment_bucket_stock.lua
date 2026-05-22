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
    local quantity = tonumber(ARGV[1]);
    redis.call('incrby', KEYS[1] , quantity);
    return 1;
end
return -10;

