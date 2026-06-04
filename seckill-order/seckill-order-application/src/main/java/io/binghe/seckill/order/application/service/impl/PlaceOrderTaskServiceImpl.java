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
package io.binghe.seckill.order.application.service.impl;

import cn.hutool.core.util.NumberUtil;
import io.binghe.seckill.common.cache.distribute.DistributedCacheService;
import io.binghe.seckill.common.cache.local.LocalCacheService;
import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.common.lock.DistributedLock;
import io.binghe.seckill.common.lock.factoty.DistributedLockFactory;
import io.binghe.seckill.dubbo.interfaces.goods.SeckillGoodsDubboService;
import io.binghe.seckill.dubbo.interfaces.stock.SeckillStockDubboService;
import io.binghe.seckill.mq.MessageSenderService;
import io.binghe.seckill.order.application.model.task.SeckillOrderTask;
import io.binghe.seckill.order.application.service.PlaceOrderTaskService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 订单任务服务实现类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
@ConditionalOnProperty(name = "submit.order.type", havingValue = "async")
public class PlaceOrderTaskServiceImpl implements PlaceOrderTaskService {
    private final Logger logger = LoggerFactory.getLogger(PlaceOrderTaskServiceImpl.class);
    @Autowired
    private MessageSenderService messageSenderService;
    @Autowired
    private LocalCacheService<Long, Integer> localCacheService;
    @Autowired
    private DistributedCacheService distributedCacheService;
    @Autowired
    private DistributedLockFactory distributedLockFactory;
    @DubboReference(version = "1.0.0", check = false)
    private SeckillStockDubboService seckillStockDubboService;
    @DubboReference(version = "1.0.0", check = false)
    private SeckillGoodsDubboService seckillGoodsDubboService;
    @Value("${submit.order.token.multiple:1.5}")
    private Double multiple;
    @Value("${place.order.type:lua}")
    private String placeOrderType;

    //可重入锁
    private Lock lock = new ReentrantLock();

    @Override
    public boolean submitOrderTask(SeckillOrderTask seckillOrderTask) {
        if (seckillOrderTask == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        String taskKey = SeckillConstants.getKey(SeckillConstants.ORDER_TASK_ID_KEY, seckillOrderTask.getOrderTaskId());
        //检测是否执行过
        Long result = distributedCacheService.checkExecute(taskKey, SeckillConstants.ORDER_TASK_EXPIRE_SECONDS);
        //已经执行过恢复缓存库存的方法
        if (NumberUtil.equals(result, SeckillConstants.CHECK_RECOVER_STOCK_HAS_EXECUTE)){
            throw new SeckillException(ErrorCode.REDUNDANT_SUBMIT);
        }
        //获取可用的下单许可
        Long goodsId = seckillOrderTask.getSeckillOrderCommand().getGoodsId();
        Integer availableOrderTokens = this.getAvailableOrderTokens(goodsId);
        //不存在下单许可
        if (availableOrderTokens == null || availableOrderTokens <= 0){
            throw new SeckillException(ErrorCode.ORDER_TOKENS_NOT_AVAILABLE);
        }
        //未获取到下单许可
        if (!this.takeOrderToken(goodsId)){
            logger.info("submitOrderTask|获取下单许可失败|{},{}", seckillOrderTask.getUserId(), seckillOrderTask.getOrderTaskId());
            throw new SeckillException(ErrorCode.ORDER_TOKENS_NOT_AVAILABLE);
        }
        //发送消息
        boolean sendSuccess = messageSenderService.send(seckillOrderTask);
        if (!sendSuccess){
            logger.info("submitOrderTask|下单任务提交失败|{},{}", seckillOrderTask.getUserId(), seckillOrderTask.getOrderTaskId());
            //恢复下单许可
            this.recoverOrderToken(goodsId);
            //清除是否被执行过的数据
            distributedCacheService.delete(taskKey);
        }
        return sendSuccess;
    }

    /**
     * 获取下单许可，需要重点理解为何要循环三次处理业务
     */
    private boolean takeOrderToken(Long goodsId){
        for (int i = 0; i < 3; i++){
            Long result = distributedCacheService.takeOrderToken(SeckillConstants.getKey(SeckillConstants.ORDER_TASK_AVAILABLE_TOKENS_KEY, String.valueOf(goodsId)));
            if (result == null){
                return false;
            }
            if (result == SeckillConstants.LUA_RESULT_NOT_EXECUTE){
                this.refreshLatestAvailableTokens(goodsId, false);
                continue;
            }
            return result == SeckillConstants.LUA_RESULT_EXECUTE_TOKEN_SUCCESS;
        }
        return false;
    }

    /**
     * 恢复下单许可
     */
    private boolean recoverOrderToken(Long goodsId){
        Long result = distributedCacheService.recoverOrderToken(SeckillConstants.getKey(SeckillConstants.ORDER_TASK_AVAILABLE_TOKENS_KEY, String.valueOf(goodsId)));
        if (result == null){
            return false;
        }
        if (result == SeckillConstants.LUA_RESULT_NOT_EXECUTE){
            this.refreshLatestAvailableTokens(goodsId, false);
            return true;
        }
        return result == SeckillConstants.LUA_RESULT_EXECUTE_TOKEN_SUCCESS;
    }
    /**
     * 获取可用的下单许可
     */
    private Integer getAvailableOrderTokens(Long goodsId) {
        Integer availableOrderTokens = localCacheService.getIfPresent(goodsId);
        if (availableOrderTokens != null){
            return availableOrderTokens;
        }
        return this.refreshLocalAvailableTokens(goodsId, true);
    }

    /**
     * 刷新本地缓存可用的下单许可，注意DoubleCheck
     */
    private Integer refreshLocalAvailableTokens(Long goodsId, boolean doubleCheck) {
        if (doubleCheck){
            Integer availableOrderTokens = localCacheService.getIfPresent(goodsId);
            if (availableOrderTokens != null){
                return availableOrderTokens;
            }
        }
        String availableTokensKey = SeckillConstants.getKey(SeckillConstants.ORDER_TASK_AVAILABLE_TOKENS_KEY, String.valueOf(goodsId));
        Integer latestAvailableOrderTokens = distributedCacheService.getObject(availableTokensKey, Integer.class);
        if (latestAvailableOrderTokens != null){
            if (lock.tryLock()){
                try{
                    localCacheService.put(goodsId, latestAvailableOrderTokens);
                }finally {
                    lock.unlock();
                }
            }
            return latestAvailableOrderTokens;
        }
        return this.refreshLatestAvailableTokens(goodsId, false);
    }

    /**
     * 刷新分布式缓存的下单许可，double check
     */
    private Integer refreshLatestAvailableTokens(Long goodsId, boolean doubleCheck) {
        String lockKey = SeckillConstants.getKey(SeckillConstants.LOCK_REFRESH_LATEST_AVAILABLE_TOKENS_KEY, String.valueOf(goodsId));
        DistributedLock distributedLock = distributedLockFactory.getDistributedLock(lockKey);
        try{
            boolean isLock = distributedLock.tryLock();
            if (!isLock){
                return null;
            }
            Integer latestAvailableOrderTokens;
            String availableTokensKey = SeckillConstants.getKey(SeckillConstants.ORDER_TASK_AVAILABLE_TOKENS_KEY, String.valueOf(goodsId));
            if (doubleCheck){
                //本地缓存已经存在数据
                Integer availableOrderTokens = localCacheService.getIfPresent(goodsId);
                if (availableOrderTokens != null){
                    return availableOrderTokens;
                }
                //获取分布式缓存数据
                latestAvailableOrderTokens = distributedCacheService.getObject(availableTokensKey, Integer.class);
                //分布式缓存中存在数据，设置到本地缓存，并且返回数据
                if (latestAvailableOrderTokens != null){
                    localCacheService.put(goodsId, latestAvailableOrderTokens);
                    return latestAvailableOrderTokens;
                }
            }
            //本地缓存和分布式缓存都没有数据，获取商品的库存数据
//            String goodsKey = SeckillConstants.getKey(SeckillConstants.GOODS_ITEM_STOCK_KEY_PREFIX, String.valueOf(goodsId));
//            //获取商品库存
//            Integer availableStock = distributedCacheService.getObject(goodsKey, Integer.class);
//            if (availableStock == null || availableStock <= 0){
//                return null;
//            }
            SeckillBusinessCache<Integer> availableStockCache = null;
            //分桶模式
            if (SeckillConstants.PLACE_ORDER_TYPE_BUCKET.equals(placeOrderType)){
                availableStockCache = seckillStockDubboService.getAvailableStock(goodsId, 1L);
            }else{
                availableStockCache = seckillGoodsDubboService.getAvailableStock(goodsId, 1L);
            }
            if (availableStockCache == null || !availableStockCache.isExist() || availableStockCache.isRetryLater() || availableStockCache.getData() == null){
                return null;
            }
            Integer availableStock = availableStockCache.getData();
            //根据配置的比例计算下单许可
            latestAvailableOrderTokens = (int) Math.ceil(availableStock * multiple);
            distributedCacheService.put(availableTokensKey, latestAvailableOrderTokens, SeckillConstants.ORDER_TASK_EXPIRE_SECONDS, TimeUnit.SECONDS);
            localCacheService.put(goodsId, latestAvailableOrderTokens);
            return latestAvailableOrderTokens;
        }catch (Exception e){
            logger.error("refreshLatestAvailableTokens|刷新下单许可失败:{}", goodsId, e);
        }finally {
            distributedLock.unlock();
        }
        return null;
    }
}
