if (redis.call('exists', KEYS[1]) == 1) then
    redis.call('incrby', KEYS[1], 1);
    return 1;
end
return -100;