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
import io.binghe.seckill.common.model.message.ErrorMessage;
import io.binghe.seckill.order.domain.model.entity.SeckillOrder;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 订单
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public interface SeckillOrderService {

//    /**
//     * 保存订单
//     */
//    Long saveSeckillOrder(Long userId, SeckillOrderCommand seckillOrderCommand);

    /**
     * 根据用户id获取订单列表
     */
    List<SeckillOrder> getSeckillOrderByUserId(Long userId);

    /**
     * 根据用户id获取订单列表
     */
    List<SeckillOrder> getSeckillOrderByGoodsId(Long goodsId);

    /**
     * 删除订单
     */
    void deleteOrder(ErrorMessage errorMessage);

    /**
     * 根据任务id获取订单号
     */
    SeckillOrderSubmitDTO getSeckillOrderSubmitDTOByTaskId(String taskId, Long userId, Long goodsId);
}
