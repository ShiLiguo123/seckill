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

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.NumberUtil;
import com.alibaba.fastjson.JSONObject;
import io.binghe.seckill.common.cache.distribute.DistributedCacheService;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.common.model.message.ErrorMessage;
import io.binghe.seckill.order.application.command.SeckillOrderCommand;
import io.binghe.seckill.order.application.place.SeckillPlaceOrderService;
import io.binghe.seckill.order.application.security.SecurityService;
import io.binghe.seckill.order.application.service.SeckillOrderService;
import io.binghe.seckill.order.domain.model.entity.SeckillOrder;
import io.binghe.seckill.order.domain.service.SeckillOrderDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 订单业务
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {
    private final Logger logger = LoggerFactory.getLogger(SeckillOrderServiceImpl.class);
    @Autowired
    private SeckillOrderDomainService seckillOrderDomainService;
    @Autowired
    private DistributedCacheService distributedCacheService;


    @Override
    public List<SeckillOrder> getSeckillOrderByUserId(Long userId) {
        return seckillOrderDomainService.getSeckillOrderByUserId(userId);
    }

    @Override
    public List<SeckillOrder> getSeckillOrderByActivityId(Long activityId) {
        return seckillOrderDomainService.getSeckillOrderByActivityId(activityId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(ErrorMessage errorMessage) {
        //成功提交过事务，才能清理订单，增加缓存库存
        Boolean submitTransaction = distributedCacheService.hasKey(SeckillConstants.getKey(SeckillConstants.ORDER_TX_KEY, String.valueOf(errorMessage.getTxNo())));
        if (BooleanUtil.isFalse(submitTransaction)){
            logger.info("deleteOrder|订单微服务未执行本地事务|{}",errorMessage.getTxNo());
            return;
        }
        seckillOrderDomainService.deleteOrder(errorMessage.getTxNo());
        this.handlerCacheStock(errorMessage);
    }

    /**
     * 处理缓存库存
     */
    private void handlerCacheStock(ErrorMessage errorMessage) {
        //订单微服务之前未抛出异常，说明已经扣减了缓存中的库存，此时需要将缓存中的库存增加回来
        if (BooleanUtil.isFalse(errorMessage.getException())){
            String luaKey = SeckillConstants.getKey(SeckillConstants.ORDER_TX_KEY, String.valueOf(errorMessage.getTxNo())).concat(SeckillConstants.LUA_SUFFIX);
            Long result = distributedCacheService.checkRecoverStockByLua(luaKey, SeckillConstants.TX_LOG_EXPIRE_SECONDS);
            //已经执行过恢复缓存库存的方法
            if (NumberUtil.equals(result, SeckillConstants.CHECK_RECOVER_STOCK_HAS_EXECUTE)){
                logger.info("handlerCacheStock|已经执行过恢复缓存库存的方法|{}", JSONObject.toJSONString(errorMessage));
                return;
            }
            //只有分布式锁方式和Lua脚本方法才会扣减缓存中的库存
            String key = SeckillConstants.getKey(SeckillConstants.GOODS_ITEM_STOCK_KEY_PREFIX, String.valueOf(errorMessage.getGoodsId()));
            //分布式锁方式
            logger.info("handlerCacheStock|回滚缓存库存|{}", JSONObject.toJSONString(errorMessage));
            if (SeckillConstants.PLACE_ORDER_TYPE_LOCK.equalsIgnoreCase(errorMessage.getPlaceOrderType())){
                distributedCacheService.increment(key, errorMessage.getQuantity());
            }else if (SeckillConstants.PLACE_ORDER_TYPE_LUA.equalsIgnoreCase(errorMessage.getPlaceOrderType())){  // Lua方式
                distributedCacheService.incrementByLua(key, errorMessage.getQuantity());
            }
        }
    }
}
