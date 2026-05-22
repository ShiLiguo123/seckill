/**
 * Copyright 2022-9999 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.binghe.seckill.common.cache.distribute.redis;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import io.binghe.seckill.common.cache.distribute.DistributedCacheService;
import io.binghe.seckill.common.utils.serializer.ProtoStuffSerializerUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description Redis缓存
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
@ConditionalOnProperty(name = "distributed.cache.type", havingValue = "redis")
public class RedisCacheService implements DistributedCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final DefaultRedisScript<Long> DECREASE_STOCK_SCRIPT;
    private static final DefaultRedisScript<Long> INCREASE_STOCK_SCRIPT;
    private static final DefaultRedisScript<Long> INIT_STOCK_SCRIPT;
    private static final DefaultRedisScript<Long> CHECK_EXECUTE_SCRIPT;
    private static final DefaultRedisScript<Long> TAKE_ORDER_TOKEN_SCRIPT;
    private static final DefaultRedisScript<Long> RECOVER_ORDER_TOKEN_SCRIPT;
    private static final DefaultRedisScript<Long> DECREMENT_BUCKET_STOCK_SCRIPT;
    private static final DefaultRedisScript<Long> INCREMENT_BUCKET_STOCK_SCRIPT;

    static {
        //扣减库存
        DECREASE_STOCK_SCRIPT = new DefaultRedisScript<>();
        DECREASE_STOCK_SCRIPT.setLocation(new ClassPathResource("lua/decrement_goods_stock.lua"));
        DECREASE_STOCK_SCRIPT.setResultType(Long.class);

        //增加库存
        INCREASE_STOCK_SCRIPT = new DefaultRedisScript<>();
        INCREASE_STOCK_SCRIPT.setLocation(new ClassPathResource("lua/increment_goods_stock.lua"));
        INCREASE_STOCK_SCRIPT.setResultType(Long.class);

        //初始化库存
        INIT_STOCK_SCRIPT = new DefaultRedisScript<>();
        INIT_STOCK_SCRIPT.setLocation(new ClassPathResource("lua/init_goods_stock.lua"));
        INIT_STOCK_SCRIPT.setResultType(Long.class);

        //检测是否执行过恢复缓存库存的操作
        CHECK_EXECUTE_SCRIPT = new DefaultRedisScript<>();
        CHECK_EXECUTE_SCRIPT.setLocation(new ClassPathResource("lua/check_execute.lua"));
        CHECK_EXECUTE_SCRIPT.setResultType(Long.class);

        //获取下单许可
        TAKE_ORDER_TOKEN_SCRIPT = new DefaultRedisScript<>();
        TAKE_ORDER_TOKEN_SCRIPT.setLocation(new ClassPathResource("lua/take_order_token.lua"));
        TAKE_ORDER_TOKEN_SCRIPT.setResultType(Long.class);

        //恢复下单许可
        RECOVER_ORDER_TOKEN_SCRIPT = new DefaultRedisScript<>();
        RECOVER_ORDER_TOKEN_SCRIPT.setLocation(new ClassPathResource("lua/recover_order_token.lua"));
        RECOVER_ORDER_TOKEN_SCRIPT.setResultType(Long.class);

        //扣减分桶库存
        DECREMENT_BUCKET_STOCK_SCRIPT = new DefaultRedisScript<>();
        DECREMENT_BUCKET_STOCK_SCRIPT.setLocation(new ClassPathResource("lua/decrement_bucket_stock.lua"));
        DECREMENT_BUCKET_STOCK_SCRIPT.setResultType(Long.class);

        //恢复分桶库存
        INCREMENT_BUCKET_STOCK_SCRIPT = new DefaultRedisScript<>();
        INCREMENT_BUCKET_STOCK_SCRIPT.setLocation(new ClassPathResource("lua/increment_bucket_stock.lua"));
        INCREMENT_BUCKET_STOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public void put(String key, String value) {
        if (StringUtils.isEmpty(key) || value == null) {
            return;
        }
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void put(String key, Object value) {
        if (StringUtils.isEmpty(key) || value == null) {
            return;
        }
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void put(String key, Object value, long timeout, TimeUnit unit) {
        if (StringUtils.isEmpty(key) || value == null) {
            return;
        }
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    @Override
    public void put(String key, Object value, long expireTime) {
        if (StringUtils.isEmpty(key) || value == null) {
            return;
        }
        redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
    }

    @Override
    public <T> T getObject(String key, Class<T> targetClass) {
        Object result = redisTemplate.opsForValue().get(key);
        if (result == null) {
            return null;
        }
        try {
            return JSON.parseObject(result.toString(), targetClass);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Object getObject(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public String getString(String key) {
        Object result = redisTemplate.opsForValue().get(key);
        if (result == null) {
            return null;
        }
        return String.valueOf(result);
    }

    @Override
    public <T> List<T> getList(String key, Class<T> targetClass) {
        Object result = redisTemplate.execute((RedisCallback<Object>) connection ->
                connection.get(key.getBytes()));
        if (result == null) {
            return null;
        }
        return ProtoStuffSerializerUtils.deserializeList(String.valueOf(result).getBytes(), targetClass);
    }

    @Override
    public Boolean delete(String key) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        return redisTemplate.delete(key);
    }

    @Override
    public Boolean deleteKeyPrefix(String prefix) {
        if (StrUtil.isEmpty(prefix)){
            return false;
        }
        if (!prefix.endsWith("*")){
            prefix = prefix.concat("*");
        }
        Set<String> keys = redisTemplate.keys(prefix);
        if (CollectionUtil.isEmpty(keys)){
            return false;
        }
        Long deleteCount = redisTemplate.delete(keys);
        return deleteCount != null && deleteCount > 0;
    }

    @Override
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    @Override
    public Long addSet(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    @Override
    public Long removeSet(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }

    @Override
    public Boolean isMemberSet(String key, Object o) {
        return redisTemplate.opsForSet().isMember(key, o);
    }

    @Override
    public Long execute(RedisScript<Long> script, List<String> keys, Object... args) {
        return redisTemplate.execute(script, keys, args);
    }

    @Override
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    @Override
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    @Override
    public Long decrementByLua(String key, Integer quantity) {
        return redisTemplate.execute(DECREASE_STOCK_SCRIPT, Collections.singletonList(key), quantity);
    }

    @Override
    public Long incrementByLua(String key, Integer quantity) {
        return redisTemplate.execute(INCREASE_STOCK_SCRIPT, Collections.singletonList(key), quantity);
    }

    @Override
    public Long initByLua(String key, Integer quantity) {
        return redisTemplate.execute(INIT_STOCK_SCRIPT, Collections.singletonList(key), quantity);
    }

    @Override
    public Long checkExecute(String key, Long seconds) {
        return redisTemplate.execute(CHECK_EXECUTE_SCRIPT, Collections.singletonList(key), seconds);
    }

    @Override
    public Long takeOrderToken(String key) {
        return redisTemplate.execute(TAKE_ORDER_TOKEN_SCRIPT, Collections.singletonList(key));
    }

    @Override
    public Long recoverOrderToken(String key) {
        return redisTemplate.execute(RECOVER_ORDER_TOKEN_SCRIPT, Collections.singletonList(key));
    }

    @Override
    public Long decrementBucketStock(List<String> keys, Integer quantity) {
        return redisTemplate.execute(DECREMENT_BUCKET_STOCK_SCRIPT, keys, quantity);
    }

    @Override
    public Long incrementBucketStock(List<String> keys, Integer quantity) {
        return redisTemplate.execute(INCREMENT_BUCKET_STOCK_SCRIPT, keys, quantity);
    }
}