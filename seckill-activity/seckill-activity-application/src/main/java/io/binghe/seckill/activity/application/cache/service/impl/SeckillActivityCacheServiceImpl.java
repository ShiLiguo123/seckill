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
package io.binghe.seckill.activity.application.cache.service.impl;

import com.alibaba.fastjson.JSON;
import io.binghe.seckill.activity.application.builder.SeckillActivityBuilder;
import io.binghe.seckill.activity.application.cache.service.SeckillActivityCacheService;
import io.binghe.seckill.activity.domain.model.entity.SeckillActivity;
import io.binghe.seckill.activity.domain.service.SeckillActivityDomainService;
import io.binghe.seckill.common.cache.distribute.DistributedCacheService;
import io.binghe.seckill.common.cache.local.LocalCacheService;
import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.lock.DistributedLock;
import io.binghe.seckill.common.lock.factoty.DistributedLockFactory;
import io.binghe.seckill.common.utils.string.StringUtil;
import io.binghe.seckill.common.utils.time.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 秒杀活动缓存数据服务实现类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
public class SeckillActivityCacheServiceImpl implements SeckillActivityCacheService {
    private final static Logger logger = LoggerFactory.getLogger(SeckillActivityCacheServiceImpl.class);
    //本地缓存，基于Guava实现
    //private static final Cache<Long, SeckillActivityCache> seckillActivityLocalCache = LocalCacheFactory.getLocalCache();
    @Autowired
    private LocalCacheService<Long, SeckillBusinessCache<SeckillActivity>> localCacheService;
    //更新活动时获取分布式锁使用
    private static final String SECKILL_ACTIVITY_UPDATE_CACHE_LOCK_KEY = "SECKILL_ACTIVITY_UPDATE_CACHE_LOCK_KEY_";
    //本地可重入锁
    private final Lock localCacheUpdatelock = new ReentrantLock();

    @Autowired
    private DistributedCacheService distributedCacheService;
    @Autowired
    private SeckillActivityDomainService seckillActivityDomainService;
    @Autowired
    private DistributedLockFactory distributedLockFactory;

    @Override
    public SeckillBusinessCache<SeckillActivity> getCachedSeckillActivity(Long activityId, Long version) {
        //先从本地缓存中获取数据
        SeckillBusinessCache<SeckillActivity> seckillActivityCache = localCacheService.getIfPresent(activityId);
        if (seckillActivityCache != null){
            //传递的版本号为空，则直接返回本地缓存中的数据
            if (version == null){
                logger.info("SeckillActivityCache|命中本地缓存|{}", activityId);
                return seckillActivityCache;
            }
            //传递的版本号小于等于缓存中的版本号，则说明缓存中的数据比客户端的数据新，直接返回本地缓存中的数据
            if (version.compareTo(seckillActivityCache.getVersion()) <= 0){
                logger.info("SeckillActivityCache|命中本地缓存|{}", activityId);
                return seckillActivityCache;
            }
            //传递的版本号大于缓存中的版本号，说明缓存中的数据比较落后，从分布式缓存获取数据并更新到本地缓存
            if (version.compareTo(seckillActivityCache.getVersion()) > 0){
                return getDistributedCache(activityId);
            }
        }
        //从分布式缓存中获取数据，并设置到本地缓存中
        return getDistributedCache(activityId);
    }

    /**
     * 从分布式缓存中获取数据
     */
    private SeckillBusinessCache<SeckillActivity> getDistributedCache(Long activityId) {
        logger.info("SeckillActivityCache|读取分布式缓存|{}", activityId);
        //从分布式缓存中获取数据
        SeckillBusinessCache<SeckillActivity> seckillActivityCache = SeckillActivityBuilder.getSeckillBusinessCache(distributedCacheService.getObject(buildCacheKey(activityId)), SeckillActivity.class);
        //分布式缓存中没有数据
        if (seckillActivityCache == null){
            // 尝试更新分布式缓存中的数据，注意的是只用一个线程去更新分布式缓存中的数据
            seckillActivityCache = tryUpdateSeckillActivityCacheByLock(activityId, true);
        }
        //获取的数据不为空，并且不需要重试
        if (seckillActivityCache != null && !seckillActivityCache.isRetryLater()){
            //获取本地锁，更新本地缓存
            if (localCacheUpdatelock.tryLock()){
                try {
                    localCacheService.put(activityId, seckillActivityCache);
                    logger.info("SeckillActivityCache|本地缓存已经更新|{}", activityId);
                }finally {
                    localCacheUpdatelock.unlock();
                }
            }
        }
        return seckillActivityCache;
    }

    /**
     * 利用分布式锁保证只有一个线程去更新分布式缓存中的数据
     */
    @Override
    public SeckillBusinessCache<SeckillActivity> tryUpdateSeckillActivityCacheByLock(Long activityId, boolean doubleCheck) {
        logger.info("SeckillActivityCache|更新分布式缓存|{}", activityId);
        //获取分布式锁
        DistributedLock lock = distributedLockFactory.getDistributedLock(SECKILL_ACTIVITY_UPDATE_CACHE_LOCK_KEY.concat(String.valueOf(activityId)));
        try {
            boolean isLockSuccess = lock.tryLock(1, 5, TimeUnit.SECONDS);
            //未获取到分布式锁的线程快速返回，不占用系统资源
            if (!isLockSuccess){
                return new SeckillBusinessCache<SeckillActivity>().retryLater();
            }
            SeckillBusinessCache<SeckillActivity> seckillActivityCache;
            if (doubleCheck){
                //获取锁成功后，再次从缓存中获取数据，防止高并发下多个线程争抢锁的过程中，后续的线程再等待1秒的过程中，前面的线程释放了锁，后续的线程获取锁成功后再次更新分布式缓存数据
                seckillActivityCache = SeckillActivityBuilder.getSeckillBusinessCache(distributedCacheService.getObject(buildCacheKey(activityId)), SeckillActivity.class);
                if (seckillActivityCache != null){
                    return seckillActivityCache;
                }
            }
            SeckillActivity seckillActivity = seckillActivityDomainService.getSeckillActivityById(activityId);
            if (seckillActivity == null){
                seckillActivityCache = new SeckillBusinessCache<SeckillActivity>().notExist();
            }else{
                seckillActivityCache = new SeckillBusinessCache<SeckillActivity>().with(seckillActivity).withVersion(SystemClock.millisClock().now());
            }
            //将数据保存到分布式缓存
            distributedCacheService.put(buildCacheKey(activityId), JSON.toJSONString(seckillActivityCache), SeckillConstants.FIVE_MINUTES);
            logger.info("SeckillActivityCache|分布式缓存已经更新|{}", activityId);
            return seckillActivityCache;
            //将数据放入分布式缓存
        } catch (InterruptedException e) {
            logger.error("SeckillActivityCache|更新分布式缓存失败|{}", activityId);
            return new SeckillBusinessCache<SeckillActivity>().retryLater();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String buildCacheKey(Object key) {
        return StringUtil.append(SeckillConstants.SECKILL_ACTIVITY_CACHE_KEY, key);
    }

}
