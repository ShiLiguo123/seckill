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
package io.binghe.seckill.order.application.place;

import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.common.model.dto.goods.SeckillGoodsDTO;
import io.binghe.seckill.common.model.enums.SeckillOrderStatus;
import io.binghe.seckill.common.model.message.TxMessage;
import io.binghe.seckill.common.utils.beans.BeanUtil;
import io.binghe.seckill.common.utils.id.SnowFlakeFactory;
import io.binghe.seckill.order.application.model.command.SeckillOrderCommand;
import io.binghe.seckill.order.domain.model.entity.SeckillOrder;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 下单接口
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillPlaceOrderService {

    /**
     * 下单操作
     */
    Long placeOrder(Long userId, SeckillOrderCommand seckillOrderCommand);

    /**
     * 本地事务执行保存订单操作
     */
    void saveOrderInTransaction(TxMessage txMessage);

    /**
     * 构建订单
     */
    default SeckillOrder buildSeckillOrder(Long userId, SeckillOrderCommand seckillOrderCommand, SeckillGoodsDTO seckillGoods){
        SeckillOrder seckillOrder = new SeckillOrder();
        BeanUtil.copyProperties(seckillOrderCommand, seckillOrder);
        seckillOrder.setId(SnowFlakeFactory.getSnowFlakeFromCache().nextId());
        seckillOrder.setGoodsName(seckillGoods.getGoodsName());
        seckillOrder.setUserId(userId);
        seckillOrder.setActivityPrice(seckillGoods.getActivityPrice());
        BigDecimal orderPrice = seckillGoods.getActivityPrice().multiply(BigDecimal.valueOf(seckillOrder.getQuantity()));
        seckillOrder.setOrderPrice(orderPrice);
        seckillOrder.setStatus(SeckillOrderStatus.CREATED.getCode());
        seckillOrder.setCreateTime(new Date());
        return seckillOrder;
    }

    /**
     * 构建订单
     */
    default SeckillOrder buildSeckillOrder(TxMessage txMessage){
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setId(txMessage.getTxNo());
        seckillOrder.setUserId(txMessage.getUserId());
        seckillOrder.setGoodsId(txMessage.getGoodsId());
        seckillOrder.setGoodsName(txMessage.getGoodsName());
        seckillOrder.setActivityPrice(txMessage.getActivityPrice());
        seckillOrder.setQuantity(txMessage.getQuantity());
        BigDecimal orderPrice = txMessage.getActivityPrice().multiply(BigDecimal.valueOf(seckillOrder.getQuantity()));
        seckillOrder.setOrderPrice(orderPrice);
        seckillOrder.setActivityId(txMessage.getActivityId());
        seckillOrder.setStatus(SeckillOrderStatus.CREATED.getCode());
        seckillOrder.setCreateTime(new Date());
        return seckillOrder;
    }

    /**
     * 检测商品信息
     */
    default void checkSeckillGoods(SeckillOrderCommand seckillOrderCommand, SeckillGoodsDTO seckillGoods){
        //商品不存在
        if (seckillGoods == null){
            throw new SeckillException(ErrorCode.GOODS_NOT_EXISTS);
        }
        //已经超出活动时间范围
        if (!seckillGoods.isInSeckilling()){
            throw new SeckillException(ErrorCode.BEYOND_TIME);
        }
        //商品未上线
        if (!seckillGoods.isOnline()){
            throw new SeckillException(ErrorCode.GOODS_PUBLISH);
        }
        //商品已下架
        if (seckillGoods.isOffline()){
            throw new SeckillException(ErrorCode.GOODS_OFFLINE);
        }
        //触发限购
        if (seckillGoods.getLimitNum() < seckillOrderCommand.getQuantity()){
            throw new SeckillException(ErrorCode.BEYOND_LIMIT_NUM);
        }
        // 库存不足
        if (seckillGoods.getAvailableStock() == null || seckillGoods.getAvailableStock() <= 0 || seckillOrderCommand.getQuantity() > seckillGoods.getAvailableStock()){
            throw new SeckillException(ErrorCode.STOCK_LT_ZERO);
        }
    }

    /**
     * 事务消息
     */
    default TxMessage getTxMessage(String destination, Long txNo, Long userId, String placeOrderType, Boolean exception,
                                   SeckillOrderCommand seckillOrderCommand, SeckillGoodsDTO seckillGoods, Integer bucketSerialNo, String orderTaskId){
        //构建事务消息
        return new TxMessage(destination, txNo, seckillOrderCommand.getGoodsId(), seckillOrderCommand.getQuantity(),
                seckillOrderCommand.getActivityId(), seckillOrderCommand.getVersion(), userId, seckillGoods.getGoodsName(),
                seckillGoods.getActivityPrice(), placeOrderType, exception, bucketSerialNo, orderTaskId);
    }

}
