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
package io.binghe.seckill.interfaces.controller;

import io.binghe.seckill.application.service.SeckillOrderService;
import io.binghe.seckill.domain.code.HttpCode;
import io.binghe.seckill.domain.dto.SeckillOrderDTO;
import io.binghe.seckill.domain.model.SeckillOrder;
import io.binghe.seckill.domain.response.ResponseMessage;
import io.binghe.seckill.domain.response.ResponseMessageBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description 订单
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */

@RestController
@RequestMapping(value = "/order")
public class SeckillOrderController {
    @Autowired
    private SeckillOrderService seckillOrderService;

    /**
     * 保存秒杀订单
     */
    @RequestMapping(value = "/saveSeckillOrder", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<SeckillOrder> saveSeckillOrder(SeckillOrderDTO seckillOrderDTO){
        SeckillOrder seckillOrder = seckillOrderService.saveSeckillOrder(seckillOrderDTO);
        return ResponseMessageBuilder.build(HttpCode.SUCCESS.getCode());
    }
    /**
     * 获取用户维度的订单列表
     */
    @RequestMapping(value = "/getSeckillOrderByUserId", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<List<SeckillOrder>> getSeckillOrderByUserId(Long userId){
        List<SeckillOrder> seckillOrderList = seckillOrderService.getSeckillOrderByUserId(userId);
        return ResponseMessageBuilder.build(HttpCode.SUCCESS.getCode(), seckillOrderList);
    }

    /**
     * 获取活动维度的订单列表
     */
    @RequestMapping(value = "/getSeckillOrderByActivityId", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseMessage<List<SeckillOrder>> getSeckillOrderByActivityId(Long activityId){
        List<SeckillOrder> seckillOrderList = seckillOrderService.getSeckillOrderByActivityId(activityId);
        return ResponseMessageBuilder.build(HttpCode.SUCCESS.getCode(), seckillOrderList);
    }
}
