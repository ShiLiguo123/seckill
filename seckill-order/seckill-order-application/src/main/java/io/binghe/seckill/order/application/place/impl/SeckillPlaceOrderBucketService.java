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
package io.binghe.seckill.order.application.place.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.Cache;
import io.binghe.seckill.common.cache.distribute.DistributedCacheService;
import io.binghe.seckill.common.cache.local.guava.LocalCacheFactory;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.common.model.dto.goods.SeckillGoodsDTO;
import io.binghe.seckill.common.model.message.TxMessage;
import io.binghe.seckill.common.utils.id.SnowFlakeFactory;
import io.binghe.seckill.dubbo.interfaces.goods.SeckillGoodsDubboService;
import io.binghe.seckill.dubbo.interfaces.stock.SeckillStockDubboService;
import io.binghe.seckill.mq.MessageSenderService;
import io.binghe.seckill.order.application.model.command.SeckillOrderCommand;
import io.binghe.seckill.order.application.place.SeckillPlaceOrderService;
import io.binghe.seckill.order.domain.model.entity.SeckillOrder;
import io.binghe.seckill.order.domain.service.SeckillOrderDomainService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 基于库存分桶下单
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
@ConditionalOnProperty(name = "place.order.type", havingValue = "bucket")
public class SeckillPlaceOrderBucketService implements SeckillPlaceOrderService {
    private final Logger logger = LoggerFactory.getLogger(SeckillPlaceOrderBucketService.class);
    //本地存放库存分桶的缓存
    private static final Cache<Long, Integer> localBucketsQuantityCache = LocalCacheFactory.getLocalCache();

    @DubboReference(version = "1.0.0", check = false)
    private SeckillStockDubboService seckillStockDubboService;
    @DubboReference(version = "1.0.0", check = false)
    private SeckillGoodsDubboService seckillGoodsDubboService;
    @Autowired
    private SeckillOrderDomainService seckillOrderDomainService;
    @Autowired
    private MessageSenderService messageSenderService;
    @Autowired
    private DistributedCacheService distributedCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long placeOrder(Long userId, SeckillOrderCommand seckillOrderCommand) {
        if (userId == null || seckillOrderCommand == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        //获取商品
        SeckillGoodsDTO seckillGoods = seckillGoodsDubboService.getSeckillGoods(seckillOrderCommand.getGoodsId(), seckillOrderCommand.getVersion());
        //检测商品信息
        this.checkSeckillGoods(seckillOrderCommand, seckillGoods);
        //获取txNo
        long txNo = SnowFlakeFactory.getSnowFlakeFromCache().nextId();
        //获取分桶数量
        Integer bucketsQuantity = this.getBucketsQuantity(seckillOrderCommand.getGoodsId());
        //计算用户下单的分桶
        Integer bucketSerialNo = this.getBucketSerialNo(userId, bucketsQuantity);
        //获取库存分桶数据key
        String stockBucketKey = this.getStockBucketKey(seckillOrderCommand.getGoodsId(), bucketSerialNo);
        //获取库存编排时加锁的Key
        String stockBucketSuspendKey = this.getStockBucketSuspendKey(seckillOrderCommand.getGoodsId());
        //获取库存校对key
        String stockBucketAlignKey = this.getStockBucketAlignKey(seckillOrderCommand.getGoodsId());
        //封装执行Lua脚本的Key
        List<String> keys = Arrays.asList(stockBucketKey, stockBucketSuspendKey, stockBucketAlignKey);
        //异常标志
        boolean exception = false;
        Long decrementResult = 0L;
        try{
            //扣减缓存中的分桶库存
            decrementResult = distributedCacheService.decrementBucketStock(keys, seckillOrderCommand.getQuantity());
            this.checkResult(decrementResult);
        }catch (Exception e){
            exception = true;
            logger.error("SeckillPlaceOrderLuaService|下单异常|参数:{}|异常信息:{}", JSONObject.toJSONString(seckillOrderCommand), e.getMessage());
            //扣减分桶库存的脚本执行成功，则需要回滚分桶库存
            if (decrementResult == SeckillConstants.LUA_BUCKET_STOCK_EXECUTE_SUCCESS){
                Long incrementResult = distributedCacheService.incrementBucketStock(keys, seckillOrderCommand.getQuantity());
                if (incrementResult != SeckillConstants.LUA_BUCKET_STOCK_EXECUTE_SUCCESS){
                    logger.error("placeOrder|恢复预扣减的库存失败|{}|{}", userId, JSONUtil.toJsonStr(seckillOrderCommand), e);
                }
            }
        }
        //发送事务消息
        messageSenderService.sendMessageInTransaction(this.getTxMessage(SeckillConstants.TOPIC_BUCKET_TX_MSG, txNo, userId, SeckillConstants.PLACE_ORDER_TYPE_BUCKET, exception, seckillOrderCommand, seckillGoods, bucketSerialNo, seckillOrderCommand.getOrderTaskId()), null);
        return txNo;
    }

    /**
     * 检测结果的执行情况
     */
    private void checkResult(Long result) {
        //分桶库存不存在
        if (result == null || result == SeckillConstants.LUA_BUCKET_STOCK_NOT_EXISTS){
            throw new SeckillException(ErrorCode.BUCKET_STOCK_NOT_EXISTS);
        }
        //库存维护中
        if (result == SeckillConstants.LUA_BUCKET_STOCK_SUSPEND){
            throw new SeckillException(ErrorCode.BUCKET_STOCK_SUSPEND);
        }
        //库存校准中
        if(result == SeckillConstants.LUA_BUCKET_STOCK_CALIBRATION){
            throw new SeckillException(ErrorCode.BUCKET_STOCK_ALIGN);
        }
        //库存不足
        if (result == SeckillConstants.LUA_BUCKET_STOCK_NOT_ENOUGH){
            throw new SeckillException(ErrorCode.STOCK_LT_ZERO);
        }
    }

    /**
     * 获取库存校对key
     */
    private String getStockBucketAlignKey(Long goodsId){
        return SeckillConstants.getKey(SeckillConstants.GOODS_STOCK_BUCKETS_ALIGN_KEY, String.valueOf(goodsId));
    }

    /**
     * 获取库存编排时加锁的Key
     */
    private String getStockBucketSuspendKey(Long goodsId){
        return SeckillConstants.getKey(SeckillConstants.GOODS_STOCK_BUCKETS_SUSPEND_KEY, String.valueOf(goodsId));
    }

    /**
     * 获取库存分桶数据Key
     */
    private String getStockBucketKey(Long goodsId, Integer serialNo){
       return SeckillConstants.getKey(SeckillConstants.getKey(SeckillConstants.GOODS_BUCKET_AVAILABLE_STOCKS_KEY, String.valueOf(goodsId)),String.valueOf(serialNo));
    }

    /**
     * 获取分桶编号
     */
    private Integer getBucketSerialNo(Long userId, Integer bucketsQuantity){
        if (userId == null || bucketsQuantity == null || bucketsQuantity < 0){
            return null;
        }
        //只设置了一个分桶
        if (bucketsQuantity == 1){
            return 0;
        }
        return Math.abs(userId.hashCode()) % bucketsQuantity;
    }
    /**
     * 获取库存分桶数量
     */
    private Integer getBucketsQuantity(Long goodsId){
        Integer bucketQuantity = localBucketsQuantityCache.getIfPresent(goodsId);
        if (bucketQuantity != null){
            return bucketQuantity;
        }
        //从分布式缓存中获取分桶数量
        String bucketKey = SeckillConstants.getKey(SeckillConstants.GOODS_BUCKETS_QUANTITY_KEY, String.valueOf(goodsId));
        bucketQuantity = distributedCacheService.getObject(bucketKey, Integer.class);
        if (bucketQuantity != null){
            localBucketsQuantityCache.put(goodsId, bucketQuantity);
        }
        return bucketQuantity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrderInTransaction(TxMessage txMessage) {
        try{
            Boolean submitTransaction = distributedCacheService.hasKey(SeckillConstants.getKey(SeckillConstants.ORDER_TX_KEY, String.valueOf(txMessage.getTxNo())));
            if (BooleanUtil.isTrue(submitTransaction)){
                logger.info("saveOrderInTransaction|已经执行过本地事务|{}", txMessage.getTxNo());
                return;
            }
            //构建订单
            SeckillOrder seckillOrder = this.buildSeckillOrder(txMessage);
            //保存订单
            seckillOrderDomainService.saveSeckillOrder(seckillOrder);
            //保存事务日志
            distributedCacheService.put(SeckillConstants.getKey(SeckillConstants.ORDER_TX_KEY, String.valueOf(txMessage.getTxNo())), txMessage.getTxNo(), SeckillConstants.TX_LOG_EXPIRE_DAY, TimeUnit.DAYS);
        }catch (Exception e){
            logger.error("saveOrderInTransaction|异常|{}", e);
            distributedCacheService.delete(SeckillConstants.getKey(SeckillConstants.ORDER_TX_KEY, String.valueOf(txMessage.getTxNo())));
            this.rollbackCacheStack(txMessage);
            throw e;
        }
    }


    /**
     * 回滚缓存库存
     */
    private void rollbackCacheStack(TxMessage txMessage) {
        //扣减过缓存库存
        if (BooleanUtil.isFalse(txMessage.getException())){
            String luaKey = SeckillConstants.getKey(SeckillConstants.ORDER_TX_KEY, String.valueOf(txMessage.getTxNo())).concat(SeckillConstants.LUA_SUFFIX);
            Long result = distributedCacheService.checkExecute(luaKey, SeckillConstants.TX_LOG_EXPIRE_SECONDS);
            //已经执行过恢复缓存库存的方法
            if (NumberUtil.equals(result, SeckillConstants.CHECK_RECOVER_STOCK_HAS_EXECUTE)){
                logger.info("handlerCacheStock|已经执行过恢复缓存库存的方法|{}", JSONObject.toJSONString(txMessage));
                return;
            }
            //回滚分桶库存
            //获取分桶数量
            Integer bucketsQuantity = this.getBucketsQuantity(txMessage.getGoodsId());
            //计算用户下单的分桶
            Integer bucketSerialNo = this.getBucketSerialNo(txMessage.getUserId(), bucketsQuantity);
            //获取库存分桶数据key
            String stockBucketKey = this.getStockBucketKey(txMessage.getGoodsId(), bucketSerialNo);
            //获取库存编排时加锁的Key
            String stockBucketSuspendKey = this.getStockBucketSuspendKey(txMessage.getGoodsId());
            //获取库存校对key
            String stockBucketAlignKey = this.getStockBucketAlignKey(txMessage.getGoodsId());
            //封装执行Lua脚本的Key
            List<String> keys = Arrays.asList(stockBucketKey, stockBucketSuspendKey, stockBucketAlignKey);
            Long incrementResult = distributedCacheService.incrementBucketStock(keys, txMessage.getQuantity());
            if (incrementResult == null || incrementResult != SeckillConstants.LUA_BUCKET_STOCK_EXECUTE_SUCCESS){
                logger.error("rollbackCacheStack|执行回滚分桶库存失败|{}", txMessage);
            }
        }
    }
}
