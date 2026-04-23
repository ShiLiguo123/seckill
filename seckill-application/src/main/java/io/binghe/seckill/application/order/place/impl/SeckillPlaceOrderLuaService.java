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
package io.binghe.seckill.application.order.place.impl;

import io.binghe.seckill.application.command.SeckillOrderCommand;
import io.binghe.seckill.application.order.place.SeckillPlaceOrderService;
import io.binghe.seckill.application.service.SeckillGoodsService;
import io.binghe.seckill.domain.constants.SeckillConstants;
import io.binghe.seckill.domain.model.dto.SeckillGoodsDTO;
import io.binghe.seckill.domain.model.entity.SeckillOrder;
import io.binghe.seckill.domain.service.SeckillOrderDomainService;
import io.binghe.seckill.infrastructure.cache.distribute.DistributedCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 同步下单
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
@ConditionalOnProperty(name = "place.order.type", havingValue = "lua")
public class SeckillPlaceOrderLuaService implements SeckillPlaceOrderService {
    @Autowired
    private SeckillOrderDomainService seckillOrderDomainService;
    @Autowired
    private SeckillGoodsService seckillGoodsService;
    @Autowired
    private DistributedCacheService distributedCacheService;

    @Override
    public Long placeOrder(Long userId, SeckillOrderCommand seckillOrderCommand) {
        boolean decrementStock = false;
        SeckillGoodsDTO seckillGoods = seckillGoodsService.getSeckillGoods(seckillOrderCommand.getGoodsId(), seckillOrderCommand.getVersion());
        //检测商品
        this.checkSeckillGoods(seckillOrderCommand, seckillGoods);
        String key = SeckillConstants.getKey(SeckillConstants.GOODS_ITEM_STOCK_KEY_PREFIX, String.valueOf(seckillOrderCommand.getGoodsId()));
        Long result = distributedCacheService.decrementByLua(key, seckillOrderCommand.getQuantity());
        distributedCacheService.checkResult(result);
        decrementStock = true;
        try{
            SeckillOrder seckillOrder = this.buildSeckillOrder(userId, seckillOrderCommand, seckillGoods);
            seckillOrderDomainService.saveSeckillOrder(seckillOrder);
            seckillGoodsService.updateDbAvailableStock(seckillOrderCommand.getQuantity(), seckillOrderCommand.getGoodsId());
            return seckillOrder.getId();
        }catch (Exception e){
            //将内存中的库存增加回去
            if (decrementStock){
                distributedCacheService.incrementByLua(key, seckillOrderCommand.getQuantity());
            }
            throw e;
        }
    }
}
