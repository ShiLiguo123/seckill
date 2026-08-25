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
package io.shiliguo.seckill.order.application.place.impl;

import cn.hutool.core.util.BooleanUtil;
import com.alibaba.fastjson.JSONObject;
import io.shiliguo.seckill.common.cache.distribute.DistributedCacheService;
import io.shiliguo.seckill.common.constants.SeckillConstants;
import io.shiliguo.seckill.common.exception.ErrorCode;
import io.shiliguo.seckill.common.exception.SeckillException;
import io.shiliguo.seckill.common.model.dto.goods.SeckillGoodsDTO;
import io.shiliguo.seckill.common.utils.id.SnowFlakeFactory;
import io.shiliguo.seckill.dubbo.interfaces.goods.SeckillGoodsDubboService;
import io.shiliguo.seckill.mq.MessageSenderService;
import io.shiliguo.seckill.common.model.message.TxMessage;
import io.shiliguo.seckill.order.application.model.command.SeckillOrderCommand;
import io.shiliguo.seckill.order.application.place.SeckillPlaceOrderService;
import io.shiliguo.seckill.order.domain.model.entity.SeckillOrder;
import io.shiliguo.seckill.order.domain.service.SeckillOrderDomainService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * @version 1.0.0
 * @description 分布式锁下单
 */
@Service
@ConditionalOnProperty(name = "place.order.type", havingValue = "db")
public class SeckillPlaceOrderDbService implements SeckillPlaceOrderService {
    private final Logger logger = LoggerFactory.getLogger(SeckillPlaceOrderDbService.class);
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
        long txNo = SnowFlakeFactory.getSnowFlakeFromCache().nextId();
        boolean exception = false;
        try{
            //获取商品库存
            Integer availableStock = seckillGoodsDubboService.getAvailableStockById(seckillOrderCommand.getGoodsId());
            //库存不足
            if (availableStock == null || availableStock < seckillOrderCommand.getQuantity()){
                throw new SeckillException(ErrorCode.STOCK_LT_ZERO);
            }
        }catch (Exception e){
            exception = true;
            logger.error("SeckillPlaceOrderDbService|下单异常|参数:{}|异常信息:{}", JSONObject.toJSONString(seckillOrderCommand), e.getMessage());
        }
        //事务消息
        //发送事务消息
        messageSenderService.sendMessageInTransaction(this.getTxMessage(SeckillConstants.TOPIC_TX_MSG, txNo, userId, SeckillConstants.PLACE_ORDER_TYPE_DB, exception, seckillOrderCommand, seckillGoods, 0, seckillOrderCommand.getOrderTaskId()), null);
        return txNo;
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
            logger.error("saveOrderInTransaction|异常|{}", e.getMessage());
            distributedCacheService.delete(SeckillConstants.getKey(SeckillConstants.ORDER_TX_KEY, String.valueOf(txMessage.getTxNo())));
            throw e;
        }
    }

}
