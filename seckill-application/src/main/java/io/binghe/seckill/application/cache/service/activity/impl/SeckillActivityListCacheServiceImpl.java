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
package io.binghe.seckill.application.cache.service.activity.impl;

import com.alibaba.fastjson.JSON;
import io.binghe.seckill.application.builder.SeckillActivityBuilder;
import io.binghe.seckill.application.cache.model.SeckillBusinessCache;
import io.binghe.seckill.application.cache.service.activity.SeckillActivityListCacheService;
import io.binghe.seckill.domain.constants.SeckillConstants;
import io.binghe.seckill.domain.model.entity.SeckillActivity;
import io.binghe.seckill.domain.repository.SeckillActivityRepository;
import io.binghe.seckill.infrastructure.cache.distribute.DistributedCacheService;
import io.binghe.seckill.infrastructure.cache.local.LocalCacheService;
import io.binghe.seckill.infrastructure.lock.DistributedLock;
import io.binghe.seckill.infrastructure.lock.factoty.DistributedLockFactory;
import io.binghe.seckill.infrastructure.utils.string.StringUtil;
import io.binghe.seckill.infrastructure.utils.time.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 秒杀活动Service实现类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
public class SeckillActivityListCacheServiceImpl implements SeckillActivityListCacheService {
    private final static Logger logger = LoggerFactory.getLogger(SeckillActivityListCacheServiceImpl.class);
    @Autowired
    private LocalCacheService<Long, SeckillBusinessCache<List<SeckillActivity>>> localCacheService;
    //分布式锁的key
    private static final String SECKILL_ACTIVITES_UPDATE_CACHE_LOCK_KEY = "SECKILL_ACTIVITIES_UPDATE_CACHE_LOCK_KEY_";
    //本地锁
    private final Lock localCacheUpdatelock = new ReentrantLock();

    @Autowired
    private DistributedCacheService distributedCacheService;
    @Autowired
    private SeckillActivityRepository seckillActivityRepository;
    @Autowired
    private DistributedLockFactory distributedLockFactory;

    @Override
    public String buildCacheKey(Object key) {
        return StringUtil.append(SeckillConstants.SECKILL_ACTIVITIES_CACHE_KEY, key);
    }

    @Override
    public SeckillBusinessCache<List<SeckillActivity>> getCachedActivities(Integer status, Long version) {
        //获取本地缓存
        SeckillBusinessCache<List<SeckillActivity>> seckillActivitiyListCache = localCacheService.getIfPresent(status.longValue());
        if (seckillActivitiyListCache != null){
            if (version == null){
                logger.info("SeckillActivitesCache|命中本地缓存|{}", status);
                return seckillActivitiyListCache;
            }
            //传递过来的版本小于或等于缓存中的版本号
            if (version.compareTo(seckillActivitiyListCache.getVersion()) <= 0){
                logger.info("SeckillActivitesCache|命中本地缓存|{}", status);
                return seckillActivitiyListCache;
            }
            if (version.compareTo(seckillActivitiyListCache.getVersion()) > 0){
                return getDistributedCache(status);
            }
        }
        return getDistributedCache(status);
    }

    /**
     * 获取分布式缓存中的数据
     */
    private SeckillBusinessCache<List<SeckillActivity>> getDistributedCache(Integer status) {
        logger.info("SeckillActivitesCache|读取分布式缓存|{}", status);
        SeckillBusinessCache<List<SeckillActivity>> seckillActivitiyListCache = SeckillActivityBuilder.getSeckillBusinessCacheList(distributedCacheService.getObject(buildCacheKey(status)),  SeckillActivity.class);
        if (seckillActivitiyListCache == null){
            seckillActivitiyListCache = tryUpdateSeckillActivityCacheByLock(status);
        }
        if (seckillActivitiyListCache != null && !seckillActivitiyListCache.isRetryLater()){
            if (localCacheUpdatelock.tryLock()){
                try {
                    localCacheService.put(status.longValue(), seckillActivitiyListCache);
                    logger.info("SeckillActivitesCache|本地缓存已经更新|{}", status);
                }finally {
                    localCacheUpdatelock.unlock();
                }
            }
        }
        return seckillActivitiyListCache;
    }

    /**
     * 根据状态更新分布式缓存数据
     */
    @Override
    public SeckillBusinessCache<List<SeckillActivity>> tryUpdateSeckillActivityCacheByLock(Integer status) {
        logger.info("SeckillActivitesCache|更新分布式缓存|{}", status);
        DistributedLock lock = distributedLockFactory.getDistributedLock(SECKILL_ACTIVITES_UPDATE_CACHE_LOCK_KEY.concat(String.valueOf(status)));
        try {
            boolean isLockSuccess = lock.tryLock(1, 5, TimeUnit.SECONDS);
            if (!isLockSuccess){
                return new SeckillBusinessCache<List<SeckillActivity>>().retryLater();
            }
            List<SeckillActivity> seckillActivityList = seckillActivityRepository.getSeckillActivityList(status);
            SeckillBusinessCache<List<SeckillActivity>> seckillActivitiyListCache;
            if (seckillActivityList == null){
                seckillActivitiyListCache = new SeckillBusinessCache<List<SeckillActivity>>().notExist();
            }else {
                seckillActivitiyListCache = new SeckillBusinessCache<List<SeckillActivity>>().with(seckillActivityList).withVersion(SystemClock.millisClock().now());
            }
            distributedCacheService.put(buildCacheKey(status), JSON.toJSONString(seckillActivitiyListCache), SeckillConstants.FIVE_MINUTES);
            logger.info("SeckillActivitesCache|分布式缓存已经更新|{}", status);
            return seckillActivitiyListCache;
        } catch (InterruptedException e) {
            logger.info("SeckillActivitesCache|更新分布式缓存失败|{}", status);
            return new SeckillBusinessCache<List<SeckillActivity>>().retryLater();
        } finally {
            lock.unlock();
        }
    }
}
