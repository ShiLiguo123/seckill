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
package io.binghe.seckill.stock.application.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.alibaba.fastjson.JSON;
import io.binghe.seckill.common.cache.distribute.DistributedCacheService;
import io.binghe.seckill.common.cache.model.SeckillBusinessCache;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.common.lock.DistributedLock;
import io.binghe.seckill.common.lock.factoty.DistributedLockFactory;
import io.binghe.seckill.common.model.dto.stock.SeckillStockDTO;
import io.binghe.seckill.common.model.message.ErrorMessage;
import io.binghe.seckill.common.model.message.TxMessage;
import io.binghe.seckill.mq.MessageSenderService;
import io.binghe.seckill.stock.application.cache.SeckillStockBucketCacheService;
import io.binghe.seckill.stock.application.model.command.SeckillStockBucketWrapperCommand;
import io.binghe.seckill.stock.application.model.dto.SeckillStockBucketDTO;
import io.binghe.seckill.stock.application.service.SeckillStockBucketArrangementService;
import io.binghe.seckill.stock.application.service.SeckillStockBucketService;
import io.binghe.seckill.stock.domain.model.dto.SeckillStockBucketDeduction;
import io.binghe.seckill.stock.domain.service.SeckillStockBucketDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 分桶库存服务
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
public class SeckillStockBucketServiceImpl implements SeckillStockBucketService {
    private static final Logger logger = LoggerFactory.getLogger(SeckillStockBucketServiceImpl.class);

    @Autowired
    private DistributedLockFactory distributedLockFactory;
    @Autowired
    private SeckillStockBucketArrangementService seckillStockBucketArrangementService;
    @Autowired
    private SeckillStockBucketCacheService seckillStockBucketCacheService;
    @Autowired
    private SeckillStockBucketDomainService seckillStockBucketDomainService;
    @Autowired
    private DistributedCacheService distributedCacheService;
    @Autowired
    private MessageSenderService messageSenderService;

    @Override
    public void arrangeStockBuckets(Long userId, SeckillStockBucketWrapperCommand stockBucketWrapperCommand) {
        if (userId == null || stockBucketWrapperCommand == null) {
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        stockBucketWrapperCommand.setUserId(userId);
        if (stockBucketWrapperCommand.isEmpty()){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        logger.info("arrangeBuckets|编排库存分桶|{}", JSON.toJSON(stockBucketWrapperCommand));
        String lockKey = SeckillConstants.getKey(SeckillConstants.getKey(SeckillConstants.GOODS_BUCKET_ARRANGEMENT_KEY, String.valueOf(stockBucketWrapperCommand.getUserId())), String.valueOf(stockBucketWrapperCommand.getGoodsId()));
        DistributedLock lock = distributedLockFactory.getDistributedLock(lockKey);
        try{
            boolean isLock = lock.tryLock();
            if (!isLock){
                throw new SeckillException(ErrorCode.FREQUENTLY_ERROR);
            }
            //获取到锁，编排库存
            seckillStockBucketArrangementService.arrangeStockBuckets(stockBucketWrapperCommand.getGoodsId(),
                    stockBucketWrapperCommand.getStockBucketCommand().getTotalStock(),
                    stockBucketWrapperCommand.getStockBucketCommand().getBucketsQuantity(),
                    stockBucketWrapperCommand.getStockBucketCommand().getArrangementMode());
            logger.info("arrangeStockBuckets|库存编排完成|{}", stockBucketWrapperCommand.getGoodsId());
        }catch (SeckillException e){
            logger.error("arrangeStockBuckets|库存编排失败|{}", stockBucketWrapperCommand.getGoodsId(), e);
            throw e;
        }catch (Exception e){
            logger.error("arrangeStockBuckets|库存编排错误|{}", stockBucketWrapperCommand.getGoodsId(), e);
            throw new SeckillException(ErrorCode.BUCKET_CREATE_FAILED);
        }finally {
            lock.unlock();
        }
    }

    @Override
    public SeckillStockBucketDTO getTotalStockBuckets(Long goodsId, Long version) {
        if (goodsId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        logger.info("stockBucketsSummary|获取库存分桶数据|{}", goodsId);
        return seckillStockBucketArrangementService.getSeckillStockBucketDTO(goodsId, version);
    }

    @Override
    public SeckillBusinessCache<Integer> getAvailableStock(Long goodsId, Long version) {
        if (goodsId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillStockBucketCacheService.getAvailableStock(goodsId, version);
    }

    @Override
    public SeckillBusinessCache<SeckillStockDTO> getSeckillStock(Long goodsId, Long version) {
        if (goodsId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillStockBucketCacheService.getSeckillStock(goodsId, version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decreaseStock(TxMessage txMessage) {
        Boolean decrementStock = distributedCacheService.hasKey(SeckillConstants.getKey(SeckillConstants.STOCK_TX_KEY, String.valueOf(txMessage.getTxNo())));
        if (BooleanUtil.isTrue(decrementStock)){
            logger.info("updateAvailableStock|秒杀商品微服务已经扣减过库存|{}", txMessage.getTxNo());
            return true;
        }
        boolean isUpdate = false;
        try{
            isUpdate = seckillStockBucketDomainService.decreaseStock(new SeckillStockBucketDeduction(txMessage.getGoodsId(), txMessage.getQuantity(), txMessage.getUserId(), txMessage.getBucketSerialNo()));
            //成功扣减库存成功
            if (isUpdate){
                distributedCacheService.put(SeckillConstants.getKey(SeckillConstants.STOCK_TX_KEY, String.valueOf(txMessage.getTxNo())), txMessage.getTxNo(), SeckillConstants.TX_LOG_EXPIRE_DAY, TimeUnit.DAYS);
            }else{
                //发送失败消息给订单微服务
                messageSenderService.send(getErrorMessage(txMessage));
            }
        }catch (Exception e){
            isUpdate = false;
            logger.error("decreaseStock|抛出异常|{}|{}",txMessage.getTxNo(), e.getMessage());
            //发送失败消息给订单微服务
            messageSenderService.send(getErrorMessage(txMessage));
        }
        return isUpdate;
    }

    /**
     * 发送给订单微服务的错误消息
     */
    private ErrorMessage getErrorMessage(TxMessage txMessage){
        return new ErrorMessage(SeckillConstants.TOPIC_ERROR_MSG, txMessage.getTxNo(), txMessage.getGoodsId(), txMessage.getQuantity(), txMessage.getPlaceOrderType(), txMessage.getException(), txMessage.getBucketSerialNo(), txMessage.getUserId(), txMessage.getOrderTaskId());
    }
}
