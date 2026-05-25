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
package io.binghe.seckill.order.infrastructure.repository;

import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.order.domain.model.entity.SeckillOrder;
import io.binghe.seckill.order.domain.repository.SeckillOrderRepository;
import io.binghe.seckill.order.infrastructure.mapper.SeckillGoodsOrderMapper;
import io.binghe.seckill.order.infrastructure.mapper.SeckillUserOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 订单
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Component
public class SeckillOrderRepositoryImpl implements SeckillOrderRepository {
    @Autowired
    private SeckillUserOrderMapper seckillUserOrderMapper;
    @Autowired
    private SeckillGoodsOrderMapper seckillGoodsOrderMapper;

    @Override
    public boolean saveSeckillOrder(SeckillOrder seckillOrder) {
        if (seckillOrder == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        int userResult = seckillUserOrderMapper.saveSeckillOrder(seckillOrder);
        int goodsResult = seckillGoodsOrderMapper.saveSeckillOrder(seckillOrder);
        return userResult == 1 && goodsResult == 1;
    }

    @Override
    public List<SeckillOrder> getSeckillOrderByUserId(Long userId) {
        if (userId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillUserOrderMapper.getSeckillOrderByUserId(userId);
    }

    @Override
    public List<SeckillOrder> getSeckillOrderByGoodsId(Long goodsId) {
        if (goodsId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        return seckillGoodsOrderMapper.getSeckillOrderByGoodsId(goodsId);
    }

    @Override
    public void deleteOrderShardingUserId(Long orderId, Long userId) {
        if (orderId == null || userId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        seckillUserOrderMapper.deleteOrder(userId, orderId);
    }

    @Override
    public void deleteOrderShardingGoodsId(Long orderId, Long goodsId) {
        if (goodsId == null || orderId == null){
            throw new SeckillException(ErrorCode.PARAMS_INVALID);
        }
        seckillGoodsOrderMapper.deleteOrder(goodsId, orderId);
    }


}
