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
package io.binghe.seckill.activity.application.cache.service;

import io.binghe.seckill.activity.domain.model.entity.SeckillActivity;
import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.cache.service.SeckillCacheService;

import java.util.Date;
import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 加有缓存的秒杀活动服务接口
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillActivityListCacheService extends SeckillCacheService {

    /**
     * 增加二级缓存的根据状态获取活动列表
     */
    SeckillBusinessCache<List<SeckillActivity>> getCachedActivities(Integer status, Long version);

    /**
     * 增加二级缓存的根据时间和状态获取活动列表
     */
    SeckillBusinessCache<List<SeckillActivity>>  getCachedActivities(Date currentTime, Integer status, Long version);

    /**
     * 更新缓存数据
     */
    SeckillBusinessCache<List<SeckillActivity>>  tryUpdateSeckillActivityCacheByLock(Integer status, boolean doubleCheck);

    /**
     * 更新缓存数据
     */
    SeckillBusinessCache<List<SeckillActivity>>  tryUpdateSeckillActivityCacheByLock(Date currentTime, Integer status, boolean doubleCheck);
}
