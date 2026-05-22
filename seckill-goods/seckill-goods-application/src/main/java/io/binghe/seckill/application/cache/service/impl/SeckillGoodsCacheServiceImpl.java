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
import com.google.common.cache.Cache;
import io.binghe.seckill.application.builder.SeckillGoodsBuilder;
import io.binghe.seckill.application.cache.service.SeckillGoodsCacheService;
import io.binghe.seckill.common.cache.distribute.DistributedCacheService;
import io.binghe.seckill.common.cache.local.guava.LocalCacheFactory;
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 获取商品信息
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
public class SeckillGoodsCacheServiceImpl implements SeckillGoodsCacheService {
    private final static Logger logger = LoggerFactory.getLogger(SeckillGoodsCacheServiceImpl.class);
    private static final Cache<Long, SeckillBusinessCache<SeckillGoods>> localSeckillGoodsCacheService = LocalCacheFactory.getLocalCache();
    private static final Cache<Long, SeckillBusinessCache<Integer>> localAvailableStockCacheService = LocalCacheFactory.getLocalCache();
    //更新活动时获取分布式锁使用
    private static final String SECKILL_GOODS_UPDATE_CACHE_LOCK_KEY = "SECKILL_GOODS_UPDATE_CACHE_LOCK_KEY_";
    //本地可重入锁
    private final Lock localCacheUpdatelock = new ReentrantLock();
    @DubboReference(version = "1.0.0", check = false)
    private SeckillStockDubboService seckillStockDubboService;
    @Autowired
    private DistributedCacheService distributedCacheService;
    @Autowired
    private SeckillGoodsDomainService seckillGoodsDomainService;
    @Autowired
    private DistributedLockFactory distributedLockFactory;
    @Value("${place.order.type:lua}")
    private String placeOrderType;

    @Override
    public String buildCacheKey(Object key) {
        return StringUtil.append(SeckillConstants.SECKILL_GOODS_CACHE_KEY, key);
    }

    @Override
    public SeckillBusinessCache<SeckillGoods> getSeckillGoods(Long goodsId, Long version) {
        //从本地缓存中获取数据
        SeckillBusinessCache<SeckillGoods> seckillGoodsCache = localSeckillGoodsCacheService.getIfPresent(goodsId);
        if (seckillGoodsCache != null){
            //版本号为空，则直接返回本地缓存中的数据
            if (seckillGoodsCache.getVersion() == null){
                logger.info("SeckillGoodsCache|命中本地缓存|{}", goodsId);
                return seckillGoodsCache;
            }
            //传递的版本号小于等于缓存中的版本号，则说明缓存中的数据比客户端的数据新，直接返回本地缓存中的数据
            if (version.compareTo(seckillGoodsCache.getVersion()) <= 0){
                logger.info("SeckillGoodsCache|命中本地缓存|{}", goodsId);
                return seckillGoodsCache;
            }
            //传递的版本号大于缓存中的版本号，说明缓存中的数据比较落后，从分布式缓存获取数据并更新到本地缓存
            if (version.compareTo(seckillGoodsCache.getVersion()) > 0){
                return getDistributedCache(goodsId);
            }
        }
        return getDistributedCache(goodsId);
    }

    /**
     * 获取分布式缓存数据
     */
    private SeckillBusinessCache<SeckillGoods> getDistributedCache(Long goodsId) {
        logger.info("SeckillGoodsCache|读取分布式缓存|{}", goodsId);
        //从分布式缓存中获取数据
        SeckillBusinessCache<SeckillGoods> seckillGoodsCache = SeckillGoodsBuilder.getSeckillBusinessCache(distributedCacheService.getObject(buildCacheKey(goodsId)), SeckillGoods.class);
        //分布式缓存中没有数据
        if (seckillGoodsCache == null){
            // 尝试更新分布式缓存中的数据，注意的是只用一个线程去更新分布式缓存中的数据
            seckillGoodsCache = tryUpdateSeckillGoodsCacheByLock(goodsId, true);
        }
        //获取的数据不为空，并且不需要重试
        if (seckillGoodsCache != null && !seckillGoodsCache.isRetryLater()){
            //获取本地锁，更新本地缓存
            if (localCacheUpdatelock.tryLock()){
                try {
                    localSeckillGoodsCacheService.put(goodsId, seckillGoodsCache);
                    logger.info("SeckillGoodsCache|本地缓存已经更新|{}", goodsId);
                }finally {
                    localCacheUpdatelock.unlock();
                }
            }
        }
        return seckillGoodsCache;
    }

    @Override
    public SeckillBusinessCache<SeckillGoods> tryUpdateSeckillGoodsCacheByLock(Long goodsId, boolean doubleCheck) {
        logger.info("SeckillGoodsCache|更新分布式缓存|{}", goodsId);
        //获取分布式锁，保证只有一个线程在更新分布式缓存
        DistributedLock lock = distributedLockFactory.getDistributedLock(SECKILL_GOODS_UPDATE_CACHE_LOCK_KEY.concat(String.valueOf(goodsId)));
        try {
            boolean isSuccess = lock.tryLock(2, 5, TimeUnit.SECONDS);
            //未获取到分布式锁的线程快速返回，不占用系统资源
            if (!isSuccess){
                return new SeckillBusinessCache<SeckillGoods>().retryLater();
            }
            SeckillBusinessCache<SeckillGoods> seckillGoodsCache;
            if (doubleCheck){
                //获取锁成功后，再次从缓存中获取数据，防止高并发下多个线程争抢锁的过程中，后续的线程再等待1秒的过程中，前面的线程释放了锁，后续的线程获取锁成功后再次更新分布式缓存数据
                seckillGoodsCache = SeckillGoodsBuilder.getSeckillBusinessCache(distributedCacheService.getObject(buildCacheKey(goodsId)), SeckillGoods.class);
                if (seckillGoodsCache != null){
                    return seckillGoodsCache;
                }
            }
            SeckillGoods seckillGoods = seckillGoodsDomainService.getSeckillGoodsId(goodsId);
            if (seckillGoods == null){
                seckillGoodsCache = new SeckillBusinessCache<SeckillGoods>().notExist();
            }else {
                seckillGoodsCache = new SeckillBusinessCache<SeckillGoods>().with(this.getSeckillGoods(seckillGoods)).withVersion(SystemClock.millisClock().now());
            }
            //将数据保存到分布式缓存
            distributedCacheService.put(buildCacheKey(goodsId), JSON.toJSONString(seckillGoodsCache), SeckillConstants.FIVE_MINUTES);
            logger.info("SeckillGoodsCache|分布式缓存已经更新|{}", goodsId);
            return seckillGoodsCache;
        } catch (InterruptedException e) {
            logger.error("SeckillGoodsCache|更新分布式缓存失败|{}", goodsId);
            return new SeckillBusinessCache<SeckillGoods>().retryLater();
        }finally {
            lock.unlock();
        }
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

    @Override
    public SeckillBusinessCache<Integer> getAvailableStock(Long goodsId, Long version) {
        SeckillBusinessCache<Integer> availableStockCache = localAvailableStockCacheService.getIfPresent(goodsId);
        if (availableStockCache != null){
            //版本号为空，则直接返回本地缓存中的数据
            if (availableStockCache.getVersion() == null){
                logger.info("availableStockCache|命中本地缓存|{}", goodsId);
                return availableStockCache;
            }
            //传递的版本号小于等于缓存中的版本号，则说明缓存中的数据比客户端的数据新，直接返回本地缓存中的数据
            if (version.compareTo(availableStockCache.getVersion()) <= 0){
                logger.info("availableStockCache|命中本地缓存|{}", goodsId);
                return availableStockCache;
            }
            //传递的版本号大于缓存中的版本号，说明缓存中的数据比较落后，从分布式缓存获取数据并更新到本地缓存
            if (version.compareTo(availableStockCache.getVersion()) > 0){
                return getDistributedAvailableCache(goodsId);
            }
        }
        return getDistributedAvailableCache(goodsId);
    }

    /**
     * 从分布式缓存获取数据
     */
    private SeckillBusinessCache<Integer> getDistributedAvailableCache(Long goodsId) {
        logger.info("SeckillGoodsCache|读取分布式缓存|{}", goodsId);
        //从分布式缓存中获取数据
        String goodsKey = SeckillConstants.getKey(SeckillConstants.GOODS_ITEM_STOCK_KEY_PREFIX, String.valueOf(goodsId));
        //获取商品库存
        Integer availableStock = distributedCacheService.getObject(goodsKey, Integer.class);
        if (availableStock == null){
            availableStock = seckillGoodsDomainService.getAvailableStockById(goodsId);
            distributedCacheService.put(goodsKey, availableStock);
        }
        SeckillBusinessCache<Integer> availableStockCache = new SeckillBusinessCache<Integer>().with(availableStock).withVersion(SystemClock.millisClock().now());
        localAvailableStockCacheService.put(goodsId, availableStockCache);
        return availableStockCache;
    }
}
