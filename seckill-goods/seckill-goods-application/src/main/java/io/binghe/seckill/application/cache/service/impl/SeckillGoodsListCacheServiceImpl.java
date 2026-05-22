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
package io.binghe.seckill.application.cache.service.impl;

import com.alibaba.fastjson.JSON;
import io.binghe.seckill.application.builder.SeckillGoodsBuilder;
import io.binghe.seckill.application.cache.service.SeckillGoodsListCacheService;
import io.binghe.seckill.common.cache.distribute.DistributedCacheService;
import io.binghe.seckill.common.cache.local.LocalCacheService;
import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.lock.DistributedLock;
import io.binghe.seckill.common.lock.factoty.DistributedLockFactory;
import io.binghe.seckill.common.model.dto.stock.SeckillStockDTO;
import io.binghe.seckill.common.utils.string.StringUtil;
import io.binghe.seckill.common.utils.time.SystemClock;
import io.binghe.seckill.dubbo.interfaces.stock.SeckillStockDubboService;
import io.binghe.seckill.goods.domain.model.entity.SeckillGoods;
import io.binghe.seckill.goods.domain.service.SeckillGoodsDomainService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 秒杀商品缓存数据
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
public class SeckillGoodsListCacheServiceImpl implements SeckillGoodsListCacheService {
    private final static Logger logger = LoggerFactory.getLogger(SeckillGoodsListCacheServiceImpl.class);
    @Autowired
    private LocalCacheService<Long, SeckillBusinessCache<List<SeckillGoods>>> localCacheService;
    //更新活动时获取分布式锁使用
    private static final String SECKILL_GOODS_LIST_UPDATE_CACHE_LOCK_KEY = "SECKILL_GOODS_LIST_UPDATE_CACHE_LOCK_KEY_";
    //本地可重入锁
    private final Lock localCacheUpdatelock = new ReentrantLock();
    @Autowired
    private DistributedCacheService distributedCacheService;
    @Autowired
    private SeckillGoodsDomainService seckillGoodsDomainService;
    @Autowired
    private DistributedLockFactory distributedLockFactory;
    @DubboReference(version = "1.0.0", check = false)
    private SeckillStockDubboService seckillStockDubboService;
    @Value("${place.order.type:lua}")
    private String placeOrderType;

    @Override
    public String buildCacheKey(Object key) {
        return StringUtil.append(SeckillConstants.SECKILL_GOODSES_CACHE_KEY, key);
    }

    @Override
    public SeckillBusinessCache<List<SeckillGoods>> getCachedGoodsList(Long activityId, Long version) {
        //获取本地缓存中的数据
        SeckillBusinessCache<List<SeckillGoods>> seckillGoodsListCache = localCacheService.getIfPresent(activityId);
        if (seckillGoodsListCache != null){
            //版本号为空，表示命中本地缓存
            if (version == null){
                logger.info("SeckillGoodsListCache|命中本地缓存|{}", activityId);
                return seckillGoodsListCache;
            }
            //传递的版本号比缓存中的版本号小，则直接返回缓存中的数据
            if (version.compareTo(seckillGoodsListCache.getVersion()) <= 0){
                logger.info("SeckillGoodsListCache|命中本地缓存|{}", activityId);
                return seckillGoodsListCache;
            }
            //传递的版本号大于缓存中色版本号，则更新缓存
            if (version.compareTo(seckillGoodsListCache.getVersion()) > 0){
                return getDistributedCache(activityId);
            }
        }
        return getDistributedCache(activityId);
    }

    /**
     * 获取分布式缓存中的数据
     */
    private SeckillBusinessCache<List<SeckillGoods>> getDistributedCache(Long activityId) {
        logger.info("SeckillGoodsListCache|读取分布式缓存|{}", activityId);
        SeckillBusinessCache<List<SeckillGoods>> seckillGoodsListCache = SeckillGoodsBuilder.getSeckillBusinessCacheList(distributedCacheService.getObject(buildCacheKey(activityId)), SeckillGoods.class);
        //分布式缓存中的数据为空
        if (seckillGoodsListCache == null){
            //使用一个线程尝试去更新分布式缓存中的数据
            seckillGoodsListCache = tryUpdateSeckillGoodsCacheByLock(activityId, true);
        }
        if (seckillGoodsListCache != null && !seckillGoodsListCache.isRetryLater()){
            if (localCacheUpdatelock.tryLock()){
                try {
                    localCacheService.put(activityId, seckillGoodsListCache);
                    logger.info("SeckillGoodsListCache|本地缓存已经更新|{}", activityId);
                }finally {
                    localCacheUpdatelock.unlock();
                }
            }
        }
        return seckillGoodsListCache;
    }

    /**
     * 尝试去更新分布式缓存中的数据
     */
    @Override
    public SeckillBusinessCache<List<SeckillGoods>> tryUpdateSeckillGoodsCacheByLock(Long activityId, boolean doubleCheck) {
        logger.info("SeckillGoodsListCache|更新分布式缓存|{}", activityId);
        DistributedLock lock = distributedLockFactory.getDistributedLock(SECKILL_GOODS_LIST_UPDATE_CACHE_LOCK_KEY.concat(String.valueOf(activityId)));
        try {
            boolean isSuccess = lock.tryLock(2, 5, TimeUnit.SECONDS);
            if (!isSuccess){
                return new SeckillBusinessCache<List<SeckillGoods>>().retryLater();
            }
            SeckillBusinessCache<List<SeckillGoods>> seckillGoodsListCache;
            if (doubleCheck){
                //获取锁成功后，再次从缓存中获取数据，防止高并发下多个线程争抢锁的过程中，后续的线程再等待1秒的过程中，前面的线程释放了锁，后续的线程获取锁成功后再次更新分布式缓存数据
                seckillGoodsListCache = SeckillGoodsBuilder.getSeckillBusinessCacheList(distributedCacheService.getObject(buildCacheKey(activityId)), SeckillGoods.class);
                if (seckillGoodsListCache != null){
                    return seckillGoodsListCache;
                }
            }
            List<SeckillGoods> seckillGoodsList = seckillGoodsDomainService.getSeckillGoodsByActivityId(activityId);
            if (seckillGoodsList == null){
                seckillGoodsListCache = new SeckillBusinessCache<List<SeckillGoods>>().notExist();
            }else {
                seckillGoodsList = this.getSeckillGoodsList(seckillGoodsList);
                seckillGoodsListCache = new SeckillBusinessCache<List<SeckillGoods>>().with(seckillGoodsList).withVersion(SystemClock.millisClock().now());
            }
            //更新到分布式缓存中
            distributedCacheService.put(buildCacheKey(activityId), JSON.toJSONString(seckillGoodsListCache), SeckillConstants.FIVE_MINUTES);
            logger.info("SeckillGoodsListCache|分布式缓存已经更新|{}", activityId);
            return seckillGoodsListCache;
        } catch (InterruptedException e) {
            logger.info("SeckillGoodsListCache|更新分布式缓存失败|{}", activityId);
            return new SeckillBusinessCache<List<SeckillGoods>>().retryLater();
        }finally {
            lock.unlock();
        }
    }

    private List<SeckillGoods> getSeckillGoodsList(List<SeckillGoods> seckillGoodsList) {
        if (!SeckillConstants.PLACE_ORDER_TYPE_BUCKET.equals(placeOrderType)){
            return seckillGoodsList;
        }
        return seckillGoodsList.stream().map(this::getSeckillGoods).collect(Collectors.toList());
    }

    //兼容分桶库存
    private SeckillGoods getSeckillGoods(SeckillGoods seckillGoods){
        //不是分桶库存模式
        if (!SeckillConstants.PLACE_ORDER_TYPE_BUCKET.equals(placeOrderType)){
            return seckillGoods;
        }
        //是分桶库存模式，获取分桶库存
        SeckillBusinessCache<SeckillStockDTO> seckillStockCache = seckillStockDubboService.getSeckillStock(seckillGoods.getId(), 1L);
        if (seckillStockCache == null || !seckillStockCache.isExist() || seckillStockCache.isRetryLater() || seckillStockCache.getData() == null){
            return seckillGoods;
        }
        seckillGoods.setInitialStock(seckillStockCache.getData().getTotalStock());
        seckillGoods.setAvailableStock(seckillStockCache.getData().getAvailableStock());
        return seckillGoods;
    }
}
