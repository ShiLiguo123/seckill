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
package io.binghe.seckill.gateway.risk.window.impl;

import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.gateway.risk.window.SlidingWindowLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 基于Redis实现的滑动窗口
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
@ConditionalOnProperty(name = "distributed.cache.type", havingValue = "redis")
public class RedisSlidingWindowLimitService implements SlidingWindowLimitService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean passThough(String key, long windowPeriod, int windowSize) {
        //风控key
        String riskControlKey = SeckillConstants.getKey(SeckillConstants.RISK_CONTROL_KEY_PREFIX, key);
        //获取当前时间
        long currentTimeStamp = System.currentTimeMillis();
        long length = windowPeriod * windowSize;
        long start = currentTimeStamp - length;
        //计算过期时间
        long expireTime = length + windowPeriod;
        redisTemplate.opsForZSet().add(riskControlKey, String.valueOf(currentTimeStamp), currentTimeStamp);
        // 移除[0,start]区间内的值
        redisTemplate.opsForZSet().removeRangeByScore(riskControlKey, 0, start);
        // 获取窗口内元素个数
        Long count = redisTemplate.opsForZSet().zCard(riskControlKey);
        // 过期时间 窗口长度+一个时间间隔
        redisTemplate.expire(riskControlKey, expireTime, TimeUnit.MILLISECONDS);
        //count为空不能通过
        if (count == null) {
            return false;
        }
        return count <= windowSize;
    }
}
