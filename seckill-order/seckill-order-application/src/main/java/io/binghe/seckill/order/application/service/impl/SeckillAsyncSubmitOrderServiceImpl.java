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

import io.binghe.seckill.common.cache.distribute.DistributedCacheService;
import io.binghe.seckill.common.constants.SeckillConstants;
import io.binghe.seckill.common.exception.ErrorCode;
import io.binghe.seckill.common.exception.SeckillException;
import io.binghe.seckill.common.model.dto.order.SeckillOrderSubmitDTO;
import io.binghe.seckill.order.application.model.command.SeckillOrderCommand;
import io.binghe.seckill.order.application.model.task.SeckillOrderTask;
import io.binghe.seckill.order.application.service.OrderTaskGenerateService;
import io.binghe.seckill.order.application.service.PlaceOrderTaskService;
import io.binghe.seckill.order.application.service.SeckillSubmitOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 异步提交订单
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
@Service
@ConditionalOnProperty(name = "submit.order.type", havingValue = "async")
public class SeckillAsyncSubmitOrderServiceImpl extends SeckillBaseSubmitOrderServiceImpl implements SeckillSubmitOrderService {
    @Autowired
    private OrderTaskGenerateService orderTaskGenerateService;
    @Autowired
    private PlaceOrderTaskService placeOrderTaskService;
    @Autowired
    private DistributedCacheService distributedCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillOrderSubmitDTO saveSeckillOrder(Long userId, SeckillOrderCommand seckillOrderCommand) {
        //进行基本的检查
        this.checkSeckillOrder(userId, seckillOrderCommand);
        //生成订单任务id
        String orderTaskId = orderTaskGenerateService.generatePlaceOrderTaskId(userId, seckillOrderCommand.getGoodsId());
        //将taskId存入seckillOrderCommand
        seckillOrderCommand.setOrderTaskId(orderTaskId);
        //构造下单任务
        SeckillOrderTask seckillOrderTask = new SeckillOrderTask(SeckillConstants.TOPIC_ORDER_MSG, orderTaskId, userId, seckillOrderCommand);
        //提交订单
        boolean isSubmit = placeOrderTaskService.submitOrderTask(seckillOrderTask);
        //提交失败
        if (!isSubmit){
            throw new SeckillException(ErrorCode.ORDER_FAILED);
        }
        return new SeckillOrderSubmitDTO(orderTaskId, seckillOrderCommand.getGoodsId(), SeckillConstants.TYPE_TASK);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePlaceOrderTask(SeckillOrderTask seckillOrderTask) {
        Long orderId = seckillPlaceOrderService.placeOrder(seckillOrderTask.getUserId(), seckillOrderTask.getSeckillOrderCommand());
        if (orderId != null){
            String key = SeckillConstants.getKey(SeckillConstants.ORDER_TASK_ORDER_ID_KEY, seckillOrderTask.getOrderTaskId());
            distributedCacheService.put(key, orderId, SeckillConstants.ORDER_TASK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
    }
}
