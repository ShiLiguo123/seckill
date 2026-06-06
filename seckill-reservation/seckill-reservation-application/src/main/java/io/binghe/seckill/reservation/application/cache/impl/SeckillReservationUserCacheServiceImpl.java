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
package io.binghe.seckill.reservation.application.cache.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import io.binghe.seckill.common.cache.distribute.DistributedCacheService;
import io.binghe.seckill.common.cache.local.guava.LocalCacheFactory;
import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.lock.DistributedLock;
import io.binghe.seckill.common.lock.factoty.DistributedLockFactory;
import io.binghe.seckill.common.model.enums.SeckillReservationUserStatus;
import io.binghe.seckill.common.utils.string.StringUtil;
import io.binghe.seckill.common.utils.time.SystemClock;
import io.binghe.seckill.reservation.application.builder.SeckillReservationUserBuilder;
import io.binghe.seckill.reservation.application.cache.SeckillReservationUserCacheService;
import io.binghe.seckill.reservation.domain.event.SeckillReservationUserEvent;
import io.binghe.seckill.reservation.domain.model.entity.SeckillReservationUser;
import io.binghe.seckill.reservation.domain.service.SeckillReservationDomainService;
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
 * @description SeckillReservationUserCacheService实现类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
public class SeckillReservationUserCacheServiceImpl implements SeckillReservationUserCacheService {
    private final static Logger logger = LoggerFactory.getLogger(SeckillReservationUserCacheServiceImpl.class);
    //存储预约信息
    private static final Cache<String, SeckillBusinessCache<SeckillReservationUser>> localSeckillReservationUserCacheService = LocalCacheFactory.getLocalCache();
    //存储用户维度预约列表
    private static final Cache<Long, SeckillBusinessCache<List<SeckillReservationUser>>> localSeckillReservationUserListCacheService = LocalCacheFactory.getLocalCache();
    //存储商品维度预约列表
    private static final Cache<Long, SeckillBusinessCache<List<SeckillReservationUser>>> localSeckillReservationGoodsListCacheService = LocalCacheFactory.getLocalCache();
    //更新预约时获取分布式锁使用
    private static final String SECKILL_RESERVATION_USER_UPDATE_CACHE_LOCK_KEY = "SECKILL_RESERVATION_USER_UPDATE_CACHE_LOCK_KEY_";
    //更新用户维度预约列表时获取分布式锁使用
    private static final String SECKILL_RESERVATION_USER_LIST_UPDATE_CACHE_LOCK_KEY = "SECKILL_RESERVATION_USER_LIST_UPDATE_CACHE_LOCK_KEY_";
    //更新商品维度预约列表时获取分布式锁使用
    private static final String SECKILL_RESERVATION_GOODS_LIST_UPDATE_CACHE_LOCK_KEY = "SECKILL_RESERVATION_GOODS_LIST_UPDATE_CACHE_LOCK_KEY_";
    //本地可重入锁
    private final Lock localCacheUpdatelock = new ReentrantLock();
    //本地用户维度列表可重入锁
    private final Lock localCacheUserListUpdatelock = new ReentrantLock();
    //本地商品维度列表可重入锁
    private final Lock localCacheGoodsListUpdatelock = new ReentrantLock();
    @Autowired
    private DistributedCacheService distributedCacheService;
    @Autowired
    private SeckillReservationDomainService seckillReservationDomainService;
    @Autowired
    private DistributedLockFactory distributedLockFactory;

    @Override
    public SeckillBusinessCache<SeckillReservationUser> getSeckillReservationUserCacheByUserIdAndGoodsId(Long userId, Long goodsId, Long version) {
        String localKey = StringUtil.append(userId, goodsId);
        //从本地缓存中获取数据
        SeckillBusinessCache<SeckillReservationUser> seckillReservationUserCache = localSeckillReservationUserCacheService.getIfPresent(localKey);
        if (seckillReservationUserCache != null){
            //版本号为空，则直接返回本地缓存中的数据
            if (seckillReservationUserCache.getVersion() == null){
                logger.info("SeckillReservationUserCache|命中本地缓存|{}|{}", userId, goodsId);
                return seckillReservationUserCache;
            }
            //传递的版本号小于等于缓存中的版本号，则说明缓存中的数据比客户端的数据新，直接返回本地缓存中的数据
            if (version.compareTo(seckillReservationUserCache.getVersion()) <= 0){
                logger.info("SeckillReservationUserCache|命中本地缓存|{}|{}", userId, goodsId);
                return seckillReservationUserCache;
            }
            //传递的版本号大于缓存中的版本号，说明缓存中的数据比较落后，从分布式缓存获取数据并更新到本地缓存
            if (version.compareTo(seckillReservationUserCache.getVersion()) > 0){
                return getSeckillReservationUserDistributedCache(userId, goodsId);
            }
        }
        return getSeckillReservationUserDistributedCache(userId, goodsId);
    }

    /**
     * 获取分布式缓存数据
     */
    private SeckillBusinessCache<SeckillReservationUser> getSeckillReservationUserDistributedCache(Long userId, Long goodsId) {
        logger.info("SeckillReservationUserCache|读取分布式缓存|{}|{}", userId, goodsId);
        String localKey = StringUtil.append(userId, goodsId);
        String distributeKey = StringUtil.append(SeckillConstants.SECKILL_RESERVATION_USER_CACHE_KEY, localKey);
        //从分布式缓存中获取数据
        SeckillBusinessCache<SeckillReservationUser> seckillReservationUserCache = SeckillReservationUserBuilder.getSeckillBusinessCache(distributedCacheService.getObject(distributeKey), SeckillReservationUser.class);
        //分布式缓存中没有数据
        if (seckillReservationUserCache == null){
            // 尝试更新分布式缓存中的数据，注意的是只用一个线程去更新分布式缓存中的数据
            seckillReservationUserCache = tryUpdateSeckillReservationUserCacheByUserIdAndGoodsId(userId, goodsId, true);
        }
        //获取的数据不为空，并且不需要重试
        if (seckillReservationUserCache != null && !seckillReservationUserCache.isRetryLater()){
            //获取本地锁，更新本地缓存
            if (localCacheUpdatelock.tryLock()){
                try {
                    localSeckillReservationUserCacheService.put(localKey, seckillReservationUserCache);
                    logger.info("SeckillReservationUserCache|本地缓存已经更新|{}|{}", userId, goodsId);
                }finally {
                    localCacheUpdatelock.unlock();
                }
            }
        }
        return seckillReservationUserCache;
    }

    @Override
    public SeckillBusinessCache<SeckillReservationUser> tryUpdateSeckillReservationUserCacheByUserIdAndGoodsId(Long userId, Long goodsId, boolean doubleCheck) {
        logger.info("SeckillReservationUserCache|更新分布式缓存|{}|{}", userId, goodsId);
        String localKey = StringUtil.append(userId, goodsId);
        String distributeKey = StringUtil.append(SeckillConstants.SECKILL_RESERVATION_USER_CACHE_KEY, localKey);
        //获取分布式锁，保证只有一个线程在更新分布式缓存
        DistributedLock lock = distributedLockFactory.getDistributedLock(SECKILL_RESERVATION_USER_UPDATE_CACHE_LOCK_KEY.concat(localKey));
        try {
            boolean isSuccess = lock.tryLock(2, 5, TimeUnit.SECONDS);
            //未获取到分布式锁的线程快速返回，不占用系统资源
            if (!isSuccess){
                return new SeckillBusinessCache<SeckillReservationUser>().retryLater();
            }
            SeckillBusinessCache<SeckillReservationUser> seckillReservationUserCache;
            if (doubleCheck){
                //获取锁成功后，再次从缓存中获取数据，防止高并发下多个线程争抢锁的过程中，后续的线程再等待1秒的过程中，前面的线程释放了锁，后续的线程获取锁成功后再次更新分布式缓存数据
                seckillReservationUserCache = SeckillReservationUserBuilder.getSeckillBusinessCache(distributedCacheService.getObject(distributeKey), SeckillReservationUser.class);
                if (seckillReservationUserCache != null){
                    return seckillReservationUserCache;
                }
            }
            SeckillReservationUser seckillReservationUser = seckillReservationDomainService.getSeckillReservationUser(userId, goodsId, SeckillReservationUserStatus.NORMAL.getCode());
            if (seckillReservationUser == null){
                seckillReservationUserCache = new SeckillBusinessCache<SeckillReservationUser>().notExist();
            }else {
                seckillReservationUserCache = new SeckillBusinessCache<SeckillReservationUser>().with(seckillReservationUser).withVersion(SystemClock.millisClock().now());
            }
            //将数据保存到分布式缓存
            distributedCacheService.put(distributeKey, JSON.toJSONString(seckillReservationUserCache), SeckillConstants.HOURS_24);
            logger.info("SeckillReservationUserCache|分布式缓存已经更新|{}|{}", userId, goodsId);
            return seckillReservationUserCache;
        } catch (InterruptedException e) {
            logger.error("SeckillReservationUserCache|更新分布式缓存失败|{}|{}", userId, goodsId);
            return new SeckillBusinessCache<SeckillReservationUser>().retryLater();
        }finally {
            lock.unlock();
        }
    }

    @Override
    public SeckillBusinessCache<List<SeckillReservationUser>> getUserListCacheByGoodsId(Long goodsId, Long version) {
        //从本地缓存中获取数据
        SeckillBusinessCache<List<SeckillReservationUser>> seckillReservationUserListCache = localSeckillReservationUserListCacheService.getIfPresent(goodsId);
        if (seckillReservationUserListCache != null){
            //版本号为空，则直接返回本地缓存中的数据
            if (seckillReservationUserListCache.getVersion() == null){
                logger.info("SeckillReservationUserListCache|命中本地缓存|{}",  goodsId);
                return seckillReservationUserListCache;
            }
            //传递的版本号小于等于缓存中的版本号，则说明缓存中的数据比客户端的数据新，直接返回本地缓存中的数据
            if (version.compareTo(seckillReservationUserListCache.getVersion()) <= 0){
                logger.info("SeckillReservationUserListCache|命中本地缓存|{}", goodsId);
                return seckillReservationUserListCache;
            }
            //传递的版本号大于缓存中的版本号，说明缓存中的数据比较落后，从分布式缓存获取数据并更新到本地缓存
            if (version.compareTo(seckillReservationUserListCache.getVersion()) > 0){
                return getSeckillReservationUserListDistributedCache(goodsId);
            }
        }
        return getSeckillReservationUserListDistributedCache(goodsId);
    }

    /**
     * 从分布式缓存获取数据
     */
    private SeckillBusinessCache<List<SeckillReservationUser>> getSeckillReservationUserListDistributedCache(Long goodsId) {
        logger.info("SeckillReservationUserListCache|读取分布式缓存|{}", goodsId);
        String distributeKey = StringUtil.append(SeckillConstants.SECKILL_RESERVATION_USER_LIST_CACHE_KEY, goodsId);
        //从分布式缓存中获取数据
        SeckillBusinessCache<List<SeckillReservationUser>> seckillReservationUserListCache = SeckillReservationUserBuilder.getSeckillBusinessCacheList(distributedCacheService.getObject(distributeKey), SeckillReservationUser.class);
        //分布式缓存中没有数据
        if (seckillReservationUserListCache == null){
            // 尝试更新分布式缓存中的数据，注意的是只用一个线程去更新分布式缓存中的数据
            seckillReservationUserListCache = tryUpdatetUserListCacheByGoodsId(goodsId, true);
        }
        //获取的数据不为空，并且不需要重试
        if (seckillReservationUserListCache != null && !seckillReservationUserListCache.isRetryLater()){
            //获取本地锁，更新本地缓存
            if (localCacheUserListUpdatelock.tryLock()){
                try {
                    localSeckillReservationUserListCacheService.put(goodsId, seckillReservationUserListCache);
                    logger.info("SeckillReservationUserListCache|本地缓存已经更新|{}", goodsId);
                }finally {
                    localCacheUserListUpdatelock.unlock();
                }
            }
        }
        return seckillReservationUserListCache;
    }

    @Override
    public SeckillBusinessCache<List<SeckillReservationUser>> tryUpdatetUserListCacheByGoodsId(Long goodsId, boolean doubleCheck) {
        logger.info("SeckillReservationUserListCache|更新分布式缓存|{}", goodsId);
        String distributeKey = StringUtil.append(SeckillConstants.SECKILL_RESERVATION_USER_LIST_CACHE_KEY, goodsId);
        //获取分布式锁，保证只有一个线程在更新分布式缓存
        DistributedLock lock = distributedLockFactory.getDistributedLock(SECKILL_RESERVATION_USER_LIST_UPDATE_CACHE_LOCK_KEY.concat(String.valueOf(goodsId)));
        try {
            boolean isSuccess = lock.tryLock(2, 5, TimeUnit.SECONDS);
            //未获取到分布式锁的线程快速返回，不占用系统资源
            if (!isSuccess){
                return new SeckillBusinessCache<List<SeckillReservationUser>>().retryLater();
            }
            SeckillBusinessCache<List<SeckillReservationUser>> seckillReservationUserListCache;
            if (doubleCheck){
                //获取锁成功后，再次从缓存中获取数据，防止高并发下多个线程争抢锁的过程中，后续的线程再等待1秒的过程中，前面的线程释放了锁，后续的线程获取锁成功后再次更新分布式缓存数据
                seckillReservationUserListCache = SeckillReservationUserBuilder.getSeckillBusinessCacheList(distributedCacheService.getObject(distributeKey), SeckillReservationUser.class);
                if (seckillReservationUserListCache != null){
                    return seckillReservationUserListCache;
                }
            }
            List<SeckillReservationUser> seckillReservationUserList = seckillReservationDomainService.getUserListByGoodsId(goodsId, SeckillReservationUserStatus.NORMAL.getCode());
            if (seckillReservationUserList == null || seckillReservationUserList.isEmpty()){
                seckillReservationUserListCache = new SeckillBusinessCache<List<SeckillReservationUser>>().notExist();
            }else {
                seckillReservationUserListCache = new SeckillBusinessCache<List<SeckillReservationUser>>().with(seckillReservationUserList).withVersion(SystemClock.millisClock().now());
            }
            //将数据保存到分布式缓存
            distributedCacheService.put(distributeKey, JSON.toJSONString(seckillReservationUserListCache), SeckillConstants.HOURS_24);
            logger.info("SeckillReservationUserListCache|分布式缓存已经更新|{}", goodsId);
            return seckillReservationUserListCache;
        } catch (InterruptedException e) {
            logger.error("SeckillReservationUserListCache|更新分布式缓存失败|{}", goodsId);
            return new SeckillBusinessCache<List<SeckillReservationUser>>().retryLater();
        }finally {
            lock.unlock();
        }
    }

    @Override
    public SeckillBusinessCache<List<SeckillReservationUser>> getGoodsListCacheByUserId(Long userId, Long version) {
        //从本地缓存中获取数据
        SeckillBusinessCache<List<SeckillReservationUser>> seckillReservationGoodsListCache = localSeckillReservationGoodsListCacheService.getIfPresent(userId);
        if (seckillReservationGoodsListCache != null){
            //版本号为空，则直接返回本地缓存中的数据
            if (seckillReservationGoodsListCache.getVersion() == null){
                logger.info("SeckillReservationGoodsListCache|命中本地缓存|{}",  userId);
                return seckillReservationGoodsListCache;
            }
            //传递的版本号小于等于缓存中的版本号，则说明缓存中的数据比客户端的数据新，直接返回本地缓存中的数据
            if (version.compareTo(seckillReservationGoodsListCache.getVersion()) <= 0){
                logger.info("SeckillReservationGoodsListCache|命中本地缓存|{}", userId);
                return seckillReservationGoodsListCache;
            }
            //传递的版本号大于缓存中的版本号，说明缓存中的数据比较落后，从分布式缓存获取数据并更新到本地缓存
            if (version.compareTo(seckillReservationGoodsListCache.getVersion()) > 0){
                return getSeckillReservationGoodsListDistributedCache(userId);
            }
        }
        return getSeckillReservationGoodsListDistributedCache(userId);
    }

    /**
     * 获取分布式缓存数据
     */
    private SeckillBusinessCache<List<SeckillReservationUser>> getSeckillReservationGoodsListDistributedCache(Long userId) {
        logger.info("SeckillReservationGoodsListCache|读取分布式缓存|{}", userId);
        String distributeKey = StringUtil.append(SeckillConstants.SECKILL_RESERVATION_USER_LIST_CACHE_KEY, userId);
        //从分布式缓存中获取数据
        SeckillBusinessCache<List<SeckillReservationUser>> seckillReservationUserListCache = SeckillReservationUserBuilder.getSeckillBusinessCacheList(distributedCacheService.getObject(distributeKey), SeckillReservationUser.class);
        //分布式缓存中没有数据
        if (seckillReservationUserListCache == null){
            // 尝试更新分布式缓存中的数据，注意的是只用一个线程去更新分布式缓存中的数据
            seckillReservationUserListCache = tryUpdateGoodsListCacheByUserId(userId, true);
        }
        //获取的数据不为空，并且不需要重试
        if (seckillReservationUserListCache != null && !seckillReservationUserListCache.isRetryLater()){
            //获取本地锁，更新本地缓存
            if (localCacheGoodsListUpdatelock.tryLock()){
                try {
                    localSeckillReservationGoodsListCacheService.put(userId, seckillReservationUserListCache);
                    logger.info("SeckillReservationGoodsListCache|本地缓存已经更新|{}", userId);
                }finally {
                    localCacheGoodsListUpdatelock.unlock();
                }
            }
        }
        return seckillReservationUserListCache;
    }

    @Override
    public SeckillBusinessCache<List<SeckillReservationUser>> tryUpdateGoodsListCacheByUserId(Long userId, boolean doubleCheck) {
        logger.info("SeckillReservationGoodsListCache|更新分布式缓存|{}", userId);
        String distributeKey = StringUtil.append(SeckillConstants.SECKILL_RESERVATION_USER_LIST_CACHE_KEY, userId);
        //获取分布式锁，保证只有一个线程在更新分布式缓存
        DistributedLock lock = distributedLockFactory.getDistributedLock(SECKILL_RESERVATION_GOODS_LIST_UPDATE_CACHE_LOCK_KEY.concat(String.valueOf(userId)));
        try {
            boolean isSuccess = lock.tryLock(2, 5, TimeUnit.SECONDS);
            //未获取到分布式锁的线程快速返回，不占用系统资源
            if (!isSuccess){
                return new SeckillBusinessCache<List<SeckillReservationUser>>().retryLater();
            }
            SeckillBusinessCache<List<SeckillReservationUser>> seckillReservationUserListCache;
            if (doubleCheck){
                //获取锁成功后，再次从缓存中获取数据，防止高并发下多个线程争抢锁的过程中，后续的线程再等待1秒的过程中，前面的线程释放了锁，后续的线程获取锁成功后再次更新分布式缓存数据
                seckillReservationUserListCache = SeckillReservationUserBuilder.getSeckillBusinessCacheList(distributedCacheService.getObject(distributeKey), SeckillReservationUser.class);
                if (seckillReservationUserListCache != null){
                    return seckillReservationUserListCache;
                }
            }
            List<SeckillReservationUser> seckillReservationUserList = seckillReservationDomainService.getGoodsListByUserId(userId, SeckillReservationUserStatus.NORMAL.getCode());
            if (seckillReservationUserList == null || seckillReservationUserList.isEmpty()){
                seckillReservationUserListCache = new SeckillBusinessCache<List<SeckillReservationUser>>().notExist();
            }else {
                seckillReservationUserListCache = new SeckillBusinessCache<List<SeckillReservationUser>>().with(seckillReservationUserList).withVersion(SystemClock.millisClock().now());
            }
            //将数据保存到分布式缓存
            distributedCacheService.put(distributeKey, JSON.toJSONString(seckillReservationUserListCache), SeckillConstants.HOURS_24);
            logger.info("SeckillReservationGoodsListCache|分布式缓存已经更新|{}", userId);
            return seckillReservationUserListCache;
        } catch (InterruptedException e) {
            logger.error("SeckillReservationGoodsListCache|更新分布式缓存失败|{}", userId);
            return new SeckillBusinessCache<List<SeckillReservationUser>>().retryLater();
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteSeckillReservationUserFromCache(SeckillReservationUserEvent seckillReservationUserEvent) {
        String localKey = StringUtil.append(seckillReservationUserEvent.getId(), seckillReservationUserEvent.getGoodsId());
        localSeckillReservationUserCacheService.invalidate(localKey);

        String distributeKey = StringUtil.append(SeckillConstants.SECKILL_RESERVATION_USER_CACHE_KEY, localKey);
        distributedCacheService.delete(distributeKey);

        localSeckillReservationUserListCacheService.invalidate(seckillReservationUserEvent.getGoodsId());
        distributeKey = StringUtil.append(SeckillConstants.SECKILL_RESERVATION_USER_LIST_CACHE_KEY, seckillReservationUserEvent.getGoodsId());
        distributedCacheService.delete(distributeKey);

        localSeckillReservationGoodsListCacheService.invalidate(seckillReservationUserEvent.getId());
        distributeKey = StringUtil.append(SeckillConstants.SECKILL_RESERVATION_USER_LIST_CACHE_KEY, seckillReservationUserEvent.getId());
        distributedCacheService.delete(distributeKey);
    }

}
