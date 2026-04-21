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
package io.binghe.seckill.application.order.place;

import io.binghe.seckill.application.command.SeckillOrderCommand;
import io.binghe.seckill.domain.code.HttpCode;
import io.binghe.seckill.domain.exception.SeckillException;
import io.binghe.seckill.domain.model.dto.SeckillGoodsDTO;
import io.binghe.seckill.domain.model.entity.SeckillOrder;
import io.binghe.seckill.domain.model.enums.SeckillGoodsStatus;
import io.binghe.seckill.domain.model.enums.SeckillOrderStatus;
import io.binghe.seckill.infrastructure.utils.beans.BeanUtil;
import io.binghe.seckill.infrastructure.utils.id.SnowFlakeFactory;

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
     * 检测商品信息
     */
    default void checkSeckillGoods(SeckillOrderCommand seckillOrderCommand, SeckillGoodsDTO seckillGoods){
        //商品不存在
        if (seckillGoods == null){
            throw new SeckillException(HttpCode.GOODS_NOT_EXISTS);
        }
        //商品未上线
        if (seckillGoods.getStatus() == SeckillGoodsStatus.PUBLISHED.getCode()){
            throw new SeckillException(HttpCode.GOODS_PUBLISH);
        }
        //商品已下架
        if (seckillGoods.getStatus() == SeckillGoodsStatus.OFFLINE.getCode()){
            throw new SeckillException(HttpCode.GOODS_OFFLINE);
        }
        //触发限购
        if (seckillGoods.getLimitNum() < seckillOrderCommand.getQuantity()){
            throw new SeckillException(HttpCode.BEYOND_LIMIT_NUM);
        }
        // 库存不足
        if (seckillGoods.getAvailableStock() == null || seckillGoods.getAvailableStock() <= 0 || seckillOrderCommand.getQuantity() > seckillGoods.getAvailableStock()){
            throw new SeckillException(HttpCode.STOCK_LT_ZERO);
        }
    }
}
