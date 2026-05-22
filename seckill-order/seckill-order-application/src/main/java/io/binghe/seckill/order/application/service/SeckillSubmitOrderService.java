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
package io.binghe.seckill.order.application.service;

import io.binghe.seckill.common.model.dto.order.SeckillOrderSubmitDTO;
import io.binghe.seckill.order.application.model.command.SeckillOrderCommand;
import io.binghe.seckill.order.application.model.task.SeckillOrderTask;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 提交订单服务
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillSubmitOrderService {

    /**
     * 保存订单
     */
    SeckillOrderSubmitDTO saveSeckillOrder(Long userId, SeckillOrderCommand seckillOrderCommand);


    /**
     * 处理订单任务
     */
    default void handlePlaceOrderTask(SeckillOrderTask seckillOrderTask){

    }

    /**
     * 实现基础校验功能
     */
    default void checkSeckillOrder(Long userId, SeckillOrderCommand seckillOrderCommand){
    }
}
