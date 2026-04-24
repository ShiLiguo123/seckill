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

import io.binghe.seckill.common.cache.distribute.DistributedCacheService;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.common.model.dto.SeckillGoodsDTO;
import io.binghe.seckill.dubbo.interfaces.goods.SeckillGoodsDubboService;
import io.binghe.seckill.order.application.command.SeckillOrderCommand;
import io.binghe.seckill.order.application.place.SeckillPlaceOrderService;
import io.binghe.seckill.order.domain.model.entity.SeckillOrder;
import io.binghe.seckill.order.domain.service.SeckillOrderDomainService;
import org.apache.dubbo.config.annotation.DubboReference;
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

    @DubboReference(version = "1.0.0")
    private SeckillGoodsDubboService seckillGoodsDubboService;
    @Autowired
    private DistributedCacheService distributedCacheService;

    @Override
    public Long placeOrder(Long userId, SeckillOrderCommand seckillOrderCommand) {
        SeckillGoodsDTO seckillGoods = seckillGoodsDubboService.getSeckillGoods(seckillOrderCommand.getGoodsId(), seckillOrderCommand.getVersion());
        //检测商品
        this.checkSeckillGoods(seckillOrderCommand, seckillGoods);
        //获取商品限购信息
        Object limitObj = distributedCacheService.getObject(SeckillConstants.getKey(SeckillConstants.GOODS_ITEM_LIMIT_KEY_PREFIX, String.valueOf(seckillOrderCommand.getGoodsId())));
        //如果从Redis获取到的限购信息为null，则说明商品已经下线
        if (limitObj == null){
            throw new SeckillException(ErrorCode.GOODS_OFFLINE);
        }

        if (Integer.parseInt(String.valueOf(limitObj)) < seckillOrderCommand.getQuantity()){
            throw new SeckillException(ErrorCode.BEYOND_LIMIT_NUM);
        }
        String key = SeckillConstants.getKey(SeckillConstants.GOODS_ITEM_STOCK_KEY_PREFIX, String.valueOf(seckillOrderCommand.getGoodsId()));
        Long result = distributedCacheService.decrementByLua(key, seckillOrderCommand.getQuantity());
        this.checkResult(result);
        try{
            SeckillOrder seckillOrder = this.buildSeckillOrder(userId, seckillOrderCommand, seckillGoods);
            seckillOrderDomainService.saveSeckillOrder(seckillOrder);
            seckillGoodsDubboService.updateDbAvailableStock(seckillOrderCommand.getQuantity(), seckillOrderCommand.getGoodsId());
            return seckillOrder.getId();
        }catch (Exception e){
            //将内存中的库存增加回去
            distributedCacheService.incrementByLua(key, seckillOrderCommand.getQuantity());
            throw e;
        }
    }

    private void checkResult(Long result){
        if (result == SeckillConstants.LUA_RESULT_GOODS_STOCK_NOT_EXISTS) {
            throw new SeckillException(ErrorCode.STOCK_IS_NULL);
        }
        if (result == SeckillConstants.LUA_RESULT_GOODS_STOCK_PARAMS_LT_ZERO){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        if (result == SeckillConstants.LUA_RESULT_GOODS_STOCK_LT_ZERO){
            throw new SeckillException(ErrorCode.STOCK_LT_ZERO);
        }
    }
}
